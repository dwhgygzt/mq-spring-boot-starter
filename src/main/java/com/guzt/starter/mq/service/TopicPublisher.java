package com.guzt.starter.mq.service;

import com.guzt.starter.mq.pojo.TopicMessage;
import com.guzt.starter.mq.pojo.TopicMessageSendResult;

/**
 * 推送消息到MQ服务端
 * 发布（pub）模式
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
@SuppressWarnings("unused")
public interface TopicPublisher extends Admin{

    /**
     * 同步推送消息
     *
     * @param topicMessage 消息对象
     */
    TopicMessageSendResult publish(TopicMessage topicMessage);

    /**
     * 异步推送消息
     *
     * @param topicMessage      消息对象
     * @param topicSendCallback 异步结果处理
     */
    void publishAsync(TopicMessage topicMessage, TopicSendCallback topicSendCallback);

}

