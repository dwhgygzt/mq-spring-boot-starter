package com.middol.starter.mq.service.impl.apache;

import com.middol.starter.mq.exception.MqException;
import com.middol.starter.mq.exception.TopicMqException;
import com.middol.starter.mq.pojo.XaTopicMessage;
import com.middol.starter.mq.service.XaTopicPublisher;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * ApacheRocketMq推送消息到MQ服务端
 * 发布（pub）模式
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
 */
public class ApacheXaRocketMqPublisher implements XaTopicPublisher {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    /**
     * apache rocketmq producer
     */
    TransactionMQProducer producer;

    String beanName;

    boolean isStarted;

    public ApacheXaRocketMqPublisher(TransactionMQProducer producer, String beanName) {

        this.producer = producer;
        this.beanName = beanName;
    }

    public ApacheXaRocketMqPublisher() {
    }

    @Override
    public void publishInTransaction(XaTopicMessage topicMessage, Object businessParam) {
        if (!StringUtils.hasText(topicMessage.getLocalTransactionExecuterId())) {
            throw new MqException("XaTopicMessage对象请设置localTransactionExecuterId属性");
        }
        Message message = new Message();
        if (topicMessage.getUserProperties() != null) {
            topicMessage.getUserProperties().forEach((k, v) -> message.putUserProperty(k.toString(), v.toString()));
        }
        message.setKeys(topicMessage.getBussinessKey());
        message.setBody(topicMessage.getMessageBody());
        message.setTags(topicMessage.getTags());
        message.setTopic(topicMessage.getTopicName());
        message.putUserProperty(XaTopicMessage.LOCALTRANSACTION_EXECUTERID_KEY, topicMessage.getLocalTransactionExecuterId());
        SendResult sendResult;
        try {
            sendResult = producer.sendMessageInTransaction(message, businessParam);
        } catch (Exception e) {
            throw new TopicMqException("ApacheRocketMq 发送异常 ", message.getTopic(), e);
        }
        if (!sendResult.getSendStatus().equals(SendStatus.SEND_OK)) {
            throw new TopicMqException("ApacheRocketMq businessKey = " + message.getKeys() + " 发送异常 " + sendResult.getSendStatus().toString(), message.getTopic());
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
        logger.info("【MQ】ApacheXaRocketMqPublisher[" + beanName + "] start...");
        try {
            producer.start();
            isStarted = true;
        } catch (MQClientException e) {
            logger.error("【MQ】ApacheXaRocketMqPublisher[" + beanName + "] start error", e);
        }
    }

    @Override
    public void close() {
        logger.info("【MQ】ApacheXaRocketMqPublisher[" + beanName + "] close...");
        producer.shutdown();
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public TransactionMQProducer getProducer() {
        return producer;
    }

    public void setProducer(TransactionMQProducer producer) {
        this.producer = producer;
    }

}
