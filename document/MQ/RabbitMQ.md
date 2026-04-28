# RabbitMQ

## 1. RabbitMQ 是什么

RabbitMQ 是一个消息中间件，也叫 **Message Broker**，常用于服务之间的异步通信。

它的核心作用是：

> 接收生产者发送的消息，根据路由规则把消息投递到对应队列，再由消费者消费。

一句话理解：

> RabbitMQ 是一个基于 Exchange、Queue、Binding 的消息队列，用来做异步解耦、削峰填谷、消息路由和可靠投递。

---

## 2. RabbitMQ 适合解决什么问题

### 2.1 应用解耦

没有 MQ：

```text
订单服务 -> 库存服务
订单服务 -> 积分服务
订单服务 -> 短信服务
订单服务 -> 优惠券服务
```

使用 RabbitMQ：

```text
订单服务 -> RabbitMQ -> 库存服务
                    -> 积分服务
                    -> 短信服务
                    -> 优惠券服务
```

订单服务只负责发送订单事件，下游服务自己订阅消费。

---

### 2.2 异步处理

例如下单后：

```text
创建订单
扣库存
发积分
发短信
发优惠券
```

其中发短信、发积分、发优惠券可以异步处理，减少主流程耗时。

---

### 2.3 流量削峰

高并发请求先进入 RabbitMQ，消费者按照自身处理能力慢慢消费：

```text
用户请求 -> RabbitMQ -> Consumer -> 数据库
```

适合：

```text
秒杀
抢购
批量任务
异步通知
```

---

### 2.4 消息路由

RabbitMQ 的 Exchange 支持多种路由模式，适合复杂消息分发：

```text
direct
fanout
topic
headers
```

---

## 3. RabbitMQ 核心角色

| 角色 | 说明 |
|---|---|
| Producer | 生产者，发送消息 |
| Consumer | 消费者，消费消息 |
| Broker | RabbitMQ 服务节点 |
| Exchange | 交换机，负责路由消息 |
| Queue | 队列，负责存储消息 |
| Binding | 绑定关系，连接 Exchange 和 Queue |
| Routing Key | 路由键，Exchange 根据它路由消息 |
| Virtual Host | 虚拟主机，用于资源隔离 |
| Channel | 信道，复用 TCP 连接 |

---

## 4. RabbitMQ 架构

```text
Producer
   ↓
Exchange
   ↓ Binding + Routing Key
Queue
   ↓
Consumer
```

更完整：

```text
+----------+       +----------+       +--------+       +----------+
| Producer | ----> | Exchange | ----> | Queue  | ----> | Consumer |
+----------+       +----------+       +--------+       +----------+
                         |
                    Binding Key
                         |
                   Routing Rules
```

---

## 5. Exchange 交换机

Exchange 是 RabbitMQ 的核心。

生产者一般不会直接把消息发送到 Queue，而是发送到 Exchange。

Exchange 根据以下信息把消息路由到一个或多个 Queue：

```text
Exchange 类型
Routing Key
Binding Key
Header 属性
```

---

## 6. Exchange 类型

### 6.1 Direct Exchange

Direct Exchange 根据 Routing Key 精确匹配。

```text
Routing Key = order.create
Binding Key = order.create
```

匹配成功，消息进入队列。

示例：

```text
Exchange: order.direct

Queue: order.create.queue
Binding Key: order.create

Queue: order.pay.queue
Binding Key: order.pay
```

发送：

```text
routingKey = order.create
```

只会进入：

```text
order.create.queue
```

适合：

```text
明确业务类型路由
订单创建
订单支付
订单取消
```

---

### 6.2 Fanout Exchange

Fanout Exchange 广播消息，不看 Routing Key。

```text
Producer -> Fanout Exchange -> 所有绑定 Queue
```

示例：

```text
系统通知 -> fanout exchange -> 短信队列
                         -> 邮件队列
                         -> 站内信队列
```

适合：

```text
广播通知
缓存刷新
配置变更
多系统订阅同一事件
```

---

### 6.3 Topic Exchange

Topic Exchange 支持通配符匹配。

常见通配符：

| 符号 | 说明 |
|---|---|
| `*` | 匹配一个单词 |
| `#` | 匹配零个或多个单词 |

示例：

```text
routingKey: order.create
bindingKey: order.*
```

可以匹配：

```text
order.create
order.pay
order.cancel
```

