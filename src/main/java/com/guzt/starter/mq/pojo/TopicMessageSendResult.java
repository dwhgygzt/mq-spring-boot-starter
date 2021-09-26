package com.guzt.starter.mq.pojo;

import java.io.Serializable;

/**
 * 公共Topic消息体 Message 发送结果
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
public class TopicMessageSendResult implements Serializable {

    private static final long serialVersionUID = 1L;

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
     *   业务唯一id，由发送方发送时传递的值.
     * </p>
     *
     * <p>
     *   <strong>由发送方发送时传递的值</strong>
     * </p>
     */
    private String businessKey;

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
     * 消息 的 tag 来源于发送方配置值
     */
    private String tags;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}
