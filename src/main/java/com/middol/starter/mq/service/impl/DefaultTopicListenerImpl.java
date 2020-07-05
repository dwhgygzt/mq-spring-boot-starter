package com.middol.starter.mq.service.impl;

import com.middol.starter.mq.pojo.MessageStatus;
import com.middol.starter.mq.pojo.TopicMessage;
import com.middol.starter.mq.service.TopicListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TopicListener 的默认实现，主要用于容错
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
 */
public class DefaultTopicListenerImpl implements TopicListener {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public static String DEFAULT_SUBSCRIBER_BEANNAME = "default_SubscriberBeanName";
    public static String DEFAULT_TOPICNAME = "default_TopicName";
    public static String DEFAULT_TAG = "default_Tag";

    @Override

    public String getSubscriberBeanName() {
        return DEFAULT_SUBSCRIBER_BEANNAME;
    }

    @Override
    public String getTopicName() {
        return DEFAULT_TOPICNAME;
    }

    @Override
    public String getTagExpression() {
        return DEFAULT_TAG;
    }

    @Override
    public MessageStatus subscribe(TopicMessage topicMessage) {
        if (topicMessage != null) {
            logger.debug("消息 id={}, key={} 被 DefaultTopicListenerImpl 消费了", topicMessage.getMessageId(), topicMessage.getBussinessKey());
            return MessageStatus.CommitMessage;
        } else {
            return MessageStatus.ReconsumeLater;
        }
    }
}
