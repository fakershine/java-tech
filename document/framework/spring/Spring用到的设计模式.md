# Spring 用到的设计模式总结

Spring 框架中大量使用设计模式，核心目标是：**降低耦合、增强扩展性、统一对象创建和管理、方便功能增强**。

---

## 1. 工厂模式

### 使用场景

Spring IOC 容器本质上就是一个大工厂，负责创建和管理 Bean。

常见接口：

```text
BeanFactory
ApplicationContext
FactoryBean
```

### 实现原理

业务代码不再自己 `new` 对象，而是交给 Spring 容器创建。

```java
UserService userService = applicationContext.getBean(UserService.class);
```

### 总结

```text
Spring IOC 使用工厂模式统一创建和管理 Bean。
```

---

## 2. 单例模式

### 使用场景

Spring 中 Bean 默认是单例的。

```java
@Service
public class UserService {
}
```

默认作用域：

```text
singleton
```

同一个 Bean 在 Spring 容器中只会创建一个实例。

### 优势

- 减少对象创建开销。
- 方便统一管理 Bean。
- 适合无状态 Service。

### 总结

```text
Spring 默认 Bean 作用域是 singleton，本质上使用了单例模式。
```

---

## 3. 代理模式

### 使用场景

Spring AOP 底层使用代理模式。

常见场景：

- 事务
- 日志
- 权限校验
- 缓存
- 异步
- 方法增强

例如：

```java
@Transactional
public void createOrder() {
}
```

Spring 会为 Bean 创建代理对象，在方法前后增强事务逻辑。

### 实现方式

| 代理方式 | 说明 |
|---|---|
| JDK 动态代理 | 基于接口 |
| CGLIB 代理 | 基于子类继承 |

### 总结

```text
Spring AOP 使用代理模式，在不修改业务代码的情况下增强方法功能。
```

---

## 4. 模板方法模式

### 使用场景

Spring 中很多模板类都使用了模板方法模式。

常见类：

```text
JdbcTemplate
RedisTemplate
RestTemplate
TransactionTemplate
```

### 实现原理

模板类封装固定流程，把可变部分交给回调实现。

以 `JdbcTemplate` 为例：

```text
获取连接
  ↓
创建 Statement
  ↓
执行 SQL
  ↓
处理结果
  ↓
释放资源
```

用户只需要关注 SQL 和结果处理。

### 总结

```text
Spring Template 系列类使用模板方法模式，封装通用流程，开放可变步骤。
```

---

## 5. 策略模式

### 使用场景

Spring 中很多地方会根据不同策略选择不同实现。

常见场景：

- 资源加载策略
- Bean 实例化策略
- AOP 代理策略
- 事务传播行为
- HandlerMapping 匹配策略
- ViewResolver 视图解析策略

例如 AOP 代理：

```text
有接口 -> JDK 动态代理
无接口 -> CGLIB 代理
```

### 总结

```text
Spring 通过策略模式支持不同场景下选择不同实现。
```

---

## 6. 观察者模式

### 使用场景

Spring 事件机制使用观察者模式。

核心组件：

```text
ApplicationEvent
ApplicationListener
ApplicationEventPublisher
```

### 示例

```java
@Component
public class OrderListener implements ApplicationListener<OrderEvent> {

    @Override
    public void onApplicationEvent(OrderEvent event) {
        // 处理订单事件
    }
}
```

发布事件：

```java
applicationContext.publishEvent(new OrderEvent(order));
```

### 总结

```text
Spring 事件发布和监听机制使用观察者模式。
```

---

## 7. 适配器模式

### 使用场景

Spring MVC 中大量使用适配器模式。

典型类：

```text
HandlerAdapter
```

### 实现原理

Spring MVC 中 Controller 有多种写法：

```text
@Controller
HttpRequestHandler
Controller 接口
Servlet
```

不同 Handler 调用方式不同，Spring 通过 `HandlerAdapter` 统一适配调用。

### 总结

```text
Spring MVC 使用 HandlerAdapter 适配不同类型的 Controller。
```

---

## 8. 责任链模式

### 使用场景

Spring 中很多拦截增强流程使用责任链模式。

