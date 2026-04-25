# DDD 总结

DDD 全称：

```text
Domain-Driven Design
```

中文叫：

```text
领域驱动设计
```

核心思想：

```text
以业务领域为核心来设计系统，而不是只围绕数据库表做 CRUD。
```

---

## 1. DDD 解决什么问题

传统开发常见问题：

```text
业务逻辑散落在 Controller / Service
Service 越写越胖
代码只是在操作数据库表
业务概念不清晰
模块边界混乱
系统越来越难维护
```

DDD 解决的是：

```text
复杂业务系统的模型设计、边界划分和代码组织问题。
```

---

## 2. DDD 适合什么场景

适合：

```text
业务复杂
规则多
流程长
系统生命周期长
多人协作
领域概念丰富
需要持续演进
```

例如：

```text
电商
支付
风控
供应链
交易
订单
会员
营销
金融系统
```

不太适合：

```text
简单 CRUD
后台管理小系统
报表系统
业务规则很少的系统
```

---

## 3. DDD 核心概念

| 概念 | 说明 |
|---|---|
| 领域 Domain | 业务范围，例如订单、支付、库存 |
| 子域 Subdomain | 领域下的子业务模块 |
| 限界上下文 Bounded Context | 模型生效的边界 |
| 实体 Entity | 有唯一标识的业务对象 |
| 值对象 Value Object | 没有唯一标识，只关注属性值 |
| 聚合 Aggregate | 一组强一致相关对象 |
| 聚合根 Aggregate Root | 聚合的入口对象 |
| 领域服务 Domain Service | 不适合放到实体中的领域逻辑 |
| 仓储 Repository | 负责聚合的持久化 |
| 领域事件 Domain Event | 领域中已经发生的业务事实 |
| 防腐层 ACL | 隔离外部系统模型污染 |

---

## 4. 领域 Domain

领域就是业务问题空间。

例如电商系统可以拆成：

```text
订单领域
商品领域
库存领域
支付领域
会员领域
营销领域
物流领域
```

DDD 要求开发先理解业务，再设计模型。

---

## 5. 子域 Subdomain

子域是对领域的进一步拆分。

常见子域类型：

| 类型 | 说明 |
|---|---|
| 核心子域 | 企业核心竞争力，重点投入 |
| 支撑子域 | 支撑核心业务，可自研 |
| 通用子域 | 通用能力，可买可用开源 |

例如电商：

```text
核心子域：交易、订单、定价
支撑子域：库存、物流
通用子域：权限、通知、日志
```

---

## 6. 限界上下文

限界上下文是 DDD 中最重要的概念之一。

它表示：

```text
一个业务模型只在特定边界内有效。
```

例如“订单”在不同上下文中含义不同：

```text
交易上下文：订单表示用户购买商品的交易单
物流上下文：订单表示需要配送的运单来源
售后上下文：订单表示可退款、可退货的凭证
```

所以不能用一个大而全的 `Order` 模型覆盖所有场景。

---

## 7. 实体 Entity

实体有唯一标识，关注生命周期和状态变化。

例如：

```text
订单 Order
用户 User
商品 Product
账户 Account
```

特点：

```text
有 ID
可以变化
两个对象即使属性相同，只要 ID 不同，就是不同实体
```

示例：

```java
public class Order {
    private OrderId id;
    private OrderStatus status;
    private Money amount;

    public void pay() {
        if (this.status != OrderStatus.WAIT_PAY) {
            throw new BusinessException("订单状态不允许支付");
        }
        this.status = OrderStatus.PAID;
    }
}
```

---

## 8. 值对象 Value Object

值对象没有唯一标识，只关注属性值。

例如：

```text
金额 Money
地址 Address
手机号 PhoneNumber
时间范围 DateRange
坐标 Location
```

特点：

```text
无 ID
不可变更推荐
通过属性判断相等
可以被实体引用
```

示例：

```java
public class Money {
    private final BigDecimal amount;
    private final String currency;

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new BusinessException("币种不一致");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
}
```

---

## 9. 聚合 Aggregate

聚合是一组业务上强一致的对象集合。

例如订单聚合：

```text
Order 订单
OrderItem 订单明细
OrderAddress 收货地址
```

聚合的作用：

```text
控制一致性边界
避免对象随意被外部修改
```

---

## 10. 聚合根 Aggregate Root

聚合根是聚合的唯一入口。

例如：

```text
Order 是聚合根
OrderItem 不能被外部直接修改
必须通过 Order 修改
```

示例：

```java
order.addItem(productId, quantity);
order.changeAddress(address);
order.pay();
```

不推荐：

```java
orderItem.setQuantity(10);
```

推荐：

```java
order.changeItemQuantity(itemId, 10);
```

---

## 11. 领域服务 Domain Service

有些业务逻辑不适合放在单个实体或值对象中，就放到领域服务。

适合领域服务的逻辑：

```text
跨多个聚合的业务规则
不属于某个实体的领域行为
复杂业务计算
```

示例：

```java
public class OrderPriceDomainService {

    public Money calculate(Order order, Promotion promotion) {
        // 计算订单价格
    }
}
```

注意：

```text
领域服务不是 CRUD Service。
```

---

## 12. 仓储 Repository

Repository 负责聚合的持久化。

核心职责：