示例：

```text
routingKey: user.order.create
bindingKey: user.#
```

可以匹配：

```text
user.order.create
user.order.pay.success
user.login
```

适合：

```text
多级业务分类
日志分类
事件订阅
灵活路由
```

---

### 6.4 Headers Exchange

Headers Exchange 不依赖 Routing Key，而是根据消息 Header 匹配。

适合：

```text
根据多个属性路由
复杂匹配场景
```

实际业务中用得相对少。

---

## 7. Queue 队列

Queue 用来存储消息。

消费者从 Queue 中消费消息。

一个 Queue 可以绑定到多个 Exchange，一个 Exchange 也可以绑定多个 Queue。

```text
Exchange A -> Queue 1
Exchange A -> Queue 2
Exchange B -> Queue 1
```

常见队列参数：

| 参数 | 说明 |
|---|---|
| durable | 队列是否持久化 |
| exclusive | 是否排他队列 |
| autoDelete | 没有消费者后是否自动删除 |
| x-message-ttl | 消息 TTL |
| x-dead-letter-exchange | 死信交换机 |
| x-dead-letter-routing-key | 死信路由键 |
| x-max-length | 队列最大长度 |

---

## 8. Binding 绑定关系

Binding 是 Exchange 和 Queue 之间的绑定关系。

```text
Exchange + Binding Key -> Queue
```

例如：

```text
order.exchange -- order.create --> order.create.queue
order.exchange -- order.pay    --> order.pay.queue
```

生产者发送消息：

```text
routingKey = order.create
```

Exchange 会根据 Binding Key 把消息投递到对应 Queue。

---

## 9. RabbitMQ 消息发送流程

```text
1. Producer 创建连接
2. 创建 Channel
3. 声明 Exchange
4. 声明 Queue
5. 建立 Binding
6. Producer 发送消息到 Exchange
7. Exchange 根据路由规则投递到 Queue
8. Consumer 从 Queue 消费消息
9. Consumer ack 确认消息
```

流程图：

```text
Producer
   ↓ basicPublish
Exchange
   ↓ route
Queue
   ↓ basicConsume / basicGet
Consumer
   ↓ basicAck
RabbitMQ 删除消息
```

---

## 10. Consumer 消费模式

### 10.1 推模式

RabbitMQ 主动推送消息给 Consumer。

常用方式：

```text
basicConsume
```

消费者注册监听器，RabbitMQ 有消息时推送。

---

### 10.2 拉模式

消费者主动拉取消息。

常用方式：

```text
basicGet
```

适合简单测试，不适合高并发消费场景。

---

## 11. 消息确认机制 Ack

Consumer 处理完消息后，需要告诉 RabbitMQ：

```text
这条消息已经处理成功，可以删除了
```

这就是 Ack。

---

## 12. 自动 Ack 和手动 Ack

### 12.1 自动 Ack

消费者收到消息后，RabbitMQ 立即认为消费成功。

```text
autoAck = true
```

缺点：

```text
如果消费者收到消息后宕机，消息可能丢失
```

不推荐核心业务使用。

---

### 12.2 手动 Ack

消费者业务处理成功后，主动 ack。

```text
autoAck = false
```

推荐核心业务使用：

```text
1. 收到消息
2. 执行业务逻辑
3. 业务成功
4. 手动 ack
5. RabbitMQ 删除消息
```

如果处理失败，可以：

```text
basicNack
basicReject
requeue
进入死信队列
```

---

## 13. basicAck、basicNack、basicReject

| 方法 | 说明 |
|---|---|
| basicAck | 确认消息消费成功 |
| basicNack | 否定确认，可批量，可选择是否重新入队 |
| basicReject | 拒绝单条消息，可选择是否重新入队 |

### 13.1 basicAck

```java
channel.basicAck(deliveryTag, false);
```

表示当前消息处理成功。

---

### 13.2 basicNack

```java
channel.basicNack(deliveryTag, false, true);
```

第三个参数 `requeue`：

| requeue | 说明 |
|---|---|
| true | 重新入队 |
| false | 丢弃或进入死信队列 |

---

### 13.3 basicReject

```java
channel.basicReject(deliveryTag, false);
```

只拒绝一条消息。

---

## 14. Publisher Confirm 生产者确认

Consumer Ack 是消费者确认。

Publisher Confirm 是生产者确认。

