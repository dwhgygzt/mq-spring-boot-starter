package com.middol.starter.mq.properties.aliyun;

import com.middol.starter.mq.properties.BaseProperties;
import com.middol.starter.mq.properties.aliyun.publisher.RocketMqPubProperties;
import com.middol.starter.mq.properties.aliyun.subscriber.RocketMqSubProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 阿里云OSS配置文件
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
 */
@ConfigurationProperties(prefix = "middol.mq.aliyun.rocketmq")
public class AliyunRocketMqProperties extends BaseProperties {

    /**
     * 阿里云主账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM账号进行API访问或日常运维，
     * 请登录 https://ram.console.aliyun.com 创建RAM账号。
     */
    private String accessKeyId;

    /**
     * 阿里云主账号AccessKey 对应的 Secret
     */
    private String accessKeySecret;

    /**
     * mq 服务地址
     */
    private String nameServerAddr;


    /**
     * 消息生产者
     */
    private List<RocketMqPubProperties> publishers;

    /**
     * 消息订阅者
     */
    private List<RocketMqSubProperties> subscribers;


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

    public List<RocketMqPubProperties> getPublishers() {
        return publishers;
    }

    public void setPublishers(List<RocketMqPubProperties> publishers) {
        this.publishers = publishers;
    }

    public List<RocketMqSubProperties> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(List<RocketMqSubProperties> subscribers) {
        this.subscribers = subscribers;
    }
}
