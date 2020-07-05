package com.middol.starter.mq.service;


import com.middol.starter.mq.pojo.LocalTransactionStatus;
import com.middol.starter.mq.pojo.XaTopicMessage;

/**
 * 发送半消息后，本地事务执行器
 * https://help.aliyun.com/document_detail/29548.html?spm=a2c4g.11186623.6.598.4b9e7e80WgS7Fs
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
 */
public interface XaTopicLocalTransactionExecuter {


    /**
     * 本地事务执行器id, 一个应用启动一个事务消息的发送服务， 多个模块发送事务消息，每个模块对应一个 ExecuterId
     *
     * @return executerId
     */
    String getLocalTransactionExecuterId();


    /**
     * When send transactional prepare(half) message succeed, this method will be invoked to execute local transaction.
     *
     * @param msg           Half(prepare) message
     * @param businessParam Custom business parameter
     * @return Transaction state
     */
    LocalTransactionStatus executeLocalTransaction(XaTopicMessage msg, Object businessParam);

    /**
     * When no response to prepare(half) message. broker will send check message to check the transaction status, and this
     * method will be invoked to get local transaction status.
     *
     * @param msg Check message
     * @return Transaction state
     */
    LocalTransactionStatus checkLocalTransaction(XaTopicMessage msg);

}
