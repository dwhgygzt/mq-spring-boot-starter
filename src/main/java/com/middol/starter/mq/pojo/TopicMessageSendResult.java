package com.middol.starter.mq.pojo;

import java.io.Serializable;

/**
 * 公共Topic消息体 Message 发送结果
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
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
     *   消息主题名称, 最长不超过255个字符; 由a-z, A-Z, 0-9, 以及中划线"-"和下划线"_"构成.
     * </p>
     *
     * <p>
     *   <strong>一条合法消息本成员变量不能为空</strong>
     * </p>
     */
    private String topicName;

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
}
