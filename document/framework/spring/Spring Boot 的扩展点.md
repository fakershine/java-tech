# Spring Boot 的扩展点

## 1. 什么是 Spring Boot 扩展点

Spring Boot 扩展点可以理解为：

> 在 Spring Boot 启动、配置加载、Bean 注册、Bean 创建、容器刷新、应用启动完成等不同阶段，允许我们插入自定义逻辑。

常见用途：

```text
加载外部配置
修改 BeanDefinition
动态注册 Bean
增强 Bean
创建代理对象
监听启动事件
缓存预热
启动后执行任务
自动装配 Starter
Web 请求扩展
全局异常处理
```

---

## 2. Spring Boot 启动流程简图

```text
main 方法
   ↓
SpringApplication.run()
   ↓
创建 SpringApplication
   ↓
准备 Environment
   ↓
创建 ApplicationContext
   ↓
ApplicationContextInitializer
   ↓
加载 BeanDefinition
   ↓
BeanFactoryPostProcessor
   ↓
实例化 Bean
   ↓
BeanPostProcessor
   ↓
刷新容器
   ↓
ApplicationRunner / CommandLineRunner
   ↓
应用启动完成
```

---

# 一、启动阶段扩展点

## 3. ApplicationListener

`ApplicationListener` 用来监听 Spring Boot 启动过程中的事件。

常见事件：

```text
ApplicationStartingEvent
ApplicationEnvironmentPreparedEvent
ApplicationContextInitializedEvent
ApplicationPreparedEvent
ApplicationStartedEvent
ApplicationReadyEvent
ApplicationFailedEvent
```

适合场景：

```text
启动日志
环境检查
启动耗时统计
失败告警
应用启动完成通知
```

示例：

```java
@Component
public class MyApplicationListener
        implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        System.out.println("应用启动完成");
    }
}
```

如果要监听很早期的事件，比如 `ApplicationStartingEvent`，此时 Spring 容器还没创建，不能只靠 `@Component`，需要手动注册：

```java
public class Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Application.class);
        application.addListeners(new MyEarlyApplicationListener());
        application.run(args);
    }
}
```

---

## 4. ApplicationContextInitializer

`ApplicationContextInitializer` 会在 `ApplicationContext` 创建后、`refresh()` 之前执行。

适合场景：

```text
修改 Environment
添加 PropertySource
设置 active profiles
注册基础组件
在容器刷新前做初始化
```

示例：

```java
public class MyApplicationContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();

        Map<String, Object> map = new HashMap<>();
        map.put("custom.key", "custom-value");

        environment.getPropertySources().addFirst(
                new MapPropertySource("customPropertySource", map)
        );
    }
}
```

注册方式：

```java
public class Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Application.class);
        application.addInitializers(new MyApplicationContextInitializer());
        application.run(args);
    }
}
```

---

## 5. EnvironmentPostProcessor

`EnvironmentPostProcessor` 是 Spring Boot 提供的扩展点，用于在容器刷新前处理 `Environment`。

适合场景：

```text
加载外部配置
解密配置
动态添加配置源
修改配置优先级
配置中心接入
```

示例：

```java
public class MyEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment,
            SpringApplication application
    ) {
        Map<String, Object> map = new HashMap<>();
        map.put("app.name", "demo-app");

        environment.getPropertySources().addFirst(
                new MapPropertySource("myProperties", map)
        );
    }
}
```

Spring Boot 3 推荐注册文件：

```text
META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor
```

内容：

```text
com.example.MyEnvironmentPostProcessor
```

Spring Boot 2.x 常见注册方式：

```text
META-INF/spring.factories
```

```properties
org.springframework.boot.env.EnvironmentPostProcessor=\
com.example.MyEnvironmentPostProcessor
```

---

# 二、BeanDefinition 阶段扩展点

## 6. ImportSelector

`ImportSelector` 可以根据条件动态导入配置类。

常用于：

```text
@EnableXXX 注解
框架自动开启某些功能
根据条件导入配置类
```

示例：

```java
public class MyImportSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[]{
                "com.example.config.MyConfig"
        };
    }
}
```