作用：

```text
确认 Producer 发送的消息是否成功到达 RabbitMQ Broker
```

---

## 15. Publisher Confirm 流程

```text
1. Producer 开启 confirm 模式
2. Producer 发送消息
3. Broker 接收并处理消息
4. Broker 返回 ack / nack
5. Producer 根据结果处理
```

示例：

```text
Producer -> RabbitMQ: publish message
RabbitMQ -> Producer: confirm ack
```

---

## 16. Return Callback

Publisher Confirm 只能确认消息是否到达 Exchange。

如果消息到达 Exchange，但没有路由到任何 Queue，需要使用 Return Callback。

常见场景：

```text
Exchange 存在
Routing Key 错误
没有匹配 Queue
消息无法路由
```

需要开启：

```text
mandatory = true
```

流程：

```text
Producer -> Exchange 成功
Exchange -> Queue 失败
RabbitMQ -> Return Callback
```

---

## 17. RabbitMQ 如何保证消息不丢失

需要从三个阶段看：

```text
生产阶段
Broker 存储阶段
消费阶段
```

---

### 17.1 生产阶段不丢失

措施：

```text
开启 Publisher Confirm
开启 Return Callback
发送失败记录日志
发送失败重试
核心消息落库补偿
```

流程：

```text
Producer 发送消息
   ↓
Broker confirm ack
   ↓
发送成功
```

如果收到 nack 或超时未确认，需要重试或补偿。

---

### 17.2 Broker 存储阶段不丢失

措施：

```text
Exchange 持久化
Queue 持久化
Message 持久化
使用镜像队列或 Quorum Queue
```

队列持久化：

```java
durable = true
```

消息持久化：

```java
deliveryMode = 2
```

注意：

```text
只设置 Queue 持久化不够，消息本身也要持久化。
```

---

### 17.3 消费阶段不丢失

措施：

```text
关闭自动 Ack
业务处理成功后手动 Ack
处理失败 basicNack
失败消息进入死信队列
消费端做好幂等
```

推荐流程：

```text
1. Consumer 收到消息
2. 执行业务逻辑
3. 执行成功 basicAck
4. 执行失败 basicNack，进入重试或死信
```

---

## 18. RabbitMQ 会不会重复消费

会。

常见原因：

```text
消费者处理成功但 ack 失败
消费者处理过程中宕机
网络异常
消息重新入队
手动 nack requeue
消费者超时或连接断开
```

所以 RabbitMQ 通常也要按：

```text
至少消费一次
```

来设计。

消费者必须做幂等。

---

## 19. 消费幂等怎么做

### 19.1 业务唯一 ID

例如：

```text
orderId
payId
requestId
messageId
eventId
```

消费前先判断是否处理过。

---

### 19.2 去重表

```sql
create table mq_consume_record (
    id bigint primary key auto_increment,
    message_id varchar(128) not null,
    consumer_group varchar(128) not null,
    create_time datetime not null,
    unique key uk_msg_group(message_id, consumer_group)
);
```

消费时先插入去重表。

唯一键冲突说明已经消费过。

---

### 19.3 状态机控制

例如订单状态：

```text
待支付 -> 已支付 -> 已发货 -> 已完成
```

如果订单已经是已支付，再收到支付成功消息，直接忽略。

---

### 19.4 Redis SETNX

```text
SETNX messageId 1
EXPIRE messageId 86400
```

适合一致性要求没那么高的场景。

---

## 20. 死信队列 DLQ

死信队列用于存放无法正常消费的消息。

RabbitMQ 中，消息会先进入死信交换机 DLX，再由 DLX 路由到死信队列。

---

## 21. 消息变成死信的常见原因

常见情况：

```text
消息被 reject，并且 requeue=false
消息被 nack，并且 requeue=false
消息 TTL 过期
队列达到最大长度
```

流程：

```text
正常队列
   ↓ 消费失败 / TTL 过期 / 队列满
Dead Letter Exchange
   ↓
Dead Letter Queue
```

---

## 22. 死信队列用途

适合：

```text
保存失败消息
后续人工排查
失败消息补偿
延迟重试
异常告警
```

例如：

```text
order.queue
   ↓ 消费失败
order.dlx
   ↓
order.dead.queue
```

---

## 23. TTL 过期时间

TTL 表示消息存活时间。

可以设置：

```text
消息级 TTL
队列级 TTL
```

