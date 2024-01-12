# 本组件目的：

考虑到公司同一套系统能支持阿里云上部署和客户内网部署，业务代码不修改的情况下，只修改yml文件配置属性即可迁移，
抽取各主流mq产品框架的共同点为统一一套接口。

# 有哪些组件？

目前包括如下：

- rocketmq （阿里云 ， apche开源）
- rabbitmq （开源） 2021-02-10 增加

# 消息队列组件使用场景

例如常见场景：

用户创建支付产品订单动作---》订单系统、支付中心系统、商品库存系统、实时报表系统等 都需要及时知晓该动作信息。

- 用户完成创建支付产品订单动作 消息发布者
- 订单系统 消息监听组1
- 商品库存系统 消息监听组2
- 支付中心系统 消息监听组3
- 实时报表系统 消息监听组4
- 各微服务里面的具体应用看成监听组里面的真正的消息消费者
- 每一个监听组里面的消费者采用负载均衡策略，每一条消息只能被同一个组里面的一个消费者消费
- 创建支付订单这个动作统一看成是一个 消息主题（Payment Topic）

# 底层采用rabbitmq

**pom 文件引入如下配置：**

```xml

<dependencys>

    <dependency>
        <groupId>com.guzt</groupId>
        <artifactId>mymq-spring-boot-starter</artifactId>
        <version>1.3-SNAPSHOT</version>
    </dependency>
    <!-- 因为本系统采用 provid策略 最低限度引入依赖的jar包 -->
    <dependency>
        <groupId>com.rabbitmq</groupId>
        <artifactId>amqp-client</artifactId>
        <version>5.16.0</version>
    </dependency>

</dependencys>
```

**application.yml 增加如下配置，用到哪一种配置就配置哪一种，没有用到的就不加或者 enable = false**

```yaml

########################## Rabbitmq测试 ########################################
guzt:
  mq:
    amqp:
      rabbitmq:
        enable: true
        user-name: xxx
        password: bbb
        host: 192.168.10.66
        port: 5672
        virtualHost: /
        publishers:
          - { beanName: publishService1, exchangeName: topic_pay_test, groupId: group1 }
          - { beanName: publishService2, exchangeName: topic_pay_test2, groupId: group2 }
        subscribers:
          - { beanName: subscriberService1, exchangeName: topic_pay_test, groupId: group1 }
          - { beanName: subscriberService2, exchangeName: topic_pay_test2, groupId: group2 }

##################################################################

```

yml文件中一行 {beanName: publishService1, exchangeName: topic_pay_test, groupId: group1}
表示创建一个发送/消费服务的service，在springBoot应用中可以和其他bean一样引入使用。

**其中groupId 表示一个群组，rabbitmq中主要针对 消息订阅者， 这个groupId 就是队列的名称**

为什么有 beanName 属性进行设置？

- 1 是为了支持一个应用可以配置多个消息发布/消费者
- 2 是一个消息消费者一一对应一个具体的TopicListener接口实现Bean，一一对应的关系体现在beanName是否相同

**消息发布者：**
ymal文件配置好后，可以直接在controller或service层bean里面引用消息发布者bean

```java

@RestController
@RequestMapping("/api/mq")
public class MqTestController {
    /**
     * 订单类型消息发布者， 普通类型的消息发送服务端 ResourceName 对应yml文件中配置
     */
    @Lazy
    @Resource(name = "publishService1")
    private TopicPublisher topicPublisher1;

    /**
     * 用户中心类型消息发布者， 普通类型的消息发送服务端 ResourceName 对应yml文件中配置
     */
    @Lazy
    @Resource(name = "publishService2")
    private TopicPublisher topicPublisher2;

    /**
     * 普通消息发送测试.
     *
     * @param message 消息体
     * @return 发送成功
     */
    @PostMapping("push")
    public String singlePush(String message) {
        if (StrUtil.isEmpty(message)) {
            return "消息体不能为空";
        }

        // 模拟创建订单消息
        TopicMessage msg1 = new TopicMessage();
        msg1.setTopicName("PAY_ORDER");
        msg1.setTags("CREATE");
        msg1.setBussinessKey(System.currentTimeMillis() + "");
        msg1.setMessageBody(message.getBytes(StandardCharsets.UTF_8));
        topicPublisher1.publish(msg1);

        // 模拟删除订单消息
        TopicMessage msg2 = new TopicMessage();
        msg2.setTopicName("PAY_ORDER");
        msg2.setTags("DELETE");
        msg2.setBussinessKey(System.currentTimeMillis() + "");
        msg2.setMessageBody(message.getBytes(StandardCharsets.UTF_8));
        topicPublisher1.publish(msg2);

        // 模拟用户注册消息 
        TopicMessage msg3 = new TopicMessage();
        msg3.setTopicName("USER_CENTER");
        msg3.setTags("ADD");
        msg3.setBussinessKey(System.currentTimeMillis() + "");
        msg3.setMessageBody(message.getBytes(StandardCharsets.UTF_8));
        topicPublisher2.publish(msg3);


        return "发送3条消息成功";
    }

}

```

