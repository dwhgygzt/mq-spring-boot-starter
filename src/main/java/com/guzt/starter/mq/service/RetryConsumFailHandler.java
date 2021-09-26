package com.guzt.starter.mq.service;


import com.guzt.starter.mq.pojo.Message;

/**
 * MQ消费者,尝试了最大次数后失败时的处理者
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
public interface RetryConsumFailHandler {

    /**
     * 处理
     *
     * @param message 消费失败的消息
     */
    void handle(Message message);
}
