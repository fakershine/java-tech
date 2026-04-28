# Spring Boot 自动装配原理总结

Spring Boot 自动装配的核心思想是：**根据项目中引入的依赖、配置文件和容器中已有 Bean，自动创建并配置需要的 Bean，减少手动配置**。

---

## 1. 自动装配入口

Spring Boot 启动类通常使用：

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

`@SpringBootApplication` 是组合注解，包含：

```java
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan
```

其中，自动装配的核心是：

```java
@EnableAutoConfiguration
```

Spring Boot 官方文档也说明，`@SpringBootApplication` 包含 `@EnableAutoConfiguration`，用于开启 Spring Boot 的自动配置机制。:contentReference[oaicite:0]{index=0}

---

## 2. @EnableAutoConfiguration 的作用

`@EnableAutoConfiguration` 的作用是：

```text
开启自动装配
根据 classpath 中的依赖和容器中的 Bean
自动推断并创建合适的配置
```

例如：

```text
引入 spring-boot-starter-web
  ↓
classpath 中存在 Spring MVC、Tomcat
  ↓
Spring Boot 自动配置 DispatcherServlet、Tomcat、WebMVC 等组件
```

官方 API 文档中也说明，自动配置类通常会根据 classpath 中存在的类和用户已经定义的 Bean 来决定是否生效。:contentReference[oaicite:1]{index=1}

---

## 3. 自动配置类从哪里来

Spring Boot 会加载自动配置类。

### Spring Boot 2.x

主要从：

```text
META-INF/spring.factories
```

中读取：

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.xxx.XxxAutoConfiguration
```

### Spring Boot 3.x

主要从：

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

中读取自动配置类。

自定义自动配置时，官方文档也要求将自动配置类完整类名写入：

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

:contentReference[oaicite:2]{index=2}

---

## 4. 自动装配核心流程

整体流程：

```text
启动 Spring Boot
  ↓
加载 @SpringBootApplication
  ↓
进入 @EnableAutoConfiguration
  ↓
通过 ImportSelector 加载自动配置类
  ↓
读取 AutoConfiguration.imports / spring.factories
  ↓
筛选自动配置类
  ↓
根据 @Conditional 条件判断是否生效
  ↓
创建符合条件的 Bean
  ↓
注册到 Spring 容器
```

简化理解：

```text
找配置类 -> 判断条件 -> 创建 Bean
```

---

## 5. 条件装配

自动配置类不是全部都会生效，而是通过 `@Conditional` 系列注解进行条件判断。

常见条件注解：

| 注解 | 作用 |
|---|---|
| `@ConditionalOnClass` | classpath 中存在某个类时生效 |
| `@ConditionalOnMissingClass` | classpath 中不存在某个类时生效 |
| `@ConditionalOnBean` | 容器中存在某个 Bean 时生效 |
| `@ConditionalOnMissingBean` | 容器中不存在某个 Bean 时生效 |
| `@ConditionalOnProperty` | 配置文件中某个属性满足条件时生效 |
| `@ConditionalOnWebApplication` | Web 应用环境下生效 |
| `@ConditionalOnResource` | 存在某个资源文件时生效 |

官方文档中也将条件注解分为 class、bean、property、resource、web application 等类型。:contentReference[oaicite:3]{index=3}

---

## 6. 自动配置类示例

以 Web 自动配置为例，可以简化理解为：

```java
@AutoConfiguration
@ConditionalOnClass(DispatcherServlet.class)
@ConditionalOnWebApplication
public class WebMvcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DispatcherServlet dispatcherServlet() {
        return new DispatcherServlet();
    }
}
```

含义：

```text
如果 classpath 中存在 DispatcherServlet
并且当前是 Web 应用
并且用户没有自己定义 DispatcherServlet
则 Spring Boot 自动创建一个 DispatcherServlet
```

---

## 7. @ConditionalOnMissingBean 的意义

自动装配遵循一个重要原则：

```text
约定大于配置
用户优先
```

也就是说：

```text
如果用户没有配置，Spring Boot 自动配置
如果用户自己配置了，Spring Boot 自动配置让位
```

例如：

```java
@Bean
public DataSource dataSource() {
    return customDataSource;
}
```

如果用户自己定义了 `DataSource`，自动配置中的 `DataSource` 就不会再创建。

这就是 `@ConditionalOnMissingBean` 的作用。

---

## 8. Starter 和自动装配的关系

`starter` 本身通常不直接实现功能，它主要负责引入依赖。

例如：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

它会引入：

```text
Spring MVC
Tomcat
Jackson
Validation
相关 AutoConfiguration
```

然后自动装配根据这些依赖自动创建 Bean。

简单理解：

```text
starter 负责引入依赖
auto-configuration 负责创建 Bean
```

官方文档也说明，自动配置通常可以和 starter 关联，starter 提供自动配置代码以及典型依赖。:contentReference[oaicite:4]{index=4}

---

## 9. 自动装配和组件扫描的区别

| 对比项 | 自动装配 | 组件扫描 |
|---|---|---|
| 核心注解 | `@EnableAutoConfiguration` | `@ComponentScan` |
| 扫描来源 | 自动配置类文件 | 当前包及子包 |
| 主要对象 | 框架组件 Bean | 业务 Bean |
| 常见 Bean | DataSource、RedisTemplate、DispatcherServlet | Service、Controller、Component |
| 是否依赖条件判断 | 是 | 一般不是 |

简单理解：

```text
@ComponentScan 扫描自己的业务类。
@EnableAutoConfiguration 装配框架提供的配置类。
```

---

## 10. 自定义 Starter 的自动装配流程

如果自己实现一个 starter，通常需要：

### 1. 编写配置属性类

```java
@ConfigurationProperties(prefix = "demo")
public class DemoProperties {
    private String name;
}
```

---

### 2. 编写自动配置类

```java
@AutoConfiguration
@EnableConfigurationProperties(DemoProperties.class)
@ConditionalOnClass(DemoService.class)
public class DemoAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DemoService demoService(DemoProperties properties) {
        return new DemoService(properties.getName());
    }
}
```

---

### 3. 注册自动配置类

Spring Boot 3.x：

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

内容：

```text
com.demo.DemoAutoConfiguration
```

Spring Boot 2.x：

```text
META-INF/spring.factories
```

内容：

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.demo.DemoAutoConfiguration
```

---

## 11. 自动装配总结流程

```text
1. @SpringBootApplication 启动应用。
2. @EnableAutoConfiguration 开启自动装配。
3. Spring Boot 读取自动配置类列表。
4. 自动配置类通过 @Conditional 判断是否生效。
5. 满足条件时创建对应 Bean。
6. 如果用户已经自定义 Bean，自动配置通常不会重复创建。
7. Bean 注册到 Spring 容器，应用直接使用。
```

---

## 12. 总结

Spring Boot 自动装配的核心是 `@EnableAutoConfiguration`。

它会通过导入选择器读取自动配置类列表，Spring Boot 2.x 主要从 `spring.factories` 中读取，Spring Boot 3.x 主要从 `AutoConfiguration.imports` 中读取。

读取到自动配置类后，并不是全部生效，而是通过 `@ConditionalOnClass`、`@ConditionalOnMissingBean`、`@ConditionalOnProperty` 等条件注解判断是否满足装配条件。

如果满足条件，Spring Boot 就会自动创建对应 Bean；如果用户已经自己定义了 Bean，自动配置通常会通过 `@ConditionalOnMissingBean` 让用户配置优先。

一句话总结：

```text
Spring Boot 自动装配 = @EnableAutoConfiguration 加载自动配置类 + @Conditional 条件判断 + 自动注册 Bean。
```
