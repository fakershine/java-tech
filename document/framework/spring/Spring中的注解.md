# Spring 中的常用注解

## 1. Spring 注解分类

Spring 常用注解可以按功能分为：

```text
组件注册注解
依赖注入注解
配置类注解
Bean 生命周期注解
AOP 注解
事务注解
Spring MVC 注解
参数校验注解
配置绑定注解
条件装配注解
异步和定时任务注解
缓存注解
测试注解
```

---

# 一、组件注册相关注解

## 2. `@Component`

`@Component` 是最基础的组件注解。

作用：

> 把一个类交给 Spring 容器管理。

示例：

```java
@Component
public class UserService {
}
```

Spring 启动时会扫描该类，并创建 Bean。

---

## 3. `@Service`

`@Service` 通常用在业务层。

```java
@Service
public class UserService {
}
```

本质上：

```text
@Service 是 @Component 的特殊语义版本
```

作用和 `@Component` 类似，只是语义更清晰。

---

## 4. `@Repository`

`@Repository` 通常用在 DAO / Mapper 层。

```java
@Repository
public class UserRepository {
}
```

作用：

```text
标识数据访问层组件
交给 Spring 容器管理
可以配合异常转换
```

---

## 5. `@Controller`

`@Controller` 通常用在 Spring MVC 控制层。

```java
@Controller
public class UserController {
}
```

作用：

```text
标识 Controller
接收 Web 请求
返回页面或数据
```

---

## 6. `@RestController`

`@RestController` 是 REST 接口常用注解。

```java
@RestController
public class UserController {
}
```

它等价于：

```java
@Controller
@ResponseBody
```

也就是说：

```text
返回值直接写入 HTTP 响应体
不会走视图解析器
```

---

## 7. `@Component`、`@Service`、`@Repository`、`@Controller` 区别

| 注解 | 常用层级 | 作用 |
|---|---|---|
| `@Component` | 通用组件 | 普通 Bean |
| `@Service` | 业务层 | Service Bean |
| `@Repository` | 数据访问层 | DAO / Repository Bean |
| `@Controller` | 控制层 | MVC Controller |
| `@RestController` | 控制层 | REST API Controller |

本质：

```text
@Service、@Repository、@Controller 都是 @Component 的衍生注解
```

---

# 二、依赖注入相关注解

## 8. `@Autowired`

`@Autowired` 是 Spring 提供的依赖注入注解。

默认按类型注入。

```java
@Service
public class OrderService {

    @Autowired
    private UserService userService;
}
```

如果同类型有多个 Bean，可以配合 `@Qualifier`。

---

## 9. `@Qualifier`

`@Qualifier` 用来指定 Bean 名称。

```java
@Autowired
@Qualifier("userServiceImplA")
private UserService userService;
```

适合多个实现类场景。

---

## 10. `@Resource`

`@Resource` 默认按名称注入。

```java
@Resource
private UserService userService;
```

指定名称：

```java
@Resource(name = "userServiceImplA")
private UserService userService;
```

区别：

```text
@Autowired 默认按类型
@Resource 默认按名称
```

---

## 11. `@Value`

`@Value` 用于注入配置值。

```java
@Value("${server.port}")
private Integer port;
```

也可以写默认值：

```java
@Value("${app.name:default-app}")
private String appName;
```

---

## 12. 构造器注入

实际开发中推荐构造器注入。

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
方便测试
避免字段注入隐藏问题
对象创建时依赖完整
```

---

# 三、配置类相关注解

## 13. `@Configuration`

`@Configuration` 表示当前类是配置类。

```java
@Configuration
public class AppConfig {
}
```

配置类中通常配合 `@Bean` 使用。

---

## 14. `@Bean`

`@Bean` 用于手动注册 Bean。

```java
@Configuration
public class AppConfig {

