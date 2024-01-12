package com.guzt.starter.mq.properties;

/**
 * 基础消费者配置文件
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
public class BaseSubProperties {

    /**
     * 最大失败重试消费次数 默认3次
     */
    private Integer maxRetryCount = 3;


    public Integer getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(Integer maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }
}
