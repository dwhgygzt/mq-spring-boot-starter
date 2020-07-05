package com.middol.starter.mq.pojo;

import java.util.Properties;

/**
 * 公共Topic消息体 Message
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
 */
public class TopicMessage implements Message {
    private static final long serialVersionUID = 1L;

    /**
     * 用户其他属性
     */
    private Properties userProperties;

    /**
     * <p>
     *   消息唯一主键.
     * </p>
     *
     * <p>
     *   <strong>由具体mq产品生成</strong>
     * </p>
     */
    private String messageId;

    /**
     * <p>
     *   消息主题名称, 最长不超过255个字符; 由a-z, A-Z, 0-9, 以及中划线"-"和下划线"_"构成.
     * </p>
     *
     * <p>
     *   <strong>一条合法消息本成员变量不能为空</strong>
     * </p>
     */
    private String topicName;

    /**
     * <p>
     *   消息标签, 合法标识符, 尽量简短且见名知意.
     * </p>
     *
     * <p>
     *   建议传递该值
     * </p>
     */
    private String tags;

    /**
     * <p>
     *   业务主键，例如商户订单号.
     * </p>
     *
     * <p>
     *   建议传递该值
     * </p>
     */
    private String bussinessKey;

    /**
     * <p>
     *    消息体, 消息体长度默认不超过4M, 具体请参阅集群部署文档描述.
     * </p>
     *
     * <p>
     *    <strong>一条合法消息本成员变量不能为空</strong>
     * </p>
     */
    private byte[] messageBody;

    /**
     * 默认构造函数; 必要属性后续通过Set方法设置.
     */
    public TopicMessage() {
        this(null, null, "", null);
    }

    /**
     * 有参构造函数.
     * @param topicName 消息主题
     * @param tags 消息标签
     * @param bussinessKey 业务主键
     * @param messageBody 消息体
     */
    public TopicMessage(String topicName, String tags, String bussinessKey, byte[] messageBody) {
        this.topicName = topicName;
        this.tags = tags;
        this.bussinessKey = bussinessKey;
        this.messageBody = messageBody;
    }

    /**
     * 有参构造函数.
     * @param messageId 唯一主键
     * @param topicName 消息主题
     * @param tags 消息标签
     * @param bussinessKey 业务主键
     * @param messageBody 消息体
     */
    public TopicMessage(String messageId,String topicName, String tags, String bussinessKey, byte[] messageBody) {
        this.messageId = messageId;
        this.topicName = topicName;
        this.tags = tags;
        this.bussinessKey = bussinessKey;
        this.messageBody = messageBody;
    }

    @Override
    public String toString() {
        return "TopicMessage [topicName=" + topicName + ", tags=" + tags + ", messageBody=" + (messageBody != null ? messageBody.length : 0) + "]";
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getBussinessKey() {
        return bussinessKey;
    }

    public void setBussinessKey(String bussinessKey) {
        this.bussinessKey = bussinessKey;
    }

    @Override
    public String getMessageId() {
        return messageId;
    }

    @Override
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    @Override
    public byte[] getMessageBody() {
        return messageBody;
    }

    @Override
    public void setMessageBody(byte[] messageBody) {
        this.messageBody = messageBody;
    }

    public Properties getUserProperties() {
        return userProperties;
    }

    public void setUserProperties(Properties userProperties) {
        this.userProperties = userProperties;
    }

    /**
     * 添加用户自定义属性键值对; 该键值对在消费消费时可被获取.
     * @param key 自定义键
     * @param value 对应值
     */
    public void putUserProperties(final String key, final String value) {
        if (null == this.userProperties) {
            this.userProperties = new Properties();
        }

        if (key != null && value != null) {
            this.userProperties.put(key, value);
        }
    }

    /**
     * 获取用户自定义键的值
     * @param key 自定义键
     * @return 用户自定义键值
     */
    public String getUserProperties(final String key) {
        if (null != this.userProperties) {
            return (String) this.userProperties.get(key);
        }

        return null;
    }
}

