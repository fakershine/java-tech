# Spring Boot 常见面试题总结

## 目录

- [一、Spring Boot 自动装配原理](#一spring-boot-自动装配原理)
- [二、@SpringBootApplication 包含哪些注解](#二springbootapplication-包含哪些注解)
- [三、starter 原理是什么](#三starter-原理是什么)
- [四、application.yml 加载顺序](#四applicationyml-加载顺序)
- [五、如何自定义 starter](#五如何自定义-starter)
- [六、Spring Boot 如何做优雅停机](#六spring-boot-如何做优雅停机)
- [七、面试速记版](#七面试速记版)
- [八、总览表](#八总览表)
- [九、完整面试回答模板](#九完整面试回答模板)

---

# 一、Spring Boot 自动装配原理

## 1. 什么是自动装配？

Spring Boot 自动装配是指：

> Spring Boot 会根据当前项目引入的依赖、配置文件中的属性、容器中已有的 Bean，自动判断并创建合适的 Bean。

简单来说：

```text
开发者只需要引入 starter 依赖，
Spring Boot 就会自动帮我们完成相关组件的配置。
```

例如，引入：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

Spring Boot 会自动配置：

```text
Tomcat
Spring MVC
DispatcherServlet
Jackson
参数转换器
静态资源处理
错误处理
```

---

## 2. 自动装配解决了什么问题？

传统 Spring 项目中，需要大量 XML 或 Java 配置。

例如配置 Spring MVC 时，可能需要手动配置：

```text
DispatcherServlet
HandlerMapping
HandlerAdapter
ViewResolver
MessageConverter
静态资源映射
文件上传解析器
```

Spring Boot 自动装配后，很多常见配置都可以由框架自动完成。

---

## 3. 自动装配的核心注解

Spring Boot 自动装配的核心入口是：

```java
@SpringBootApplication
```

而 `@SpringBootApplication` 中最关键的是：

```java
@EnableAutoConfiguration
```

`@EnableAutoConfiguration` 用于开启自动装配。

---

## 4. 自动装配整体流程

Spring Boot 自动装配大致流程如下：

```text
1. 启动类标注 @SpringBootApplication
2. @SpringBootApplication 包含 @EnableAutoConfiguration
3. @EnableAutoConfiguration 导入 AutoConfigurationImportSelector
4. AutoConfigurationImportSelector 加载自动配置类
5. Spring Boot 读取自动配置候选类
6. 根据条件注解进行过滤
7. 满足条件的自动配置类生效
8. 自动配置类向 Spring 容器中注册 Bean
```

---

## 5. 自动装配核心流程图

```text
@SpringBootApplication
        |
        v
@EnableAutoConfiguration
        |
        v
@Import(AutoConfigurationImportSelector.class)
        |
        v
读取自动配置类
        |
        v
条件判断
        |
        v
创建 Bean
        |
        v
完成自动装配
```

---

## 6. 自动配置类从哪里来？

在 Spring Boot 2.7+ 和 Spring Boot 3.x 中，自动配置类通常声明在：

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

示例：

```text
org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration
```

在 Spring Boot 2.7 之前，自动配置类主要通过：

```text
META-INF/spring.factories
```

配置，例如：

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration,\
org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

---

## 7. AutoConfigurationImportSelector 的作用

`AutoConfigurationImportSelector` 是自动装配的关键类。

它的主要作用是：

```text
从指定位置加载自动配置类，
然后根据条件过滤，
最后把符合条件的配置类导入 Spring 容器。
```

---

## 8. 条件注解是什么？

Spring Boot 自动装配并不是所有配置类都会生效。

它会通过大量条件注解判断当前环境是否满足要求。

常见条件注解包括：

| 注解 | 作用 |
|---|---|
| `@ConditionalOnClass` | classpath 中存在某个类时生效 |
| `@ConditionalOnMissingClass` | classpath 中不存在某个类时生效 |
| `@ConditionalOnBean` | 容器中存在某个 Bean 时生效 |
| `@ConditionalOnMissingBean` | 容器中不存在某个 Bean 时生效 |
| `@ConditionalOnProperty` | 配置文件中存在某个属性或属性值匹配时生效 |
| `@ConditionalOnWebApplication` | 当前是 Web 应用时生效 |
| `@ConditionalOnNotWebApplication` | 当前不是 Web 应用时生效 |
| `@ConditionalOnResource` | 存在某个资源文件时生效 |
| `@ConditionalOnExpression` | SpEL 表达式满足条件时生效 |

---

## 9. 自动装配示例

以 Spring MVC 自动装配为例。

Spring Boot 中存在一个自动配置类：

```java
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class, WebMvcConfigurer.class })
@ConditionalOnMissingBean(WebMvcConfigurationSupport.class)
public class WebMvcAutoConfiguration {
}
```

它的含义是：

```text
当前是 Servlet Web 应用；
classpath 中存在 Servlet、DispatcherServlet 等类；
容器中没有用户自定义的 WebMvcConfigurationSupport；
那么 WebMvcAutoConfiguration 生效。
```

---

## 10. 自动装配为什么不会覆盖用户配置？

Spring Boot 自动配置类中大量使用：

```java
@ConditionalOnMissingBean
```

含义是：

```text
只有当容器中不存在某个 Bean 时，Spring Boot 才会自动创建默认 Bean。
```

如果用户自己定义了 Bean，Spring Boot 默认配置就不会再生效。

示例：

```java
@Bean
public ObjectMapper objectMapper() {
    return new ObjectMapper();
}
```

如果用户自己定义了 `ObjectMapper`，Spring Boot 默认的 `ObjectMapper` 自动配置就可能不会生效。

---

## 11. 自动装配的优点

Spring Boot 自动装配的优点：

1. 减少配置；
2. 降低项目搭建成本；
3. 提高开发效率；
4. 提供合理默认值；
5. 支持按需加载；
6. 支持用户自定义覆盖默认配置；
7. 通过 starter 实现模块化集成。

---

## 12. 面试回答

Spring Boot 自动装配是指 Spring Boot 根据当前项目的依赖、配置文件和容器中已有 Bean，自动判断并创建需要的 Bean。

它的核心入口是 `@SpringBootApplication`，其中包含 `@EnableAutoConfiguration`。`@EnableAutoConfiguration` 通过 `@Import` 导入 `AutoConfigurationImportSelector`，该类会读取自动配置候选类，例如 Spring Boot 2.7+ 和 Spring Boot 3.x 中的 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件。

读取到自动配置类之后，Spring Boot 会通过 `@ConditionalOnClass`、`@ConditionalOnMissingBean`、`@ConditionalOnProperty` 等条件注解进行判断，只有满足条件的自动配置类才会生效，最终向 Spring 容器中注册 Bean。

---

# 二、@SpringBootApplication 包含哪些注解

## 1. @SpringBootApplication 是什么？

`@SpringBootApplication` 是 Spring Boot 启动类上最常用的注解。

示例：

```java
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

它是一个组合注解。

---

## 2. @SpringBootApplication 包含的核心注解

`@SpringBootApplication` 主要包含三个核心注解：

```text
1. @SpringBootConfiguration
2. @EnableAutoConfiguration
3. @ComponentScan
```

等价于：

```java
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan
public class DemoApplication {
}
```

---

## 3. @SpringBootConfiguration

`@SpringBootConfiguration` 本质上是一个配置类注解。

它里面包含：

```java
@Configuration
```

作用：

```text
标识当前类是 Spring Boot 配置类。
```

示例：

```java
@SpringBootConfiguration
public class AppConfig {
}
```

它和普通的 `@Configuration` 类似，只不过更强调这是 Spring Boot 应用的主配置类。

---

## 4. @EnableAutoConfiguration

`@EnableAutoConfiguration` 用于开启 Spring Boot 自动装配。

作用：

```text
根据当前依赖和配置，自动导入符合条件的自动配置类。
```

它是 Spring Boot 自动装配的核心。

---

## 5. @ComponentScan

`@ComponentScan` 用于开启组件扫描。

默认扫描范围：

```text
启动类所在包及其子包
```

例如启动类在：

```text
com.example.demo
```

那么默认会扫描：

```text
com.example.demo
com.example.demo.controller
com.example.demo.service
com.example.demo.mapper
```

---

## 6. 为什么启动类要放在根包下？

推荐项目结构：

```text
com.example.demo
├── DemoApplication.java
├── controller
├── service
├── mapper
└── config
```

原因是：

```text
@ComponentScan 默认扫描启动类所在包及其子包。
```

如果启动类放得太深，可能导致其他包下的组件扫描不到。

---

## 7. @SpringBootApplication 常用属性

### 7.1 exclude

排除某些自动配置类：

```java
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class DemoApplication {
}
```

适合场景：

```text
项目暂时不需要数据库，
但引入了数据库相关依赖，
可以排除数据源自动配置。
```

---

### 7.2 scanBasePackages

指定扫描包路径：

```java
@SpringBootApplication(scanBasePackages = "com.example")
public class DemoApplication {
}
```

---

## 8. 面试回答

`@SpringBootApplication` 是 Spring Boot 的核心启动注解，它是一个组合注解，主要包含 `@SpringBootConfiguration`、`@EnableAutoConfiguration` 和 `@ComponentScan`。

`@SpringBootConfiguration` 表示当前类是配置类；`@EnableAutoConfiguration` 用于开启自动装配；`@ComponentScan` 用于扫描启动类所在包及其子包下的组件。

---

# 三、starter 原理是什么

## 1. starter 是什么？

starter 是 Spring Boot 提供的一种依赖聚合机制。

它本质上是一个 Maven 或 Gradle 依赖包。

作用是：

```text
把某个功能需要的依赖统一打包管理，
开发者只需要引入一个 starter，
就可以获得一整套功能依赖和自动配置。
```

---

## 2. starter 示例

例如引入 Web starter：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

它会间接引入：

```text
Spring MVC
Tomcat
Jackson
Validation
Logging
```

---

## 3. starter 本身做了什么？

starter 通常主要做两件事：

```text
1. 聚合依赖
2. 配合自动配置类完成默认配置
```

starter 本身一般不写太多业务代码。

它更多是一个“依赖入口”。

---

## 4. starter 和 auto-configuration 的关系

starter 和自动配置通常是配套使用的。

```text
starter：负责引入依赖
auto-configuration：负责自动创建 Bean
```

例如：

```text
spring-boot-starter-web
    |
    v
引入 Spring MVC、Tomcat、Jackson 等依赖
    |
    v
Spring Boot 检测到相关类存在
    |
    v
WebMvcAutoConfiguration 生效
    |
    v
自动配置 Web 环境
```

---

## 5. starter 原理流程

```text
1. 项目引入 starter
2. starter 传递引入相关依赖
3. Spring Boot 启动
4. 自动装配机制读取自动配置类
5. 条件注解判断依赖是否存在
6. 条件满足则创建相关 Bean
7. 功能生效
```

---

## 6. 常见官方 starter

| starter | 作用 |
|---|---|
| `spring-boot-starter-web` | Web MVC 应用开发 |
| `spring-boot-starter-webflux` | 响应式 Web 应用开发 |
| `spring-boot-starter-jdbc` | JDBC 数据库访问 |
| `spring-boot-starter-data-jpa` | Spring Data JPA |
| `spring-boot-starter-data-redis` | Redis 集成 |
| `spring-boot-starter-amqp` | RabbitMQ 集成 |
| `spring-boot-starter-security` | Spring Security |
| `spring-boot-starter-test` | 测试相关依赖 |
| `spring-boot-starter-actuator` | 监控和管理端点 |

---

## 7. starter 命名规范

官方 starter 命名通常是：

```text
spring-boot-starter-xxx
```

例如：

```text
spring-boot-starter-web
spring-boot-starter-data-redis
```

第三方或自定义 starter 推荐命名为：

```text
xxx-spring-boot-starter
```

例如：

```text
sms-spring-boot-starter
demo-spring-boot-starter
```

这样可以避免和 Spring Boot 官方 starter 命名冲突。

---

## 8. 面试回答

starter 是 Spring Boot 的依赖聚合机制，本质上是一个依赖包，用来把某个功能所需的依赖统一管理起来。

starter 通常和自动配置类配合使用。starter 负责引入依赖，自动配置类负责根据条件创建 Bean。项目引入 starter 后，Spring Boot 启动时会通过自动装配机制加载自动配置类，并通过条件注解判断当前环境是否满足要求，满足后就自动创建相关 Bean。

---

# 四、application.yml 加载顺序

## 1. Spring Boot 配置文件有哪些？

Spring Boot 常见配置文件有：

```text
application.properties
application.yml
application.yaml
```

其中 `application.yml` 是 YAML 格式配置文件。

---

## 2. application.yml 常见位置

Spring Boot 默认会从以下位置加载配置文件：

```text
1. classpath 根目录
2. classpath:/config/
3. 当前项目根目录
4. 当前项目根目录下的 config/ 目录
5. 当前项目根目录下 config/ 的直接子目录
```

---

## 3. 配置文件优先级原则

Spring Boot 配置加载有一个核心规则：

> 后加载的配置会覆盖先加载的配置。

也就是说：

```text
优先级高的配置会覆盖优先级低的配置。
```

---

## 4. application.yml 文件位置优先级

常见配置文件位置优先级从低到高可以理解为：

```text
1. jar 包内部 application.yml
2. jar 包内部 application-{profile}.yml
3. jar 包外部 application.yml
4. jar 包外部 application-{profile}.yml
```

优先级最高的是：

```text
jar 包外部的 profile 配置
```

例如：

```text
application-prod.yml
```

如果当前激活的是：

```yaml
spring:
  profiles:
    active: prod
```

那么 `application-prod.yml` 会生效，并覆盖 `application.yml` 中相同的配置项。

---

## 5. properties 和 yml 同时存在时

如果同一位置同时存在：

```text
application.properties
application.yml
```

一般情况下：

```text
application.properties 优先级高于 application.yml
```

实际项目中建议统一使用一种格式，避免混用导致配置覆盖关系不清晰。

---

## 6. 外部配置优先于内部配置

假设 jar 包内部有：

```text
BOOT-INF/classes/application.yml
```

jar 包同级目录也有：

```text
application.yml
```

那么启动时，外部配置优先级更高。

示例：

```bash
java -jar demo.jar
```

目录结构：

```text
demo.jar
application.yml
```

外部的 `application.yml` 会覆盖 jar 包内部同名配置。

---

## 7. 命令行参数优先级更高

命令行参数优先级通常高于配置文件。

示例：

```bash
java -jar demo.jar --server.port=9090
```

如果 `application.yml` 中配置的是：

```yaml
server:
  port: 8080
```

最终端口会是：

```text
9090
```

---

## 8. 常见外部化配置优先级

常见优先级从低到高可以简化理解为：

```text
默认配置
配置文件
环境变量
JVM 系统属性
命令行参数
测试相关配置
```

更具体地说：

| 优先级 | 配置来源 |
|---|---|
| 低 | `SpringApplication.setDefaultProperties` 默认配置 |
| 中 | `application.yml` / `application.properties` |
| 中高 | OS 环境变量 |
| 高 | Java System properties，例如 `-Dserver.port=9090` |
| 更高 | 命令行参数，例如 `--server.port=9090` |
| 测试最高 | `@SpringBootTest(properties=...)`、`@TestPropertySource` 等 |

---

## 9. spring.config.location

可以通过 `spring.config.location` 指定配置文件位置。

示例：

```bash
java -jar demo.jar --spring.config.location=file:/opt/config/application.yml
```

注意：

```text
spring.config.location 会替换默认搜索位置。
```

---

## 10. spring.config.additional-location

可以通过 `spring.config.additional-location` 添加额外配置位置。

示例：

```bash
java -jar demo.jar --spring.config.additional-location=file:/opt/config/
```

注意：

```text
additional-location 是在默认位置基础上追加额外配置位置。
```

---

## 11. profile 配置加载

application.yml：

```yaml
spring:
  profiles:
    active: dev
```

application-dev.yml：

```yaml
server:
  port: 8081
```

application-prod.yml：

```yaml
server:
  port: 8082
```

当激活 `dev`：

```text
application.yml + application-dev.yml 生效
```

当激活 `prod`：

```text
application.yml + application-prod.yml 生效
```

---

## 12. 面试回答

Spring Boot 会自动加载 `application.properties`、`application.yml` 或 `application.yaml` 配置文件。

配置文件可以放在 classpath 根目录、classpath:/config/、jar 包外部当前目录、当前目录下 config/ 目录等位置。整体原则是外部配置优先于内部配置，profile 配置优先于普通配置，命令行参数优先于配置文件。

一般可以简单记忆为：jar 包内部配置优先级较低，jar 包外部配置优先级较高；`application-{profile}.yml` 优先级高于 `application.yml`；命令行参数优先级更高。

---

# 五、如何自定义 starter

## 1. 自定义 starter 的目的

自定义 starter 的目的：

```text
把公司内部通用功能封装起来，
让其他项目只需要引入一个 starter，
就能自动获得相关能力。
```

常见场景：

1. 统一日志 starter；
2. 统一 Redis starter；
3. 统一 MQ starter；
4. 短信发送 starter；
5. 文件上传 starter；
6. 接口签名 starter；
7. 幂等组件 starter；
8. 分布式锁 starter。

---

## 2. 自定义 starter 的基本组成

一个完整的 starter 通常包含两个模块：

```text
1. xxx-spring-boot-autoconfigure
2. xxx-spring-boot-starter
```

---

### 2.1 autoconfigure 模块

`autoconfigure` 模块负责编写自动配置逻辑。

主要包含：

```text
配置属性类
自动配置类
核心功能 Bean
条件注解
AutoConfiguration.imports 文件
```

---

### 2.2 starter 模块

`starter` 模块主要负责依赖聚合。

它通常不写业务代码，只负责引入：

```text
autoconfigure 模块
第三方依赖
功能依赖
```

---

## 3. 自定义 starter 项目结构

示例：

```text
demo-spring-boot-starter-parent
├── demo-spring-boot-autoconfigure
│   ├── src/main/java
│   │   └── com.example.demo.autoconfigure
│   │       ├── DemoProperties.java
│   │       ├── DemoService.java
│   │       └── DemoAutoConfiguration.java
│   └── src/main/resources
│       └── META-INF/spring
│           └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│
└── demo-spring-boot-starter
    └── pom.xml
```

---

## 4. 第一步：定义属性类

```java
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo")
public class DemoProperties {

    private boolean enabled = true;

    private String prefix = "hello";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
```

配置文件中可以这样使用：

```yaml
demo:
  enabled: true
  prefix: "你好"
```

---

## 5. 第二步：定义核心服务类

```java
public class DemoService {

    private final DemoProperties properties;

    public DemoService(DemoProperties properties) {
        this.properties = properties;
    }

    public String sayHello(String name) {
        return properties.getPrefix() + "，" + name;
    }
}
```

---

## 6. 第三步：定义自动配置类

Spring Boot 3.x 推荐使用：

```java
@AutoConfiguration
```

示例：

```java
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(DemoService.class)
@EnableConfigurationProperties(DemoProperties.class)
@ConditionalOnProperty(prefix = "demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DemoAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DemoService demoService(DemoProperties properties) {
        return new DemoService(properties);
    }
}
```

---

## 7. 第四步：配置 AutoConfiguration.imports

在 `resources` 目录下创建文件：

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

内容：

```text
com.example.demo.autoconfigure.DemoAutoConfiguration
```

注意：

```text
每行写一个自动配置类全限定名。
```

---

## 8. 第五步：starter 模块引入 autoconfigure

starter 模块的 `pom.xml`：

```xml
<dependencies>
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>demo-spring-boot-autoconfigure</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

如果依赖第三方包，也可以在 starter 中统一引入。

---

## 9. 第六步：业务项目引入 starter

业务项目中引入：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>demo-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

然后配置：

```yaml
demo:
  enabled: true
  prefix: "Hello"
```

直接使用：

```java
@RestController
public class DemoController {

    private final DemoService demoService;

    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    @GetMapping("/hello")
    public String hello() {
        return demoService.sayHello("Spring Boot");
    }
}
```

---

## 10. Spring Boot 2.x 老方式

在 Spring Boot 2.7 之前，常见方式是在：

```text
META-INF/spring.factories
```

中配置：

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.example.demo.autoconfigure.DemoAutoConfiguration
```

Spring Boot 2.7+ 和 Spring Boot 3.x 推荐使用：

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

---

## 11. 自定义 starter 注意事项

1. 不建议在自动配置类上使用 `@ComponentScan`；
2. 自动配置类应该放到独立包路径；
3. 使用 `@ConditionalOnMissingBean`，方便用户覆盖默认 Bean；
4. 使用 `@ConditionalOnClass`，避免缺少依赖时报错；
5. 使用 `@ConditionalOnProperty`，支持配置开关；
6. 使用 `@ConfigurationProperties` 管理配置；
7. starter 只做依赖聚合，不写复杂业务；
8. 自动配置类通过 `AutoConfiguration.imports` 暴露；
9. 命名建议使用 `xxx-spring-boot-starter`。

---

## 12. 面试回答

自定义 starter 通常分为两个模块：`xxx-spring-boot-autoconfigure` 和 `xxx-spring-boot-starter`。

`autoconfigure` 模块负责编写自动配置类、配置属性类和核心 Bean，并通过条件注解控制是否生效。Spring Boot 3.x 中需要在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件中声明自动配置类。

`starter` 模块主要负责依赖聚合，引入 autoconfigure 模块和相关第三方依赖。业务项目只需要引入 starter，就可以通过 Spring Boot 自动装配机制自动创建相关 Bean。

---

# 六、Spring Boot 如何做优雅停机

## 1. 什么是优雅停机？

优雅停机是指应用停止时，不是立即强制终止进程，而是：

```text
1. 停止接收新请求
2. 等待正在处理的请求执行完成
3. 释放资源
4. 关闭线程池
5. 关闭数据库连接
6. 最后退出应用
```

目标是：

```text
尽量避免请求中断、数据不一致、任务丢失。
```

---

## 2. 为什么需要优雅停机？

在生产环境中，应用经常会因为以下原因停止：

1. 发布新版本；
2. 容器重启；
3. Kubernetes 滚动更新；
4. 服务器维护；
5. 服务扩缩容；
6. 主动下线实例。

如果没有优雅停机，可能导致：

```text
请求处理中断
事务执行一半失败
消息消费丢失
异步任务中断
数据写入不完整
调用方收到异常
```

---

## 3. Spring Boot 开启优雅停机

Spring Boot 2.3+ 支持内置优雅停机。

常见配置：

```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 20s
```

含义：

| 配置 | 说明 |
|---|---|
| `server.shutdown=graceful` | 开启优雅停机 |
| `spring.lifecycle.timeout-per-shutdown-phase=20s` | 每个关闭阶段最多等待 20 秒 |

---

## 4. Spring Boot 优雅停机过程

大致流程：

```text
1. 应用收到 SIGTERM 信号
2. SpringApplication 开始关闭 ApplicationContext
3. Web 服务器停止接收新请求
4. 已经进入的请求继续处理
5. SmartLifecycle Bean 按 phase 顺序停止
6. 执行 @PreDestroy 方法
7. 执行 DisposableBean.destroy()
8. 执行自定义 destroy-method
9. 关闭线程池、连接池等资源
10. JVM 进程退出
```

---

## 5. Linux 下正常停止

推荐使用：

```bash
kill -15 <pid>
```

或者：

```bash
kill <pid>
```

`kill` 默认发送的是：

```text
SIGTERM
```

这会触发 Spring Boot 优雅停机流程。

---

## 6. 不推荐 kill -9

不推荐使用：

```bash
kill -9 <pid>
```

因为 `kill -9` 发送的是：

```text
SIGKILL
```

进程会被操作系统强制杀死，应用没有机会执行关闭逻辑。

这会导致：

1. `@PreDestroy` 不执行；
2. 线程池来不及关闭；
3. 连接池来不及释放；
4. 请求可能直接中断；
5. 临时文件或缓存可能无法清理。

---

## 7. 容器环境中的优雅停机

在 Docker 或 Kubernetes 中，停止容器通常会发送 `SIGTERM`。

Kubernetes 中可以配合：

```yaml
terminationGracePeriodSeconds: 30
```

示例：

```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      terminationGracePeriodSeconds: 30
      containers:
        - name: demo
          image: demo:1.0.0
```

含义：

```text
Kubernetes 会给应用最多 30 秒时间完成优雅退出。
```

---

## 8. 配合 readiness 探针

在 Kubernetes 中，优雅停机通常还需要配合 readiness 探针。

思路：

```text
1. 应用准备下线
2. readiness 变为不可用
3. Kubernetes 从 Service Endpoint 中摘除实例
4. 流量不再进入该实例
5. 等待已有请求处理完
6. 应用关闭
```

---

## 9. 处理线程池优雅关闭

如果项目中使用自定义线程池，也需要手动配置关闭策略。

示例：

```java
@Bean
public ThreadPoolTaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(20);
    executor.setQueueCapacity(1000);
    executor.setThreadNamePrefix("biz-pool-");

    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);

    executor.initialize();

    return executor;
}
```

关键配置：

| 配置 | 说明 |
|---|---|
| `setWaitForTasksToCompleteOnShutdown(true)` | 关闭时等待任务执行完成 |
| `setAwaitTerminationSeconds(30)` | 最多等待 30 秒 |

---

## 10. 使用 @PreDestroy 释放资源

```java
@Component
public class ResourceCleaner {

    @PreDestroy
    public void destroy() {
        System.out.println("释放资源");
    }
}
```

适合释放：

```text
本地缓存
临时文件
连接资源
自定义客户端
后台任务
```

---

## 11. 实现 DisposableBean

```java
@Component
public class MyDisposableBean implements DisposableBean {

    @Override
    public void destroy() {
        System.out.println("销毁资源");
    }
}
```

---

## 12. 实现 SmartLifecycle

如果需要控制启动和停止顺序，可以实现 `SmartLifecycle`。

```java
@Component
public class MySmartLifecycle implements SmartLifecycle {

    private volatile boolean running = false;

    @Override
    public void start() {
        running = true;
        System.out.println("启动组件");
    }

    @Override
    public void stop() {
        running = false;
        System.out.println("停止组件");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return 0;
    }
}
```

`phase` 越大，启动越晚，停止越早。

---

## 13. 消息消费场景的优雅停机

如果项目中有 MQ 消费者，例如 RocketMQ、RabbitMQ、Kafka，需要注意：

```text
1. 停止拉取新消息
2. 等待正在消费的消息处理完成
3. 提交 offset 或 ack
4. 关闭消费者客户端
```

否则可能导致：

```text
消息重复消费
消息丢失
消费状态不一致
```

---

## 14. 数据库连接池关闭

Spring 容器关闭时，常见连接池如 HikariCP 会自动关闭。

如果有自定义资源，需要确保：

```text
连接池关闭
连接释放
事务结束
后台任务停止
```

---

## 15. 优雅停机完整配置示例

```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      probes:
        enabled: true
```

---

## 16. 优雅停机注意事项

1. 使用 `SIGTERM`，不要使用 `kill -9`；
2. 配置 `server.shutdown=graceful`；
3. 配置合理的关闭超时时间；
4. 自定义线程池要等待任务完成；
5. MQ 消费者要停止拉取新消息并完成 ack；
6. Kubernetes 中配置 `terminationGracePeriodSeconds`；
7. 配合 readiness 探针摘除流量；
8. 避免关闭过程中继续接收新请求；
9. 释放数据库连接、文件句柄、网络连接等资源。

---

## 17. 面试回答

Spring Boot 可以通过内置的 graceful shutdown 实现优雅停机。

常见做法是在配置文件中设置 `server.shutdown=graceful`，并通过 `spring.lifecycle.timeout-per-shutdown-phase` 设置关闭阶段的等待时间。应用收到 `SIGTERM` 后，会关闭 ApplicationContext，Web 服务器会停止接收新请求，同时允许已经进入的请求在超时时间内处理完成。

同时，项目中的自定义线程池、MQ 消费者、连接池和其他资源也要配合关闭。例如线程池可以设置 `setWaitForTasksToCompleteOnShutdown(true)`，资源释放可以通过 `@PreDestroy`、`DisposableBean` 或 `SmartLifecycle` 实现。生产环境中不要使用 `kill -9`，因为它会强制杀死进程，无法触发优雅停机逻辑。

---

# 七、面试速记版

## 1. Spring Boot 自动装配原理

Spring Boot 自动装配是根据依赖、配置和已有 Bean 自动创建 Bean。

核心流程：

```text
@SpringBootApplication
-> @EnableAutoConfiguration
-> AutoConfigurationImportSelector
-> 读取 AutoConfiguration.imports / spring.factories
-> 条件注解过滤
-> 注册 Bean
```

---

## 2. @SpringBootApplication 包含哪些注解？

核心包含：

```text
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan
```

其中：

```text
@SpringBootConfiguration：标识配置类
@EnableAutoConfiguration：开启自动装配
@ComponentScan：开启组件扫描
```

---

## 3. starter 原理是什么？

starter 本质是依赖聚合包。

```text
starter 负责引入依赖
auto-configuration 负责自动配置 Bean
```

引入 starter 后，Spring Boot 根据 classpath 和条件注解判断是否创建相关 Bean。

---

## 4. application.yml 加载顺序

简单记忆：

```text
外部配置 > 内部配置
profile 配置 > 普通配置
命令行参数 > 配置文件
properties > yml
```

常见顺序：

```text
jar 内 application.yml
jar 内 application-{profile}.yml
jar 外 application.yml
jar 外 application-{profile}.yml
命令行参数
```

---

## 5. 如何自定义 starter？

核心步骤：

```text
1. 创建 autoconfigure 模块
2. 定义 Properties 配置属性类
3. 定义核心功能类
4. 定义 AutoConfiguration 自动配置类
5. 配置 AutoConfiguration.imports
6. 创建 starter 模块聚合依赖
7. 业务项目引入 starter
```

---

## 6. Spring Boot 如何做优雅停机？

配置：

```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

核心流程：

```text
收到 SIGTERM
停止接收新请求
等待已有请求处理完成
关闭 Spring 容器
释放资源
退出进程
```

注意不要使用：

```bash
kill -9
```

---

# 八、总览表

| 问题 | 核心结论 |
|---|---|
| Spring Boot 自动装配原理 | 通过 `@EnableAutoConfiguration` 导入自动配置类，再用条件注解判断是否生效 |
| @SpringBootApplication 包含哪些注解 | `@SpringBootConfiguration`、`@EnableAutoConfiguration`、`@ComponentScan` |
| starter 原理是什么 | starter 聚合依赖，自动配置类负责创建 Bean |
| application.yml 加载顺序 | 外部配置优先于内部配置，profile 配置优先于普通配置，命令行参数优先级更高 |
| 如何自定义 starter | 编写 properties、auto configuration、AutoConfiguration.imports，再用 starter 聚合依赖 |
| Spring Boot 如何做优雅停机 | 配置 graceful shutdown，停止新请求，等待旧请求完成，释放资源 |

---

# 九、完整面试回答模板

Spring Boot 自动装配是指 Spring Boot 会根据当前项目引入的依赖、配置文件中的属性以及容器中已有的 Bean，自动判断并创建需要的 Bean。它的核心入口是 `@SpringBootApplication`，其中包含 `@EnableAutoConfiguration`。`@EnableAutoConfiguration` 会通过 `AutoConfigurationImportSelector` 加载自动配置类，然后通过条件注解判断是否生效，最终把符合条件的 Bean 注册到 Spring 容器中。

`@SpringBootApplication` 是一个组合注解，主要包含 `@SpringBootConfiguration`、`@EnableAutoConfiguration` 和 `@ComponentScan`。`@SpringBootConfiguration` 表示当前类是配置类，`@EnableAutoConfiguration` 开启自动装配，`@ComponentScan` 开启组件扫描，默认扫描启动类所在包及其子包。

starter 是 Spring Boot 的依赖聚合机制，本质上是一个 Maven 或 Gradle 依赖包。starter 负责引入某个功能所需的一组依赖，自动配置类负责根据当前 classpath、配置属性和容器状态创建对应的 Bean。也就是说，starter 负责依赖聚合，auto-configuration 负责自动配置。

`application.yml` 的加载顺序可以简单理解为：外部配置优先于内部配置，profile 配置优先于普通配置，命令行参数优先于配置文件。如果 jar 包内部和外部都有配置文件，外部配置会覆盖内部配置。如果激活了 profile，例如 `prod`，那么 `application-prod.yml` 会覆盖 `application.yml` 中相同的配置项。

自定义 starter 通常分为两个模块：`xxx-spring-boot-autoconfigure` 和 `xxx-spring-boot-starter`。autoconfigure 模块负责编写配置属性类、核心功能类和自动配置类，并通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 暴露自动配置类。starter 模块负责聚合 autoconfigure 模块和相关第三方依赖。业务项目引入 starter 后，Spring Boot 会通过自动装配机制创建相关 Bean。

Spring Boot 优雅停机是指应用关闭时先停止接收新请求，然后等待已经进入的请求处理完成，最后释放资源并退出。常见配置是 `server.shutdown=graceful` 和 `spring.lifecycle.timeout-per-shutdown-phase`。生产环境中应该使用 `SIGTERM` 触发关闭流程，例如 `kill pid`，不要使用 `kill -9`，因为 `kill -9` 会强制杀死进程，导致 Spring 容器没有机会执行资源释放逻辑。