**消息监听：**

1. 订阅者 MyMessageListenerService 订阅订单中心类的主题消息（PAY_ORDER），且订阅所有动作消息（*）
2. 订阅者 MyMessageListenerService2 订阅用户中心类的主题消息（USER_CENTER），且只订阅注册、更新动作消息（ADD||UPDATE）
3. 请确保getSubscriberBeanName方法返回值和ymal文件中消息消费者中的beanName一致！

```java

@Component
public class MyMessageListenerService implements TopicListener {

    @Override
    public String getSubscriberBeanName() {
        // 请确保和ymal文件中消息消费者中的beanName一致！！！！！！！！
        return "subscriberService1";
    }

    @Override
    public String getTopicName() {
        // 订阅订单中心类的主题消息
        return "PAY_ORDER";
    }

    @Override
    public String getTagExpression() {
        // 订阅所有动作消息
        return "*";
    }

    @Override
    public MessageStatus subscribe(TopicMessage topicMessage) {
        System.out.println("消费消息：" + new String(topicMessage.getMessageBody(), StandardCharsets.UTF_8));
        return MessageStatus.CommitMessage;
    }
}

```

```java

@Component
public class MyMessageListenerService2 implements TopicListener {

    @Override
    public String getSubscriberBeanName() {
        // 请确保和ymal文件中消息消费者中的beanName一致！！！！！！！！
        return "subscriberService2";
    }

    @Override
    public String getTopicName() {
        // 用户中心类的主题消息
        return "USER_CENTER";
    }

    @Override
    public String getTagExpression() {
        // 只订阅tag为 ADD  和  UPDATE 两种消息
        return "ADD||UPDATE";
    }

    @Override
    public MessageStatus subscribe(TopicMessage topicMessage) {
        System.out.println("消费消息： " + new String(topicMessage.getMessageBody(), StandardCharsets.UTF_8));
        return MessageStatus.CommitMessage;
    }
}
```

# 消息订阅关系一致性

订阅关系一致指的是：
**同一个消费者Group ID下所有Consumer实例所订阅的Topic、Group ID、Tag必须完全一致**

一旦订阅关系不一致，消息消费的逻辑就会混乱，甚至导致消息丢失，Rabbitmq、Rocketmq消息订阅者均需要保持订阅关系一致性。

详细信息请查看阿里云官网文档：
https://help.aliyun.com/document_detail/43523.html?spm=a2c4g.11186623.6.731.30093227P1Qhed

# 底层采用rocketmq

**消息发布和订阅代码均无需做任何修改！** ,只有pom.xml和yml文件中连接消息服务器的配置需要做少量修改。

pom.xml 文件引入如下配置

