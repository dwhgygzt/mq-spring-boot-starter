package com.middol.starter.mq.properties.amqp.publisher;

/**
 * 消息生产者配置
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
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
}
