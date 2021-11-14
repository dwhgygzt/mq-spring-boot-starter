package com.guzt.starter.mq.properties.amqp.publisher;

/**
 * 消息生产者配置
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
public class RabbitMqPubProperties {

    /**
     * 消息生产者在spring中的beanName
     */
    private String beanName;

    /**
     * 交换器名称
     */
    private String exchangeName;

    /**
     * 生产者和消费者归属的组id, 一个微服务一个groupId 不能为空！！！
     */
    private String groupId;

    /**
     * 异步发送-核心线程池大小
     */
    private Integer asyncPubCorePoolSize = 2;

    /**
     * 异步发送-最大线程池大小
     */
    private Integer asyncMaximumPoolSize = 150;

    /**
     * 异步发送-线程最大空闲时间 单位：秒
     */
    private Integer asyncKeepAliveSeconds = 10;

    /**
     * 发送消息的类型 SIMPLE 简单类型  TRANSACTION 事务类型
     */
    private String messageType;

    /**
     * 【专门针对XA事务消息】设置事务消息的回查间隔时间
     */
    private Integer checkImmunityTimeInSeconds = 10;

    /**
     * 【专门针对XA事务消息】最大回查失败后重试次数 默认10次
     */
    private Integer checkImmunityMaxCount = 10;


    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public Integer getAsyncPubCorePoolSize() {
        return asyncPubCorePoolSize;
    }

    public void setAsyncPubCorePoolSize(Integer asyncPubCorePoolSize) {
        this.asyncPubCorePoolSize = asyncPubCorePoolSize;
    }

    public Integer getAsyncMaximumPoolSize() {
        return asyncMaximumPoolSize;
    }

    public void setAsyncMaximumPoolSize(Integer asyncMaximumPoolSize) {
        this.asyncMaximumPoolSize = asyncMaximumPoolSize;
    }

    public Integer getAsyncKeepAliveSeconds() {
        return asyncKeepAliveSeconds;
    }

    public void setAsyncKeepAliveSeconds(Integer asyncKeepAliveSeconds) {
        this.asyncKeepAliveSeconds = asyncKeepAliveSeconds;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public Integer getCheckImmunityTimeInSeconds() {
        return checkImmunityTimeInSeconds;
    }

    public void setCheckImmunityTimeInSeconds(Integer checkImmunityTimeInSeconds) {
        this.checkImmunityTimeInSeconds = checkImmunityTimeInSeconds;
    }

    public Integer getCheckImmunityMaxCount() {
        return checkImmunityMaxCount;
    }

    public void setCheckImmunityMaxCount(Integer checkImmunityMaxCount) {
        this.checkImmunityMaxCount = checkImmunityMaxCount;
    }
}
