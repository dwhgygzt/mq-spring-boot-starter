package com.guzt.starter.mq.util;

import com.guzt.starter.mq.service.RetryConsumFailHandler;
import com.guzt.starter.mq.service.TopicListener;
import com.guzt.starter.mq.service.TopicSubscriber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

/**
 * 工具类
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
public class MqUtil {

    /**
     * 工具方法
     *
     * @param listenerMap TopicListener
     * @return Map
     */
    public static Map<String, List<TopicListener>> topicListenerGroupBySubBean(Map<String, TopicListener> listenerMap) {
        List<TopicListener> topicListenerList = new ArrayList<>(4);
        listenerMap.forEach((k, v) -> topicListenerList.add(v));
        //groupBy SubscriberBeanName
        return topicListenerList.stream().collect(groupingBy(TopicListener::getSubscriberBeanName));
    }

    /**
     * 工具方法 设置消费服务监听，并且启动消费服务
     *
     * @param subscriberBeanName       ignore
     * @param subscriber               ignore
     * @param listenerMapBySubBeanName ignore
     * @param retryConsumFailHandler   ignore
     */
    public static void setListenerAndStartSub(
            String subscriberBeanName,
            TopicSubscriber subscriber,
            RetryConsumFailHandler retryConsumFailHandler,
            Map<String, List<TopicListener>> listenerMapBySubBeanName) throws IOException {

        List<TopicListener> listeners = listenerMapBySubBeanName.get(subscriberBeanName);
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        for (TopicListener listener : listeners) {
            subscriber.subscribe(listener.getTopicName(), listener.getTagExpression(), listener);
        }
        subscriber.setRetryConsumFailHandler(retryConsumFailHandler);
        subscriber.start();
    }

}
