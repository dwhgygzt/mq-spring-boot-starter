package com.guzt.starter.mq.service.impl.aliyun;

import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.transaction.TransactionProducer;
import com.aliyun.openservices.ons.api.transaction.TransactionStatus;
import com.guzt.starter.mq.exception.MqException;
import com.guzt.starter.mq.pojo.LocalTransactionStatus;
import com.guzt.starter.mq.pojo.XaTopicMessage;
import com.guzt.starter.mq.service.XaTopicPublisher;
import com.guzt.starter.mq.service.XaTopicPublisherExecuteStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * 阿里云推送消息到MQ服务端
 * 发布（pub）模式
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
@SuppressWarnings("unused")
public class AliyunXaRocketMqPublisher implements XaTopicPublisher {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    /**
     * 阿里云 rocketmq producer
     */
    TransactionProducer producer;

    String beanName;

    public AliyunXaRocketMqPublisher(TransactionProducer producer, String beanName) {
        this.producer = producer;
        this.beanName = beanName;
    }

    public AliyunXaRocketMqPublisher() {
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
        logger.info("【MQ】AliyunXaRocketMqPublisher[" + beanName + "] start...");
        producer.start();
    }

    @Override
    public void close() {
        logger.info("【MQ】AliyunXaRocketMqPublisher[" + beanName + "] close...");
        producer.shutdown();
    }

    @Override
    public void publishInTransaction(XaTopicMessage topicMessage, Object businessParam) {
        if (!StringUtils.hasText(topicMessage.getLocalTransactionExecuterId())) {
            throw new MqException("XaTopicMessage对象请设置localTransactionExecuterId属性");
        }
        Message message = new Message();
        message.setUserProperties(topicMessage.getUserProperties());
        message.setKey(topicMessage.getBussinessKey());
        message.setBody(topicMessage.getMessageBody());
        message.setTag(topicMessage.getTags());
        message.setTopic(topicMessage.getTopicName());
        message.putUserProperties(XaTopicMessage.LOCALTRANSACTION_EXECUTERID_KEY, topicMessage.getLocalTransactionExecuterId());
        producer.send(message, (msg, arg) -> {
            LocalTransactionStatus status = XaTopicPublisherExecuteStrategy.executeLocalTransaction(topicMessage, arg);
            if (status.equals(LocalTransactionStatus.COMMIT)) {
                return TransactionStatus.CommitTransaction;
            } else if (status.equals(LocalTransactionStatus.ROLLBACK)) {
                return TransactionStatus.RollbackTransaction;
            } else {
                return TransactionStatus.Unknow;
            }
        }, businessParam);
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public TransactionProducer getProducer() {
        return producer;
    }

    public void setProducer(TransactionProducer producer) {
        this.producer = producer;
    }
}
