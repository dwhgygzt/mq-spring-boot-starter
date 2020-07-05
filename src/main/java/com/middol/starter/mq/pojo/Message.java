package com.middol.starter.mq.pojo;

import java.io.Serializable;

/**
 * 公共抽象消息体 Message
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
 */
public interface Message extends Serializable {

    /**
     * 获得消息体ID
     * @return String
     */
    String getMessageId();


    /**
     * 设置消息体ID
     *
     * @param messageId 消息唯一id
     */
    void setMessageId(String messageId);

    /**
     * 获得消息体
     *
     * @return byte[]
     */
    byte[] getMessageBody();

    /**
     * 设置消息体
     *
     * @param messageBody 消息体 byte[]
     */
    void setMessageBody(byte[] messageBody);


}