### 23.1 队列级 TTL

队列中所有消息统一过期时间。

```text
x-message-ttl = 60000
```

表示 60 秒过期。

---

### 23.2 消息级 TTL

每条消息单独设置过期时间。

```text
expiration = 60000
```

表示该消息 60 秒后过期。

---

## 24. 延迟消息

RabbitMQ 原生常用延迟方案有两种：

```text
TTL + 死信队列
延迟消息插件
```

---

### 24.1 TTL + 死信队列

流程：

```text
Producer -> 延迟队列
消息 TTL 到期
延迟队列 -> DLX
DLX -> 业务队列
Consumer 消费
```

适合：

```text
订单超时取消
支付超时关闭
任务延迟执行
定时通知
```

示例：

```text
order.delay.queue
   TTL 30min
   DLX -> order.close.exchange
   ↓
order.close.queue
```

---

### 24.2 延迟消息插件

RabbitMQ 也可以通过插件实现延迟消息。

常见插件：

```text
rabbitmq_delayed_message_exchange
```

使用方式：

```text
x-delayed-message exchange
x-delay header
```

---

## 25. 工作队列 Work Queue

Work Queue 表示多个消费者竞争消费同一个队列中的消息。

```text
Queue
   ↓
Consumer 1
Consumer 2
Consumer 3
```

特点：

```text
一条消息只会被一个消费者消费
多个消费者分担压力
适合任务分发
```

---

## 26. 发布订阅 Publish / Subscribe

发布订阅通常使用 Fanout Exchange。

```text
Producer -> Fanout Exchange -> Queue A -> Consumer A
                         -> Queue B -> Consumer B
                         -> Queue C -> Consumer C
```

特点：

```text
一条消息广播给多个队列
每个队列都有一份消息
不同消费者各自消费
```

适合：

```text
通知广播
缓存刷新
多系统订阅事件
```

---

## 27. Routing 路由模式

Routing 模式通常使用 Direct Exchange。

```text
Producer -> Direct Exchange
              routingKey=error
                   ↓
              error.queue
```

适合：

```text
根据明确类型分发消息
日志等级
订单事件类型
```

---

## 28. Topic 主题模式

Topic 模式使用 Topic Exchange。

```text
routingKey = order.pay.success
bindingKey = order.*.*
```

适合：

```text
多级事件分类
复杂订阅关系
日志系统
业务事件总线
```

---

## 29. Fair Dispatch 公平分发

默认情况下，RabbitMQ 可能会把消息平均分发给消费者。

如果某个消费者处理很慢，可能导致它积压很多未处理消息。

可以通过 prefetch 控制消费者一次最多拿多少条未确认消息。

```text
prefetch = 1
```

表示消费者处理完一条并 ack 后，RabbitMQ 再投递下一条。

---

## 30. Quorum Queue

Quorum Queue 是 RabbitMQ 推荐用于更高可靠性场景的队列类型之一，使用复制机制提高可用性。

适合：

```text
核心业务消息
高可靠队列
替代传统镜像队列
```

---

## 31. RabbitMQ 高可用

常见高可用方案：

```text
普通集群
Quorum Queue
持久化消息
Publisher Confirm
Consumer Manual Ack
死信队列
生产端补偿
消费端幂等
```

需要注意：

```text
RabbitMQ 集群不是简单地让所有队列数据自动复制到所有节点。
队列高可用需要使用合适的队列类型和复制策略。
```

---

## 32. RabbitMQ 和 Kafka 对比

| 对比项 | RabbitMQ | Kafka |
|---|---|---|
| 模型 | 消息队列 / Broker | 分布式事件流平台 |
| 核心结构 | Exchange + Queue | Topic + Partition |
| 路由能力 | 强，支持多种 Exchange | 相对简单，主要靠 Topic / Key |
| 吞吐量 | 中高 | 很高 |
| 消息顺序 | 单队列内有序 | 单 Partition 内有序 |
| 消息保留 | 消费后通常删除 | 按时间/大小保留 |
| 延迟消息 | TTL + DLX / 插件 | 原生较弱 |
| 消费确认 | Ack 机制清晰 | Offset 提交 |
| 适合场景 | 业务消息、复杂路由、异步任务 | 日志、流处理、大数据管道 |

简单理解：

```text
RabbitMQ 更偏传统业务消息和复杂路由
Kafka 更偏高吞吐事件流和日志管道
```

