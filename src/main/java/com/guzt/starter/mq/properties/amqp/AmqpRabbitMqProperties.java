package com.guzt.starter.mq.properties.amqp;

import com.guzt.starter.mq.properties.BaseProperties;
import com.guzt.starter.mq.properties.amqp.publisher.RabbitMqPubProperties;
import com.guzt.starter.mq.properties.amqp.subscriber.RabbitMqSubProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;


/**
 * 阿里云OSS配置文件
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
@ConfigurationProperties(prefix = "guzt.mq.amqp.rabbitmq")
public class AmqpRabbitMqProperties extends BaseProperties {

    /**
     * rabbitmq 消息存储空间名称，类似于mysql的db名称【必填】
     */
    private String virtualHost;
    /**
     * rabbitmq的服务器地址【必填】
     */
    private String host;
    /**
     * 连接端口【必填】
     */
    private Integer port;

    /**
     * rabbitmq 登录用户名【必填】
     */
    private String userName;

    /**
     * rabbitmq 登录用户密码【必填】
     */
    private String password;

    /**
     * 下面的参数一般默认值即可，按实际业务情况可做适当修改
     */
    private Integer requestedChannelMax;
    private Integer requestedFrameMax;
    private Integer requestedHeartbeat;
    private Integer connectionTimeout;
    private Integer handshakeTimeout;
    private Integer shutdownTimeout;

    /**
     * 消息生产者
     */
    private List<RabbitMqPubProperties> publishers;

    /**
     * 消息订阅者
     */
    private List<RabbitMqSubProperties> subscribers;

    public String getVirtualHost() {
        return virtualHost;
    }

    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getRequestedChannelMax() {
        return requestedChannelMax;
    }

    public void setRequestedChannelMax(Integer requestedChannelMax) {
        this.requestedChannelMax = requestedChannelMax;
    }

    public Integer getRequestedFrameMax() {
        return requestedFrameMax;
    }

    public void setRequestedFrameMax(Integer requestedFrameMax) {
        this.requestedFrameMax = requestedFrameMax;
    }

    public Integer getRequestedHeartbeat() {
        return requestedHeartbeat;
    }

    public void setRequestedHeartbeat(Integer requestedHeartbeat) {
        this.requestedHeartbeat = requestedHeartbeat;
    }

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Integer getHandshakeTimeout() {
        return handshakeTimeout;
    }

    public void setHandshakeTimeout(Integer handshakeTimeout) {
        this.handshakeTimeout = handshakeTimeout;
    }

    public Integer getShutdownTimeout() {
        return shutdownTimeout;
    }

    public void setShutdownTimeout(Integer shutdownTimeout) {
        this.shutdownTimeout = shutdownTimeout;
    }

    public List<RabbitMqPubProperties> getPublishers() {
        return publishers;
    }

    public void setPublishers(List<RabbitMqPubProperties> publishers) {
        this.publishers = publishers;
    }

    public List<RabbitMqSubProperties> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(List<RabbitMqSubProperties> subscribers) {
        this.subscribers = subscribers;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
