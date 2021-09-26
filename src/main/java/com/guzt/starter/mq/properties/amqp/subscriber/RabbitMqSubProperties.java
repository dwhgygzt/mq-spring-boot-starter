package com.guzt.starter.mq.properties.amqp.subscriber;

import com.guzt.starter.mq.properties.BaseSubProperties;

/**
 * 消息生产者配置
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
public class RabbitMqSubProperties extends BaseSubProperties {

    /**
     * 消息生产者在spring中的beanName
     */
    private String beanName;

    /**
     * 交换器名称
     */
    private String exchangeName;

    /**
     * 此处的groupId 代表队列名称！！！
     * 生产者和消费者归属的组id, 一个微服务一个groupId 不能为空！！！
     */
    private String groupId;

    /**
     * 消费失败 重新消费的时间间隔，默认 30s
     */
    private Integer retryConsumIntervalSeconds = 30;

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

    public Integer getRetryConsumIntervalSeconds() {
        return retryConsumIntervalSeconds;
    }

    public void setRetryConsumIntervalSeconds(Integer retryConsumIntervalSeconds) {
        this.retryConsumIntervalSeconds = retryConsumIntervalSeconds;
    }
}