---

## 33. RabbitMQ 和 RocketMQ 对比

| 对比项 | RabbitMQ | RocketMQ |
|---|---|---|
| 协议 | AMQP 等 | 自定义协议 |
| 路由能力 | 很强 | 一般 |
| 吞吐量 | 中高 | 高 |
| 事务消息 | 支持有限 | 支持较好 |
| 延迟消息 | TTL + DLX / 插件 | 原生支持较好 |
| 顺序消息 | 单队列有序 | 支持顺序消息 |
| 死信队列 | 支持 | 支持 |
| 适合场景 | 复杂路由、异步任务 | 业务消息、事务消息、高并发场景 |

---

## 34. Spring Boot 使用 RabbitMQ

### 34.1 Maven 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

---

### 34.2 配置

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    publisher-confirm-type: correlated
    publisher-returns: true
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 1
```

---

### 34.3 声明 Exchange、Queue、Binding

```java
@Configuration
public class RabbitConfig {

    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_QUEUE = "order.create.queue";
    public static final String ORDER_ROUTING_KEY = "order.create";

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE).build();
    }

    @Bean
    public Binding orderBinding() {
        return BindingBuilder
                .bind(orderQueue())
                .to(orderExchange())
                .with(ORDER_ROUTING_KEY);
    }
}
```

---

### 34.4 发送消息

```java
@Service
public class OrderProducer {

    private final RabbitTemplate rabbitTemplate;

    public OrderProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendOrderMessage(String orderId) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.ORDER_EXCHANGE,
                RabbitConfig.ORDER_ROUTING_KEY,
                orderId
        );
    }
}
```

---

### 34.5 消费消息

```java
@Component
public class OrderConsumer {

