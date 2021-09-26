package com.guzt.starter.mq.exception;

/**
 * 消息发送失败统一异常类
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
public class MqException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private String messageId;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    /**
     * 默认异常构造函数.
     */
    public MqException() {
    }

    /**
     * 异常接口构造函数
     *
     * @param message 需要向外传递的异常信息
     */
    public MqException(String message) {
        super(message);
    }

    /**
     * 异常接口构造函数
     *
     * @param cause 需要向外传递的异常
     */
    public MqException(Throwable cause) {
        super(cause);
    }

    /**
     * 异常接口构造函数
     *
     * @param message 需要向外传递的异常信息
     * @param cause 需要向外传递的异常
     */
    public MqException(String message, Throwable cause) {
        super(message, cause);
    }
}

