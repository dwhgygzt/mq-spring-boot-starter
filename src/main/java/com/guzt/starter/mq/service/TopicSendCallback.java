package com.guzt.starter.mq.service;

import com.guzt.starter.mq.exception.TopicMqException;
import com.guzt.starter.mq.pojo.TopicMessageSendResult;

/**
 * 异步发送完成后, 回调接口.
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
public interface TopicSendCallback {


    /**
     * 发送成功回调的方法.
     *
     * @param topicMessageSendResult 发送结果
     */
    void onSuccess(TopicMessageSendResult topicMessageSendResult);

    /**
     * 发送失败回调方法.
     *
     * @param topicMqException 异常信息.
     */
    void onFail(TopicMqException topicMqException);

}
