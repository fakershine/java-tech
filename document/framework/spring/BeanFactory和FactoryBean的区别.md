# BeanFactory 和 FactoryBean 的区别

## 1. 核心区别

| 对比项 | BeanFactory | FactoryBean |
|---|---|---|
| 类型 | Spring 容器接口 | Spring Bean 的一种扩展接口 |
| 作用 | 管理 Bean | 用来创建复杂 Bean |
| 角色 | 容器 | 工厂 Bean |
| 使用方 | Spring 框架底层使用 | 开发者 / 框架扩展使用 |
| 获取对象 | `getBean()` | `getObject()` |
| 常见场景 | IOC 容器、Bean 管理 | MyBatis Mapper、Feign Client、代理对象创建 |

一句话：

> `BeanFactory` 是 Spring 容器；`FactoryBean` 是一个特殊的 Bean，用来生产其他 Bean。

---

## 2. BeanFactory 是什么

`BeanFactory` 是 Spring IOC 容器的顶层接口。

它负责：

```text
创建 Bean
管理 Bean
获取 Bean
依赖注入
生命周期管理
```

常见方法：

```java
Object getBean(String name);

<T> T getBean(Class<T> requiredType);

boolean containsBean(String name);
```

示例：

```java
BeanFactory beanFactory = applicationContext;

UserService userService = beanFactory.getBean(UserService.class);
```

可以理解为：

```text
BeanFactory = Spring 的 Bean 容器
```

---

## 3. FactoryBean 是什么

`FactoryBean` 是 Spring 提供的一个扩展接口。

它本身也是一个 Bean，但它的作用不是直接作为普通 Bean 使用，而是**负责创建另一个对象**。

接口定义大致如下：

```java
public interface FactoryBean<T> {

    T getObject() throws Exception;

    Class<?> getObjectType();

    default boolean isSingleton() {
        return true;
    }
}
```

核心方法：

| 方法 | 说明 |
|---|---|
| `getObject()` | 返回真正要放入容器的对象 |
| `getObjectType()` | 返回对象类型 |
| `isSingleton()` | 返回是否单例 |

---

## 4. FactoryBean 示例

假设有一个对象创建过程比较复杂：

```java
public class SmsClient {

    private String url;

    public SmsClient(String url) {
        this.url = url;
    }

    public void send(String msg) {
        System.out.println("发送短信：" + msg);
    }
}
```

定义一个 `FactoryBean`：

```java
@Component("smsClient")
public class SmsClientFactoryBean implements FactoryBean<SmsClient> {

    @Override
    public SmsClient getObject() {
        return new SmsClient("http://sms-server");
    }

    @Override
    public Class<?> getObjectType() {
        return SmsClient.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
```

使用：

```java
@Autowired
private SmsClient smsClient;
```

注意：这里注入的不是 `SmsClientFactoryBean`，而是：

```text
SmsClientFactoryBean#getObject() 返回的 SmsClient
```

---

## 5. FactoryBean 的特殊获取方式

如果 Bean 名称是：

```text
smsClient
```

正常获取：

```java
Object bean = applicationContext.getBean("smsClient");
```

拿到的是：

```text
FactoryBean#getObject() 返回的对象
```

也就是：

```text
SmsClient
```

如果想拿到 `FactoryBean` 本身，需要加 `&`：

```java
Object factoryBean = applicationContext.getBean("&smsClient");
```

拿到的是：

```text
SmsClientFactoryBean
```

---

## 6. BeanFactory 和 FactoryBean 为什么容易混

这两个名字很像，但角色完全不同：

```text
BeanFactory：Spring 容器
FactoryBean：生产 Bean 的工厂 Bean
```

可以这样记：

```text
BeanFactory：管理所有 Bean 的大工厂
FactoryBean：某一个特殊 Bean，用来创建复杂对象
```

---

## 7. FactoryBean 常见应用场景

### 7.1 MyBatis Mapper

MyBatis 中通常只写接口：

```java
public interface UserMapper {
    User selectById(Long id);
}
```

但可以直接注入：

```java
@Autowired
private UserMapper userMapper;
```

原因是 MyBatis 会通过类似 `FactoryBean` 的机制创建 Mapper 代理对象。

大致流程：

```text
MapperFactoryBean
   ↓ getObject()
创建 Mapper 动态代理对象
   ↓
注入到 Spring 容器
```

所以拿到的 `UserMapper` 实际是代理对象。

---

### 7.2 FeignClient

Feign 也是类似思路：

```java
@FeignClient(name = "user-service")
public interface UserClient {
    UserDTO getUser(Long id);
}
```

Spring 容器里注入的并不是接口实现类，而是 Feign 创建的代理对象。

底层也是通过类似 `FactoryBean` 的方式创建代理对象。

---

### 7.3 复杂对象创建

这些对象创建过程复杂，就适合用 `FactoryBean` 封装：

```text
数据库连接客户端
RPC 代理对象
第三方 SDK Client
缓存 Client
加密工具类
动态代理对象
```

---

## 8. 普通 Bean 和 FactoryBean 的区别

### 普通 Bean

```java
@Component
public class UserService {
}
```

获取：

```java
applicationContext.getBean("userService");
```

返回：

```text
UserService 本身
```

---

### FactoryBean

```java
@Component("smsClient")
public class SmsClientFactoryBean implements FactoryBean<SmsClient> {
}
```

获取：

```java
applicationContext.getBean("smsClient");
```

返回：

```text
SmsClientFactoryBean#getObject() 返回的 SmsClient
```

获取 FactoryBean 本身：

```java
applicationContext.getBean("&smsClient");
```

返回：

```text
SmsClientFactoryBean 本身
```

---

## 9. 总结

`BeanFactory` 和 `FactoryBean` 名字很像，但作用完全不同。

`BeanFactory` 是 Spring IOC 容器的顶层接口，负责创建、管理和获取 Bean，比如 `getBean()`、依赖注入、生命周期管理等都和它有关。

`FactoryBean` 是 Spring 提供的一个扩展接口，它本身也是一个 Bean，但它的作用是创建另一个复杂对象。实现 `FactoryBean` 后，Spring 容器中通过 beanName 获取到的不是 FactoryBean 本身，而是它的 `getObject()` 方法返回的对象。如果想获取 FactoryBean 本身，需要在 beanName 前加 `&`。

比如 MyBatis 的 Mapper、Feign 的 Client 代理对象，都可以通过类似 FactoryBean 的机制创建。

---

## 10. 一句话总结

> `BeanFactory` 是 Spring 的 IOC 容器，负责管理 Bean；`FactoryBean` 是一个特殊的 Bean，负责创建复杂 Bean。正常通过名字获取 FactoryBean 时，拿到的是 `getObject()` 返回的对象，想拿 FactoryBean 本身要加 `&`。