```xml

<dependencies>
    <!-- 消息队列通用接口 -->
    <dependency>
        <groupId>com.guzt</groupId>
        <artifactId>mymq-spring-boot-starter</artifactId>
        <version>1.3-SNAPSHOT</version>
    </dependency>

    <!-- 如果你使用的是阿里云RocketMq 请引入对应的SDK 因为本系统采用 provid策略 最低限度引入依赖的jar包  -->

    <!-- 阿里云 RockMq -->
    <dependency>
        <groupId>com.aliyun.openservices</groupId>
        <artifactId>ons-client</artifactId>
        <version>${aliyun.ons.client.version}</version>
        <scope>provided</scope>
    </dependency>

    <!-- 如果你使用的是Apache开源的RocketMq 请引入对应的SDK 因为本系统采用 provid策略 最低限度引入依赖的jar包 -->
    <!-- 社区版本 RocketMQ -->
    <dependency>
        <groupId>org.apache.rocketmq</groupId>
        <artifactId>rocketmq-client</artifactId>
        <scope>provided</scope>
        <version>${apache.rocketmq.version}</version>
    </dependency>
    <dependency>
        <groupId>org.apache.rocketmq</groupId>
        <artifactId>rocketmq-acl</artifactId>
        <scope>provided</scope>
        <version>${apache.rocketmq.version}</version>
    </dependency>
</dependencies>
```

yaml 文件中相较于 rabbitmq 修改用户名密码，mq服务器地址即可。

`注意： 这里的groupId 需要事先在rocketmq服务端手动创建完成。`

```yaml

########################## RocketMQ测试 ########################################
guzt:
  mq:
    aliyun:
      rocketmq:
        enable: true  # enable为true表示启用该功能
        access-key-id: xxx
        access-key-secret: bbb
        name-server-addr: https://xxxxx.mq-internet-access.mq-internet.aliyuncs.com:80
        publishers:
          - { beanName: publishService1, exchangeName: topic_pay_test, groupId: group1 }
          - { beanName: publishService2, exchangeName: topic_pay_test2, groupId: group2 }
        subscribers:
          - { beanName: subscriberService1, exchangeName: topic_pay_test, groupId: group1 }
          - { beanName: subscriberService2, exchangeName: topic_pay_test2, groupId: group2 }
    apache:
      rocketmq:
        enable: false  # enable为false表示不启用该功能
        access-key-id: xxx
        access-key-secret: bbb
        name-server-addr: 192.168.10.99:9876
        publishers:
          - { beanName: publishService1, exchangeName: topic_pay_test, groupId: group1 }
          - { beanName: publishService2, exchangeName: topic_pay_test2, groupId: group2 }
        subscribers:
          - { beanName: subscriberService1, exchangeName: topic_pay_test, groupId: group1 }
          - { beanName: subscriberService2, exchangeName: topic_pay_test2, groupId: group2 }

##################################################################

```



# 事务消息 

上面创建的消息发送者均发送的是普通类消息，这里的发送事务消息指的是指半消息机制功能，

半消息机制阐述可查看阿里云官网：
https://help.aliyun.com/document_detail/29548.html?spm=a2c4g.11186623.6.598.62ca4c0709fx2g

简单说就是【发送消息给消费者】 和  【执行本地数据库事务】 要么一起成功要失败全部取消

半消息机制提供类似 X/Open XA 的分布式事务功能，通过消息队列 RocketMQ 版事务消息，能达到分布式事务的最终一致。
针对事务消息，必须有一个本地事务执行器，为了执行本地事务和**消息的回查**

RocketMQ 官方版本和开源版本均支持半消息机制，RabbitMQ本身没有这个功能，我们采用了死信队列机制模拟了半消息机制。

（1）yml文件中信息如下，主要修改消息创建者，消息消费者不变：
```yaml
guzt:
  mq:
    amqp:
      rabbitmq:
        enable: true
        user-name: xxx
        password: bbb
        host: 192.168.10.66
        port: 5672
        virtualHost: /
        publishers:
          - { beanName: xaTopicPublisher, exchangeName: EX_XATEST_MSG_DEV, groupId: GROUP_XATEST_MSG_DEV, messageType: TRANSACTION, checkImmunityTimeInSeconds: 5, checkImmunityMaxCount: 3 }
        subscribers:
          - { beanName: xaTopicSubscriber, exchangeName: EX_XATEST_MSG_DEV, groupId: GROUP_XATEST_MSG_DEV }

```
和普通类消息配置区别就是 设置了 messageType 和  checkImmunityTimeInSeconds 、checkImmunityMaxCount 信息。

- messageType: 默认为SIMPLE表示普通消息，TRANSACTION表示事务消息

