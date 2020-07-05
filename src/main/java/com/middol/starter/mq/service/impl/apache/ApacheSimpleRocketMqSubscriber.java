package com.middol.starter.mq.service.impl.apache;

import com.middol.starter.mq.pojo.MessageStatus;
import com.middol.starter.mq.pojo.TopicMessage;
import com.middol.starter.mq.service.TopicListener;
import com.middol.starter.mq.service.TopicSubscriber;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;

/**
 * ApacheRocketMq 订阅消息
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
 */
public class ApacheSimpleRocketMqSubscriber implements TopicSubscriber {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 阿里云 rocketMq消费服务
     */
    DefaultMQPushConsumer consumer;

    String beanName;

    boolean isStarted;

    public ApacheSimpleRocketMqSubscriber(DefaultMQPushConsumer consumer, String beanName) {
        this.consumer = consumer;
        this.beanName = beanName;
    }

    public ApacheSimpleRocketMqSubscriber() {
    }

    @Override
    public void subscribe(String topic, String tagExpression, TopicListener listener) {
        try {
            consumer.subscribe(topic, tagExpression);
        } catch (MQClientException e) {
            logger.error("【MQ】ApacheSimpleRocketMqSubscriber[" + beanName + "] start error", e);
            return;
        }
        // 注册回调实现类来处理从broker拉取回来的消息
        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            // msgs.size() >= 1<br> DefaultMQPushConsumer.consumeMessageBatchMaxSize=1,you can modify here
            MessageExt messageExt = msgs.get(0);
            TopicMessage topicMessage = new TopicMessage();
            Map<String, String> userPropertiesMap = messageExt.getProperties();
            if (userPropertiesMap != null && !userPropertiesMap.isEmpty()) {
                Properties userProperties = new Properties();
                userPropertiesMap.forEach(userProperties::put);
                topicMessage.setUserProperties(userProperties);
            }
            topicMessage.setBussinessKey(messageExt.getKeys());
            topicMessage.setMessageBody(messageExt.getBody());
            topicMessage.setTags(messageExt.getTags());
            topicMessage.setTopicName(messageExt.getTopic());

            MessageStatus messageStatus = listener.subscribe(topicMessage);
            if (messageStatus.equals(MessageStatus.CommitMessage)) {
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            } else {
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        });
    }

    @Override
    public void unsubscribe(String topicName) {
        consumer.unsubscribe(topicName);
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
        logger.info("【MQ】ApacheSimpleRocketMqSubscriber[" + beanName + "] start...");
        try {
            consumer.start();
            isStarted = true;
        } catch (MQClientException e) {
            logger.error("【MQ】ApacheSimpleRocketMqSubscriber[" + beanName + "] start error", e);
        }
    }

    @Override
    public void close() {
        logger.info("【MQ】ApacheSimpleRocketMqSubscriber[" + beanName + "] close...");
        consumer.shutdown();
    }

    public DefaultMQPushConsumer getConsumer() {
        return consumer;
    }

    public void setConsumer(DefaultMQPushConsumer consumer) {
        this.consumer = consumer;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }
}