自定义注解：

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(MyImportSelector.class)
public @interface EnableMyFeature {
}
```

使用：

```java
@EnableMyFeature
@SpringBootApplication
public class Application {
}
```

---

## 7. DeferredImportSelector

`DeferredImportSelector` 是延迟执行的 `ImportSelector`。

适合场景：

```text
自动装配
批量导入配置类
需要在普通配置类处理后再导入
```

简单理解：

```text
ImportSelector：普通导入
DeferredImportSelector：延迟导入，优先级更靠后
```

Spring Boot 自动装配底层就和这类机制有关。

---

## 8. ImportBeanDefinitionRegistrar

`ImportBeanDefinitionRegistrar` 可以直接向 Spring 容器注册 `BeanDefinition`。

适合场景：

```text
动态注册 Bean
扫描自定义注解
Mapper 接口注册
RPC 接口代理注册
FeignClient 注册
```

示例：

```java
public class MyImportBeanDefinitionRegistrar
        implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(
            AnnotationMetadata importingClassMetadata,
            BeanDefinitionRegistry registry
    ) {
        RootBeanDefinition beanDefinition =
                new RootBeanDefinition(MyService.class);

        registry.registerBeanDefinition("myService", beanDefinition);
    }
}
```

配合注解：

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(MyImportBeanDefinitionRegistrar.class)
public @interface EnableMyRegistrar {
}
```

---

## 9. BeanDefinitionRegistryPostProcessor

`BeanDefinitionRegistryPostProcessor` 可以在 Bean 实例化之前修改或注册 `BeanDefinition`。

它比 `BeanFactoryPostProcessor` 更早。

适合场景：

```text
动态注册 BeanDefinition
修改扫描出来的 BeanDefinition
注册代理类 BeanDefinition
框架级 Bean 扩展
```

示例：

```java
@Component
public class MyBeanDefinitionRegistryPostProcessor
        implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(
            BeanDefinitionRegistry registry
    ) throws BeansException {
        RootBeanDefinition beanDefinition =
                new RootBeanDefinition(MyService.class);

        registry.registerBeanDefinition("myService", beanDefinition);
    }

    @Override
    public void postProcessBeanFactory(
            ConfigurableListableBeanFactory beanFactory
    ) throws BeansException {
    }
}
```

---

## 10. BeanFactoryPostProcessor

`BeanFactoryPostProcessor` 可以在 Bean 实例化之前修改 `BeanDefinition`。

适合场景：

```text
修改 BeanDefinition 属性
修改 Bean 作用域
修改 Bean 初始化方式
读取配置动态调整 Bean 定义
```

示例：

```java
@Component
public class MyBeanFactoryPostProcessor
        implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(
            ConfigurableListableBeanFactory beanFactory
    ) throws BeansException {

        BeanDefinition beanDefinition =
                beanFactory.getBeanDefinition("userService");

        beanDefinition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    }
}
```

---

# 三、Bean 创建阶段扩展点

## 11. BeanPostProcessor

`BeanPostProcessor` 是最常用的 Spring 扩展点之一。

它可以在 Bean 初始化前后进行增强。

适合场景：

```text
Bean 初始化前后增强
包装代理对象
处理自定义注解
统一字段注入
AOP 代理
日志增强
```

示例：

```java
@Component
public class MyBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(
            Object bean,
            String beanName
    ) throws BeansException {
        System.out.println("初始化前：" + beanName);
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(
            Object bean,
            String beanName
    ) throws BeansException {
        System.out.println("初始化后：" + beanName);
        return bean;
    }
}
```

---

## 12. InstantiationAwareBeanPostProcessor

`InstantiationAwareBeanPostProcessor` 是 `BeanPostProcessor` 的子接口。

它介入得更早，可以在 Bean 实例化前后做处理。

适合场景：

```text
实例化前创建代理对象
属性注入前处理
自定义依赖注入逻辑
AOP 底层扩展
```

常见方法：

```java
postProcessBeforeInstantiation()
postProcessAfterInstantiation()
postProcessProperties()
```

示例：

```java
@Component
public class MyInstantiationAwareBeanPostProcessor
        implements InstantiationAwareBeanPostProcessor {

    @Override
    public Object postProcessBeforeInstantiation(
            Class<?> beanClass,
            String beanName
    ) throws BeansException {
        System.out.println("实例化前：" + beanName);
        return null;
    }
}
```

---

## 13. SmartInstantiationAwareBeanPostProcessor

`SmartInstantiationAwareBeanPostProcessor` 更偏底层。

主要用于：

```text
预测 Bean 类型
推断构造方法
提前暴露代理对象
处理循环依赖中的代理问题
```

Spring AOP 解决循环依赖时就涉及这类扩展点。

---

# 四、Bean 生命周期扩展点

## 14. @PostConstruct

Bean 属性注入完成后执行。

适合场景：

```text
初始化资源
加载本地缓存
注册本地任务
启动前准备
```

示例：

```java
@Component
public class UserService {

    @PostConstruct
    public void init() {
        System.out.println("UserService 初始化");
    }
}
```

