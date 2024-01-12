package com.guzt.starter.mq.service;

import com.guzt.starter.mq.pojo.XaTopicMessage;

/**
 * 主要用于rabbitmq 事务消息，用来检测消息是否真的发送成功.
 * 发布（pub）模式
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
public interface XaTopicPublishChecker {

    /**
     * 用来缓存已经发送成功的消息信息，缓存一段时间
     *
     * @param topicMessage 事务消息
     */
    void cacheCommit(XaTopicMessage topicMessage);

    /**
     * 用来缓存已经发送成功的消息信息，缓存一段时间
     *
     * @param topicMessage 事务消息
     */
    void cacheRollback(XaTopicMessage topicMessage);

    /**
     * 用来判断是否存在缓存中，如果存在表示消息一定一定已经发送成功了，
     * 否则消息可能没用发送成功，例如：提交的时候异常了
     *
     * @param topicMessage 事务消息
     * @return true 存在   false 不存在
     */
    boolean isCommitExists(XaTopicMessage topicMessage);

    /**
     * 用来判断是否存在缓存中，如果存在表示消息一定一定已经发送成功了，
     * 否则消息可能没用发送成功，例如：提交的时候异常了
     *
     * @param topicMessage 事务消息
     * @return true 存在   false 不存在
     */
    boolean isRollbackExists(XaTopicMessage topicMessage);

    /**
     * 删除缓存消息
     *
     * @param topicMessage 事务消息
     */
    void deleteCommitCache(XaTopicMessage topicMessage);

    /**
     * 删除缓存消息
     *
     * @param topicMessage 事务消息
     */
    void deleteRollbackCache(XaTopicMessage topicMessage);
}
