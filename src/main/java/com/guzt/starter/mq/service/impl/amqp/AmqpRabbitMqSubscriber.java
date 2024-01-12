package com.guzt.starter.mq.service.impl.amqp;

import com.guzt.starter.mq.exception.TopicMqException;
import com.guzt.starter.mq.pojo.MessageStatus;
import com.guzt.starter.mq.pojo.PublishType;
import com.guzt.starter.mq.pojo.TopicMessage;
import com.guzt.starter.mq.properties.amqp.subscriber.RabbitMqSubProperties;
import com.guzt.starter.mq.service.RetryConsumFailHandler;
import com.guzt.starter.mq.service.TopicListener;
import com.guzt.starter.mq.service.TopicSubscriber;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * 开源RabbitMq推送消息到MQ服务端
 * 订阅（Sub）模式
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
@SuppressWarnings("unused")
public class AmqpRabbitMqSubscriber implements TopicSubscriber {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 死信队列 交换机标识符.
     * <p>
     * 消息变成死信有以下几种情况
     * 消息被拒绝(basic.reject / basic.nack)，并且requeue = false
     * 消息TTL过期
     * 队列达到最大长度
     */
    private static final String X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";

    /**
     * 重试队列标识符前缀
     */
    private static final String RETRY_LETTER_FIX = "retry-letter-";

    /**
     * groupId 是队列的名称，tag为队列可接受那些消息的 routdingKey
     */
    private final Map<String, List<String>> queueTagBindMap = new HashMap<>(4);

    Channel channel;

    Connection connection;

    String exchangeName;

    String beanName;

    boolean isStarted;

    String groupId;

    RabbitMqSubProperties rabbitMqSubProperties;

    RetryConsumFailHandler retryConsumFailHandler;

    public AmqpRabbitMqSubscriber(Connection connection, RabbitMqSubProperties rabbitMqSubProperties) {
        this.rabbitMqSubProperties = rabbitMqSubProperties;
        this.connection = connection;
        this.exchangeName = rabbitMqSubProperties.getExchangeName();
        this.beanName = rabbitMqSubProperties.getBeanName();
        this.groupId = rabbitMqSubProperties.getGroupId();
    }

    public AmqpRabbitMqSubscriber() {

    }

    private synchronized void putBindInfoToMap(String queue, String tag) {
        List<String> tags;
        if (queueTagBindMap.get(queue) != null) {
            tags = queueTagBindMap.get(queue);
        } else {
            tags = new ArrayList<>(4);
        }
        tags.add(tag);

        queueTagBindMap.put(queue, tags);
    }

