package com.middol.starter.mq.properties.apache.publisher;

/**
 * 消息生产者配置
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
 */
public class ApacheMqPubProperties {

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
     * 发送消息超时时间，单位毫秒 10000
     */
    private Integer sendMsgTimeout;

    /**
     * 消息Body超过多大开始压缩（Consumer收到消息会自动解压缩），单位字节
     */
    private Integer compressMsgBodyOverHowmuch;

    /**
     * 如果发送消息返回sendResult，但是sendStatus!=SEND_OK，是否重试发送
     */
    private Boolean retryanotherbrokerwhennotstoreok;

    /**
     * 如果消息发送失败，最大重试次数，该参数只对同步发送模式起作用 2
     */
    private Integer retryTimesWhenSendFailed;

    /**
     * 客户端限制的消息大小，超过报错，同时服务端也会限制，所以需要跟服务端配合使用。 单位M 默认 4M
     */
    private Integer maxMessageSize;

    /**
     * Broker回查Producer事务状态时，线程池最小线程数
     */
    private Integer checkThreadPoolMinSize = 2;

    /**
     * Broker回查Producer事务状态时，线程池最大线程数
     */
    private Integer checkThreadPoolMaxSize = 10;

    /**
     * Broker回查Producer事务状态时，Producer本地缓冲请求队列大小
     */
    private Integer checkKeepAliveSeconds = 100;

    /**
     * checkBlockQueue 2000
     */
    private Integer checkBlockQueueSize = 2000;


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

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public Integer getSendMsgTimeout() {
        return sendMsgTimeout;
    }

    public void setSendMsgTimeout(Integer sendMsgTimeout) {
        this.sendMsgTimeout = sendMsgTimeout;
    }

    public Integer getCompressMsgBodyOverHowmuch() {
        return compressMsgBodyOverHowmuch;
    }

    public void setCompressMsgBodyOverHowmuch(Integer compressMsgBodyOverHowmuch) {
        this.compressMsgBodyOverHowmuch = compressMsgBodyOverHowmuch;
    }

    public Boolean getRetryanotherbrokerwhennotstoreok() {
        return retryanotherbrokerwhennotstoreok;
    }

    public void setRetryanotherbrokerwhennotstoreok(Boolean retryanotherbrokerwhennotstoreok) {
        this.retryanotherbrokerwhennotstoreok = retryanotherbrokerwhennotstoreok;
    }

    public Integer getRetryTimesWhenSendFailed() {
        return retryTimesWhenSendFailed;
    }

    public void setRetryTimesWhenSendFailed(Integer retryTimesWhenSendFailed) {
        this.retryTimesWhenSendFailed = retryTimesWhenSendFailed;
    }

    public Integer getMaxMessageSize() {
        return maxMessageSize;
    }

    public void setMaxMessageSize(Integer maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
    }

    public Integer getCheckThreadPoolMinSize() {
        return checkThreadPoolMinSize;
    }

    public void setCheckThreadPoolMinSize(Integer checkThreadPoolMinSize) {
        this.checkThreadPoolMinSize = checkThreadPoolMinSize;
    }

    public Integer getCheckThreadPoolMaxSize() {
        return checkThreadPoolMaxSize;
    }

    public void setCheckThreadPoolMaxSize(Integer checkThreadPoolMaxSize) {
        this.checkThreadPoolMaxSize = checkThreadPoolMaxSize;
    }

    public Integer getCheckKeepAliveSeconds() {
        return checkKeepAliveSeconds;
    }

    public void setCheckKeepAliveSeconds(Integer checkKeepAliveSeconds) {
        this.checkKeepAliveSeconds = checkKeepAliveSeconds;
    }

    public Integer getCheckBlockQueueSize() {
        return checkBlockQueueSize;
    }

    public void setCheckBlockQueueSize(Integer checkBlockQueueSize) {
        this.checkBlockQueueSize = checkBlockQueueSize;
    }
}
