package com.middol.starter.mq.service.impl;

import com.middol.starter.mq.pojo.LocalTransactionStatus;
import com.middol.starter.mq.pojo.XaTopicMessage;
import com.middol.starter.mq.service.XaTopicLocalTransactionExecuter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XaTopicLocalTransactionExecuter 的默认实现.
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
 */
public class DefaultXaTltExecuterImpl implements XaTopicLocalTransactionExecuter {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public String getLocalTransactionExecuterId() {
        return "defaultExecuterId";
    }

    @Override
    public LocalTransactionStatus executeLocalTransaction(XaTopicMessage msg, Object businessParam) {
        if (msg != null && logger.isDebugEnabled()) {
            logger.debug("默认本地事务执行 消息key={}，返回 COMMIT...", msg.getBussinessKey());
        }
        return LocalTransactionStatus.COMMIT;
    }

    @Override
    public LocalTransactionStatus checkLocalTransaction(XaTopicMessage msg) {
        if (msg != null && logger.isDebugEnabled()) {
            logger.debug("默认消息key{}回查，返回 COMMIT...", msg.getBussinessKey());
        }
        return LocalTransactionStatus.COMMIT;
    }
}