    @Override
    public void subscribe(String topic, String tagExpression, TopicListener listener) throws IOException {
        /*
         * 声明队列
         * 参数1：队列名称
         * 参数2：是否定义持久化队列
         * 参数3：是否独占本次连接
         * 参数4：是否在不使用的时候自动删除队列
         * 参数5：队列其它参数
         */
        Map<String, Object> arguments1 = new HashMap<>(2);
        arguments1.put(X_DEAD_LETTER_EXCHANGE, RETRY_LETTER_FIX + exchangeName);
        channel.queueDeclare(groupId, true, false, false, arguments1);

        // 申明重试队列
        Map<String, Object> arguments2 = new HashMap<>(4);
        arguments2.put(X_DEAD_LETTER_EXCHANGE, exchangeName);
        channel.queueDeclare(RETRY_LETTER_FIX + "QUE-" + exchangeName, true, false, false, arguments2);
        channel.queueBind(RETRY_LETTER_FIX + "QUE-" + exchangeName, RETRY_LETTER_FIX + exchangeName, "#");

        //--------------队列绑定交换机 begin ----------------------
        if (tagExpression == null) {
            tagExpression = "*";
        }
        String orSpitStr = "||";
        if (tagExpression.contains(orSpitStr)) {
            String[] tags = StringUtils.split(tagExpression, orSpitStr);
            if (tags == null) {
                tags = new String[]{tagExpression.trim()};
            }
            for (String tag : tags) {
                channel.queueBind(groupId, exchangeName, tag.trim());
                putBindInfoToMap(groupId, tag);
            }
        } else {
            channel.queueBind(groupId, exchangeName, tagExpression.trim());
            putBindInfoToMap(groupId, tagExpression.trim());
        }

        //--------------队列绑定交换机 end ----------------------
        /*
         *
         * 参数1：队列名称
         * 参数2：是否自动确认，设置为true为表示消息接收到自动向mq回复接收到了，mq接收到回复会删除消息，设置为false则需要手动确认
         * 参数3：a client-generated consumer tag to establish context
         * 参数4：消息接收到后回调
         */
        channel.basicConsume(groupId, false, beanName, new DefaultConsumer(this.channel) {

            /*
             * @param consumerTag 消息者标签，在channel.basicConsume时候可以指定
             * @param envelope 消息包的内容，可从中获取消息id，消息routingkey，交换机，消息和重传 标志(收到消息失败后是否需要重新发送)
             * @param properties 属性信息
             * @param body 消息
             * @throws IOException ignore
             */
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) {
                //路由的key
                String routingKey = envelope.getRoutingKey();
                //获取交换机信息
                String exchange = envelope.getExchange();
                //获取消息ID
                long deliveryTag = envelope.getDeliveryTag();
                if (logger.isDebugEnabled()) {
                    logger.debug("接收到消息 consumerTag={}, routingKey={}, exchange={}, deliveryTag={}, bodyLength={}",
                            consumerTag, routingKey, exchange, deliveryTag, body.length);
                }
                MessageStatus messageStatus;
                TopicMessage topicMessage = new TopicMessage();
                String msgTopic = properties.getAppId();
                if (topic.equals(msgTopic)) {
                    Properties userProperties = new Properties();
                    if (properties.getHeaders() != null) {
                        properties.getHeaders().forEach((k, v) -> userProperties.put(k, v.toString()));
                    }
                    topicMessage.setMessageId(properties.getMessageId());
                    topicMessage.setBussinessKey(userProperties.getProperty("ext-bussinessKey"));
                    topicMessage.setCurrentRetyPubishCount(Integer.parseInt(userProperties.getProperty("ext-currentRetyPubishCount")));
                    topicMessage.setCurrentRetyConsumCount(Integer.parseInt(userProperties.getProperty("ext-currentRetyConsumCount")));

                    topicMessage.setUserProperties(userProperties);
                    topicMessage.setMessageBody(body);
                    topicMessage.setTags(routingKey);
                    topicMessage.setTopicName(topic);
                    messageStatus = listener.subscribe(topicMessage);
                } else {
                    logger.warn("【MQ】AmqpRabbitMqSublisher[{}] 消息接受错误，接受到非 topicName={} 的消息, 该消息的topicName是{}, routingKey={}",
                            beanName, topic, msgTopic, routingKey);
                    messageStatus = MessageStatus.CommitMessage;
                }
                try {
                    if (messageStatus.equals(MessageStatus.ReconsumeLater)) {
                        failureFrequency(topicMessage);
                    }
                    // deliveryTag: 用来标识信道中投递的消息。RabbitMQ 推送消息给Consumer时，会附带一个deliveryTag，
                    // 以便Consumer可以在消息确认时告诉RabbitMQ到底是哪条消息被确认了
                    // multiple=true: 消息id<=deliveryTag的消息，都会被确认
                    // myltiple=false: 消息id=deliveryTag的消息，都会被确认
                    channel.basicAck(envelope.getDeliveryTag(), false);
                    logger.debug("消息最终被确认-消费完毕 topicName={}, messageId={}, bussinessKey={}, routingKey={}, groupId={}",
                            topic, topicMessage.getMessageId(), topicMessage.getBussinessKey(), routingKey, groupId);
                } catch (IOException e) {
                    logger.error("消息最终被确认-异常 topicName={}, messageId={}, bussinessKey={}, routingKey={}, groupId={}",
                            topic, topicMessage.getMessageId(), topicMessage.getBussinessKey(), routingKey, groupId, e);
                }
            }
        });
    }

    protected void failureFrequency(TopicMessage topicMessage) throws IOException {
        String messageUniqueId = topicMessage.getTopicName() + "_" + topicMessage.getTags() + "_" + topicMessage.getMessageId();
        int retryCnt = topicMessage.getCurrentRetyConsumCount();
        int maxRetryCnt = rabbitMqSubProperties.getMaxRetryCount();
        if (retryCnt < maxRetryCnt) {
            logger.debug("当前消费失败第{}次，消息设置最大重新投递次数{}，消息重新给重试队列等待重投.... topicName={}, messageId={}, bussinessKey={}, routingKey={}, groupId={}",
                    retryCnt + 1, maxRetryCnt, topicMessage.getTopicName(), topicMessage.getMessageId(),
                    topicMessage.getBussinessKey(), topicMessage.getTags(), groupId);
            AmqpRabbitMqPublisher.publishTopicMessage(
                    channel, RETRY_LETTER_FIX + exchangeName, topicMessage,
                    PublishType.CONSUM_FAIL_RETRY.getValue(), rabbitMqSubProperties.getRetryConsumIntervalSeconds() * 1000);
        } else {
            logger.debug("当前消费失败第{}次，消息超过最大重新投递次数{} ，直接消费完成！ topicName={}, messageId={}, bussinessKey={}, routingKey={}, groupId={}",
                    retryCnt + 1, maxRetryCnt, topicMessage.getTopicName(), topicMessage.getMessageId(),
                    topicMessage.getBussinessKey(), topicMessage.getTags(), groupId);
            retryConsumFailHandler.handle(topicMessage);
        }
    }

    @Override
    public void unsubscribe(String topicName) throws IOException {
        List<String> tags = queueTagBindMap.get(groupId);
        for (String routdingKey : tags) {
            channel.queueUnbind(groupId, exchangeName, routdingKey);
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

        logger.debug("【MQ】AmqpRabbitMqSubscriber[" + beanName + "] start...");
        try {
            this.channel = connection.createChannel();
            /*
             * 声明交换机
             * 参数1：交换机名称
             * 参数2：交换机类型，fanout、topic、direct、headers
             *  Fanout：广播，将消息交给所有绑定到交换机的队列
             *  Direct：定向，把消息交给符合指定routing key 的队列
             *  Topic：通配符，把消息交给符合routing pattern（路由模式） 的队列
             */
            channel.exchangeDeclare(exchangeName, BuiltinExchangeType.TOPIC);
            channel.basicQos(1, false);

            // 声明重试交换机
            channel.exchangeDeclare(RETRY_LETTER_FIX + exchangeName, BuiltinExchangeType.TOPIC);

            isStarted = true;
        } catch (IOException e) {
            logger.error("【MQ】AmqpRabbitMqSubscriber[" + beanName + "] start Exception", e);
            throw new TopicMqException("AmqpRabbitMqSubscriber start fail");
        }
    }

    @Override
    public void close() {
        logger.debug("【MQ】AmqpRabbitMqSublisher[" + beanName + "] close...");
        try {
            channel.close();
            connection.close();
        } catch (IOException | TimeoutException e) {
            logger.error("【MQ】AmqpRabbitMqSublisher[" + beanName + "] close Exception", e);
        }
    }

    @Override
    public void setRetryConsumFailHandler(RetryConsumFailHandler retryConsumFailHandler) {
        this.retryConsumFailHandler = retryConsumFailHandler;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    public void setStarted(boolean started) {
        isStarted = started;
    }

    public Map<String, List<String>> getQueueTagBindMap() {
        return queueTagBindMap;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }


    public RabbitMqSubProperties getRabbitMqSubProperties() {
        return rabbitMqSubProperties;
    }
}
