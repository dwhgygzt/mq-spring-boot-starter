package com.guzt.starter.mq.service.impl.amqp;

import com.guzt.starter.mq.exception.TopicMqException;
import com.guzt.starter.mq.pojo.LocalTransactionStatus;
import com.guzt.starter.mq.pojo.PublishType;
import com.guzt.starter.mq.pojo.XaTopicMessage;
import com.guzt.starter.mq.properties.amqp.publisher.RabbitMqPubProperties;
import com.guzt.starter.mq.service.XaTopicPublishChecker;
import com.guzt.starter.mq.service.XaTopicPublisher;
import com.guzt.starter.mq.service.XaTopicPublisherExecuteStrategy;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeoutException;


/**
 * 开源RabbitMq推送消息到MQ服务端
 * 发布（pub）模式
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
@SuppressWarnings("unused")
public class AmqpXaRabbitMqPublisher implements XaTopicPublisher {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    static SecureRandom secureRandom = new SecureRandom();

    RabbitMqPubProperties rabbitMqPubProperties;

    XaTopicPublishChecker xaTopicPublishChecker;

    Channel channel;

    Channel xaChannel;

    Connection connection;

    String beanName;

    String exchangeName;

    String groupId;

    boolean isStarted;


    String timeOutExchangeName;
    String xaCheckExchangeName;
    String timeOutQueueName;
    String xaCheckQueueName;

    /**
     * 重试队列标识符前缀
     */
    private static final String XA_CHECK_LETTER_FIX = "xa-check-letter-";

    /**
     * 死信队列 交换机标识符.
     * <p>
     * 消息变成死信有以下几种情况
     * 消息被拒绝(basic.reject / basic.nack)，并且requeue = false
     * 消息TTL过期
     * 队列达到最大长度
     */
    private static final String X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";

    public AmqpXaRabbitMqPublisher(Connection connection,
                                   RabbitMqPubProperties rabbitMqPubProperties,
                                   XaTopicPublishChecker xaTopicPublishChecker) {
        this.connection = connection;
        this.rabbitMqPubProperties = rabbitMqPubProperties;
        this.exchangeName = rabbitMqPubProperties.getExchangeName();
        this.beanName = rabbitMqPubProperties.getBeanName();
        this.groupId = rabbitMqPubProperties.getGroupId();
        this.xaTopicPublishChecker = xaTopicPublishChecker;

        this.timeOutExchangeName = XA_CHECK_LETTER_FIX + "timeout-" + exchangeName;
        this.xaCheckExchangeName = XA_CHECK_LETTER_FIX + exchangeName;
        // timeOutQueueName 的死信队列为 xaCheckQueueName
        // 业务消息先进入timeOutQueueName等待超时后进入xaCheckQueueName
        // 监听xaCheckQueueName 进行判断超时的消息是否应该发给消费者
        this.timeOutQueueName = XA_CHECK_LETTER_FIX + "timeout-QUE-" + exchangeName;
        this.xaCheckQueueName = XA_CHECK_LETTER_FIX + "QUE-" + exchangeName;

    }

    public AmqpXaRabbitMqPublisher() {

    }

    private boolean judgeSendToConsumer(XaTopicMessage topicMessage) throws IOException, InterruptedException {
        boolean canBasicAck = true;
        int retryCnt = topicMessage.getCurrentRetyPubishCount();
        int maxRetryCnt = rabbitMqPubProperties.getCheckImmunityMaxCount();

        // 1. 优先从发送成功标志的缓存中判断，如果存在则表示事务消息发送成功
        if (xaTopicPublishChecker.isCommitExists(topicMessage)) {
            xaTopicPublishChecker.deleteCommitCache(topicMessage);
            judgeSendToConsumerLog(topicMessage, "发送成功");
        } else if (xaTopicPublishChecker.isRollbackExists(topicMessage)) {
            xaTopicPublishChecker.deleteRollbackCache(topicMessage);
            judgeSendToConsumerLog(topicMessage, "取消发送");
        } else if (retryCnt < maxRetryCnt) {
            // 2. 如果不存在则需要判断本地事务情况
            LocalTransactionStatus status = XaTopicPublisherExecuteStrategy.checkLocalTransaction(topicMessage);
            if (LocalTransactionStatus.UNKNOW.equals(status)) {
                judgeSendToConsumerLog(topicMessage, "UNKNOW，" + (retryCnt + 1 < maxRetryCnt ? "下次继续回查" : "终止回查"));
                if (retryCnt + 1 < maxRetryCnt) {
                    AMQP.BasicProperties props = converFirstPubTopicMessage(topicMessage, PublishType.PUBLISH_FAIL_RETRY.getValue(),
                            rabbitMqPubProperties.getCheckImmunityTimeInSeconds() * 1000);
                    xaChannel.basicPublish(timeOutExchangeName, topicMessage.getTags(), props, topicMessage.getMessageBody());
                    if (!xaChannel.waitForConfirms()) {
                        canBasicAck = false;
                    }
                }
            } else if (LocalTransactionStatus.COMMIT.equals(status)) {
                judgeSendToConsumerLog(topicMessage, "COMMIT，但不确定是否发成功，因此补发一次消息给消费者");
                AMQP.BasicProperties props = converFirstPubTopicMessage(topicMessage, PublishType.PUBLISH_FAIL_RETRY.getValue(), null);
                channel.basicPublish(exchangeName, topicMessage.getTags(), props, topicMessage.getMessageBody());
                if (xaChannel.waitForConfirms()) {
                    xaTopicPublishChecker.cacheCommit(topicMessage);
                } else {
                    canBasicAck = false;
                }
            } else {
                judgeSendToConsumerLog(topicMessage, "调用查询本地事务状态接口-取消发送");
            }
        } else {
            judgeSendToConsumerLog(topicMessage, "次数超限");
        }

        return canBasicAck;
    }

    private void judgeSendToConsumerLog(XaTopicMessage topicMessage, String status) {
        int retryCnt = topicMessage.getCurrentRetyPubishCount();
        int maxRetryCnt = rabbitMqPubProperties.getCheckImmunityMaxCount();
        logger.debug("事务消息发送结果回查第 {} 次，最多 {} 次，结果为【{}】，topic={}, businessKey={}, tags={}",
                retryCnt + 1,
                maxRetryCnt,
                status,
                topicMessage.getTopicName(),
                topicMessage.getBussinessKey(),
                topicMessage.getTags());
    }

    protected void xaCheckListener() throws IOException {
        /*
         * 参数1：队列名称
         * 参数2：是否自动确认，设置为true为表示消息接收到自动向mq回复接收到了，mq接收到回复会删除消息，设置为false则需要手动确认
         * 参数3：a client-generated consumer tag to establish context
         * 参数4：消息接收到后回调
         */
        xaChannel.basicConsume(xaCheckQueueName, false, beanName + "_xa_check",
                new DefaultConsumer(this.xaChannel) {
                    /*
                     * @param consumerTag 消息者标签，在channel.basicConsume时候可以指定
                     * @param envelope 消息包的内容，可从中获取消息id，消息routingkey，交换机，消息和重传 标志(收到消息失败后是否需要重新发送)
                     * @param properties 属性信息
                     * @param body 消息
                     * @throws IOException ignore
                     */
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope,
                                               AMQP.BasicProperties properties, byte[] body) throws IOException {
                        boolean canBasicAck = false;
                        Properties userProperties = new Properties();
                        if (properties.getHeaders() != null) {
                            properties.getHeaders().forEach((k, v) -> userProperties.put(k, v.toString()));
                        }
                        String localTransactionExecuterId = userProperties.getProperty(XaTopicMessage.LOCALTRANSACTION_EXECUTERID_KEY);
                        if (StringUtils.hasText(localTransactionExecuterId)) {
                            XaTopicMessage topicMessage = new XaTopicMessage();
                            topicMessage.setMessageId(properties.getMessageId());
                            topicMessage.setBussinessKey(userProperties.getProperty("ext-bussinessKey"));
                            topicMessage.setCurrentRetyPubishCount(Integer.parseInt(userProperties.getProperty("ext-currentRetyPubishCount")));
                            topicMessage.setCurrentRetyConsumCount(Integer.parseInt(userProperties.getProperty("ext-currentRetyConsumCount")));
                            topicMessage.setLocalTransactionExecuterId(localTransactionExecuterId);
                            topicMessage.setUserProperties(userProperties);
                            topicMessage.setMessageBody(body);
                            topicMessage.setTags(envelope.getRoutingKey());
                            topicMessage.setTopicName(properties.getAppId());

                            // 检查事务消息是否可以发给消息者
                            try {
                                canBasicAck = judgeSendToConsumer(topicMessage);
                            } catch (InterruptedException e) {
                                logger.error("检查事务消息是否可以发给消息者业务代码异常", e);
                            }
                        } else {
                            logger.error("队列 {} 接受到非事务消息，消息topicName为{}，获取属性参数localTransactionExecuterId为空，" +
                                    "请检查发送事务消息的代码是否设置了参数localTransactionExecuterId", xaCheckQueueName, properties.getAppId());
                            canBasicAck = true;
                        }
                        // deliveryTag: 用来标识信道中投递的消息。RabbitMQ 推送消息给Consumer时，会附带一个deliveryTag，
                        // 以便Consumer可以在消息确认时告诉RabbitMQ到底是哪条消息被确认了
                        // multiple=true: 消息id<=deliveryTag的消息，都会被确认
                        // myltiple=false: 消息id=deliveryTag的消息，都会被确认
                        if (canBasicAck) {
                            xaChannel.basicAck(envelope.getDeliveryTag(), false);
                        } else {
                            // 消息重新投递
                            xaChannel.basicNack(envelope.getDeliveryTag(), false, true);
                            logger.debug("XA事务回查确认失败 canBasicAck= false，消息重新投递确认 topicName={}, messageId={}, bussinessKey={}, routingKey={}, groupId={}",
                                    properties.getAppId(), properties.getMessageId(), userProperties.getProperty("ext-bussinessKey"),
                                    envelope.getRoutingKey(), xaCheckQueueName);
                        }
                    }
                });

        logger.debug("【MQ】AmqpRabbitMqSubscriber[{}] , exchangeName[{}]   started", beanName + "_xa_check", xaCheckQueueName);
    }

    public static AMQP.BasicProperties converFirstPubTopicMessage(XaTopicMessage topicMessage, String publishType, Integer expiration) {
        // routingKey 设置
        String routingKey = topicMessage.getTags();
        if (!StringUtils.hasText(routingKey)) {
            routingKey = "*";
            topicMessage.setTags(routingKey);
        }

        if (PublishType.FIRST.getValue().equals(publishType)) {
            // 消息唯一id channel.getNextPublishSeqNo() 消息消费发送者重启之后就从1开始了
            if (!StringUtils.hasText(topicMessage.getMessageId())) {
                topicMessage.setMessageId(UUID.randomUUID().toString().replaceAll("-", "")
                        + secureRandom.nextInt(1000000));
            }
        } else if (PublishType.PUBLISH_FAIL_RETRY.getValue().equals(publishType)) {
            topicMessage.setCurrentRetyPubishCount(topicMessage.getCurrentRetyPubishCount() + 1);
        } else if (PublishType.CONSUM_FAIL_RETRY.getValue().equals(publishType)) {
            topicMessage.setCurrentRetyConsumCount(topicMessage.getCurrentRetyConsumCount() + 1);
        }

        // headers 设置
        Map<String, Object> headers = new HashMap<>(8);
        if (!ObjectUtils.isEmpty(topicMessage.getUserProperties())) {
            topicMessage.getUserProperties().forEach((k, v) -> headers.put(k.toString(), v.toString()));
        }
        headers.put(XaTopicMessage.LOCALTRANSACTION_EXECUTERID_KEY, topicMessage.getLocalTransactionExecuterId());
        headers.put("ext-currentRetyConsumCount", "" + topicMessage.getCurrentRetyConsumCount());
        headers.put("ext-currentRetyPubishCount", "" + topicMessage.getCurrentRetyPubishCount());
        headers.put("ext-bussinessKey", topicMessage.getBussinessKey());

        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder()
                .appId(topicMessage.getTopicName())
                .messageId(topicMessage.getMessageId()).headers(headers);
        if (expiration != null && expiration > 0) {
            builder.expiration(String.valueOf(expiration));
        }
        return builder.build();
    }

    @Override
    public void publishInTransaction(XaTopicMessage topicMessage, Object businessParam) {
        try {
            // 1. 首先向延迟回查队列里面发送消息
            AMQP.BasicProperties props = converFirstPubTopicMessage(topicMessage, PublishType.FIRST.getValue(),
                    rabbitMqPubProperties.getCheckImmunityTimeInSeconds() * 1000);
            xaChannel.basicPublish(timeOutExchangeName, topicMessage.getTags(), props, topicMessage.getMessageBody());
            if (!xaChannel.waitForConfirms()) {
                TopicMqException topicMqException = new TopicMqException("事务检查消息发送后，确认结果：不成功, topic=" + topicMessage.getTopicName()
                        + ", tags=" + topicMessage.getTags());
                topicMqException.setMessageId(topicMessage.getMessageId());
                topicMqException.setTag(topicMessage.getTags());
                topicMqException.setBusinessKey(topicMessage.getBussinessKey());
                throw topicMqException;
            }
            // 2. 执行本地数据库事务
            LocalTransactionStatus status = XaTopicPublisherExecuteStrategy.executeLocalTransaction(topicMessage, businessParam);
            // 3. 发送消息
            if (LocalTransactionStatus.COMMIT.equals(status)) {
                props = converFirstPubTopicMessage(topicMessage, PublishType.FIRST.getValue(), null);
                channel.basicPublish(exchangeName, topicMessage.getTags(), props, topicMessage.getMessageBody());
                if (channel.waitForConfirms()) {
                    xaTopicPublishChecker.cacheCommit(topicMessage);
                }
            } else if (LocalTransactionStatus.ROLLBACK.equals(status)) {
                xaTopicPublishChecker.cacheRollback(topicMessage);
            }
        } catch (Exception e) {
            logger.error("xa Rabbitmq publishInTransaction txRollback 发送异常 topicName={}, businessKey={}",
                    topicMessage.getTopicName(), topicMessage.getBussinessKey(), e);
            TopicMqException topicMqException = new TopicMqException(e);
            topicMqException.setMessageId(topicMessage.getMessageId());
            topicMqException.setTopicName(topicMessage.getTopicName());
            topicMqException.setBusinessKey(topicMessage.getBussinessKey());
            topicMqException.setTag(topicMessage.getTags());

            throw topicMqException;
        }
    }

    @Override
    public boolean isStarted() {
        return isStarted;
    }

    @Override
    public boolean isClosed() {
        return !isStarted;
    }

    @Override
    public void start() {
        if (isStarted) {
            return;
        }

        logger.debug("【MQ】AmqpXaRabbitMqPublisher[{}] , exchangeName[{}] start...", beanName, exchangeName);
        try {
            // 不开启发送方确认机制，采用事务方式 channel.txSelect
            // 事务机制和publisher confirm机制是两者互斥的，不能共存。
            // 如果企图将已开启事务模式的信道再设置为publisher confirm模式，RabbitMQ会报错。
            // 或者企图将已开启publisher confirm模式的信道再设置为事务模式，RabbitMQ也会报错
            this.channel = connection.createChannel();
            this.xaChannel = connection.createChannel();
            // 开启发送方确认机制
            this.xaChannel.confirmSelect();
            this.channel.confirmSelect();
            // 声明交换机
            channel.exchangeDeclare(exchangeName, BuiltinExchangeType.TOPIC);
            // 声明定时检查死信交换机
            xaChannel.exchangeDeclare(timeOutExchangeName, BuiltinExchangeType.TOPIC);
            // 声明定时检查交换机
            xaChannel.exchangeDeclare(xaCheckExchangeName, BuiltinExchangeType.TOPIC);

            /*
             * 声明定时检查死信交换机的队列
             */
            Map<String, Object> arguments1 = new HashMap<>(2);
            // 指定该死信队列为 xaCheckQueueName
            arguments1.put(X_DEAD_LETTER_EXCHANGE, xaCheckExchangeName);
            xaChannel.queueDeclare(timeOutQueueName, true, false, false, arguments1);
            xaChannel.queueBind(timeOutQueueName, timeOutExchangeName, "#");

            /*
             * 声明定时检查交换机的队列
             */
            xaChannel.queueDeclare(xaCheckQueueName, true, false, false, null);
            xaChannel.queueBind(xaCheckQueueName, xaCheckExchangeName, "#");

            xaCheckListener();
            isStarted = true;
        } catch (IOException e) {
            logger.error("【MQ】AmqpXaRabbitMqPublisher[{}] , exchangeName[{}] start Exception", beanName, exchangeName, e);
            throw new TopicMqException("AmqpXaRabbitMqPublisher start fail");
        }
    }

    @Override
    public void close() {
        logger.debug("【MQ】AmqpXaRabbitMqPublisher[{}] , exchangeName[{}] close...", beanName, exchangeName);
        try {
            channel.close();
            xaChannel.close();
            connection.close();
        } catch (IOException | TimeoutException e) {
            logger.error("【MQ】AmqpXaRabbitMqPublisher[{}] , exchangeName[{}] close Exception", beanName, exchangeName, e);
        }
    }

    public String getBeanName() {
        return beanName;
    }

    public Channel getChannel() {
        return channel;
    }

    public Connection getConnection() {
        return connection;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public String getGroupId() {
        return groupId;
    }

    public Channel getXaChannel() {
        return xaChannel;
    }

    public String getTimeOutExchangeName() {
        return timeOutExchangeName;
    }

    public String getXaCheckExchangeName() {
        return xaCheckExchangeName;
    }

    public String getTimeOutQueueName() {
        return timeOutQueueName;
    }

    public String getXaCheckQueueName() {
        return xaCheckQueueName;
    }

    public RabbitMqPubProperties getRabbitMqPubProperties() {
        return rabbitMqPubProperties;
    }

}
