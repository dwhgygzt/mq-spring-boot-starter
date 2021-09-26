package com.guzt.starter.mq.service;

import com.guzt.starter.mq.pojo.MessageStatus;
import com.guzt.starter.mq.pojo.TopicMessage;

/**
 * MQ对应的监听者，实现具体的消费业务
 * 订阅（subscribe）模式.
 * 订阅关系一致 https://help.aliyun.com/document_detail/43523.html?spm=a2c4g.11186623.6.734.60b94c07Uwhsky
 *
 * 同一个消费者Group ID下所有Consumer实例 :
 * 1.订阅的 Topic 必须一致
 * 2.订阅的 Topic 中的 Tag 必须一致
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
public interface TopicListener {

    /**
     * 对应的消费者，例如 aliyunrocketmq 中的groupId
     *
     * @return SubscriberBeanName
     */
    String getSubscriberBeanName();

    /**
     * 订阅的topic
     *
     * @return topic
     */
    String getTopicName();

    /**
     * 订阅的 tag
     *
     * @return 订阅过滤表达式字符串，ONS服务器依据此表达式进行过滤。只支持或运算<br>
     * eg: "tag1 || tag2 || tag3"<br>
     * 如果subExpression等于null或者*，则表示全部订阅
     */
    String getTagExpression();

    /**
     * 消息订阅
     *
     * @param topicMessage 从消息服务器获得的订阅消息
     * @return 执行完本地业务逻辑反馈消息服务器是否消费完毕 MessageStatus
     */
    MessageStatus subscribe(TopicMessage topicMessage);

}
