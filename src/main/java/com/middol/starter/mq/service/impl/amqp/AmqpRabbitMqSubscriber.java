package com.middol.starter.mq.service.impl.amqp;

import com.middol.starter.mq.exception.TopicMqException;
import com.middol.starter.mq.pojo.MessageStatus;
import com.middol.starter.mq.pojo.TopicMessage;
import com.middol.starter.mq.service.TopicListener;
import com.middol.starter.mq.service.TopicSubscriber;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * 开源RabbitMq推送消息到MQ服务端
 * 订阅（Sub）模式
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
 */
public class AmqpRabbitMqSubscriber implements TopicSubscriber {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * groupId 是队列的名称，tag为队列可接受那些消息的 routdingKey
     */
    private Map<String, List<String>> queueTagBindMap = new HashMap<>(4);

    Channel channel;

    Connection connection;

    String exchangeName;

    String beanName;

    boolean isStarted;

    String groupId;

    public AmqpRabbitMqSubscriber(Connection connection,
                                  String exchangeName,
                                  String beanName,
                                  String groupId) {
        this.connection = connection;
        this.exchangeName = exchangeName;
        this.beanName = beanName;
        this.groupId = groupId;
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
        channel.queueDeclare(groupId, true, false, false, null);
        //队列绑定交换机
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
        /*
         *
         * 参数1：队列名称消息监听
         * 参数2：是否自动确认，设置为true为表示消息接收到自动向mq回复接收到了，mq接收到回复会删除消息，设置为false则需要手动确认
         * 参数3：消息接收到后回调
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
                //获取消息信息
                String message = new String(body, StandardCharsets.UTF_8);

                if (logger.isDebugEnabled()) {
                    logger.debug("consumerTag={}, routingKey={}, exchange={}, deliveryTag={}, message={}",
                            consumerTag, routingKey, exchange, deliveryTag, message);
                }
                MessageStatus messageStatus = MessageStatus.ReconsumeLater;
                TopicMessage topicMessage = new TopicMessage();
                String msgTopic = properties.getAppId();
                if (topic.equals(msgTopic) || !StringUtils.isEmpty(msgTopic)) {
                    Properties userProperties = new Properties();
                    if (properties.getHeaders() != null) {
                        properties.getHeaders().forEach((k, v) -> userProperties.put(k, v.toString()));
                    }
                    topicMessage.setBussinessKey(properties.getMessageId());
                    topicMessage.setUserProperties(userProperties);
                    topicMessage.setMessageBody(body);
                    topicMessage.setTags(routingKey);
                    topicMessage.setTopicName(topic);
                    messageStatus = listener.subscribe(topicMessage);
                } else {
                    logger.warn("【MQ】AmqpRabbitMqSublisher[{}] 消息接受错误，接受到非 topicName={} 的消息, 该消息的topicName是{}, routingKey={}, message={}",
                            beanName, topic, msgTopic, routingKey, message);
                }

                try {
                    if (messageStatus.equals(MessageStatus.CommitMessage)) {
                        logger.info("消息消费完毕 topicName={}, bussinessKey={}, routingKey={}, groupId={}",
                                topic, topicMessage.getBussinessKey(), routingKey, groupId);
                        channel.basicAck(envelope.getDeliveryTag(), false);
                    } else {
                        logger.info("消息重新投递.... topicName={}, bussinessKey={}, routingKey={}, groupId={}",
                                topic, topicMessage.getBussinessKey(), routingKey, groupId);
                        channel.basicReject(envelope.getDeliveryTag(), true);
                    }
                } catch (IOException e) {
                    logger.error("消息确认异常 ", e);
                }
            }
        });
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

        logger.info("【MQ】AmqpRabbitMqSubscriber[" + beanName + "] start...");
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
            isStarted = true;
        } catch (IOException e) {
            logger.error("【MQ】AmqpRabbitMqSubscriber[" + beanName + "] start Exception", e);
            throw new TopicMqException("AmqpRabbitMqSubscriber start fail");
        }
    }

    @Override
    public void close() {
        logger.info("【MQ】AmqpRabbitMqSublisher[" + beanName + "] close...");
        try {
            channel.close();
            connection.close();
        } catch (IOException | TimeoutException e) {
            logger.error("【MQ】AmqpRabbitMqSublisher[" + beanName + "] close Exception", e);
        }
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
}
