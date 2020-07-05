# 本组件目的：
考虑到公司同一套系统能支持阿里云上部署和客户内网部署，业务代码不修改的情况下，只修改yml文件配置属性即可迁移。

# 用法
pom.xml 文件引入如下配置
```xml
<!-- 消息队列通用接口 -->
<dependency>
    <groupId>com.middol</groupId>
    <artifactId>mq-spring-boot-starter</artifactId>
    <version>公司阿里云maven仓库中最新版本号</version>
</dependency>

<!-- 如果你使用的是阿里云RocketMq 请引入对应的SDK 因为本系统采用 provid策略 最低限度引入依赖的jar包 

        <dependency>
            <groupId>com.aliyun.openservices</groupId>
            <artifactId>ons-client</artifactId>
        </dependency>
-->

<!-- 如果你使用的是Apache开源的RocketMq 请引入对应的SDK 因为本系统采用 provid策略 最低限度引入依赖的jar包 

       <dependency>
            <groupId>org.apache.rocketmq</groupId>
            <artifactId>rocketmq-client</artifactId>
        </dependency>

        <dependency>
            <groupId>org.apache.rocketmq</groupId>
            <artifactId>rocketmq-acl</artifactId>
        </dependency>
-->

```

application.yml 增加如下配置，用到哪一种配置就加哪一种，没有用到的就不加或者 enable = false，
下面假如用到的是 阿里云RocketMq 服务：
```yaml

##########################对象存储测试########################################
middol:
  mq:
     aliyun:
       rocketmq:
         enable: true
         access-key-id: xxx
         access-key-secret: bbb
         name-server-addr: http://MQ_INST_1666019888766662_BcidZfUM.mq-internet-access.mq-internet.aliyuncs.com:80
         publishers:
           - {beanName: publishService1, groupId: GID_SAAS_DEV, sendMsgTimeoutMillis: 5000}
           - {beanName: xaPublishService1, groupId: GID_XA_SAAS_DEV, sendMsgTimeoutMillis: 5000, messageType: TRANSACTION, checkImmunityTimeInSeconds: 5}
         subscribers:
           - {beanName: subscriberService1, groupId: GID_SAAS_DEV}
           - {beanName: xaSubscriberService1, groupId: GID_XA_SAAS_DEV}
     apache:
       rocketmq:
         enable: false
         access-key-id: xxx
         access-key-secret: bbb
         name-server-addr: localhost:9876
         publishers:
           - {beanName: publishService1, groupId: GID_SAAS_DEV, sendMsgTimeout: 5000}
           - {beanName: xaPublishService1, groupId: GID_XA_SAAS_DEV, sendMsgTimeout: 5000, messageType: TRANSACTION}
         subscribers:
           - {beanName: subscriberService1, groupId: GID_SAAS_DEV}
           - {beanName: xaSubscriberService1, groupId: GID_XA_SAAS_DEV}

##################################################################

```

上面对应了两种消息的配置（普通消息，事务消息），这里的事务消息指的是半消息机制，半消息机制详情可查看阿里云官网
https://help.aliyun.com/document_detail/29548.html?spm=a2c4g.11186623.6.598.62ca4c0709fx2g

事务消息： 消息队列 RocketMQ 版提供类似 X/Open XA 的分布式事务功能，通过消息队列 RocketMQ 版事务消息，能达到分布式事务的最终一致。

#消息发送订阅配置说明
yml文件中一行 {beanName: publishService1, groupId: GID_SAAS_DEV, sendMsgTimeout: 5000}
表示创建一个发送服务的service，在springBoot应用中可以和其他bean一样引入：

