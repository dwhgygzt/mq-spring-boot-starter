package com.guzt.starter.mq.service;


import com.guzt.starter.mq.pojo.LocalTransactionStatus;
import com.guzt.starter.mq.pojo.XaTopicMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * XaTopicLocalTransactionExecuter 的执行策略
 * https://help.aliyun.com/document_detail/29548.html?spm=a2c4g.11186623.6.598.4b9e7e80WgS7Fs
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
public class XaTopicPublisherExecuteStrategy {

    protected static Map<String, XaTopicLocalTransactionExecuter> executerMap = new ConcurrentHashMap<>(2);

    public static void setExecuterMap(Map<String, XaTopicLocalTransactionExecuter> executerMap) {
        XaTopicPublisherExecuteStrategy.executerMap = executerMap;
    }

    /**
     * When send transactional prepare(half) message succeed, this method will be invoked to execute local transaction.
     *
     * @param msg           Half(prepare) message
     * @param businessParam Custom business parameter
     * @return Transaction state
     */
    public static LocalTransactionStatus executeLocalTransaction(XaTopicMessage msg, Object businessParam) {
        if (msg == null) {
            return LocalTransactionStatus.UNKNOW;
        }
        XaTopicLocalTransactionExecuter executer = executerMap.get(msg.getLocalTransactionExecuterId());
        if (executer != null) {
            return executer.executeLocalTransaction(msg, businessParam);
        } else {
            return LocalTransactionStatus.UNKNOW;
        }
    }


    /**
     * When no response to prepare(half) message. broker will send check message to check the transaction status, and this
     * method will be invoked to get local transaction status.
     *
     * @param msg Check message
     * @return Transaction state
     */
    public static LocalTransactionStatus checkLocalTransaction(XaTopicMessage msg) {
        if (msg == null) {
            return LocalTransactionStatus.UNKNOW;
        }
        XaTopicLocalTransactionExecuter executer = executerMap.get(msg.getLocalTransactionExecuterId());
        if (executer != null) {
            return executer.checkLocalTransaction(msg);
        } else {
            return LocalTransactionStatus.UNKNOW;
        }
    }

}
