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
}
