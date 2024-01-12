package com.guzt.starter.mq.service;

import com.guzt.starter.mq.pojo.XaTopicMessage;

/**
 * 事务型推送消息到MQ服务端.
 * 半消息机制
 * <a href="https://help.aliyun.com/document_detail/29548.html?spm=a2c4g.11186623.6.598.4b9e7e80WgS7Fs">...</a>
 * 发布（pub）模式
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
public interface XaTopicPublisher extends Admin {

    /**
     * 推送半消息机制的事务消息到MQ服务端.
     *
     * @param topicMessage  消息体
     * @param businessParam spring 业务方法的参数
     */
    void publishInTransaction(XaTopicMessage topicMessage, Object businessParam);

}
