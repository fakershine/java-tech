# `@Component` 和 `@Bean` 的区别

## 1. 一句话区别

> `@Component` 是把**类本身**交给 Spring 扫描创建对象；  
> `@Bean` 是在配置类中通过**方法返回值**把对象交给 Spring 容器管理。

---

## 2. 核心区别

| 对比项 | `@Component` | `@Bean` |
|---|---|---|
| 使用位置 | 类上 | 方法上 |
| 创建方式 | Spring 扫描类后创建对象 | Spring 调用方法，把返回值注册为 Bean |
| 是否需要自己 `new` | 不需要 | 通常需要自己 `new` 或调用工厂方法 |
| 适合对象 | 自己写的业务类 | 第三方类、复杂对象 |
| 控制能力 | 较弱 | 更强 |
| 默认 Bean 名称 | 类名首字母小写 | 方法名 |
| 常见场景 | `Service`、`Controller`、`Repository`、普通组件 | `RestTemplate`、`ObjectMapper`、线程池、第三方 SDK Client |

---

## 3. `@Component` 示例

```java
@Component
public class UserService {
}
```

Spring 启动时会扫描到这个类，然后创建 Bean：

```text
扫描 UserService 类
   ↓
实例化 UserService 对象
   ↓
依赖注入
   ↓
初始化
   ↓
放入 Spring 容器
```

使用：

```java
@Autowired
private UserService userService;
```

适合：

```text
自己写的业务类
Service
Controller
Repository
普通工具组件
```

---

## 4. `@Bean` 示例

```java
@Configuration
public class AppConfig {

    @Bean
    public UserService userService() {
        return new UserService();
    }
}
```

Spring 会调用 `userService()` 方法，并把返回值放入容器：

```text
调用 userService() 方法
   ↓
得到 UserService 对象
   ↓
放入 Spring 容器
```

适合注册第三方类或复杂对象：

```java
@Configuration
public class AppConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        return objectMapper;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

---

## 5. 为什么有了 `@Component` 还需要 `@Bean`

因为有些类不是我们自己写的，不能直接加 `@Component`。

比如：

```text
RestTemplate
ObjectMapper
ThreadPoolExecutor
OkHttpClient
DataSource
RedisTemplate
第三方 SDK Client
```

这些对象通常需要用 `@Bean` 注册。

例如：

```java
@Configuration
public class ThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        return new ThreadPoolExecutor(
                10,
                20,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000)
        );
    }
}
```

---

## 6. 两个同时存在会加载哪个？

要分情况看。

---

### 6.1 情况一：Bean 名称不同

如果 `@Component` 和 `@Bean` 创建的是同一种类型，但 Bean 名称不同，那么**两个都会被注册到 Spring 容器中**。

例如：

```java
@Component
public class UserService {
}
```

默认 Bean 名称是：

```text
userService
```

同时又写了：

```java
@Configuration
public class AppConfig {

    @Bean
    public UserService myUserService() {
        return new UserService();
    }
}
```

这个 Bean 名称是：

```text
myUserService
```

此时容器中有两个 `UserService` 类型的 Bean：

```text
userService
myUserService
```

如果按类型注入：

```java
@Autowired
private UserService userService;
```

可能会报错：

```text
NoUniqueBeanDefinitionException
```

因为 Spring 找到了两个 `UserService` 类型的 Bean，不知道注入哪个。

解决方式：

```java
@Autowired
@Qualifier("myUserService")
private UserService userService;
```

或者使用：

```java
@Primary
```

---

### 6.2 情况二：Bean 名称相同

如果 `@Component` 和 `@Bean` 的 Bean 名称相同，就会发生 BeanDefinition 冲突。

例如：

```java
@Component("userService")
public class UserService {
}
```

同时：

```java
@Configuration
public class AppConfig {

    @Bean("userService")
    public UserService userService() {
        return new UserService();
    }
}
```

这两个 Bean 名称都叫：

```text
userService
```

在 Spring Boot 中，默认通常**不允许 Bean 覆盖**，会启动报错：

```text
BeanDefinitionOverrideException
```

也就是说：

```text
默认不是二选一加载，而是直接冲突报错
```

---

## 7. 如果开启 Bean 覆盖呢？

可以通过配置开启 Bean 覆盖：

```yaml
spring:
  main:
    allow-bean-definition-overriding: true
```

开启后，如果 Bean 名称相同：

```text
后注册的 BeanDefinition 会覆盖先注册的 BeanDefinition
```

但实际哪个后注册，和扫描顺序、配置类解析顺序有关。

在常见情况下，`@Bean` 可能覆盖 `@Component`，但不建议依赖这种顺序。

更推荐明确避免重名。

---

## 8. 推荐做法

### 8.1 不要让 `@Component` 和 `@Bean` 重名

不推荐：

```java
@Component("userService")
public class UserService {
}
```

同时：

```java
@Bean("userService")
public UserService userService() {
    return new UserService();
}
```

---

### 8.2 如果有多个同类型 Bean，明确指定名称

```java
@Bean("userServiceA")
public UserService userServiceA() {
    return new UserService();
}

@Bean("userServiceB")
public UserService userServiceB() {
    return new UserService();
}
```

注入时：

```java
@Autowired
@Qualifier("userServiceA")
private UserService userService;
```

---

### 8.3 如果想指定默认注入对象，用 `@Primary`

```java
@Bean
@Primary
public UserService userServiceA() {
    return new UserService();
}
```

---

## 9. 总结

`@Component` 和 `@Bean` 都可以把对象交给 Spring 容器管理，最终都会成为 Spring Bean。

区别在于，`@Component` 是标在类上的，Spring 通过组件扫描发现这个类，然后自动创建对象，适合我们自己写的业务类，比如 `Service`、`Controller`、`Repository` 等。

`@Bean` 是标在方法上的，通常写在 `@Configuration` 配置类中，Spring 会调用这个方法，并把方法返回的对象注册到容器中。它更适合注册第三方类，或者创建过程比较复杂的对象，比如 `RestTemplate`、`ObjectMapper`、线程池、第三方 SDK Client 等。

如果 `@Component` 和 `@Bean` 同时存在，要看 Bean 名称是否相同。如果名称不同，两个 Bean 都会注册进容器；此时按类型注入可能会因为存在多个 Bean 而报错，需要用 `@Qualifier` 或 `@Primary` 指定。如果名称相同，Spring Boot 默认一般不允许覆盖，会启动报错；如果开启了 `spring.main.allow-bean-definition-overriding=true`，则后注册的 Bean 会覆盖先注册的 Bean，但不建议依赖这种覆盖顺序。

---

## 10. 一句话总结

> `@Component` 是让 Spring 扫描类创建 Bean，`@Bean` 是通过方法返回对象注册 Bean；如果两者同时存在且名称不同，会同时注册，按类型注入可能冲突；如果名称相同，Spring Boot 默认会报错，开启覆盖后后注册的会覆盖先注册的。