    @Bean
    public UserService userService() {
        return new UserService();
    }
}
```

Bean 名称默认是方法名：

```text
userService
```

也可以指定名称：

```java
@Bean("myUserService")
public UserService userService() {
    return new UserService();
}
```

---

## 15. `@ComponentScan`

`@ComponentScan` 用于指定扫描路径。

```java
@ComponentScan("com.example")
@Configuration
public class AppConfig {
}
```

Spring Boot 中通常不用手动写，因为 `@SpringBootApplication` 已经包含了它。

---

## 16. `@Import`

`@Import` 用于导入配置类或组件。

```java
@Import(UserConfig.class)
@Configuration
public class AppConfig {
}
```

常见用途：

```text
导入普通配置类
导入 ImportSelector
导入 ImportBeanDefinitionRegistrar
实现 EnableXXX 注解
```

---

## 17. `@PropertySource`

`@PropertySource` 用于加载指定配置文件。

```java
@PropertySource("classpath:app.properties")
@Configuration
public class AppConfig {
}
```

然后可以配合 `@Value` 使用：

```java
@Value("${app.name}")
private String appName;
```

---

# 四、Spring Boot 核心注解

## 18. `@SpringBootApplication`

`@SpringBootApplication` 是 Spring Boot 启动类核心注解。

```java
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

它组合了三个注解：

```java
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan
```

---

## 19. `@EnableAutoConfiguration`

`@EnableAutoConfiguration` 开启自动装配。

作用：

```text
根据 classpath 中的依赖
根据配置文件
根据条件注解
自动创建 Bean
```

例如引入 Web 依赖后，Spring Boot 会自动配置 Tomcat、Spring MVC 等组件。

---

## 20. `@SpringBootConfiguration`

`@SpringBootConfiguration` 本质上也是配置类注解。

它是 `@Configuration` 的特殊版本。

---

# 五、条件装配注解

## 21. `@Conditional`

`@Conditional` 是 Spring 条件装配的基础注解。

```java
@Conditional(MyCondition.class)
@Bean
public UserService userService() {
    return new UserService();
}
```

只有条件满足时，Bean 才会注册。

---

## 22. Spring Boot 常用条件注解

| 注解 | 说明 |
|---|---|
| `@ConditionalOnClass` | classpath 中存在某个类时生效 |
| `@ConditionalOnMissingClass` | classpath 中不存在某个类时生效 |
| `@ConditionalOnBean` | 容器中存在某个 Bean 时生效 |
| `@ConditionalOnMissingBean` | 容器中不存在某个 Bean 时生效 |
| `@ConditionalOnProperty` | 配置属性满足条件时生效 |
| `@ConditionalOnWebApplication` | Web 应用环境生效 |
| `@ConditionalOnNotWebApplication` | 非 Web 应用环境生效 |
| `@ConditionalOnResource` | 指定资源存在时生效 |
| `@ConditionalOnExpression` | SpEL 表达式满足时生效 |

---

## 23. `@ConditionalOnMissingBean`

常用于自动配置中。

```java
@Bean
@ConditionalOnMissingBean
public UserService userService() {
    return new UserService();
}
```

含义：

```text
如果容器中没有 UserService，才创建默认 Bean
```

---

## 24. `@ConditionalOnProperty`

根据配置控制 Bean 是否生效。

```java
@Bean
@ConditionalOnProperty(
    prefix = "sms",
    name = "enabled",
    havingValue = "true"
)
public SmsClient smsClient() {
    return new SmsClient();
}
```

配置：

```yaml
sms:
  enabled: true
```

---

# 六、配置绑定相关注解

## 25. `@ConfigurationProperties`

用于把配置文件绑定到 Java 对象。

```java
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    private String accessKey;

    private String secretKey;

    private Integer timeout;

    // getter setter
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

## 26. `@EnableConfigurationProperties`

启用配置属性绑定。

```java
@Configuration
@EnableConfigurationProperties(SmsProperties.class)
public class SmsConfig {
}
```

---

## 27. `@ConfigurationPropertiesScan`

自动扫描 `@ConfigurationProperties` 类。

```java
@SpringBootApplication
@ConfigurationPropertiesScan
public class Application {
}
```

---

# 七、Bean 生命周期相关注解

## 28. `@PostConstruct`

Bean 初始化后执行。

```java
@Component
public class UserService {

