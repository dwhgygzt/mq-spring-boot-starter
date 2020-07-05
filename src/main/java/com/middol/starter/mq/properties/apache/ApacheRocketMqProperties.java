package com.middol.starter.mq.properties.apache;

import com.middol.starter.mq.properties.BaseProperties;
import com.middol.starter.mq.properties.apache.publisher.ApacheMqPubProperties;
import com.middol.starter.mq.properties.apache.subscriber.ApacheMqSubProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 阿里云OSS配置文件
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
 */
@ConfigurationProperties(prefix = "middol.mq.apache.rocketmq")
public class ApacheRocketMqProperties extends BaseProperties {
    /**
     * @see org.apache.rocketmq.acl.common.SessionCredentials
     */
    private String accessKeyId;

    /**
     * @see org.apache.rocketmq.acl.common.SessionCredentials
     */
    private String accessKeySecret;

    /**
     * mq 注册中心,服务地址
     */
    private String nameServerAddr;

    /**
     * 本机IP
     * 客户端本机IP地址，某些机器会发生无法识别客户端IP地址情况，需要应用在代码中强制指定
     */
    private String clientIp;

    /**
     * 通信层异步回调线程数 默认4个
     */
    private Integer clientCallbackExecutorThreads;

    /**
     * 轮询Name Server间隔时间，单位毫秒     30000
     */
    private Integer pollNameServerInteval;

    /**
     * 向Broker发送心跳间隔时间，单位毫秒  30000
     */
    private Integer heartbeatBrokerInterval;

    /**
     * 持久化Consumer消费进度间隔时间，单位毫秒  5000
     */
    private Integer persistConsumerOffsetInterval;

    /**
     * 消息生产者
     */
    private List<ApacheMqPubProperties> publishers;

    /**
     * 消息订阅者
     */
    private List<ApacheMqSubProperties> subscribers;


    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getNameServerAddr() {
        return nameServerAddr;
    }

    public void setNameServerAddr(String nameServerAddr) {
        this.nameServerAddr = nameServerAddr;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public Integer getClientCallbackExecutorThreads() {
        return clientCallbackExecutorThreads;
    }

    public void setClientCallbackExecutorThreads(Integer clientCallbackExecutorThreads) {
        this.clientCallbackExecutorThreads = clientCallbackExecutorThreads;
    }

    public Integer getPollNameServerInteval() {
        return pollNameServerInteval;
    }

    public void setPollNameServerInteval(Integer pollNameServerInteval) {
        this.pollNameServerInteval = pollNameServerInteval;
    }

    public Integer getHeartbeatBrokerInterval() {
        return heartbeatBrokerInterval;
    }

    public void setHeartbeatBrokerInterval(Integer heartbeatBrokerInterval) {
        this.heartbeatBrokerInterval = heartbeatBrokerInterval;
    }

    public Integer getPersistConsumerOffsetInterval() {
        return persistConsumerOffsetInterval;
    }

    public void setPersistConsumerOffsetInterval(Integer persistConsumerOffsetInterval) {
        this.persistConsumerOffsetInterval = persistConsumerOffsetInterval;
    }

    public List<ApacheMqPubProperties> getPublishers() {
        return publishers;
    }

    public void setPublishers(List<ApacheMqPubProperties> publishers) {
        this.publishers = publishers;
    }

    public List<ApacheMqSubProperties> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(List<ApacheMqSubProperties> subscribers) {
        this.subscribers = subscribers;
    }
}
