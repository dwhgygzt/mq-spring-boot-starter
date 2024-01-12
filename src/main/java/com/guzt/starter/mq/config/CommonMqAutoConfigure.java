package com.guzt.starter.mq.config;

import com.guzt.starter.mq.service.RetryConsumFailHandler;
import com.guzt.starter.mq.service.TopicListener;
import com.guzt.starter.mq.service.XaTopicLocalTransactionExecuter;
import com.guzt.starter.mq.service.XaTopicPublishChecker;
import com.guzt.starter.mq.service.impl.DefaultRetryConsumFailHandler;
import com.guzt.starter.mq.service.impl.DefaultTopicListenerImpl;
import com.guzt.starter.mq.service.impl.DefaultXaTLTExecuterImpl;
import com.guzt.starter.mq.service.impl.DefaultXaTopicPublishCheckerImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 消息队列公共配置
 *
 * @author guzt
 */
@Configuration
public class CommonMqAutoConfigure {

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

    @Bean
    @ConditionalOnMissingBean
    public XaTopicPublishChecker defaultXaTopicPublishChecker() {
        return new DefaultXaTopicPublishCheckerImpl();
    }
}