    @PostConstruct
    public void init() {
        System.out.println("初始化");
    }
}
```

适合：

```text
加载缓存
初始化资源
注册任务
```

---

## 29. `@PreDestroy`

Bean 销毁前执行。

```java
@Component
public class UserService {

    @PreDestroy
    public void destroy() {
        System.out.println("销毁资源");
    }
}
```

适合：

```text
关闭线程池
关闭连接
释放资源
```

---

## 30. `@Scope`

`@Scope` 用于指定 Bean 作用域。

```java
@Component
@Scope("prototype")
public class UserService {
}
```

常见作用域：

| 作用域 | 说明 |
|---|---|
| `singleton` | 单例，默认 |
| `prototype` | 每次获取创建新对象 |
| `request` | 每个 HTTP 请求一个实例 |
| `session` | 每个 Session 一个实例 |

---

## 31. `@Lazy`

`@Lazy` 表示懒加载。

```java
@Component
@Lazy
public class UserService {
}
```

默认单例 Bean 在容器启动时创建。

加了 `@Lazy` 后：

```text
第一次使用时才创建
```

---

## 32. `@Primary`

`@Primary` 表示优先注入。

```java
@Service
@Primary
public class UserServiceImplA implements UserService {
}
```

当同类型有多个 Bean 时，优先选择 `@Primary` 标注的 Bean。

---

## 33. `@DependsOn`

指定 Bean 初始化依赖顺序。

```java
@Component
@DependsOn("dataSource")
public class UserService {
}
```

表示：

```text
先初始化 dataSource，再初始化 userService
```

---

# 八、AOP 相关注解

## 34. `@Aspect`

`@Aspect` 表示当前类是切面类。

```java
@Aspect
@Component
public class LogAspect {
}
```

---

## 35. `@Pointcut`

定义切点。

```java
@Pointcut("execution(* com.example.service.*.*(..))")
public void serviceMethods() {
}
```

---

## 36. `@Before`

方法执行前增强。

```java
@Before("serviceMethods()")
public void before() {
    System.out.println("方法执行前");
}
```

---

## 37. `@After`

方法执行后增强，无论是否异常都会执行。

```java
@After("serviceMethods()")
public void after() {
    System.out.println("方法执行后");
}
```

---

## 38. `@AfterReturning`

方法正常返回后增强。

```java
@AfterReturning(value = "serviceMethods()", returning = "result")
public void afterReturning(Object result) {
    System.out.println("返回结果：" + result);
}
```

---

## 39. `@AfterThrowing`

方法抛出异常后增强。

```java
@AfterThrowing(value = "serviceMethods()", throwing = "e")
public void afterThrowing(Exception e) {
    System.out.println("异常：" + e.getMessage());
}
```

---

## 40. `@Around`

环绕增强，功能最强。

```java
@Around("serviceMethods()")
public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
    System.out.println("方法前");

    Object result = joinPoint.proceed();

    System.out.println("方法后");

    return result;
}
```

---

# 九、事务相关注解

## 41. `@Transactional`

`@Transactional` 用于声明式事务。

```java
@Transactional(rollbackFor = Exception.class)
public void createOrder() {
    // 创建订单
    // 扣库存
}
```

常用属性：

| 属性 | 说明 |
|---|---|
| `propagation` | 事务传播行为 |
| `isolation` | 事务隔离级别 |
| `rollbackFor` | 指定回滚异常 |
| `noRollbackFor` | 指定不回滚异常 |
| `timeout` | 超时时间 |
| `readOnly` | 是否只读事务 |
| `transactionManager` | 指定事务管理器 |

---

## 42. `@EnableTransactionManagement`

开启事务管理。

```java
@EnableTransactionManagement
@Configuration
public class TransactionConfig {
}
```

Spring Boot 通常会自动开启，普通项目中可能需要手动加。

---

# 十、Spring MVC 注解

## 43. `@RequestMapping`

通用请求映射注解。

```java
@RequestMapping("/users")
public class UserController {
}
```

方法上：

```java
@RequestMapping(value = "/list", method = RequestMethod.GET)
public List<User> list() {
    return userService.list();
}
```

---

## 44. `@GetMapping`

处理 GET 请求。

```java
@GetMapping("/users")
public List<User> list() {
    return userService.list();
}
```

---

## 45. `@PostMapping`

处理 POST 请求。

```java
@PostMapping("/users")
public void save(@RequestBody UserDTO userDTO) {
}
```

---

## 46. `@PutMapping`

处理 PUT 请求。

```java
@PutMapping("/users/{id}")
public void update(@PathVariable Long id, @RequestBody UserDTO userDTO) {
}
```

---

## 47. `@DeleteMapping`

处理 DELETE 请求。

```java
@DeleteMapping("/users/{id}")
public void delete(@PathVariable Long id) {
}
```

---

## 48. `@PathVariable`

获取路径参数。

```java
@GetMapping("/users/{id}")
public User getById(@PathVariable Long id) {
    return userService.getById(id);
}
```

路径：

```text
/users/1
```

---

## 49. `@RequestParam`

获取请求参数。

```java
@GetMapping("/users")
public List<User> list(@RequestParam String name) {
    return userService.list(name);
}
```

请求：

```text
/users?name=Tom
```

指定默认值：

```java
@RequestParam(defaultValue = "1") Integer pageNo
```

---

## 50. `@RequestBody`

接收 JSON 请求体。

```java
@PostMapping("/users")
public void save(@RequestBody UserDTO userDTO) {
}
```

请求体：

```json
{
  "name": "Tom",
  "age": 18
}
```

---

## 51. `@ResponseBody`

把返回值写入 HTTP 响应体。

```java
@ResponseBody
@GetMapping("/users/{id}")
public User getById(@PathVariable Long id) {
    return userService.getById(id);
}
```

`@RestController` 已经包含 `@ResponseBody`。

---

## 52. `@RequestHeader`

获取请求头。

```java
@GetMapping("/token")
public String token(@RequestHeader("Authorization") String token) {
    return token;
}
```

---

## 53. `@CookieValue`

获取 Cookie。

```java
@GetMapping("/cookie")
public String cookie(@CookieValue("token") String token) {
    return token;
}
```

---

## 54. `@ModelAttribute`

常用于表单参数绑定。

```java
@PostMapping("/user")
public String save(@ModelAttribute UserDTO userDTO) {
    return "success";
}
```

---

# 十一、异常处理相关注解

## 55. `@ControllerAdvice`

全局 Controller 增强。

```java
@ControllerAdvice
public class GlobalControllerAdvice {
}
```

---

## 56. `@RestControllerAdvice`

等价于：

```java
@ControllerAdvice
@ResponseBody
```

常用于 REST 接口全局异常处理。

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
}
```

