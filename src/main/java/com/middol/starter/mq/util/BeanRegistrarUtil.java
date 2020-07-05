package com.middol.starter.mq.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.AliasRegistry;
import org.springframework.util.StringUtils;

import static org.springframework.util.ObjectUtils.containsElement;
import static org.springframework.util.StringUtils.hasText;

/**
 * 工具类
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
 */
public class BeanRegistrarUtil {

    private static final Log log = LogFactory.getLog(BeanRegistrarUtil.class);

    /**
     * Register Bean
     *
     * @param beanDefinitionRegistry {@link BeanDefinitionRegistry}
     * @param beanType               the type of bean
     * @param beanName               the name of bean
     * @param beanArgBuilder         the args of bean
     * @return if it's a first time to register, return <code>true</code>, or <code>false</code>
     */
    public static boolean registerBean(
            BeanDefinitionRegistry beanDefinitionRegistry, String beanName, Class<?> beanType, BeanArgBuilder beanArgBuilder) {

        if (beanDefinitionRegistry.containsBeanDefinition(beanName)) {
            return false;
        }

        // 构造函数，初始化 、销毁方法设置
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(beanType);
        if (beanArgBuilder != null) {
            if (beanArgBuilder.getConstructorArgs() != null && beanArgBuilder.getConstructorArgs().length > 0) {
                for (Object object : beanArgBuilder.getConstructorArgs()) {
                    beanDefinitionBuilder.addConstructorArgValue(object);
                }
            }
            if (StringUtils.hasText(beanArgBuilder.getInitMethodName())) {
                beanDefinitionBuilder.setInitMethodName(beanArgBuilder.getInitMethodName());
            }
            if (StringUtils.hasText(beanArgBuilder.getDestoryMethodName())) {
                beanDefinitionBuilder.setDestroyMethodName(beanArgBuilder.getDestoryMethodName());
            }
        }

        AbstractBeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();
        beanDefinitionRegistry.registerBeanDefinition(beanName, beanDefinitionBuilder.getBeanDefinition());

        if (log.isInfoEnabled()) {
            if (beanDefinition instanceof RootBeanDefinition) {
                log.info("The Infrastructure bean definition [" + beanDefinition
                        + "with name [" + beanName + "] has been registered.");
            }
        }

        return true;
    }

    /**
     * Detect the alias is present or not in the given bean name from {@link AliasRegistry}
     *
     * @param registry {@link AliasRegistry}
     * @param beanName the bean name
     * @param alias    alias to test
     * @return if present, return <code>true</code>, or <code>false</code>
     */
    public static boolean hasAlias(AliasRegistry registry, String beanName, String alias) {
        return hasText(beanName) && hasText(alias) && containsElement(registry.getAliases(beanName), alias);
    }
}
