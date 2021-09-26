package com.guzt.starter.mq.service.impl.amqp;

import com.guzt.starter.mq.exception.TopicMqException;
import com.guzt.starter.mq.pojo.PublishType;
import com.guzt.starter.mq.pojo.TopicMessage;
import com.guzt.starter.mq.pojo.TopicMessageSendResult;
import com.guzt.starter.mq.properties.amqp.publisher.RabbitMqPubProperties;
import com.guzt.starter.mq.service.TopicPublisher;
import com.guzt.starter.mq.service.TopicSendCallback;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;


/**
 * 开源RabbitMq推送消息到MQ服务端
 * 发布（pub）模式
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
@SuppressWarnings("unused")
public class AmqpRabbitMqPublisher implements TopicPublisher {

    private Logger logger = LoggerFactory.getLogger(this.getClass());


    static SecureRandom secureRandom = new SecureRandom();

    /**
     * 创建一个线程池。
     */
    protected ExecutorService executor;

    RabbitMqPubProperties rabbitMqPubProperties;

    Channel channel;

    Connection connection;

    String beanName;

    String exchangeName;

    String groupId;

    boolean isStarted;

    public static void publishTopicMessage(
            Channel channel, String exchangeName, TopicMessage topicMessage, String publishType, Integer expiration) throws IOException {
        // 消息唯一id channel.getNextPublishSeqNo() 消息消费发送者重启之后就从1开始了
        String messageId = topicMessage.getMessageId();
        if (PublishType.FIRST.getValue().equals(publishType)) {
            messageId = UUID.randomUUID().toString().replaceAll("-", "") + secureRandom.nextInt(1000000);
        } else if (PublishType.PUBLISH_FAIL_RETRY.getValue().equals(publishType)) {
            topicMessage.setCurrentRetyPubishCount(topicMessage.getCurrentRetyPubishCount() + 1);
        } else if (PublishType.CONSUM_FAIL_RETRY.getValue().equals(publishType)) {
            topicMessage.setCurrentRetyConsumCount(topicMessage.getCurrentRetyConsumCount() + 1);
        }

        // routingKey 设置
        String routingKey = topicMessage.getTags();
        if (StringUtils.isEmpty(routingKey)) {
            routingKey = "*";
            topicMessage.setTags(routingKey);
        }

        // headers 设置
        Map<String, Object> headers = new HashMap<>(8);
        if (topicMessage.getUserProperties() != null) {
            topicMessage.getUserProperties().forEach((k, v) -> headers.put(k.toString(), v.toString()));
        }
        headers.put("ext-currentRetyConsumCount", "" + topicMessage.getCurrentRetyConsumCount());
        headers.put("ext-currentRetyPubishCount", "" + topicMessage.getCurrentRetyPubishCount());
        headers.put("ext-bussinessKey", topicMessage.getBussinessKey());

        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder()
                .appId(topicMessage.getTopicName())
                .messageId(messageId).headers(headers);
        if (expiration != null && expiration > 0) {
            builder.expiration(String.valueOf(expiration));
        }
        AMQP.BasicProperties props = builder.build();
        topicMessage.setMessageId(messageId);

        // 发送
        channel.basicPublish(exchangeName, routingKey, props, topicMessage.getMessageBody());
    }

    public AmqpRabbitMqPublisher(Connection connection, RabbitMqPubProperties rabbitMqPubProperties) {
        this.connection = connection;
        this.rabbitMqPubProperties = rabbitMqPubProperties;
        this.exchangeName = rabbitMqPubProperties.getExchangeName();
        this.beanName = rabbitMqPubProperties.getBeanName();
        this.groupId = rabbitMqPubProperties.getGroupId();

        this.executor = new ThreadPoolExecutor(
                rabbitMqPubProperties.getAsyncPubCorePoolSize(),
                rabbitMqPubProperties.getAsyncMaximumPoolSize(),
                rabbitMqPubProperties.getAsyncKeepAliveSeconds(), TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), (ThreadFactory) Thread::new);
    }

    public AmqpRabbitMqPublisher() {

    }

    @Override
    public TopicMessageSendResult publish(TopicMessage topicMessage) {
        try {
            publishTopicMessage(channel, exchangeName, topicMessage, PublishType.FIRST.getValue(), null);
            if (!channel.waitForConfirms()) {
                TopicMqException topicMqException = new TopicMqException("消息发送后，确认结果：不成功, topic=" + topicMessage.getTopicName()
                        + ", tags=" + topicMessage.getTags());
                topicMqException.setMessageId(topicMessage.getMessageId());
                topicMqException.setTag(topicMessage.getTags());
                topicMqException.setBusinessKey(topicMessage.getBussinessKey());
                throw topicMqException;
            }
        } catch (Exception e) {
            TopicMqException topicMqException = new TopicMqException(e);
            topicMqException.setMessageId(topicMessage.getMessageId());
            topicMqException.setTopicName(topicMessage.getTopicName());
            topicMqException.setBusinessKey(topicMessage.getBussinessKey());
            topicMqException.setTag(topicMessage.getTags());

            throw topicMqException;
        }

        TopicMessageSendResult topicMessageSendResult = new TopicMessageSendResult();
        topicMessageSendResult.setMessageId(topicMessage.getMessageId());
        topicMessageSendResult.setBusinessKey(topicMessage.getBussinessKey());
        topicMessageSendResult.setTags(topicMessage.getTags());
        topicMessageSendResult.setTopicName(topicMessage.getTopicName());
        return topicMessageSendResult;
    }

    @Override
    public void publishAsync(TopicMessage topicMessage, TopicSendCallback topicSendCallback) {
        // 异步confirm方法的编程实现是最复杂的,这里 暂时使用简单业务中异步方式进行
        // 如果采用rabbitmq 监听方式实现异步发送，则需要进行 channel.addConfirmListener 整个信道的全局设置
        // 这样的话，就不应该一会 异步 一会 同步发送了，因此暂时不是要rabbitmq的异步发送方式
        CompletableFuture.runAsync(() -> {
            try {
                topicSendCallback.onSuccess(publish(topicMessage));
            } catch (TopicMqException e) {
                topicSendCallback.onFail(e);
            }
        }, executor);
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

        logger.info("【MQ】AmqpRabbitMqPublisher[{}] , group[{}] start...", beanName, groupId);
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
            // 开启发送方确认机制
            channel.confirmSelect();
            isStarted = true;
        } catch (IOException e) {
            logger.error("【MQ】AmqpRabbitMqPublisher[{}] , group[{}] start Exception", beanName, groupId, e);
            throw new TopicMqException("AmqpRabbitMqPublisher start fail");
        }
    }

    @Override
    public void close() {
        logger.info("【MQ】AmqpRabbitMqPublisher[{}] , group[{}] close...", beanName, groupId);
        try {
            channel.close();
            connection.close();
        } catch (IOException | TimeoutException e) {
            logger.error("【MQ】AmqpRabbitMqPublisher[{}] , group[{}] close Exception", beanName, groupId, e);
        }
    }

    public String getBeanName() {
        return beanName;
    }

    public Executor getExecutor() {
        return executor;
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


    public RabbitMqPubProperties getRabbitMqPubProperties() {
        return rabbitMqPubProperties;
    }

}