---

## 57. `@ExceptionHandler`

处理指定异常。

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String handle(Exception e) {
        return "系统异常";
    }
}
```

---

# 十二、参数校验相关注解

## 58. `@Valid`

标准参数校验注解。

```java
@PostMapping("/user")
public void save(@RequestBody @Valid UserDTO userDTO) {
}
```

---

## 59. `@Validated`

Spring 提供的校验注解，支持分组校验。

```java
@PostMapping("/user")
public void save(@RequestBody @Validated(AddGroup.class) UserDTO userDTO) {
}
```

---

## 60. 常见校验注解

| 注解 | 说明 |
|---|---|
| `@NotNull` | 不能为 null |
| `@NotBlank` | 字符串不能为空 |
| `@NotEmpty` | 集合、数组、字符串不能为空 |
| `@Size` | 长度或大小限制 |
| `@Min` | 最小值 |
| `@Max` | 最大值 |
| `@Pattern` | 正则校验 |
| `@Email` | 邮箱格式 |
| `@Positive` | 必须为正数 |
| `@Past` | 必须是过去时间 |
| `@Future` | 必须是未来时间 |

---

# 十三、异步和定时任务注解

## 61. `@EnableAsync`

开启异步方法支持。

```java
@EnableAsync
@SpringBootApplication
public class Application {
}
```

---

## 62. `@Async`

异步执行方法。

```java
@Async
public void sendSms() {
    // 异步发送短信
}
```

注意：

```text
@Async 方法必须通过 Spring 代理对象调用才生效
同类内部调用可能失效
```

---

## 63. `@EnableScheduling`

开启定时任务。

```java
@EnableScheduling
@SpringBootApplication
public class Application {
}
```

---

## 64. `@Scheduled`

定义定时任务。

```java
@Scheduled(cron = "0/5 * * * * ?")
public void task() {
    System.out.println("每 5 秒执行一次");
}
```

常用属性：

| 属性 | 说明 |
|---|---|
| `cron` | Cron 表达式 |
| `fixedRate` | 固定频率 |
| `fixedDelay` | 固定延迟 |
| `initialDelay` | 初始延迟 |

---

# 十四、缓存相关注解

## 65. `@EnableCaching`

开启缓存支持。

```java
@EnableCaching
@SpringBootApplication
public class Application {
}
```

---

## 66. `@Cacheable`

查询时先查缓存，缓存没有再执行方法。

```java
@Cacheable(cacheNames = "user", key = "#id")
public User getById(Long id) {
    return userMapper.selectById(id);
}
```

---

## 67. `@CachePut`

方法一定执行，并把结果放入缓存。

```java
@CachePut(cacheNames = "user", key = "#user.id")
public User update(User user) {
    userMapper.updateById(user);
    return user;
}
```

---

## 68. `@CacheEvict`

删除缓存。

```java
@CacheEvict(cacheNames = "user", key = "#id")
public void delete(Long id) {
    userMapper.deleteById(id);
}
```

删除全部缓存：

```java
@CacheEvict(cacheNames = "user", allEntries = true)
public void clearUserCache() {
}
```

---

# 十五、事件相关注解

## 69. `@EventListener`

监听 Spring 事件。

```java
@Component
public class OrderEventListener {

