package com.middol.starter.mq.service.impl.apache;

import com.middol.starter.mq.exception.TopicMqException;
import com.middol.starter.mq.pojo.TopicMessage;
import com.middol.starter.mq.pojo.TopicMessageSendResult;
import com.middol.starter.mq.service.TopicPublisher;
import com.middol.starter.mq.service.TopicSendCallback;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ApacheRocketMq推送消息到MQ服务端
 * 发布（pub）模式
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
 */
public class ApacheSimpleRocketMqPublisher implements TopicPublisher {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    /**
     * apache rocketmq producer
     */
    DefaultMQProducer producer;

    String beanName;

    boolean isStarted;

    public ApacheSimpleRocketMqPublisher(DefaultMQProducer producer, String beanName) {

        this.producer = producer;
        this.beanName = beanName;
    }

    public ApacheSimpleRocketMqPublisher() {
    }

    @Override
    public TopicMessageSendResult publish(TopicMessage topicMessage) {
        Message message = new Message();
        if (topicMessage.getUserProperties() != null) {
            topicMessage.getUserProperties().forEach((k, v) -> message.putUserProperty(k.toString(), v.toString()));
        }
        message.setKeys(topicMessage.getBussinessKey());
        message.setBody(topicMessage.getMessageBody());
        message.setTags(topicMessage.getTags());
        message.setTopic(topicMessage.getTopicName());
        SendResult sendResult;
        try {
            sendResult = producer.send(message);
        } catch (Exception e) {
            throw new TopicMqException("ApacheRocketMq bussinessKey =" + message.getKeys() + " 发送异常 ", message.getTopic(), e);
        }
        if (!sendResult.getSendStatus().equals(SendStatus.SEND_OK)) {
            throw new TopicMqException("ApacheRocketMq bussinessKey =" + message.getKeys() + "发送异常 " + sendResult.getSendStatus().toString(), message.getTopic());
        }
        TopicMessageSendResult topicMessageSendResult = new TopicMessageSendResult();
        topicMessageSendResult.setMessageId(sendResult.getMsgId());
        if (sendResult.getMessageQueue() != null) {
            topicMessageSendResult.setTopicName(sendResult.getMessageQueue().getTopic());
        }
        return topicMessageSendResult;
    }

    @Override
    public void publishAsync(TopicMessage topicMessage, TopicSendCallback topicSendCallback) {
        Message message = new Message();
        if (topicMessage.getUserProperties() != null) {
            topicMessage.getUserProperties().forEach((k, v) -> message.putUserProperty(k.toString(), v.toString()));
        }
        message.setKeys(topicMessage.getBussinessKey());
        message.setBody(topicMessage.getMessageBody());
        message.setTags(topicMessage.getTags());
        message.setTopic(topicMessage.getTopicName());
        try {
            producer.send(message, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    if (!sendResult.getSendStatus().equals(SendStatus.SEND_OK)) {
                        throw new TopicMqException("ApacheRocketMq businessKey =" + message.getKeys() + " 异步发送 onSuccess方法异常 " + sendResult.getSendStatus().toString(), message.getTopic());
                    }
                    TopicMessageSendResult topicMessageSendResult = new TopicMessageSendResult();
                    if (sendResult.getMessageQueue() != null) {
                        topicMessageSendResult.setTopicName(sendResult.getMessageQueue().getTopic());
                    }
                    topicMessageSendResult.setMessageId(sendResult.getMsgId());
                    topicSendCallback.onSuccess(topicMessageSendResult);
                }

                @Override
                public void onException(Throwable e) {
                    TopicMqException topicMqException = new TopicMqException(e);
                    topicSendCallback.onFail(topicMqException);
                }
            });
        } catch (Exception e) {
            throw new TopicMqException("ApacheRocketMq businessKey =" + message.getKeys() + " 异步发送异常 ", message.getTopic(), e);
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
        logger.info("【MQ】ApacheSimpleRocketMqPublisher[" + beanName + "] start...");
        try {
            producer.start();
            isStarted = true;
        } catch (MQClientException e) {
            logger.error("【MQ】ApacheSimpleRocketMqPublisher[" + beanName + "] start error", e);
        }
    }

    @Override
    public void close() {
        logger.info("【MQ】ApacheSimpleRocketMqPublisher[" + beanName + "] close...");
        producer.shutdown();
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public DefaultMQProducer getProducer() {
        return producer;
    }

    public void setProducer(DefaultMQProducer producer) {
        this.producer = producer;
    }

}
