package com.guzt.starter.mq.properties.apache.subscriber;

import com.guzt.starter.mq.properties.BaseSubProperties;

/**
 * 消息消费者配置
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
@SuppressWarnings("unused")
public class ApacheMqSubProperties extends BaseSubProperties {

    /**
     * 消息消费者在spring中的beanName
     */
    private String beanName;

    /**
     * 生产者和消费者归属的组id, 一个微服务一个groupId 不能为空！！！
     */
    private String groupId;

    /**
     * 消费线程池最小线程数
     */
    private Integer consumeThreadMin;

    /**
     * 消费线程池最大线程数
     */
    private Integer consumeThreadMax;

    /**
     * 单队列并行消费允许的最大跨度
     */
    private Integer consumeConcurrentlyMaxSpan;

    /**
     * 拉消息本地队列缓存消息最大数
     */
    private Integer pullThresholdForQueue;

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

    public Integer getConsumeThreadMin() {
        return consumeThreadMin;
    }

    public void setConsumeThreadMin(Integer consumeThreadMin) {
        this.consumeThreadMin = consumeThreadMin;
    }

    public Integer getConsumeThreadMax() {
        return consumeThreadMax;
    }

    public void setConsumeThreadMax(Integer consumeThreadMax) {
        this.consumeThreadMax = consumeThreadMax;
    }

    public Integer getConsumeConcurrentlyMaxSpan() {
        return consumeConcurrentlyMaxSpan;
    }

    public void setConsumeConcurrentlyMaxSpan(Integer consumeConcurrentlyMaxSpan) {
        this.consumeConcurrentlyMaxSpan = consumeConcurrentlyMaxSpan;
    }

    public Integer getPullThresholdForQueue() {
        return pullThresholdForQueue;
    }

    public void setPullThresholdForQueue(Integer pullThresholdForQueue) {
        this.pullThresholdForQueue = pullThresholdForQueue;
    }
}
