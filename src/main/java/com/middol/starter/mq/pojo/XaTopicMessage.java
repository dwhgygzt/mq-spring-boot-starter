package com.middol.starter.mq.pojo;

import com.middol.starter.mq.service.XaTopicLocalTransactionExecuter;

/**
 * 公共Topic消息体 Message
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
 */
public class XaTopicMessage extends TopicMessage {
    private static final long serialVersionUID = 1L;

    public static final String LOCALTRANSACTION_EXECUTERID_KEY = "localTransactionExecuterId";

    /**
     * 本地事务执行器id, 一个应用启动一个事务消息的发送服务， 多个模块发送事务消息，每个模块对应一个 ExecuterId
     *
     * @see XaTopicLocalTransactionExecuter#getLocalTransactionExecuterId()
     */
    private String localTransactionExecuterId;

    /**
     * 默认构造函数; 必要属性后续通过Set方法设置.
     */
    public XaTopicMessage() {
        this(null, null, "", null);
    }

    /**
     * 有参构造函数.
     *
     * @param topicName    消息主题
     * @param tags         消息标签
     * @param bussinessKey 业务主键
     * @param messageBody  消息体
     */
    public XaTopicMessage(String topicName, String tags, String bussinessKey, byte[] messageBody) {
        super(topicName, tags, bussinessKey, messageBody);
    }

    /**
     * 有参构造函数.
     *
     * @param localTransactionExecuterId 本地事务执行器id
     * @param topicName                  消息主题
     * @param tags                       消息标签
     * @param bussinessKey               业务主键
     * @param messageBody                消息体
     */
    public XaTopicMessage(String localTransactionExecuterId, String topicName, String tags, String bussinessKey, byte[] messageBody) {
        super(topicName, tags, bussinessKey, messageBody);
        this.setLocalTransactionExecuterId(localTransactionExecuterId);
    }

    /**
     * 有参构造函数.
     *
     * @param localTransactionExecuterId 本地事务执行器id
     * @param messageId                  唯一主键
     * @param topicName                  消息主题
     * @param tags                       消息标签
     * @param bussinessKey               业务主键
     * @param messageBody                消息体
     */
    public XaTopicMessage(String localTransactionExecuterId, String messageId, String topicName, String tags, String bussinessKey, byte[] messageBody) {
        super(messageId, topicName, tags, bussinessKey, messageBody);
        this.setLocalTransactionExecuterId(localTransactionExecuterId);
    }

    @Override
    public String toString() {
        return "XaTopicMessage [localTransactionExecuterId=" + this.getLocalTransactionExecuterId() + ",topicName=" + this.getTopicName() + ", tags=" + this.getTags() + ", messageBody=" + (this.getMessageBody() != null ? this.getMessageBody() : 0) + "]";
    }

    public String getLocalTransactionExecuterId() {
        return localTransactionExecuterId;
    }

    public void setLocalTransactionExecuterId(String localTransactionExecuterId) {
        this.localTransactionExecuterId = localTransactionExecuterId;
    }

}

