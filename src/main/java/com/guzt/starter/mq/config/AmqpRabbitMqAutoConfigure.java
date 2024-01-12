package com.guzt.starter.mq.config;

import com.guzt.starter.mq.pojo.MessageType;
import com.guzt.starter.mq.properties.amqp.AmqpRabbitMqProperties;
import com.guzt.starter.mq.properties.amqp.publisher.RabbitMqPubProperties;
import com.guzt.starter.mq.properties.amqp.subscriber.RabbitMqSubProperties;
import com.guzt.starter.mq.service.*;
import com.guzt.starter.mq.service.impl.amqp.AmqpRabbitMqPublisher;
import com.guzt.starter.mq.service.impl.amqp.AmqpRabbitMqSubscriber;
import com.guzt.starter.mq.service.impl.amqp.AmqpXaRabbitMqPublisher;
import com.guzt.starter.mq.util.BeanArgBuilder;
import com.guzt.starter.mq.util.BeanRegistrarUtil;
import com.guzt.starter.mq.util.MqUtil;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * 开源Rabbitmq核心配置类
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
@Configuration
@ConditionalOnClass({Channel.class})
@ConditionalOnProperty(prefix = "guzt.mq.amqp.rabbitmq", value = "enable", havingValue = "true")
@EnableConfigurationProperties({AmqpRabbitMqProperties.class})
@AutoConfigureAfter(CommonMqAutoConfigure.class)
public class AmqpRabbitMqAutoConfigure implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private AmqpRabbitMqProperties amqpRabbitMqProperties;

    @Resource
    private ApplicationContext applicationContext;

    /**
     * 消息消费者
     */
    @Autowired
    private Map<String, TopicListener> listenerMap = new ConcurrentHashMap<>(4);

    @Autowired
    private Map<String, XaTopicLocalTransactionExecuter> executerMap = new ConcurrentHashMap<>(2);

    ConnectionFactory connectionFactory;

    @PostConstruct
    public void init() {
        AmqpRabbitMqProperties properties = amqpRabbitMqProperties;
        //创建链接工厂对象
        ConnectionFactory connectionFactory = new ConnectionFactory();
        //设置RabbitMQ服务主机地址,默认localhost
        connectionFactory.setHost(properties.getHost());
        //设置RabbitMQ服务端口,默认5672
        connectionFactory.setPort(properties.getPort());
        //设置虚拟主机名字，默认/
        connectionFactory.setVirtualHost(properties.getVirtualHost());
        //设置用户连接名，默认guest
        connectionFactory.setUsername(properties.getUserName());
        //设置链接密码，默认guest
        connectionFactory.setPassword(properties.getPassword());
        if (properties.getConnectionTimeout() != null) {
            connectionFactory.setConnectionTimeout(properties.getConnectionTimeout());
        }
        if (properties.getShutdownTimeout() != null) {
            connectionFactory.setShutdownTimeout(properties.getShutdownTimeout());
        }
        if (properties.getHandshakeTimeout() != null) {
            connectionFactory.setHandshakeTimeout(properties.getHandshakeTimeout());
        }
        if (properties.getRequestedChannelMax() != null) {
            connectionFactory.setRequestedChannelMax(properties.getRequestedChannelMax());
        }
        if (properties.getRequestedFrameMax() != null) {
            connectionFactory.setRequestedFrameMax(properties.getRequestedFrameMax());
        }
        if (properties.getRequestedHeartbeat() != null) {
            connectionFactory.setRequestedHeartbeat(properties.getRequestedHeartbeat());
        }

        this.connectionFactory = connectionFactory;
    }

    /**
     * rabbitmq 本组件暂时没有实现类似于rocketmq的事务机制
     * 请勿使用 XaTopicLocalTransactionExecuter
     */
    @Override
    public void afterPropertiesSet() throws IOException, TimeoutException {
        Map<String, XaTopicLocalTransactionExecuter> executerIdMap = new ConcurrentHashMap<>(2);
        this.executerMap.forEach((k, v) -> executerIdMap.put(v.getLocalTransactionExecuterId(), v));
        XaTopicPublisherExecuteStrategy.setExecuterMap(executerIdMap);

        // 发布者
        topicPubAdminService();
        // 订阅者
        topicSubAdminService();
    }

    private void topicPubAdminService() throws IOException, TimeoutException {
        List<RabbitMqPubProperties> properties = amqpRabbitMqProperties.getPublishers();
        if (properties == null || properties.isEmpty()) {
            logger.debug("没有配置消息发布者的属性, 不初始化消息发布者对象");
            return;
        }
        //获取BeanFactory
        DefaultListableBeanFactory defaultListableBeanFactory =
                (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();

        for (RabbitMqPubProperties pubItem : properties) {
            //创建链接
            Connection connection = connectionFactory.newConnection();

            BeanArgBuilder beanArgBuilder = new BeanArgBuilder();
            beanArgBuilder.setInitMethodName("start");
            beanArgBuilder.setDestoryMethodName("close");
            // 事务消息检查设置
            if (StringUtils.hasText(pubItem.getMessageType())
                    && pubItem.getMessageType().toUpperCase().equals(MessageType.TRANSACTION.getValue())) {
                beanArgBuilder.setConstructorArgs(new Object[]{connection, pubItem, applicationContext.getBean(XaTopicPublishChecker.class)});
                BeanRegistrarUtil.registerBean(
                        defaultListableBeanFactory, pubItem.getBeanName(), AmqpXaRabbitMqPublisher.class, beanArgBuilder);
            } else {
                beanArgBuilder.setConstructorArgs(new Object[]{connection, pubItem});
                BeanRegistrarUtil.registerBean(
                        defaultListableBeanFactory, pubItem.getBeanName(), AmqpRabbitMqPublisher.class, beanArgBuilder);
            }
        }
    }

    private void topicSubAdminService() throws IOException, TimeoutException {
        if (listenerMap == null || listenerMap.isEmpty()) {
            logger.debug("没有消息监听者service对象, 不初始化消息消费者对象");
            return;
        }

        List<RabbitMqSubProperties> properties = amqpRabbitMqProperties.getSubscribers();
        if (properties == null || properties.isEmpty()) {
            logger.debug("【AmqpRabbitMq】没有配置消息消息者的属性, 不初始化消息消费者对象");
            return;
        }
        //获取BeanFactory
        DefaultListableBeanFactory defaultListableBeanFactory =
                (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        RetryConsumFailHandler retryConsumFailHandler = applicationContext.getBean(RetryConsumFailHandler.class);

        Map<String, List<TopicListener>> listenerMapBySubBeanName = MqUtil.topicListenerGroupBySubBean(listenerMap);

        for (RabbitMqSubProperties subItem : properties) {

            //创建链接
            Connection connection = connectionFactory.newConnection();

            BeanArgBuilder beanArgBuilder = new BeanArgBuilder();
            beanArgBuilder.setConstructorArgs(new Object[]{connection, subItem});
            beanArgBuilder.setInitMethodName("start");
            beanArgBuilder.setDestoryMethodName("close");
            BeanRegistrarUtil.registerBean(defaultListableBeanFactory,
                    subItem.getBeanName(), AmqpRabbitMqSubscriber.class, beanArgBuilder);

            AmqpRabbitMqSubscriber subscriber = applicationContext.getBean(
                    subItem.getBeanName(), AmqpRabbitMqSubscriber.class);
            MqUtil.setListenerAndStartSub(subItem.getBeanName(), subscriber, retryConsumFailHandler, listenerMapBySubBeanName);
        }
    }

}