- checkImmunityTimeInSeconds: 设置消息服务端询问本地事务执行器刚刚发送的半消息是否可以发送给消费者的时间间隔，单位秒

- checkImmunityMaxCount: 最大回查询问本地事务执行器的次数

（2） 创建本地事务执行器

集成接口 XaTopicLocalTransactionExecuter 即可创建一个事务执行器，作用有2个：
1：执行本地数据库事务
2：用于消息服务端回查本地事务状态，LocalTransactionStatus枚举值 如果为COMMIT 则消息可以被消费者监听到
```java

@Component
public class MyXaTopicLocalTransactionExecuter implements XaTopicLocalTransactionExecuter {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private BusinessService businessService;

    // 事务执行器的唯一编号
    public static String EXECUTER_ID = "myXaTopicLocalTransactionExecuter";

    @Override
    public String getLocalTransactionExecuterId() {
        // 事务执行器的唯一编号，发送事务消息时，消息所包含的属性值需要和其一致
        return MyXaTopicLocalTransactionExecuter.EXECUTER_ID;
    }

    @Override
    public LocalTransactionStatus executeLocalTransaction(XaTopicMessage msg, Object businessParam) {
        // 执行本地事务的业务逻辑，这里的 businessParam 表示本地事务方法的参数
        // 这里校验一下：方法参数必须为Boolean
        if (!(businessParam instanceof Boolean)) {
            return LocalTransactionStatus.UNKNOW;
        }
        return businessService.executeLocalTransaction(msg.getBussinessKey(), (Boolean) businessParam);
    }

    @Override
    public LocalTransactionStatus checkLocalTransaction(XaTopicMessage msg) {
        // 回查本地事务状态，这里模拟了一下可能提交 可能回滚 2种结果
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

（3）发送事务消息
```java
@RestController
@RequestMapping("/api/mq")
public class MqTestController {
    @Lazy
    @Resource
    private XaTopicPublisher xaTopicPublisher;
    
    @PostMapping("pushXaMessage")
    public String pushXaMessage(String bussinessKey, 
                                String message, 
                                String tag, 
                                Boolean businessParam) {
        XaTopicMessage msg3 = new XaTopicMessage();
        msg3.setLocalTransactionExecuterId(MyXaTopicLocalTransactionExecuter.EXECUTER_ID);
        msg3.setTopicName("TOPIC_NAME");
        msg3.setTags(tag);
        msg3.setBussinessKey(bussinessKey);
        msg3.setMessageBody(message.getBytes(StandardCharsets.UTF_8));
        xaTopicPublisher.publishInTransaction(msg3, businessExecResult);
        
        return "事务消息发送成功";
    }
}
```
主要看下，事务消息实体类为 XaTopicMessage， 需设置属性setLocalTransactionExecuterId 和本地事务执行器中的执行器ID值保持一致
发送事务消息的方法是 publishInTransaction();

（4）针对RabbitMQ 可扩展一下消息发送回查功能

由于rabbitmq 本身没有半消息的事务机制，针对是否消息一定发成功的判断功能需实现一个接口 [XaTopicPublishChecker.java]
```java
/**
 * 主要用于rabbitmq 事务消息，用来检测消息是否真的发送成功.
 * 发布（pub）模式
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
public interface XaTopicPublishChecker {

    /**
     * 用来缓存已经发送成功的消息信息，缓存一段时间
     *
     * @param topicMessage 事务消息
     */
    void cacheCommit(XaTopicMessage topicMessage);

    /**
     * 用来缓存已经发送成功的消息信息，缓存一段时间
     *
     * @param topicMessage 事务消息
     */
    void cacheRollback(XaTopicMessage topicMessage);

    /**
     * 用来判断是否存在缓存中，如果存在表示消息一定一定已经发送成功了，
     * 否则消息可能没用发送成功，例如：提交的时候异常了
     *
     * @param topicMessage 事务消息
     * @return true 存在   false 不存在
     */
    boolean isCommitExists(XaTopicMessage topicMessage);

    /**
     * 用来判断是否存在缓存中，如果存在表示消息一定一定已经发送成功了，
     * 否则消息可能没用发送成功，例如：提交的时候异常了
     *
     * @param topicMessage 事务消息
     * @return true 存在   false 不存在
     */
    boolean isRollbackExists(XaTopicMessage topicMessage);

    /**
     * 删除缓存消息
     *
     * @param topicMessage 事务消息
     */
    void deleteCommitCache(XaTopicMessage topicMessage);

    /**
     * 删除缓存消息
     *
     * @param topicMessage 事务消息
     */
    void deleteRollbackCache(XaTopicMessage topicMessage);
}
```
默认是有实现的，针对单机环境下的应用是可以的，但是如果是负载均衡情况下

如果不采用redis实现，会出现消息重复投递的可能，

当然如果你做好消费端幂等也可以不用管这个接口。

这里给出一个简单的实现例子：消息明确发送成功或者取消发送的消息缓存一10秒钟，方便回查快速判断！
```java
@Service
public class MyXaTopicPublishChecker implements XaTopicPublishChecker {

