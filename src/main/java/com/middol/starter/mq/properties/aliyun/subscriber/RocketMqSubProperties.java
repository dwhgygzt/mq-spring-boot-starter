package com.middol.starter.mq.properties.aliyun.subscriber;

/**
 * 消息消费者配置
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
 */
public class RocketMqSubProperties {

    /**
     * 消息消费者在spring中的beanName
     */
    private String beanName;

    /**
     * 生产者和消费者归属的组id, 一个微服务一个groupId 不能为空！！！
     */
    private String groupId;

    /**
     * 顺序消息消费失败进行重试前的等待时间 单位(毫秒)
     */
    private String suspendTimeMillis;

    /**
     * 消息消费失败时的最大重试次数
     */
    private String maxReconsumeTimes;

    /**
     * 设置每条消息消费的最大超时时间,超过这个时间,这条消息将会被视为消费失败,等下次重新投递再次消费. 每个业务需要设置一个合理的值. 单位(分钟)
     */
    private String consumeTimeout;

    /**
     * 是否每次请求都带上最新的订阅关系
     */
    private String postSubscriptionWhenPull;

    /**
     * BatchConsumer每次批量消费的最大消息数量, 默认值为1, 允许自定义范围为[1, 32], 实际消费数量可能小于该值.
     */
    private String consumeMessageBatchMaxSize;

    /**
     * Consumer允许在客户端中缓存的最大消息数量，默认值为5000，设置过大可能会引起客户端OOM，取值范围为[100, 50000]
     * <p>
     * 考虑到批量拉取，实际最大缓存量会少量超过限定值
     * <p>
     * 该限制在客户端级别生效，限定额会平均分配到订阅的Topic上，比如限制为1000条，订阅2个Topic，每个Topic将限制缓存500条
     */
    private String maxCachedMessageAmount;

    /**
     * Consumer允许在客户端中缓存的最大消息容量，默认值为512 MiB，设置过大可能会引起客户端OOM，取值范围为[16, 2048]
     * <p>
     * 考虑到批量拉取，实际最大缓存量会少量超过限定值
     * <p>
     * 该限制在客户端级别生效，限定额会平均分配到订阅的Topic上，比如限制为1000MiB，订阅2个Topic，每个Topic将限制缓存500MiB
     */
    private String maxCachedMessageSizeInMiB;

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

    public String getSuspendTimeMillis() {
        return suspendTimeMillis;
    }

    public void setSuspendTimeMillis(String suspendTimeMillis) {
        this.suspendTimeMillis = suspendTimeMillis;
    }

    public String getMaxReconsumeTimes() {
        return maxReconsumeTimes;
    }

    public void setMaxReconsumeTimes(String maxReconsumeTimes) {
        this.maxReconsumeTimes = maxReconsumeTimes;
    }

    public String getConsumeTimeout() {
        return consumeTimeout;
    }

    public void setConsumeTimeout(String consumeTimeout) {
        this.consumeTimeout = consumeTimeout;
    }

    public String getPostSubscriptionWhenPull() {
        return postSubscriptionWhenPull;
    }

    public void setPostSubscriptionWhenPull(String postSubscriptionWhenPull) {
        this.postSubscriptionWhenPull = postSubscriptionWhenPull;
    }

    public String getConsumeMessageBatchMaxSize() {
        return consumeMessageBatchMaxSize;
    }

    public void setConsumeMessageBatchMaxSize(String consumeMessageBatchMaxSize) {
        this.consumeMessageBatchMaxSize = consumeMessageBatchMaxSize;
    }

    public String getMaxCachedMessageAmount() {
        return maxCachedMessageAmount;
    }

    public void setMaxCachedMessageAmount(String maxCachedMessageAmount) {
        this.maxCachedMessageAmount = maxCachedMessageAmount;
    }

    public String getMaxCachedMessageSizeInMiB() {
        return maxCachedMessageSizeInMiB;
    }

    public void setMaxCachedMessageSizeInMiB(String maxCachedMessageSizeInMiB) {
        this.maxCachedMessageSizeInMiB = maxCachedMessageSizeInMiB;
    }

}