---

## 15. InitializingBean

`InitializingBean` 的 `afterPropertiesSet()` 会在属性设置完成后执行。

示例：

```java
@Component
public class UserService implements InitializingBean {

    @Override
    public void afterPropertiesSet() {
        System.out.println("属性设置完成后执行");
    }
}
```

---

## 16. initMethod

可以通过 `@Bean(initMethod = "...")` 指定初始化方法。

```java
@Configuration
public class MyConfig {

    @Bean(initMethod = "init")
    public MyService myService() {
        return new MyService();
    }
}
```

```java
public class MyService {

    public void init() {
        System.out.println("init method");
    }
}
```

---

## 17. @PreDestroy

Bean 销毁前执行。

适合场景：

```text
释放资源
关闭线程池
关闭连接
清理缓存
```

示例：

```java
@Component
public class UserService {

    @PreDestroy
    public void destroy() {
        System.out.println("销毁资源");
    }
}
```

---

## 18. DisposableBean

`DisposableBean` 的 `destroy()` 会在 Bean 销毁时执行。

```java
@Component
public class UserService implements DisposableBean {

    @Override
    public void destroy() {
        System.out.println("Bean 销毁");
    }
}
```

---

## 19. destroyMethod

```java
@Configuration
public class MyConfig {

    @Bean(destroyMethod = "close")
    public MyClient myClient() {
        return new MyClient();
    }
}
```

---

# 五、容器刷新完成后的扩展点

## 20. SmartInitializingSingleton

`SmartInitializingSingleton` 会在所有单例 Bean 初始化完成后执行。

适合场景：

```text
所有 Bean 准备好后执行初始化
检查依赖完整性
预热本地缓存
启动定时任务
```

示例：

```java
@Component
public class MySmartInitializingSingleton
        implements SmartInitializingSingleton {

    @Override
    public void afterSingletonsInstantiated() {
        System.out.println("所有单例 Bean 初始化完成");
    }
}
```

---

## 21. ApplicationRunner

`ApplicationRunner` 会在 Spring Boot 应用启动后执行。

它接收的是封装后的 `ApplicationArguments`。

适合场景：

```text
启动后初始化数据
预热缓存
执行一次性任务
启动完成后检查
```

示例：

```java
@Component
public class MyApplicationRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("ApplicationRunner 执行");
    }
}
```

---

## 22. CommandLineRunner

`CommandLineRunner` 也会在应用启动后执行。

它接收的是原始字符串参数。

示例：

```java
@Component
public class MyCommandLineRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        System.out.println("CommandLineRunner 执行");
    }
}
```

排序：

```java
@Component
@Order(1)
public class FirstRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        System.out.println("第一个执行");
    }
}
```

---

# 六、自动装配扩展点

## 23. AutoConfiguration

自动装配是 Spring Boot 最核心的扩展机制。

适合场景：

```text
封装 starter
公司内部通用组件
自动创建默认 Bean
按条件启用功能
降低业务系统接入成本
```

---

## 24. 自定义 AutoConfiguration

Spring Boot 3 推荐使用 `@AutoConfiguration`。

示例：

```java
@AutoConfiguration
@ConditionalOnClass(MyClient.class)
@EnableConfigurationProperties(MyProperties.class)
public class MyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MyClient myClient(MyProperties properties) {
        return new MyClient(properties.getUrl());
    }
}
```

配置属性：

```java
@ConfigurationProperties(prefix = "my.client")
public class MyProperties {

    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
```

配置文件：

```yaml
my:
  client:
    url: http://localhost:8080
```

---

## 25. 注册 AutoConfiguration

Spring Boot 3 使用：

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

内容：

```text
com.example.autoconfigure.MyAutoConfiguration
```

Spring Boot 2.7 之前常见方式是：

```text
META-INF/spring.factories
```

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.example.autoconfigure.MyAutoConfiguration
```

---

## 26. 常用条件注解

| 注解 | 说明 |
|---|---|
| `@ConditionalOnClass` | classpath 中存在某个类时生效 |
| `@ConditionalOnMissingClass` | classpath 中不存在某个类时生效 |
| `@ConditionalOnBean` | 容器中存在某个 Bean 时生效 |
| `@ConditionalOnMissingBean` | 容器中不存在某个 Bean 时生效 |
| `@ConditionalOnProperty` | 配置属性满足条件时生效 |
| `@ConditionalOnWebApplication` | Web 应用环境下生效 |
| `@ConditionalOnNotWebApplication` | 非 Web 应用环境下生效 |
| `@ConditionalOnResource` | 存在指定资源时生效 |
| `@ConditionalOnExpression` | SpEL 表达式满足时生效 |

---

# 七、配置绑定扩展点

## 27. @ConfigurationProperties

`@ConfigurationProperties` 用于把配置文件绑定到 Java 对象。

适合场景：

```text
组件配置
starter 配置
多字段结构化配置
类型安全配置
```

示例：

```java
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    private String accessKey;

    private String secretKey;

    private Integer timeout;

    // getter setter
}
```

启用：

```java
@EnableConfigurationProperties(SmsProperties.class)
@Configuration
public class SmsConfig {
}
```

配置：

```yaml
sms:
  access-key: abc
  secret-key: 123
  timeout: 3000