```text
保存聚合
查询聚合
屏蔽数据库细节
```

示例：

```java
public interface OrderRepository {

    Order findById(OrderId orderId);

    void save(Order order);
}
```

领域层只依赖接口，不关心 MyBatis、JPA、SQL。

---

## 13. 领域事件 Domain Event

领域事件表示领域中已经发生的事实。

命名通常用过去式：

```text
OrderCreatedEvent
OrderPaidEvent
StockDeductedEvent
PaymentSuccessEvent
```

作用：

```text
解耦业务流程
异步通知其他模块
实现最终一致性
```

示例：

```text
订单支付成功
  ↓
发布 OrderPaidEvent
  ↓
库存服务扣减库存
  ↓
积分服务增加积分
  ↓
通知服务发送消息
```

---

## 14. 防腐层 ACL

ACL 全称：

```text
Anti-Corruption Layer
```

作用：

```text
隔离外部系统模型，避免污染本系统领域模型。
```

例如外部支付系统返回：

```json
{
  "trade_status": "TRADE_SUCCESS",
  "buyer_id": "1001"
}
```

本系统不要直接到处使用外部 DTO，而是转换成自己的领域对象：

```text
PaymentResult
PaymentStatus.SUCCESS
```

---

## 15. DDD 分层架构

常见四层：

```text
接口层 Interface
应用层 Application
领域层 Domain
基础设施层 Infrastructure
```

---

## 16. 接口层

负责接收外部请求。

包括：

```text
Controller
DTO
Request
Response
参数校验
协议适配
```

不应该写复杂业务逻辑。

---

## 17. 应用层

负责编排业务流程。

应用层做：

```text
事务控制
调用领域对象
调用领域服务
调用仓储
发布领域事件
调用外部服务
```

应用层不应该承载核心业务规则。

---

## 18. 领域层

领域层是 DDD 的核心。

包含：

```text
实体
值对象
聚合
聚合根
领域服务
领域事件
仓储接口
```

核心业务规则应该放在领域层。

---

## 19. 基础设施层

负责技术实现。

包括：

```text
数据库访问
MyBatis / JPA
Redis
MQ
HTTP Client
第三方 SDK
Repository 实现
ACL 实现
```

---

## 20. 代码结构示例

```text
com.demo.order
├── interfaces
│   └── OrderController
├── application
│   └── OrderApplicationService
├── domain
│   ├── model
│   │   ├── Order
│   │   ├── OrderItem
│   │   └── Money
│   ├── service
│   │   └── OrderPriceDomainService
│   ├── repository
│   │   └── OrderRepository
│   └── event
│       └── OrderPaidEvent
└── infrastructure
    ├── repository
    │   └── MybatisOrderRepository
    ├── mapper
    │   └── OrderMapper
    └── acl
        └── PaymentClientAdapter
```

---

## 21. DDD 和传统三层区别

| 对比项 | 传统三层 | DDD |
|---|---|---|
| 核心关注 | 数据库表和 CRUD | 业务模型和领域规则 |
| Service | 容易变成大杂烩 | 应用服务编排，领域服务表达规则 |
| Model | 多是贫血对象 | 实体和值对象包含业务行为 |
| 复杂业务 | 容易散落各处 | 收敛到领域模型 |
| 边界 | 模块边界不清晰 | 限界上下文明确 |

---

## 22. 贫血模型和充血模型

### 贫血模型

对象只有字段和 getter/setter。

```java
order.setStatus(PAID);
```

业务规则写在 Service 里。

问题：

```text
业务逻辑分散
对象没有行为
Service 越来越胖
```

---

### 充血模型

对象既有数据，也有业务行为。

```java
order.pay();
```

支付前的状态校验放在 `Order` 内部。

优点：

```text
业务规则更集中
模型表达更清晰
代码更接近业务语言
```

---

## 23. DDD 实战落地步骤

```text
1. 梳理业务流程和核心概念。
2. 识别子域。
3. 划分限界上下文。
4. 设计聚合和聚合根。
5. 定义实体和值对象。
6. 把核心规则放进领域模型。
7. 设计 Repository 接口。
8. 基础设施层实现持久化。
9. 使用领域事件解耦流程。
10. 通过 ACL 隔离外部系统。
```

---

## 24. 常见误区

```text
把 DDD 当成分包规范
所有项目都强行 DDD
实体只有 getter/setter
领域服务写成 CRUD Service
聚合设计过大
跨聚合强事务太多
直接暴露数据库 PO 给领域层
外部 DTO 污染领域模型
```

---

## 25. 总结

DDD 是领域驱动设计，核心思想是以业务领域为中心建模，而不是围绕数据库表写 CRUD。

DDD 中最重要的概念包括领域、子域、限界上下文、实体、值对象、聚合、聚合根、领域服务、仓储、领域事件和防腐层。

实体有唯一标识和值变化，值对象没有唯一标识，通常不可变；聚合是一组强一致对象的集合，聚合根是外部访问聚合的唯一入口；领域服务承载不适合放在实体中的领域逻辑；Repository 负责聚合持久化；领域事件用于解耦业务流程；防腐层用于隔离外部系统模型。

一句话总结：

```text
DDD = 以业务领域为核心，通过限界上下文划边界，通过聚合维护一致性，通过领域模型承载业务规则。
```
