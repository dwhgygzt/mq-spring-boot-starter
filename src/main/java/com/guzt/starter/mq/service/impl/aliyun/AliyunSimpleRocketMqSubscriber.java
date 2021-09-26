package com.guzt.starter.mq.service.impl.aliyun;

import com.aliyun.openservices.ons.api.Action;
import com.aliyun.openservices.ons.api.Consumer;
import com.guzt.starter.mq.pojo.MessageStatus;
import com.guzt.starter.mq.pojo.TopicMessage;
import com.guzt.starter.mq.properties.aliyun.subscriber.RocketMqSubProperties;
import com.guzt.starter.mq.service.RetryConsumFailHandler;
import com.guzt.starter.mq.service.TopicListener;
import com.guzt.starter.mq.service.TopicSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 阿里云订阅消息
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
@SuppressWarnings("unused")
public class AliyunSimpleRocketMqSubscriber implements TopicSubscriber {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 阿里云 rocketMq消费服务
     */
    Consumer consumer;

    String beanName;

    RocketMqSubProperties rocketMqSubProperties;

    RetryConsumFailHandler retryConsumFailHandler;

    public AliyunSimpleRocketMqSubscriber(Consumer consumer,
                                          RocketMqSubProperties rocketMqSubProperties) {
        this.consumer = consumer;
        this.beanName = rocketMqSubProperties.getBeanName();
        this.rocketMqSubProperties = rocketMqSubProperties;
    }

    public AliyunSimpleRocketMqSubscriber() {
    }

    @Override
    public void subscribe(String topic, String tagExpression, TopicListener listener) {
        consumer.subscribe(topic, tagExpression, (message, context) -> {
            TopicMessage topicMessage = new TopicMessage();
            topicMessage.setMessageId(message.getMsgID());
            topicMessage.setUserProperties(message.getUserProperties());
            topicMessage.setBussinessKey(message.getKey());
            topicMessage.setMessageBody(message.getBody());
            topicMessage.setTags(message.getTag());
            topicMessage.setTopicName(message.getTopic());
            topicMessage.setCurrentRetyConsumCount(message.getReconsumeTimes());

            MessageStatus messageStatus = listener.subscribe(topicMessage);
            if (messageStatus.equals(MessageStatus.CommitMessage)) {
                return Action.CommitMessage;
            } else {
                return failureFrequency(topicMessage);
            }
        });
    }

    protected Action failureFrequency(TopicMessage topicMessage) {
        String messageUniqueId = topicMessage.getTopicName() + "_" + topicMessage.getTags() + "_" + topicMessage.getMessageId();
        int retryCnt = topicMessage.getCurrentRetyConsumCount();
        if (retryCnt >= rocketMqSubProperties.getMaxRetryCount()) {
            logger.info("消息超过最大重新投递次数{} ，直接消费完成！ topicName={}, messageId={}, bussinessKey={}, routingKey={}, groupId={}",
                    rocketMqSubProperties.getMaxRetryCount(), topicMessage.getTopicName(),
                    topicMessage.getMessageId(), topicMessage.getBussinessKey(), topicMessage.getTags(), rocketMqSubProperties.getGroupId());
            retryConsumFailHandler.handle(topicMessage);
            return Action.CommitMessage;
        } else {
            logger.info("消息重新投递.... topicName={}, messageId={}, bussinessKey={}, routingKey={}, groupId={}",
                    topicMessage.getTopicName(), topicMessage.getMessageId(), topicMessage.getBussinessKey(),
                    topicMessage.getTags(), rocketMqSubProperties.getGroupId());
            return Action.ReconsumeLater;
        }
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

    @Override
    public void setRetryConsumFailHandler(RetryConsumFailHandler retryConsumFailHandler) {
        this.retryConsumFailHandler = retryConsumFailHandler;
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