    @EventListener
    public void handle(OrderCreatedEvent event) {
        System.out.println("订单创建：" + event.getOrderId());
    }
}
```

发布事件：

```java
applicationEventPublisher.publishEvent(new OrderCreatedEvent("1001"));
```

---

# 十六、测试相关注解

## 70. `@SpringBootTest`

启动 Spring Boot 测试上下文。

```java
@SpringBootTest
class UserServiceTest {
}
```

---

## 71. `@MockBean`

在测试中替换 Spring 容器中的 Bean。

```java
@MockBean
private UserService userService;
```

---

## 72. `@WebMvcTest`

只测试 Web 层。

```java
@WebMvcTest(UserController.class)
class UserControllerTest {
}
```

---

## 73. `@DataJpaTest`

只测试 JPA 相关组件。

```java
@DataJpaTest
class UserRepositoryTest {
}
```

---

# 十七、常见 Enable 注解

## 74. 常见 `@EnableXXX`

| 注解 | 作用 |
|---|---|
| `@EnableAutoConfiguration` | 开启自动装配 |
| `@EnableTransactionManagement` | 开启事务管理 |
| `@EnableAsync` | 开启异步 |
| `@EnableScheduling` | 开启定时任务 |
| `@EnableCaching` | 开启缓存 |
| `@EnableAspectJAutoProxy` | 开启 AOP |
| `@EnableConfigurationProperties` | 开启配置属性绑定 |

---

# 十八、常见注解对比

## 75. `@Controller` 和 `@RestController`

| 对比项 | `@Controller` | `@RestController` |
|---|---|---|
| 返回值 | 默认走视图解析 | 直接返回 JSON / 文本 |
| 是否包含 `@ResponseBody` | 不包含 | 包含 |
| 常见场景 | 页面应用 | REST API |

---

## 76. `@RequestParam` 和 `@PathVariable`

| 对比项 | `@RequestParam` | `@PathVariable` |
|---|---|---|
| 来源 | Query 参数 | URL 路径 |
| 示例 | `/users?id=1` | `/users/1` |
| 常见用途 | 查询条件 | 资源 ID |

---

## 77. `@RequestBody` 和 `@RequestParam`

| 对比项 | `@RequestBody` | `@RequestParam` |
|---|---|---|
| 数据位置 | 请求体 Body | URL 参数 / 表单 |
| 常见格式 | JSON | query string / form |
| 常见请求 | POST / PUT | GET / POST |

---

## 78. `@Component` 和 `@Bean`

| 对比项 | `@Component` | `@Bean` |
|---|---|---|
| 使用位置 | 类上 | 方法上 |
| 创建方式 | Spring 扫描类创建 | 手动调用方法创建 |
| 适合对象 | 自己写的类 | 第三方类、复杂对象 |
| 是否需要扫描 | 需要 | 配置类中声明 |

---

## 79. `@Autowired` 和 `@Resource`

| 对比项 | `@Autowired` | `@Resource` |
|---|---|---|
| 来源 | Spring | JDK / Jakarta |
| 默认方式 | 按类型 | 按名称 |
| 多 Bean 处理 | `@Qualifier` / `@Primary` | `name` 属性 |
| required | 支持 | 不支持 |

---

## 80. `@Valid` 和 `@Validated`

| 对比项 | `@Valid` | `@Validated` |
|---|---|---|
| 来源 | Java 标准 | Spring |
| 普通校验 | 支持 | 支持 |
| 嵌套校验 | 支持 | 支持 |
| 分组校验 | 不支持 | 支持 |
| 方法级校验 | 一般不用 | 常用 |

---

# 十九、总结

Spring 中的注解可以按功能分类理解。

第一类是组件注册注解，比如 `@Component`、`@Service`、`@Repository`、`@Controller`、`@RestController`，它们的作用是把类交给 Spring 容器管理。

第二类是依赖注入注解，比如 `@Autowired`、`@Qualifier`、`@Resource`、`@Value`。`@Autowired` 默认按类型注入，`@Resource` 默认按名称注入。

第三类是配置相关注解，比如 `@Configuration`、`@Bean`、`@ComponentScan`、`@Import`、`@PropertySource`。其中 `@Configuration` 表示配置类，`@Bean` 用来手动注册 Bean。

第四类是 Spring Boot 核心注解，比如 `@SpringBootApplication`，它组合了 `@SpringBootConfiguration`、`@EnableAutoConfiguration` 和 `@ComponentScan`，分别表示配置类、开启自动装配和组件扫描。

第五类是 Web 层注解，比如 `@RequestMapping`、`@GetMapping`、`@PostMapping`、`@PathVariable`、`@RequestParam`、`@RequestBody`、`@ResponseBody` 等，用于处理 HTTP 请求和参数绑定。

第六类是事务、AOP、缓存、异步、定时任务相关注解，比如 `@Transactional`、`@Aspect`、`@Cacheable`、`@Async`、`@Scheduled` 等。

实际面试时可以重点说清楚：组件注册、依赖注入、配置类、自动装配、Web 请求、事务和 AOP 这几类注解。

---

## 81. 一句话总结

> Spring 注解本质上是为了减少 XML 配置，通过注解完成 Bean 注册、依赖注入、配置装配、Web 请求映射、事务控制、AOP 增强、缓存、异步和定时任务等功能；最核心的是 `@Component`、`@Autowired`、`@Configuration`、`@Bean`、`@SpringBootApplication`、`@RequestMapping` 和 `@Transactional`。
