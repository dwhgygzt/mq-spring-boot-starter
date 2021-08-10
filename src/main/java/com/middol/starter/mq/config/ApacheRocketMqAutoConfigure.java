package com.middol.starter.mq.config;

import com.middol.starter.mq.pojo.LocalTransactionStatus;
import com.middol.starter.mq.pojo.MessageType;
import com.middol.starter.mq.pojo.XaTopicMessage;
import com.middol.starter.mq.properties.apache.ApacheRocketMqProperties;
import com.middol.starter.mq.properties.apache.publisher.ApacheMqPubProperties;
import com.middol.starter.mq.properties.apache.subscriber.ApacheMqSubProperties;
import com.middol.starter.mq.service.TopicListener;
import com.middol.starter.mq.service.XaTopicLocalTransactionExecuter;
import com.middol.starter.mq.service.XaTopicPublisherExecuteStrategy;
import com.middol.starter.mq.service.impl.DefaultTopicListenerImpl;
import com.middol.starter.mq.service.impl.DefaultXaTltExecuterImpl;
import com.middol.starter.mq.service.impl.apache.ApacheSimpleRocketMqPublisher;
import com.middol.starter.mq.service.impl.apache.ApacheSimpleRocketMqSubscriber;
import com.middol.starter.mq.service.impl.apache.ApacheXaRocketMqPublisher;
import com.middol.starter.mq.util.BeanArgBuilder;
import com.middol.starter.mq.util.BeanRegistrarUtil;
import com.middol.starter.mq.util.MqUtil;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.rebalance.AllocateMessageQueueAveragely;
import org.apache.rocketmq.client.hook.SendMessageContext;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.RPCHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;

/**
 * 开源Rocketmq核心配置类
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
 */
@Configuration
@ConditionalOnClass({SendMessageContext.class})
@ConditionalOnProperty(prefix = "middol.mq.apache.rocketmq", value = "enable", havingValue = "true")
@EnableConfigurationProperties({ApacheRocketMqProperties.class})
public class ApacheRocketMqAutoConfigure implements InitializingBean {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ApacheRocketMqProperties apacheRocketMqProperties;

    @Resource
    private ApplicationContext applicationContext;

    /**
     * 消息消费者
     */
    @Autowired
    private Map<String, TopicListener> listenerMap = new ConcurrentHashMap<>(4);

    @Autowired
    private Map<String, XaTopicLocalTransactionExecuter> executerMap = new ConcurrentHashMap<>(2);

    @Bean
    @ConditionalOnMissingBean
    public TopicListener defaultTopicListener() {
        return new DefaultTopicListenerImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public XaTopicLocalTransactionExecuter defaultXatltExecuter() {
        return new DefaultXaTltExecuterImpl();
    }

    @Override
    public void afterPropertiesSet() throws IOException {
        Map<String, XaTopicLocalTransactionExecuter> executerIdMap = new ConcurrentHashMap<>(2);
        this.executerMap.forEach((k, v) -> executerIdMap.put(v.getLocalTransactionExecuterId(), v));
        XaTopicPublisherExecuteStrategy.setExecuterMap(executerIdMap);

        // 发布者
        topicPubAdminService();
        // 订阅者
        topicSubAdminService();
    }

    private void topicPubAdminService() {
        List<ApacheMqPubProperties> properties = apacheRocketMqProperties.getPublishers();
        if (properties == null || properties.isEmpty()) {
            logger.info("没有配置消息发布者的属性, 不初始化消息发布者对象");
            return;
        }

        //获取BeanFactory
        DefaultListableBeanFactory defaultListableBeanFactory =
                (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();

        for (ApacheMqPubProperties pubItem : properties) {
            DefaultMQProducer producer;
            // 事务消息检查设置
            if (StringUtils.hasText(pubItem.getMessageType())
                    && pubItem.getMessageType().toUpperCase().equals(MessageType.TRANSACTION.getValue())) {
                producer = new TransactionMQProducer(pubItem.getGroupId(), getAclRpcHook());
                ExecutorService executorService = new ThreadPoolExecutor(
                        pubItem.getCheckThreadPoolMinSize(),
                        pubItem.getCheckThreadPoolMaxSize(),
                        pubItem.getCheckKeepAliveSeconds(), TimeUnit.SECONDS,
                        new ArrayBlockingQueue<>(pubItem.getCheckBlockQueueSize()), r -> {
                    Thread thread = new Thread(r);
                    thread.setName(pubItem.getBeanName() + "-check-thread");
                    return thread;
                });
                ((TransactionMQProducer) producer).setExecutorService(executorService);
                ((TransactionMQProducer) producer).setTransactionListener(new TransactionListener() {
                    @Override
                    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
                        XaTopicMessage topicMessage = createXaTopicMessage(msg);
                        return returnLocalTransactionState(XaTopicPublisherExecuteStrategy.executeLocalTransaction(topicMessage, arg));
                    }

                    @Override
                    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                        XaTopicMessage topicMessage = createXaTopicMessage(msg);
                        return returnLocalTransactionState(XaTopicPublisherExecuteStrategy.checkLocalTransaction(topicMessage));
                    }
                });
            } else {
                producer = new DefaultMQProducer(pubItem.getGroupId(), getAclRpcHook());
            }
            // 公共配置
            setCommonConfig(producer);
            if (pubItem.getSendMsgTimeout() != null) {
                producer.setSendMsgTimeout(pubItem.getSendMsgTimeout());
            }
            if (pubItem.getMaxMessageSize() != null) {
                producer.setMaxMessageSize(pubItem.getMaxMessageSize());
            }
            if (pubItem.getCompressMsgBodyOverHowmuch() != null) {
                producer.setCompressMsgBodyOverHowmuch(pubItem.getCompressMsgBodyOverHowmuch());
            }
            if (pubItem.getRetryTimesWhenSendFailed() != null) {
                producer.setRetryTimesWhenSendFailed(pubItem.getRetryTimesWhenSendFailed());
            }
            if (pubItem.getRetryAnotherBrokerWhenNotStoreOK() != null) {
                producer.setRetryAnotherBrokerWhenNotStoreOK(pubItem.getRetryAnotherBrokerWhenNotStoreOK());
            }

            BeanArgBuilder beanArgBuilder = new BeanArgBuilder();
            beanArgBuilder.setConstructorArgs(new Object[]{producer, pubItem.getBeanName()});
            beanArgBuilder.setInitMethodName("start");
            beanArgBuilder.setDestoryMethodName("close");
            if (producer instanceof TransactionMQProducer) {
                BeanRegistrarUtil.registerBean(
                        defaultListableBeanFactory, pubItem.getBeanName(), ApacheXaRocketMqPublisher.class, beanArgBuilder);
            } else {
                BeanRegistrarUtil.registerBean(
                        defaultListableBeanFactory, pubItem.getBeanName(), ApacheSimpleRocketMqPublisher.class, beanArgBuilder);
            }
        }
    }

