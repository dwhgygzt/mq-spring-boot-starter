package com.guzt.starter.mq.service.impl;

import com.guzt.starter.mq.pojo.XaTopicMessage;
import com.guzt.starter.mq.service.XaTopicPublishChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 主要用于rabbitmq 事务消息，用来检测消息是否真的发送成功.
 * 发布（pub）模式
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
public class DefaultXaTopicPublishCheckerImpl implements XaTopicPublishChecker {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<String, Long> commitMap = new ConcurrentHashMap<>(64);
    private final Map<String, Long> rollbackMap = new ConcurrentHashMap<>(64);

    @PostConstruct
    public void init() {
        ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        cleanupExecutor.scheduleAtFixedRate(() -> {
            commitMap.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() >= 15000);
            rollbackMap.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() >= 15000);
        }, 5, 10, TimeUnit.SECONDS);
    }

    @Override
    public void cacheCommit(XaTopicMessage topicMessage) {
        commitMap.put(topicMessage.getMessageId(), System.currentTimeMillis());
    }

    @Override
    public void cacheRollback(XaTopicMessage topicMessage) {
        rollbackMap.put(topicMessage.getMessageId(), System.currentTimeMillis());
    }

    @Override
    public boolean isCommitExists(XaTopicMessage topicMessage) {
        return commitMap.containsKey(topicMessage.getMessageId());
    }

    @Override
    public boolean isRollbackExists(XaTopicMessage topicMessage) {
        return rollbackMap.containsKey(topicMessage.getMessageId());
    }

    @Override
    public void deleteCommitCache(XaTopicMessage topicMessage) {
        commitMap.remove(topicMessage.getMessageId());
    }

    @Override
    public void deleteRollbackCache(XaTopicMessage topicMessage) {
        rollbackMap.remove(topicMessage.getMessageId());
    }
}
