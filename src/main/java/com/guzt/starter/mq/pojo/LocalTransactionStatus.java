package com.guzt.starter.mq.pojo;

/**
 * 通知MQ服务器，本地事务执行结果.
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
public enum LocalTransactionStatus {

    /**
     * 提交了
     */
    COMMIT,

    /**
     * 回滚
     */
    ROLLBACK,

    /**
     * 暂不清楚，待mq服务器再次检查
     */
    UNKNOW,
}
