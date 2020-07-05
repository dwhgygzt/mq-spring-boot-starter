package com.middol.starter.mq.service.impl.aliyun;

import com.aliyun.openservices.ons.api.Action;
import com.aliyun.openservices.ons.api.Consumer;
import com.middol.starter.mq.pojo.MessageStatus;
import com.middol.starter.mq.pojo.TopicMessage;
import com.middol.starter.mq.service.TopicListener;
import com.middol.starter.mq.service.TopicSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 阿里云订阅消息
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
 */
public class AliyunSimpleRocketMqSubscriber implements TopicSubscriber {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 阿里云 rocketMq消费服务
     */
    Consumer consumer;

    String beanName;

    public AliyunSimpleRocketMqSubscriber(Consumer consumer, String beanName) {
        this.consumer = consumer;
        this.beanName = beanName;
    }

    public AliyunSimpleRocketMqSubscriber() {
    }

    @Override
    public void subscribe(String topic, String tagExpression, TopicListener listener) {
        consumer.subscribe(topic, tagExpression, (message, context) -> {
            TopicMessage topicMessage = new TopicMessage();
            topicMessage.setUserProperties(message.getUserProperties());
            topicMessage.setBussinessKey(message.getKey());
            topicMessage.setMessageBody(message.getBody());
            topicMessage.setTags(message.getTag());
            topicMessage.setTopicName(message.getTopic());

            MessageStatus messageStatus = listener.subscribe(topicMessage);
            if (messageStatus.equals(MessageStatus.CommitMessage)) {
                return Action.CommitMessage;
            } else {
                return Action.ReconsumeLater;
            }
        });
    }

    @Override
    public void unsubscribe(String topicName) {
        consumer.unsubscribe(topicName);
    }

    @Override
    public boolean isStarted() {
        return consumer.isStarted();
    }

    @Override
    public boolean isClosed() {
        return consumer.isClosed();
    }

    @Override
    public void start() {
        logger.info("【MQ】AliyunSimpleRocketMqSubscriber[" + beanName + "] start...");
        consumer.start();
    }

    @Override
    public void close() {
        logger.info("【MQ】AliyunSimpleRocketMqSubscriber[" + beanName + "] close...");
        consumer.shutdown();
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }
}