    private LocalTransactionState returnLocalTransactionState(LocalTransactionStatus status) {
        if (status.equals(LocalTransactionStatus.COMMIT)) {
            return LocalTransactionState.COMMIT_MESSAGE;
        } else if (status.equals(LocalTransactionStatus.ROLLBACK)) {
            return LocalTransactionState.ROLLBACK_MESSAGE;
        } else {
            return LocalTransactionState.UNKNOW;
        }
    }

    private void topicSubAdminService() throws IOException {
        if (listenerMap == null || listenerMap.isEmpty()) {
            logger.info("没有消息监听者service对象, 不初始化消息消费者对象");
            return;
        }

        List<ApacheMqSubProperties> properties = apacheRocketMqProperties.getSubscribers();
        if (properties == null || properties.isEmpty()) {
            logger.info("没有配置消息消息者的属性, 不初始化消息消费者对象");
            return;
        }
        //获取BeanFactory
        DefaultListableBeanFactory defaultListableBeanFactory =
                (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();

        Map<String, List<TopicListener>> listenerMapBySubBeanName = MqUtil.topicListenerGroupBySubBean(listenerMap);

        for (ApacheMqSubProperties subItem : properties) {
            DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(subItem.getGroupId(), getAclRpcHook(), new AllocateMessageQueueAveragely());

            // 公共配置
            setCommonConfig(consumer);
            // Wrong time format 2017_0422_221800
            consumer.setConsumeTimestamp("20180422221800");

            BeanArgBuilder beanArgBuilder = new BeanArgBuilder();
            beanArgBuilder.setConstructorArgs(new Object[]{consumer, subItem.getBeanName()});
            // start 方法在MqUtil.setListenerAndStartSub里面调用了
            beanArgBuilder.setDestoryMethodName("close");
            BeanRegistrarUtil.registerBean(defaultListableBeanFactory,
                    subItem.getBeanName(), ApacheSimpleRocketMqSubscriber.class, beanArgBuilder);

            ApacheSimpleRocketMqSubscriber subscriber = applicationContext.getBean(
                    subItem.getBeanName(), ApacheSimpleRocketMqSubscriber.class);
            MqUtil.setListenerAndStartSub(subItem.getBeanName(), subscriber, listenerMapBySubBeanName);
        }
    }

    private RPCHook getAclRpcHook() {
        return new AclClientRPCHook(new SessionCredentials(
                apacheRocketMqProperties.getAccessKeyId(), apacheRocketMqProperties.getAccessKeySecret()));
    }

    private void setCommonConfig(ClientConfig config) {
        if (StringUtils.hasText(apacheRocketMqProperties.getClientIp())) {
            config.setClientIP(apacheRocketMqProperties.getClientIp());
        }
        if (apacheRocketMqProperties.getClientCallbackExecutorThreads() != null) {
            config.setClientCallbackExecutorThreads(apacheRocketMqProperties.getClientCallbackExecutorThreads());
        }
        if (apacheRocketMqProperties.getHeartbeatBrokerInterval() != null) {
            config.setHeartbeatBrokerInterval(apacheRocketMqProperties.getHeartbeatBrokerInterval());
        }
        if (apacheRocketMqProperties.getPollNameServerInteval() != null) {
            config.setPollNameServerInterval(apacheRocketMqProperties.getPollNameServerInteval());
        }
        if (apacheRocketMqProperties.getPersistConsumerOffsetInterval() != null) {
            config.setPersistConsumerOffsetInterval(apacheRocketMqProperties.getPersistConsumerOffsetInterval());
        }

        config.setNamesrvAddr(apacheRocketMqProperties.getNameServerAddr());

    }

    private XaTopicMessage createXaTopicMessage(Message msg) {
        XaTopicMessage topicMessage = new XaTopicMessage();
        Map<String, String> userPropertiesMap = msg.getProperties();
        if (userPropertiesMap != null && !userPropertiesMap.isEmpty()) {
            Properties userProperties = new Properties();
            userPropertiesMap.forEach(userProperties::put);
            topicMessage.setUserProperties(userProperties);
        }
        topicMessage.setBussinessKey(msg.getKeys());
        topicMessage.setMessageBody(msg.getBody());
        topicMessage.setTags(msg.getTags());
        topicMessage.setTopicName(msg.getTopic());
        topicMessage.setLocalTransactionExecuterId(msg.getUserProperty(XaTopicMessage.LOCALTRANSACTION_EXECUTERID_KEY));

        return topicMessage;
    }
}
