package com.guzt.starter.mq.service;

/**
 * 管理类接口
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
@SuppressWarnings("unused")
public interface Admin {
    /**
     * 检查服务是否已经启动.
     *
     * @return true: 如果服务已启动; 其它情况返回:false
     */
    boolean isStarted();

    /**
     * 检查服务是否已经关闭
     *
     * @return true: 如果服务已关闭; 其它情况返回:false
     */
    boolean isClosed();

    /**
     * 启动服务
     */
    void start();


    /**
     * 关闭服务
     */
    void close();
}

