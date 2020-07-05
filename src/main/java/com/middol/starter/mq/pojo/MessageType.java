package com.middol.starter.mq.pojo;

/**
 * 消费消息的返回结果
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
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

    private String value;

    public String getValue(){
        return this.value;
    }

    MessageType(String value) {
        this.value = value;
    }
}
