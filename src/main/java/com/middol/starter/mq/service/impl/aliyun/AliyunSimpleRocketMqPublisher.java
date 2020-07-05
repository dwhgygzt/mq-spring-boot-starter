package com.middol.starter.mq.service.impl.aliyun;

import com.aliyun.openservices.ons.api.*;
import com.middol.starter.mq.exception.TopicMqException;
import com.middol.starter.mq.pojo.TopicMessage;
import com.middol.starter.mq.pojo.TopicMessageSendResult;
import com.middol.starter.mq.service.TopicPublisher;
import com.middol.starter.mq.service.TopicSendCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 阿里云推送消息到MQ服务端
 * 发布（pub）模式
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
 */
public class AliyunSimpleRocketMqPublisher implements TopicPublisher {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    /**
     * 阿里云 rocketmq producer
     */
    Producer producer;

    String beanName;

    public AliyunSimpleRocketMqPublisher(Producer producer,String beanName) {
        this.producer = producer;
        this.beanName = beanName;
    }

    public AliyunSimpleRocketMqPublisher() {
    }

    @Override
    public TopicMessageSendResult publish(TopicMessage topicMessage) {
        Message message = new Message();
        message.setUserProperties(topicMessage.getUserProperties());
        message.setKey(topicMessage.getBussinessKey());
        message.setBody(topicMessage.getMessageBody());
        message.setTag(topicMessage.getTags());
        message.setTopic(topicMessage.getTopicName());
        SendResult sendResult = producer.send(message);
        TopicMessageSendResult topicMessageSendResult = new TopicMessageSendResult();
        topicMessageSendResult.setMessageId(sendResult.getMessageId());
        topicMessageSendResult.setTopicName(sendResult.getTopic());
        return topicMessageSendResult;
    }

    @Override
    public void publishAsync(TopicMessage topicMessage, TopicSendCallback topicSendCallback) {
        Message message = new Message();
        message.setUserProperties(topicMessage.getUserProperties());
        message.setKey(topicMessage.getBussinessKey());
        message.setBody(topicMessage.getMessageBody());
        message.setTag(topicMessage.getTags());
        message.setTopic(topicMessage.getTopicName());
        producer.sendAsync(message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                TopicMessageSendResult topicMessageSendResult = new TopicMessageSendResult();
                topicMessageSendResult.setTopicName(sendResult.getTopic());
                topicMessageSendResult.setMessageId(sendResult.getMessageId());
                topicSendCallback.onSuccess(topicMessageSendResult);
            }

            @Override
            public void onException(OnExceptionContext context) {
                TopicMqException topicMqException = new TopicMqException(context.getException());
                topicMqException.setTopicName(context.getTopic());
                topicMqException.setMessageId(context.getMessageId());
                topicSendCallback.onFail(topicMqException);
            }
        });
    }

    @Override
    public boolean isStarted() {
        return producer.isStarted();
    }

    @Override
    public boolean isClosed() {
        return producer.isClosed();
    }

    @Override
    public void start() {
        logger.info("【MQ】AliyunSimpleRocketMqPublisher["+beanName+"] start...");
        producer.start();
    }

    @Override
    public void close() {
        logger.info("【MQ】AliyunSimpleRocketMqPublisher[" + beanName + "] close...");
        producer.shutdown();
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public Producer getProducer() {
        return producer;
    }

    public void setProducer(Producer producer) {
        this.producer = producer;
    }

}
