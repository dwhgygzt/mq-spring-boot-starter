package com.middol.starter.mq.service.impl.amqp;

import com.middol.starter.mq.exception.TopicMqException;
import com.middol.starter.mq.pojo.TopicMessage;
import com.middol.starter.mq.pojo.TopicMessageSendResult;
import com.middol.starter.mq.service.TopicPublisher;
import com.middol.starter.mq.service.TopicSendCallback;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;


/**
 * 开源RabbitMq推送消息到MQ服务端
 * 发布（pub）模式
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
 */
public class AmqpRabbitMqPublisher implements TopicPublisher {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 创建一个线程池，用于处理要进行比对的数据库对象查询操作。
     */
    private final Executor executor = Executors.newFixedThreadPool(20, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    Channel channel;

    Connection connection;

    String beanName;

    String exchangeName;

    String groupId;

    boolean isStarted;

    public AmqpRabbitMqPublisher(Connection connection,
                                 String exchangeName,
                                 String beanName,
                                 String groupId) {
        this.connection = connection;
        this.exchangeName = exchangeName;
        this.beanName = beanName;
        this.groupId = groupId;
    }

    public AmqpRabbitMqPublisher() {

    }

    @Override
    public TopicMessageSendResult publish(TopicMessage topicMessage) {
        String routingKey = topicMessage.getTags();
        if (routingKey == null) {
            routingKey = "*";
        }
        Map<String, Object> headers = new HashMap<>(8);

        if (topicMessage.getUserProperties() != null) {
            topicMessage.getUserProperties().forEach((k, v) -> headers.put(k.toString(), v.toString()));
        }
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .appId(topicMessage.getTopicName())
                .messageId(topicMessage.getBussinessKey()).headers(headers).build();

        topicMessage.setMessageId(props.getMessageId());
        try {
            channel.basicPublish(exchangeName, routingKey, props, topicMessage.getMessageBody());
        } catch (IOException e) {
            throw new TopicMqException("AmqpRabbitMqPublisher bussinessKey =" + topicMessage.getBussinessKey() + " 发送异常 ", topicMessage.getTopicName(), e);
        }

        TopicMessageSendResult topicMessageSendResult = new TopicMessageSendResult();
        topicMessageSendResult.setMessageId(topicMessage.getBussinessKey());
        topicMessageSendResult.setTopicName(topicMessage.getTopicName());
        return topicMessageSendResult;
    }

    @Override
    public void publishAsync(TopicMessage topicMessage, TopicSendCallback topicSendCallback) {
        Future<TopicMessageSendResult> asyncFuture = CompletableFuture.supplyAsync(() -> {
            TopicMessageSendResult result = null;
            try {
                result = publish(topicMessage);
                topicSendCallback.onSuccess(result);
            } catch (TopicMqException e) {
                topicSendCallback.onFail(e);
            }
            return result;
        }, executor);

        try {
            asyncFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new TopicMqException("AmqpRabbitMqPublisher bussinessKey =" + topicMessage.getBussinessKey() + "异步发送异常 ", topicMessage.getTopicName(), e);
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

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public Executor getExecutor() {
        return executor;
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

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}