```

---

## 28. Converter / GenericConverter / Formatter

用于自定义类型转换。

适合场景：

```text
字符串转枚举
字符串转复杂对象
配置绑定类型转换
Web 参数绑定
```

示例：

```java
@Component
public class StringToUserTypeConverter
        implements Converter<String, UserType> {

    @Override
    public UserType convert(String source) {
        return UserType.valueOf(source.toUpperCase());
    }
}
```

---

# 八、Web 层扩展点

## 29. WebMvcConfigurer

`WebMvcConfigurer` 是 Spring MVC 常用扩展点。

适合场景：

```text
添加拦截器
跨域配置
参数解析器
消息转换器
静态资源映射
格式化器
视图控制器
```

示例：

```java
@Configuration
public class MyWebMvcConfigurer implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MyInterceptor())
                .addPathPatterns("/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedMethods("*")
                .allowedOrigins("*");
    }
}
```

---

## 30. HandlerInterceptor

拦截 Spring MVC 请求。

适合场景：

```text
登录校验
权限校验
接口耗时统计
TraceId 设置
请求日志
```

示例：

```java
public class MyInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        System.out.println("请求前");
        return true;
    }
}
```

---

## 31. Filter

Servlet 过滤器，比 Spring MVC 拦截器更早。

适合场景：

```text
请求包装
字符编码
跨域处理
安全过滤
请求日志
TraceId
```

示例：

```java
@Component
public class MyFilter implements Filter {

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {
        System.out.println("Filter before");
        chain.doFilter(request, response);
        System.out.println("Filter after");
    }
}
```

---

## 32. HandlerMethodArgumentResolver

自定义 Controller 方法参数解析。

适合场景：

```text
注入当前登录用户
解析 Token
解析租户信息
解析自定义注解参数
```

示例：

```java
public class CurrentUserArgumentResolver
        implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        return new UserDTO(1L, "Tom");
    }
}
```

注册：

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(
            List<HandlerMethodArgumentResolver> resolvers
    ) {
        resolvers.add(new CurrentUserArgumentResolver());
    }
}
```

---

## 33. HttpMessageConverter

用于请求体和响应体转换。

适合场景：

```text
JSON 序列化
XML 转换
自定义加密响应
自定义媒体类型
```

示例：

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void extendMessageConverters(
            List<HttpMessageConverter<?>> converters
    ) {
        // 添加或调整消息转换器
    }
}
```

---

## 34. ControllerAdvice

`@ControllerAdvice` 常用于全局增强 Controller。

适合场景：

```text
全局异常处理
全局数据绑定
统一响应处理
```

示例：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e) {
        return "系统异常：" + e.getMessage();
    }
}
```

---

# 九、事件机制扩展点

## 35. ApplicationEventPublisher

Spring 支持事件发布和监听。

适合场景：

```text
业务解耦
领域事件
异步通知
缓存刷新
日志记录
```

定义事件：

```java
public class OrderCreatedEvent extends ApplicationEvent {

    private final String orderId;

    public OrderCreatedEvent(Object source, String orderId) {
        super(source);
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }
}
```

发布事件：

```java
@Service
public class OrderService {

    @Autowired
    private ApplicationEventPublisher publisher;

    public void createOrder() {
        publisher.publishEvent(new OrderCreatedEvent(this, "1001"));
    }
}
```

监听事件：

```java
@Component
public class OrderEventListener {

    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        System.out.println("订单创建：" + event.getOrderId());
    }
}
```

---

# 十、Aware 接口扩展点

## 36. 常见 Aware 接口

Aware 接口用于让 Bean 感知 Spring 容器中的某些对象。

| 接口 | 作用 |
|---|---|
| `BeanNameAware` | 获取 Bean 名称 |
| `BeanFactoryAware` | 获取 BeanFactory |
| `ApplicationContextAware` | 获取 ApplicationContext |
| `EnvironmentAware` | 获取 Environment |
| `ResourceLoaderAware` | 获取 ResourceLoader |
| `ApplicationEventPublisherAware` | 获取事件发布器 |

