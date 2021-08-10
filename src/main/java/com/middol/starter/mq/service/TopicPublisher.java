package com.middol.starter.mq.service;

import com.middol.starter.mq.pojo.TopicMessage;
import com.middol.starter.mq.pojo.TopicMessageSendResult;

/**
 * 推送消息到MQ服务端
 * 发布（pub）模式
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
 */
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

