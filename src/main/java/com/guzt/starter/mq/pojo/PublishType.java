package com.guzt.starter.mq.pojo;

/**
 * 消息队列的发送场景
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
public enum PublishType {

    /**
     * 首次发送
     */
    FIRST("FIRST"),

    /**
     * 发送失败重新发送
     */
    PUBLISH_FAIL_RETRY("PUBLISH_FAIL_RETRY"),

    /**
     * 消费失败重新发送
     */
    CONSUM_FAIL_RETRY("CONSUM_FAIL_RETRY");

    private final String value;

    public String getValue() {
        return this.value;
    }

    PublishType(String value) {
        this.value = value;
    }
}
