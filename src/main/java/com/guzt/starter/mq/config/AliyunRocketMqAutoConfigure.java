package com.guzt.starter.mq.config;

import com.aliyun.openservices.ons.api.Admin;
import com.aliyun.openservices.ons.api.Consumer;
import com.aliyun.openservices.ons.api.ONSFactory;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.openservices.ons.api.transaction.TransactionProducer;
import com.aliyun.openservices.ons.api.transaction.TransactionStatus;
import com.guzt.starter.mq.pojo.LocalTransactionStatus;
import com.guzt.starter.mq.pojo.MessageType;
import com.guzt.starter.mq.pojo.XaTopicMessage;
import com.guzt.starter.mq.properties.aliyun.AliyunRocketMqProperties;
import com.guzt.starter.mq.properties.aliyun.publisher.RocketMqPubProperties;
import com.guzt.starter.mq.properties.aliyun.subscriber.RocketMqSubProperties;
import com.guzt.starter.mq.service.RetryConsumFailHandler;
import com.guzt.starter.mq.service.TopicListener;
import com.guzt.starter.mq.service.XaTopicLocalTransactionExecuter;
import com.guzt.starter.mq.service.XaTopicPublisherExecuteStrategy;
import com.guzt.starter.mq.service.impl.DefaultRetryConsumFailHandler;
import com.guzt.starter.mq.service.impl.DefaultTopicListenerImpl;
import com.guzt.starter.mq.service.impl.DefaultXaTLTExecuterImpl;
import com.guzt.starter.mq.service.impl.aliyun.AliyunSimpleRocketMqPublisher;
import com.guzt.starter.mq.service.impl.aliyun.AliyunSimpleRocketMqSubscriber;
import com.guzt.starter.mq.service.impl.aliyun.AliyunXaRocketMqPublisher;
import com.guzt.starter.mq.util.BeanArgBuilder;
import com.guzt.starter.mq.util.BeanRegistrarUtil;
import com.guzt.starter.mq.util.MqUtil;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * 阿里云 Rocketmq 核心配置类
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
@Configuration
@ConditionalOnClass({ONSFactory.class})
@ConditionalOnProperty(prefix = "guzt.mq.aliyun.rocketmq", value = "enable", havingValue = "true")
@EnableConfigurationProperties({AliyunRocketMqProperties.class})
public class AliyunRocketMqAutoConfigure implements InitializingBean {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private AliyunRocketMqProperties aliyunRocketMqProperties;

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
    public RetryConsumFailHandler defaultRetryConsumFailHandler() {
        return new DefaultRetryConsumFailHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public TopicListener defaultTopicListener() {
        return new DefaultTopicListenerImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public XaTopicLocalTransactionExecuter defaultXatltExecuter() {
        return new DefaultXaTLTExecuterImpl();
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
        List<RocketMqPubProperties> properties = aliyunRocketMqProperties.getPublishers();
        if (properties == null || properties.isEmpty()) {
            logger.info("没有配置消息发布者的属性, 不初始化消息发布者对象");
            return;
        }

        //获取BeanFactory
        DefaultListableBeanFactory defaultListableBeanFactory =
                (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();

        for (RocketMqPubProperties pubItem : properties) {
            Properties configProperties = commonProperties();
            configProperties.setProperty(PropertyKeyConst.GROUP_ID, pubItem.getGroupId());
            if (StringUtils.hasText(pubItem.getSendMsgTimeoutMillis())) {
                configProperties.setProperty(PropertyKeyConst.SendMsgTimeoutMillis, pubItem.getSendMsgTimeoutMillis());
            }
            if (StringUtils.hasText(pubItem.getCheckImmunityTimeInSeconds())) {
                configProperties.setProperty(PropertyKeyConst.CheckImmunityTimeInSeconds, pubItem.getCheckImmunityTimeInSeconds());
            }
            Admin producer;
            // 事务消息检查设置
            if (StringUtils.hasText(pubItem.getMessageType())
                    && pubItem.getMessageType().toUpperCase().equals(MessageType.TRANSACTION.getValue())) {
                producer = ONSFactory.createTransactionProducer(configProperties,
                        msg -> {
                            LocalTransactionStatus status = XaTopicPublisherExecuteStrategy.checkLocalTransaction(createXaTopicMessage(msg));
                            if (status.equals(LocalTransactionStatus.COMMIT)) {
                                return TransactionStatus.CommitTransaction;
                            } else if (status.equals(LocalTransactionStatus.ROLLBACK)) {
                                return TransactionStatus.RollbackTransaction;
                            } else {
                                return TransactionStatus.Unknow;
                            }
                        });
            } else {
                producer = ONSFactory.createProducer(configProperties);
            }

            BeanArgBuilder beanArgBuilder = new BeanArgBuilder();
            beanArgBuilder.setConstructorArgs(new Object[]{producer, pubItem.getBeanName()});
            beanArgBuilder.setInitMethodName("start");
            beanArgBuilder.setDestoryMethodName("close");
            if (producer instanceof TransactionProducer) {
                BeanRegistrarUtil.registerBean(
                        defaultListableBeanFactory, pubItem.getBeanName(), AliyunXaRocketMqPublisher.class, beanArgBuilder);
            } else {
                BeanRegistrarUtil.registerBean(
                        defaultListableBeanFactory, pubItem.getBeanName(), AliyunSimpleRocketMqPublisher.class, beanArgBuilder);
            }
        }
    }

    private void topicSubAdminService() throws IOException {
        if (listenerMap == null || listenerMap.isEmpty()) {
            logger.info("没有消息监听者service对象, 不初始化消息消费者对象");
            return;
        }

        List<RocketMqSubProperties> properties = aliyunRocketMqProperties.getSubscribers();
        if (properties == null || properties.isEmpty()) {
            logger.info("没有配置消息消息者的属性, 不初始化消息消费者对象");
            return;
        }
        //获取BeanFactory
        DefaultListableBeanFactory defaultListableBeanFactory =
                (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        RetryConsumFailHandler retryConsumFailHandler = applicationContext.getBean(RetryConsumFailHandler.class);

        Map<String, List<TopicListener>> listenerMapBySubBeanName = MqUtil.topicListenerGroupBySubBean(listenerMap);
        for (RocketMqSubProperties subItem : properties) {
            Properties configProperties = commonProperties();
            configProperties.setProperty(PropertyKeyConst.GROUP_ID, subItem.getGroupId());
            if (StringUtils.hasText(subItem.getSuspendTimeMillis())) {
                configProperties.setProperty(PropertyKeyConst.SuspendTimeMillis, subItem.getSuspendTimeMillis());
            }
            if (StringUtils.hasText(subItem.getMaxReconsumeTimes())) {
                configProperties.setProperty(PropertyKeyConst.MaxReconsumeTimes, subItem.getMaxReconsumeTimes());
            }
            if (StringUtils.hasText(subItem.getConsumeTimeout())) {
                configProperties.setProperty(PropertyKeyConst.ConsumeTimeout, subItem.getConsumeTimeout());
            }
            if (StringUtils.hasText(subItem.getPostSubscriptionWhenPull())) {
                configProperties.setProperty(PropertyKeyConst.PostSubscriptionWhenPull, subItem.getPostSubscriptionWhenPull());
            }
            if (StringUtils.hasText(subItem.getConsumeMessageBatchMaxSize())) {
                configProperties.setProperty(PropertyKeyConst.ConsumeMessageBatchMaxSize, subItem.getConsumeMessageBatchMaxSize());
            }
            if (StringUtils.hasText(subItem.getMaxCachedMessageAmount())) {
                configProperties.setProperty(PropertyKeyConst.MaxCachedMessageAmount, subItem.getMaxCachedMessageAmount());
            }
            if (StringUtils.hasText(subItem.getMaxCachedMessageSizeInMiB())) {
                configProperties.setProperty(PropertyKeyConst.MaxCachedMessageSizeInMiB, subItem.getMaxCachedMessageSizeInMiB());
            }
            Consumer consumer = ONSFactory.createConsumer(configProperties);


            BeanArgBuilder beanArgBuilder = new BeanArgBuilder();
            beanArgBuilder.setConstructorArgs(new Object[]{consumer, subItem});
            // start 方法在MqUtil.setListenerAndStartSub里面调用了
            beanArgBuilder.setDestoryMethodName("close");
            BeanRegistrarUtil.registerBean(
                    defaultListableBeanFactory, subItem.getBeanName(), AliyunSimpleRocketMqSubscriber.class, beanArgBuilder);

            AliyunSimpleRocketMqSubscriber subscriber = applicationContext.getBean(
                    subItem.getBeanName(), AliyunSimpleRocketMqSubscriber.class);

            MqUtil.setListenerAndStartSub(subItem.getBeanName(), subscriber, retryConsumFailHandler, listenerMapBySubBeanName);
        }
    }

    private Properties commonProperties() {
        Properties properties = new Properties();
        // AccessKey 阿里云身份验证，在阿里云服务器管理控制台创建
        properties.put(PropertyKeyConst.AccessKey, aliyunRocketMqProperties.getAccessKeyId());
        // SecretKey 阿里云身份验证，在阿里云服务器管理控制台创建
        properties.put(PropertyKeyConst.SecretKey, aliyunRocketMqProperties.getAccessKeySecret());
        // 设置 TCP 协议接入点，进入控制台的实例详情页面的 TCP 协议客户端接入点区域查看
        properties.put(PropertyKeyConst.NAMESRV_ADDR, aliyunRocketMqProperties.getNameServerAddr());
        return properties;
    }

    private XaTopicMessage createXaTopicMessage(com.aliyun.openservices.ons.api.Message msg) {
        XaTopicMessage topicMessage = new XaTopicMessage();
        topicMessage.setUserProperties(msg.getUserProperties());
        topicMessage.setBussinessKey(msg.getKey());
        topicMessage.setMessageBody(msg.getBody());
        topicMessage.setTags(msg.getTag());
        topicMessage.setTopicName(msg.getTopic());
        topicMessage.setLocalTransactionExecuterId(msg.getUserProperties(XaTopicMessage.LOCALTRANSACTION_EXECUTERID_KEY));

        return topicMessage;
    }

}
