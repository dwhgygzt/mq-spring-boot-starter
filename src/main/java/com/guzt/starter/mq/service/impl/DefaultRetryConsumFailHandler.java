package com.guzt.starter.mq.service.impl;

import com.guzt.starter.mq.pojo.Message;
import com.guzt.starter.mq.service.RetryConsumFailHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MQ消费者,尝试了最大次数后失败时的处理者
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
public class DefaultRetryConsumFailHandler implements RetryConsumFailHandler {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void handle(Message message) {
        logger.debug("MQ消费者,尝试了最大次数后失败时的处理方法， 你可以覆盖DefaultRetryConsumFailHandler中的方法，RetryConsumFailHandler： messageId={}", message.getMessageId());
    }
}
