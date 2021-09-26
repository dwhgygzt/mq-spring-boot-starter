package com.guzt.starter.mq.properties.aliyun.publisher;

/**
 * 消息生产者配置
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
public class RocketMqPubProperties {

    /**
     * 消息生产者在spring中的beanName
     */
    private String beanName;

    /**
     * 生产者和消费者归属的组id, 一个微服务一个groupId 不能为空！！！
     */
    private String groupId;

    /**
     * 发送消息的类型 SIMPLE 简单类型  TRANSACTION 事务类型
     */
    private String messageType;

    /**
     * 消息发送超时时间，如果服务端在配置的对应时间内未ACK，则发送客户端认为该消息发送失败。
     */
    private String sendMsgTimeoutMillis;

    /**
     * 设置事务消息的第一次回查延迟时间
     */
    private String checkImmunityTimeInSeconds;

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getSendMsgTimeoutMillis() {
        return sendMsgTimeoutMillis;
    }

    public void setSendMsgTimeoutMillis(String sendMsgTimeoutMillis) {
        this.sendMsgTimeoutMillis = sendMsgTimeoutMillis;
    }

    public String getCheckImmunityTimeInSeconds() {
        return checkImmunityTimeInSeconds;
    }

    public void setCheckImmunityTimeInSeconds(String checkImmunityTimeInSeconds) {
        this.checkImmunityTimeInSeconds = checkImmunityTimeInSeconds;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
}