    @RabbitListener(queues = RabbitConfig.ORDER_QUEUE)
    public void consume(
            String message,
            Channel channel,
            Message rabbitMessage
    ) throws IOException {
        long deliveryTag = rabbitMessage.getMessageProperties().getDeliveryTag();

        try {
            System.out.println("收到消息：" + message);

            // 处理业务逻辑

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
```

---

## 35. 死信队列配置示例

```java
@Configuration
public class DeadLetterConfig {

    public static final String NORMAL_EXCHANGE = "normal.exchange";
    public static final String NORMAL_QUEUE = "normal.queue";
    public static final String NORMAL_ROUTING_KEY = "normal.key";

    public static final String DLX_EXCHANGE = "dlx.exchange";
    public static final String DLX_QUEUE = "dlx.queue";
    public static final String DLX_ROUTING_KEY = "dlx.key";

    @Bean
    public DirectExchange normalExchange() {
        return new DirectExchange(NORMAL_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue normalQueue() {
        return QueueBuilder.durable(NORMAL_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable(DLX_QUEUE).build();
    }

    @Bean
    public Binding normalBinding() {
        return BindingBuilder
                .bind(normalQueue())
                .to(normalExchange())
                .with(NORMAL_ROUTING_KEY);
    }

    @Bean
    public Binding dlxBinding() {
        return BindingBuilder
                .bind(dlxQueue())
                .to(dlxExchange())
                .with(DLX_ROUTING_KEY);
    }
}
```

---

## 36. RabbitMQ 常见问题

### 36.1 消息丢失

排查方向：

```text
生产者是否开启 Confirm
是否开启 Return Callback
Exchange 是否持久化
Queue 是否持久化
Message 是否持久化
消费者是否自动 Ack
消费失败是否进入死信队列
```

解决：

```text
开启 Publisher Confirm
开启 Return Callback
Queue durable=true
Message deliveryMode=2
手动 Ack
失败进入 DLQ
生产端落库补偿
消费端幂等
```

---

### 36.2 消息重复消费

原因：

```text
消费成功但 ack 失败
消费者宕机
网络异常
nack requeue
Broker 重新投递
```

解决：

```text
业务唯一 ID
去重表
Redis SETNX
状态机控制
```

---

### 36.3 消息积压

原因：

```text
消费者处理太慢
消费者数量太少
下游数据库慢
下游 HTTP 接口慢
prefetch 设置过大
单条异常消息反复重试
```

解决：

```text
增加消费者实例
优化消费逻辑
提高消费线程数
降低 prefetch
排查慢 SQL
失败消息进 DLQ
批量处理
```

---

### 36.4 消息无法路由

原因：

```text
Exchange 不存在
Routing Key 错误
Binding Key 不匹配
Queue 没有绑定
mandatory 未开启导致消息丢失感知不到
```

解决：

```text
检查 Exchange
检查 Queue
检查 Binding
检查 Routing Key
开启 publisher-returns
```

---

### 36.5 消费者收不到消息

排查：

```text
Queue 名称是否正确
Exchange 和 Queue 是否绑定
Routing Key 是否匹配
消费者是否启动
消费者 ack 是否阻塞
队列是否有消息
是否被其他消费者消费
```

---

## 37. RabbitMQ 面试常问问题

### 37.1 RabbitMQ 如何保证消息不丢失？

从三个阶段回答：

```text
生产阶段：
  开启 Publisher Confirm
  开启 Return Callback
  发送失败重试或落库补偿

Broker 阶段：
  Exchange 持久化
  Queue 持久化
  Message 持久化
  使用 Quorum Queue 提高可用性

消费阶段：
  关闭自动 Ack
  业务成功后手动 Ack
  失败进入死信队列
  消费端做好幂等
```

---

### 37.2 RabbitMQ 为什么会重复消费？

因为 RabbitMQ 为了保证消息不丢，消费者没有 ack 时消息可能重新投递。

常见原因：

```text
消费者处理成功但 ack 失败
消费者宕机
网络异常
basicNack requeue=true
连接断开
```

解决方式：

```text
消费端做幂等
```

---

### 37.3 RabbitMQ 的 Exchange 有哪些类型？

常见类型：

```text
Direct：精确匹配 Routing Key
Fanout：广播给所有绑定队列
Topic：通配符匹配 Routing Key
Headers：根据 Header 匹配
```

---

### 37.4 RabbitMQ 死信队列是什么？

死信队列用于接收无法正常消费的消息。

消息进入死信队列的原因：

```text
消息被 reject/nack 且 requeue=false
消息 TTL 过期
队列达到最大长度
```

用途：

```text
失败补偿
异常排查
延迟重试
告警处理
```

---

### 37.5 RabbitMQ 如何实现延迟消息？

常见方式：

```text
TTL + 死信队列
延迟消息插件
```

TTL + 死信队列流程：

```text
消息进入延迟队列
TTL 到期后成为死信
投递到死信交换机
进入业务队列
消费者消费
```

---

### 37.6 RabbitMQ 如何处理消息积压？

思路：

```text
1. 查看队列 Ready 和 Unacked 数量
2. 判断是生产太快还是消费太慢
3. 查看消费者是否正常
4. 查看消费逻辑是否慢
5. 查看数据库、Redis、HTTP 下游是否慢
6. 增加消费者实例
7. 优化消费逻辑
8. 异常消息进入死信队列
```

---

## 38. 总结

RabbitMQ 是一个消息中间件，主要用于应用解耦、异步处理、流量削峰和复杂消息路由。它的核心模型是 Producer 把消息发送到 Exchange，Exchange 根据自身类型、Routing Key 和 Binding Key 把消息路由到一个或多个 Queue，Consumer 再从 Queue 中消费消息。

RabbitMQ 常见 Exchange 类型有 Direct、Fanout、Topic 和 Headers。Direct 是精确匹配路由键，Fanout 是广播，Topic 支持通配符匹配，Headers 根据消息头匹配。

可靠性方面，生产者可以通过 Publisher Confirm 确认消息是否到达 Broker，通过 Return Callback 感知消息是否路由到队列。Broker 侧需要保证 Exchange、Queue 和 Message 都持久化，核心场景可以使用 Quorum Queue。消费者侧一般关闭自动 Ack，业务处理成功后手动 Ack，失败时 Nack 或 Reject，并结合死信队列做失败补偿。

RabbitMQ 可能会出现重复消费，比如消费者处理成功但 Ack 失败、消费者宕机、网络异常等，所以消费端必须做幂等。幂等可以通过业务唯一 ID、去重表、Redis SETNX 或状态机控制实现。

---

## 39. 一句话总结

> RabbitMQ 是一个基于 Exchange、Queue、Binding 的消息中间件，优势是路由能力强、模型灵活、适合业务异步解耦；使用时重点关注消息可靠投递、手动 Ack、死信队列、延迟消息、重复消费幂等和消息积压处理。