常见场景：

- Filter 链
- Interceptor 链
- Spring Security 过滤器链
- AOP 拦截器链
- BeanPostProcessor 链

例如 Web Filter：

```text
Filter1
  ↓
Filter2
  ↓
Filter3
  ↓
Controller
```

### 总结

```text
Spring 使用责任链模式实现多个处理器按顺序增强请求或 Bean。
```

---

## 9. 装饰器模式

### 使用场景

Spring 中通过包装对象增强功能，常见于：

- HttpServletRequestWrapper
- HttpServletResponseWrapper
- BeanWrapper
- TransactionAwareDataSourceProxy

### 实现原理

在不改变原对象结构的情况下，包装一层增强能力。

```text
原始对象
  ↓
包装对象
  ↓
增强功能
```

### 总结

```text
Spring 通过装饰器模式在不修改原对象的情况下增强功能。
```

---

## 10. 建造者模式

### 使用场景

Spring 中一些复杂对象创建使用建造者模式。

常见例子：

```text
BeanDefinitionBuilder
UriComponentsBuilder
MockMvcBuilders
```

### 示例

```java
BeanDefinitionBuilder builder =
        BeanDefinitionBuilder.genericBeanDefinition(UserService.class);
```

### 总结

```text
Spring 使用建造者模式创建复杂配置对象。
```

---

## 11. 委派模式

### 使用场景

Spring MVC 中 `DispatcherServlet` 使用了委派模式。

### 实现原理

所有请求先进入 `DispatcherServlet`，再由它委派给不同组件处理。

```text
请求
  ↓
DispatcherServlet
  ↓
HandlerMapping
  ↓
HandlerAdapter
  ↓
Controller
  ↓
ViewResolver
```

### 总结

```text
DispatcherServlet 是前端控制器，负责把请求委派给不同组件处理。
```

---

## 12. 组合模式

### 使用场景

Spring 中一些复合结构使用组合模式。

常见场景：

- CompositePropertySource
- CompositeCacheManager
- HandlerMethodArgumentResolverComposite
- HandlerMethodReturnValueHandlerComposite

### 实现原理

把多个对象组合成一个整体，对外提供统一入口。

```text
Composite
 ├── Handler1
 ├── Handler2
 └── Handler3
```

### 总结

```text
Spring 使用组合模式统一管理多个同类组件。
```

---

## 13. 设计模式对比总结

| 设计模式 | Spring 中的体现 | 核心作用 |
|---|---|---|
| 工厂模式 | BeanFactory、ApplicationContext | 创建和管理 Bean |
| 单例模式 | 默认 singleton Bean | 保证容器内单实例 |
| 代理模式 | AOP、事务 | 方法增强 |
| 模板方法模式 | JdbcTemplate、RedisTemplate | 固定流程复用 |
| 策略模式 | 代理策略、资源加载、事务策略 | 动态选择实现 |
| 观察者模式 | ApplicationEvent、ApplicationListener | 事件发布订阅 |
| 适配器模式 | HandlerAdapter | 适配不同 Controller |
| 责任链模式 | Filter、Interceptor、AOP 链 | 链式增强处理 |
| 装饰器模式 | RequestWrapper、DataSourceProxy | 包装增强对象 |
| 建造者模式 | BeanDefinitionBuilder | 构建复杂对象 |
| 委派模式 | DispatcherServlet | 请求统一分发 |
| 组合模式 | CompositeXXX | 统一管理多个组件 |

---

## 14. 总结

Spring 中使用了大量设计模式。

IOC 容器使用工厂模式创建和管理 Bean，Bean 默认使用单例模式；AOP、事务、缓存等功能使用代理模式；JdbcTemplate、RedisTemplate 等使用模板方法模式；Spring 事件机制使用观察者模式；Spring MVC 中的 HandlerAdapter 使用适配器模式；Filter、Interceptor、AOP 拦截器链使用责任链模式；DispatcherServlet 使用委派模式统一分发请求。

一句话总结：

```text
Spring 的设计模式核心是：工厂管理对象，代理增强功能，模板封装流程，观察者解耦事件，适配器统一调用，责任链完成扩展。
```