```java
@RestController
@RequestMapping("/api/mq")
public class MqTestController {
    /**
     * 普通类型的消息发送服务端 ResourceName 对应yml文件中配置
     */
    @Lazy
    @Resource(name = "publishService1")
    private TopicPublisher topicPublisher;
    
    /**
     * 事务类型的消息发送服务端 ResourceName 对应yml文件中配置
     */
    @Lazy
    @Resource(name = "xaPublishService1")
    private XaTopicPublisher xaTopicPublisher;

    /**
     * 普通消息发送测试.
     *
     * @param message 消息体
     * @return 发送成功
     */
    @PostMapping("singlePush")
    public String singlePush(String message) {
        if (StrUtil.isEmpty(message)) {
            return "消息体不能为空";
        }

        TopicMessage msg = new TopicMessage();
        msg.setTopicName("SAAS-PT");
        msg.setTags("TAG1");
        msg.setBussinessKey(System.currentTimeMillis() + "");
        msg.setMessageBody(message.getBytes(StandardCharsets.UTF_8));
        topicPublisher.publish(msg);

        return "发送成功";
    }

    /**
     * 半消息事务类型消息发送
     *
     * @param message  消息体
     * @param isCommit 是否要提交测试 true 提交  false 不提交，回滚
     * @return 发送成功
     */
    @PostMapping("xaPush")
    public String xaPush(String message, Boolean isCommit) {
        if (StrUtil.isEmpty(message)) {
            return "消息体不能为空";
        }

        XaTopicMessage msg = new XaTopicMessage();
        msg.setLocalTransactionExecuterId(MyXaTopicLocalTransactionExecuter.EXECUTER_ID);
        msg.setTopicName("XA-SAAS");
        msg.setTags("TAG1");
        msg.setBussinessKey(System.currentTimeMillis() + "");
        msg.setMessageBody(message.getBytes(StandardCharsets.UTF_8));
        xaTopicPublisher.publishInTransaction(msg, isCommit);

        return "发送成功";
    }

}

```
为什么要将 beanName 属性进行设置，主要是对应消费服务的配置，消费服务配置继承通用消费服务监听接口TopicListener
不管普通消息还是事务消息，消息消费服务配置一样。
```java

@Component
public class MyMessageListenerService implements TopicListener {
    @Override
    public String getSubscriberBeanName() {
        return "subscriberService1";
    }

    @Override
    public String getTopicName() {
        return "SAAS-PT";
    }

    @Override
    public String getTagExpression() {
        return "*";
    }

    @Override
    public MessageStatus subscribe(TopicMessage topicMessage) {

        System.out.println("消费消息 message body = " + new String(topicMessage.getMessageBody(), StandardCharsets.UTF_8));

        return MessageStatus.CommitMessage;
    }
}

```
# 针对事务消息，必须有一个本地事务执行器，为了执行本地事务和消息的回查

```java
@Component
public class MyXaTopicLocalTransactionExecuter implements XaTopicLocalTransactionExecuter {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private BusinessService businessService;

    public static String EXECUTER_ID = "20201110";

    @Override
    public String getLocalTransactionExecuterId() {
        return MyXaTopicLocalTransactionExecuter.EXECUTER_ID;
    }

    @Override
    public LocalTransactionStatus executeLocalTransaction(XaTopicMessage msg, Object businessParam) {
       // 执行本地事务的业务逻辑 
       if (!(businessParam instanceof Boolean)) {
            return LocalTransactionStatus.UNKNOW;
        }
        return businessService.executeLocalTransaction(msg.getBussinessKey(), (Boolean) businessParam);
    }

    @Override
    public LocalTransactionStatus checkLocalTransaction(XaTopicMessage msg) {
        businessService.checkLocalTransaction(msg.getBussinessKey());
        String testStr = "23";
        if (msg.getBussinessKey().contains(testStr)) {
            logger.info("最终查询事务回滚...");
            return LocalTransactionStatus.ROLLBACK;
        } else {
            logger.info("最终查询事务提交...");
            return LocalTransactionStatus.COMMIT;
        }
    }
}
```

**特别说明 getLocalTransactionExecuterId 这个属性对应事务消息对象里面的 localTransactionExecuterId， 两者值保持一致**

另外针对apache rocketMq 的事务消息回查时间间隔属性值设置是在 消息服务器broker.conf配置文件中增加
transactionCheckInterval = 10 * 1000 
表示消息发送后如果没有收到应用返回的提交或回滚指令，10秒进行回查，默认是1分钟。