    @Lazy
    @Resource
    protected RedisCacheService myCacheService;

    @Override
    public void cacheCommit(XaTopicMessage topicMessage) {
        myCacheService.setObject("XA_MQ_COMMIT:" + topicMessage.getMessageId(), System.currentTimeMillis(), 10L, TimeUnit.SECONDS);
    }

    @Override
    public void cacheRollback(XaTopicMessage topicMessage) {
        myCacheService.setObject("XA_MQ_ROLLBACK:" + topicMessage.getMessageId(), System.currentTimeMillis(), 10L, TimeUnit.SECONDS);
    }

    @Override
    public boolean isCommitExists(XaTopicMessage topicMessage) {
        return myCacheService.isExists("XA_MQ_COMMIT:" + topicMessage.getMessageId());
    }

    @Override
    public boolean isRollbackExists(XaTopicMessage topicMessage) {
        return myCacheService.isExists("XA_MQ_ROLLBACK:" + topicMessage.getMessageId());
    }

    @Override
    public void deleteCommitCache(XaTopicMessage topicMessage) {
        myCacheService.removeObject("XA_MQ_COMMIT:" + topicMessage.getMessageId());
    }

    @Override
    public void deleteRollbackCache(XaTopicMessage topicMessage) {
        myCacheService.removeObject("XA_MQ_ROLLBACK:" + topicMessage.getMessageId());
    }
}
```

# 关于集群消费还是广播消费

- 默认为**集群消息**，本组件暂没有支持广播消费
- 同一个group组下的消费者**负载均衡的方式**消费同一个topic消息
- 每一个消费者可以订阅多个topic消息


# 消费失败重试机制

消费者配置中有 最大重试次数配置 maxRetryCount 默认 3次 , 如果是rabbitmq 则还可以配置每次失败后下次消费的时间间隔
retryConsumIntervalSeconds

```yaml

subscribers:
  - { beanName: subscriberService1, exchangeName: topic_pay_test, groupId: group1, maxRetryCount: 4, retryConsumIntervalSeconds: 50 }
  - { beanName: subscriberService2, exchangeName: topic_pay_test2, groupId: group2, maxRetryCount: 8 }

```

如果尝试了最大次数后依然没有被消费成功，则会将消息推送到接口 RetryConsumFailHandler 上

```java

package com.guzt.starter.mq.service;


import com.guzt.starter.mq.pojo.Message;

/**
 * MQ消费者,尝试了最大次数后失败时的处理者
 *
 * @author <a href="mailto:gzt19881123@163.com">guzhongtao</a>
 */
public interface RetryConsumFailHandler {

    /**
     * 处理
     *
     * @param message 消费失败的消息
     */
    void handle(Message message);
}

```

默认有实现，就是直接打印错误日志，然后消费提交，你可以覆盖此接口实现自己的业务逻辑。

```java
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

```

如果你要覆盖此方法，变成自己的业务逻辑，只需在你的业务方法中自行实现接口 RetryConsumFailHandler

```java
package com.xxx.mybusiness.mq;

import com.guzt.starter.mq.pojo.Message;
import com.guzt.starter.mq.service.RetryConsumFailHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MyRetryConsumFailHandler implements RetryConsumFailHandler {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void handle(Message message) {
        // 这里实现你的业务逻辑
        mailService.failWarn(message);
    }
}

```