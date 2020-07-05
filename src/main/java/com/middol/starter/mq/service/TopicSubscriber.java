package com.middol.starter.mq.service;

/**
 * MQ消费者
 * 订阅（subscribe）模式.
 * 订阅关系一致 https://help.aliyun.com/document_detail/43523.html?spm=a2c4g.11186623.6.734.60b94c07Uwhsky
 * 1.订阅的 Topic 必须一致
 * 2.订阅的 Topic 中的 Tag 必须一致
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
 */
public interface TopicSubscriber extends Admin {

    /**
     * 订阅消息
     *
     * @param topic         消息主题
     * @param tagExpression 订阅过滤表达式字符串，ONS服务器依据此表达式进行过滤。只支持或运算<br>
     *                      eg: "tag1 || tag2 || tag3"<br>
     *                      如果subExpression等于null或者*，则表示全部订阅
     * @param listener      消息回调监听器
     */
    void subscribe(String topic, String tagExpression, TopicListener listener);

    /**
     * 取消某个topic订阅
     *
     * @param topicName 要取消的主题.
     */
    void unsubscribe(String topicName);

}
