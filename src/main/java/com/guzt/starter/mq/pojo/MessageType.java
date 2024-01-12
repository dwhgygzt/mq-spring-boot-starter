package com.guzt.starter.mq.pojo;

/**
 * 消费消息的返回结果
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
public enum MessageType {

    /**
     * 普通消息
     */
    SIMPLE("SIMPLE"),

    /**
     * 事务消息
     */
    TRANSACTION("TRANSACTION");

    private final String value;

    public String getValue(){
        return this.value;
    }

    MessageType(String value) {
        this.value = value;
    }
}
