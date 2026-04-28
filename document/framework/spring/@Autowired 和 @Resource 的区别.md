# `@Autowired` 和 `@Resource` 的区别

## 1. 核心区别

| 对比项 | `@Autowired` | `@Resource` |
|---|---|---|
| 来源 | Spring 提供 | JDK / Jakarta 提供 |
| 所属包 | `org.springframework.beans.factory.annotation.Autowired` | `javax.annotation.Resource` 或 `jakarta.annotation.Resource` |
| 默认注入方式 | 按类型注入 | 按名称注入 |
| 是否支持 `required` | 支持 | 不支持 |
| 是否支持 `@Qualifier` | 支持 | 不支持 `@Qualifier`，但支持 `name` 属性 |
| 常用场景 | Spring 项目中更常见 | 按名称精确注入时常用 |

---

## 2. `@Autowired`

`@Autowired` 是 Spring 提供的注解，默认按照**类型**注入。

示例：

```java
@Service
public class OrderService {

    @Autowired
    private UserService userService;
}
```

Spring 会根据 `UserService` 类型去容器中查找 Bean。

---

## 3. `@Autowired` 按类型注入

如果容器中只有一个 `UserService` 类型的 Bean：

```java
@Service
public class UserServiceImpl implements UserService {
}
```

那么可以正常注入：

```java
@Autowired
private UserService userService;
```

---

## 4. `@Autowired` 多个 Bean 时怎么办

如果有多个实现类：

```java
@Service
public class UserServiceImplA implements UserService {
}

@Service
public class UserServiceImplB implements UserService {
}
```

这样注入会报错：

```java
@Autowired
private UserService userService;
```

因为 Spring 不知道应该注入哪个 Bean。

---

### 4.1 使用 `@Qualifier`

```java
@Autowired
@Qualifier("userServiceImplA")
private UserService userService;
```

表示明确注入名称为 `userServiceImplA` 的 Bean。

---

### 4.2 使用 `@Primary`

```java
@Service
@Primary
public class UserServiceImplA implements UserService {
}
```

如果多个 Bean 类型相同，Spring 会优先选择带有 `@Primary` 的 Bean。

---

## 5. `@Resource`

`@Resource` 默认按照**名称**注入。

示例：

```java
@Service
public class OrderService {

    @Resource
    private UserService userService;
}
```

它会优先根据字段名 `userService` 去 Spring 容器中查找同名 Bean。

---

## 6. `@Resource` 按名称注入

例如：

```java
@Service("userService")
public class UserServiceImpl implements UserService {
}
```

注入：

```java
@Resource
private UserService userService;
```

这里字段名是 `userService`，容器中也有名为 `userService` 的 Bean，所以可以注入成功。

---

## 7. `@Resource(name = "...")`

可以明确指定 Bean 名称：

```java
@Resource(name = "userServiceImplA")
private UserService userService;
```

这样就会根据名称 `userServiceImplA` 去容器中查找 Bean。

---

## 8. 注入规则区别

### 8.1 `@Autowired` 注入流程

```text
1. 先按类型查找 Bean
2. 如果只有一个 Bean，直接注入
3. 如果有多个 Bean，再根据 @Qualifier、@Primary、字段名判断
4. 如果找不到 Bean，默认报错
```

---

### 8.2 `@Resource` 注入流程

```text
1. 先按名称查找 Bean
2. 如果名称找到，直接注入
3. 如果名称找不到，再按类型查找
4. 如果按类型找到多个 Bean，报错
5. 如果找不到 Bean，报错
```

---

## 9. `required` 区别

`@Autowired` 支持 `required = false`：

```java
@Autowired(required = false)
private UserService userService;
```

表示找不到 Bean 时不报错，注入为 `null`。

`@Resource` 没有 `required` 属性。

---

## 10. 包名区别

### Spring Boot 2 / Java EE 常见

```java
import javax.annotation.Resource;
```

### Spring Boot 3 / Jakarta EE 常见

```java
import jakarta.annotation.Resource;
```

### `@Autowired`

```java
import org.springframework.beans.factory.annotation.Autowired;
```

---

## 11. 构造器注入推荐写法

实际开发中，更推荐使用构造器注入。

```java
@Service
public class OrderService {

    private final UserService userService;

    public OrderService(UserService userService) {
        this.userService = userService;
    }
}
```

优点：

```text
依赖不可变
方便单元测试
避免循环依赖隐藏问题
对象创建时依赖完整
```

如果有多个实现类，可以配合 `@Qualifier`：

```java
@Service
public class OrderService {

    private final UserService userService;

    public OrderService(
            @Qualifier("userServiceImplA") UserService userService
    ) {
        this.userService = userService;
    }
}
```

---

## 12. 推荐使用哪个

### 12.1 一般 Spring 项目

推荐：

```java
@Autowired
```

配合：

```java
@Qualifier
@Primary
```

或者直接使用构造器注入。

---

### 12.2 想按名称精确注入

可以使用：

```java
@Resource(name = "xxx")
```

示例：

```java
@Resource(name = "userServiceImplA")
private UserService userService;
```

---

## 13. 常见面试问题

### 13.1 `@Autowired` 是按名称还是按类型？

默认按类型。

如果同类型 Bean 有多个，会再结合：

```text
@Qualifier
@Primary
字段名
```

来确定具体注入哪个 Bean。

---

### 13.2 `@Resource` 是按名称还是按类型？

默认先按名称，再按类型。

```text
先根据字段名或 name 属性找 Bean
找不到再根据类型找 Bean
```

---

### 13.3 多个实现类时怎么解决注入冲突？

可以使用：

```text
@Qualifier
@Primary
@Resource(name = "xxx")
```

示例：

```java
@Autowired
@Qualifier("userServiceImplA")
private UserService userService;
```

或者：

```java
@Resource(name = "userServiceImplA")
private UserService userService;
```

---

### 13.4 `@Autowired(required = false)` 有什么用？

表示依赖不是必须的。

如果容器中找不到对应 Bean，不会报错，会注入 `null`。

```java
@Autowired(required = false)
private UserService userService;
```

---

### 13.5 `@Resource` 支持 `required = false` 吗？

不支持。

`@Resource` 没有 `required` 属性。

---

## 14. 总结

`@Autowired` 和 `@Resource` 都可以用于依赖注入，但它们的来源和默认注入规则不同。

`@Autowired` 是 Spring 提供的注解，默认按照类型注入。如果容器中同一类型只有一个 Bean，就直接注入；如果有多个 Bean，可以结合 `@Qualifier` 指定 Bean 名称，或者使用 `@Primary` 指定默认 Bean。`@Autowired` 还支持 `required = false`，表示依赖不存在时不报错。

`@Resource` 是 JDK / Jakarta 提供的注解，默认按照名称注入。它会先根据字段名或者 `name` 属性去容器中查找 Bean；如果按名称找不到，再按类型查找。`@Resource` 不支持 `@Qualifier`，也没有 `required` 属性。

实际开发中，如果是 Spring 项目，可以使用 `@Autowired`，更推荐构造器注入；如果想明确按名称注入，可以使用 `@Resource(name = "...")`。

---

## 15. 一句话总结

> `@Autowired` 默认按类型注入，是 Spring 提供的；`@Resource` 默认按名称注入，是 JDK / Jakarta 提供的。多个实现类时，`@Autowired` 通常配合 `@Qualifier` 或 `@Primary`，`@Resource` 通常使用 `name` 指定 Bean。