示例：

```java
@Component
public class MyApplicationContextAware
        implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(
            ApplicationContext applicationContext
    ) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
```

---

# 十一、扩展点执行顺序总结

## 37. 大致顺序

```text
SpringApplicationRunListener
   ↓
ApplicationListener 早期事件
   ↓
EnvironmentPostProcessor
   ↓
ApplicationContextInitializer
   ↓
BeanDefinitionRegistryPostProcessor
   ↓
BeanFactoryPostProcessor
   ↓
InstantiationAwareBeanPostProcessor
   ↓
Bean 实例化
   ↓
属性注入
   ↓
Aware 接口
   ↓
BeanPostProcessor#before
   ↓
@PostConstruct
   ↓
InitializingBean
   ↓
initMethod
   ↓
BeanPostProcessor#after
   ↓
SmartInitializingSingleton
   ↓
ApplicationStartedEvent
   ↓
ApplicationRunner / CommandLineRunner
   ↓
ApplicationReadyEvent
```

---

# 十二、常见扩展点对比

| 扩展点 | 执行时机 | 常见用途 |
|---|---|---|
| `ApplicationListener` | 监听启动事件 | 启动日志、失败告警 |
| `EnvironmentPostProcessor` | Environment 准备后 | 加载外部配置、配置解密 |
| `ApplicationContextInitializer` | Context refresh 前 | 修改环境、注册属性源 |
| `ImportSelector` | 配置类解析阶段 | 动态导入配置 |
| `ImportBeanDefinitionRegistrar` | BeanDefinition 注册阶段 | 动态注册 Bean |
| `BeanDefinitionRegistryPostProcessor` | BeanDefinition 注册后 | 增删改 BeanDefinition |
| `BeanFactoryPostProcessor` | Bean 实例化前 | 修改 BeanDefinition |
| `BeanPostProcessor` | Bean 初始化前后 | 包装代理、注解处理 |
| `@PostConstruct` | 属性注入后 | 初始化资源 |
| `InitializingBean` | 属性注入后 | 初始化资源 |
| `SmartInitializingSingleton` | 所有单例 Bean 初始化后 | 缓存预热、依赖检查 |
| `ApplicationRunner` | 容器启动后 | 启动任务 |
| `CommandLineRunner` | 容器启动后 | 启动任务 |
| `WebMvcConfigurer` | Web MVC 配置阶段 | 拦截器、跨域、转换器 |
| `ControllerAdvice` | Controller 调用阶段 | 全局异常处理 |

---

# 十三、总结

Spring Boot 提供了很多扩展点，可以按启动流程来理解。

在启动早期，可以使用 `ApplicationListener` 监听启动事件，也可以使用 `EnvironmentPostProcessor` 修改环境变量、加载外部配置，或者使用 `ApplicationContextInitializer` 在容器刷新前做初始化。

在 BeanDefinition 阶段，可以使用 `ImportSelector`、`ImportBeanDefinitionRegistrar`、`BeanDefinitionRegistryPostProcessor` 和 `BeanFactoryPostProcessor` 动态导入配置类、注册 BeanDefinition 或修改 BeanDefinition。

在 Bean 创建阶段，可以使用 `BeanPostProcessor`、`InstantiationAwareBeanPostProcessor` 对 Bean 初始化前后进行增强，Spring AOP、事务代理等底层都依赖这类扩展点。

在 Bean 生命周期阶段，可以使用 `@PostConstruct`、`InitializingBean`、`initMethod` 做初始化，使用 `@PreDestroy`、`DisposableBean`、`destroyMethod` 做销毁。

在应用启动完成后，可以使用 `ApplicationRunner` 和 `CommandLineRunner` 执行启动任务，比如缓存预热、数据初始化等。

此外，Spring Boot 最核心的扩展机制是自动装配，可以通过自定义 `@AutoConfiguration`、`@ConditionalOnClass`、`@ConditionalOnMissingBean` 和 `AutoConfiguration.imports` 来封装 starter，实现组件的自动配置。

---

# 十四、一句话总结

> Spring Boot 的扩展点本质上就是在启动流程和 Bean 生命周期的关键阶段预留回调入口；常用扩展点包括 `EnvironmentPostProcessor`、`ApplicationContextInitializer`、`BeanFactoryPostProcessor`、`BeanPostProcessor`、`ApplicationRunner`、`CommandLineRunner`、自动装配和 `WebMvcConfigurer`。
