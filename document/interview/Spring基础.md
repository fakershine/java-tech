# Spring 核心常见面试题总结

## 目录

- [一、Spring IoC 是什么](#一spring-ioc-是什么)
- [二、Spring AOP 是什么](#二spring-aop-是什么)
- [三、Bean 生命周期](#三bean-生命周期)
- [四、BeanFactory 和 ApplicationContext 区别](#四beanfactory-和-applicationcontext-区别)
- [五、Spring 如何解决循环依赖](#五spring-如何解决循环依赖)
- [六、三级缓存是什么](#六三级缓存是什么)
- [七、@Autowired 和 @Resource 区别](#七autowired-和-resource-区别)
- [八、Spring 事务传播行为有哪些](#八spring-事务传播行为有哪些)
- [九、Spring 事务失效场景有哪些](#九spring-事务失效场景有哪些)
- [十、面试速记版](#十面试速记版)
- [十一、总览表](#十一总览表)
- [十二、完整面试回答模板](#十二完整面试回答模板)

---

# 一、Spring IoC 是什么

## 1. IoC 是什么？

IoC 全称是：

```text
Inversion of Control
```

中文叫：

```text
控制反转
```

它是一种设计思想。

简单来说：

> 对象的创建、依赖关系的维护，不再由程序员在代码中手动控制，而是交给 Spring 容器统一管理。

---

## 2. 没有 IoC 之前的问题

没有 Spring IoC 时，我们通常需要自己创建对象。

示例：

```java
public class UserService {

    private UserDao userDao = new UserDao();

    public void save() {
        userDao.save();
    }
}
```

这种方式的问题是：

1. 对象创建由代码自己控制；
2. 类和类之间耦合度高；
3. 不利于扩展；
4. 不利于单元测试；
5. 替换实现类比较麻烦。

---

## 3. 使用 IoC 之后

使用 Spring IoC 后，对象由 Spring 容器创建和管理。

示例：

```java
@Service
public class UserService {

    @Autowired
    private UserDao userDao;

    public void save() {
        userDao.save();
    }
}
```

此时：

```text
UserService 不再自己 new UserDao；
UserDao 由 Spring 容器注入进来。
```

---

## 4. 什么是控制反转？

所谓控制反转，就是：

```text
原来对象创建和依赖关系由程序自己控制；
现在对象创建和依赖关系交给 Spring 容器控制。
```

控制权从程序员手中反转到了 Spring 容器中。

---

## 5. 什么是 DI？

DI 全称是：

```text
Dependency Injection
```

中文叫：

```text
依赖注入
```

DI 是 IoC 的一种具体实现方式。

IoC 是思想，DI 是实现。

---

## 6. 依赖注入的方式

Spring 中常见依赖注入方式有三种：

```text
1. 构造方法注入
2. Setter 方法注入
3. 字段注入
```

---

### 6.1 构造方法注入

```java
@Service
public class UserService {

    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }
}
```

优点：

1. 依赖不可变；
2. 可以使用 `final`；
3. 便于单元测试；
4. 可以避免部分循环依赖问题；
5. Spring 官方更推荐这种方式。

---

### 6.2 Setter 方法注入

```java
@Service
public class UserService {

    private UserDao userDao;

    @Autowired
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }
}
```

适合可选依赖。

---

### 6.3 字段注入

```java
@Service
public class UserService {

    @Autowired
    private UserDao userDao;
}
```

优点是写法简单。

缺点是：

1. 不利于单元测试；
2. 依赖关系不够清晰；
3. 无法使用 `final`；
4. 容易隐藏类的真实依赖。

---

## 7. Spring IoC 容器负责什么？

Spring IoC 容器主要负责：

1. 创建 Bean；
2. 管理 Bean 生命周期；
3. 维护 Bean 之间的依赖关系；
4. 完成依赖注入；
5. 管理 Bean 的作用域；
6. 调用 Bean 的初始化和销毁方法。

---

## 8. 常见 Bean 配置方式

Spring 中注册 Bean 的方式包括：

### 8.1 XML 配置

```xml
<bean id="userService" class="com.example.UserService"/>
```

---

### 8.2 注解配置

```java
@Service
public class UserService {
}
```

常见注解：

```text
@Component
@Service
@Repository
@Controller
```

---

### 8.3 Java 配置类

```java
@Configuration
public class AppConfig {

    @Bean
    public UserService userService() {
        return new UserService();
    }
}
```

---

## 9. 面试回答

Spring IoC 是控制反转，它是一种设计思想，指的是对象的创建和依赖关系不再由程序员手动控制，而是交给 Spring 容器统一管理。

DI 是依赖注入，是 IoC 的具体实现方式。Spring 容器会负责创建 Bean，并将 Bean 需要依赖的对象注入进去，从而降低类与类之间的耦合，提高代码的扩展性和可测试性。

---

# 二、Spring AOP 是什么

## 1. AOP 是什么？

AOP 全称是：

```text
Aspect Oriented Programming
```

中文叫：

```text
面向切面编程
```

AOP 是一种编程思想。

它主要用于处理系统中的横切关注点。

---

## 2. 什么是横切关注点？

横切关注点是指很多业务方法中都会重复出现的公共逻辑。

常见横切关注点包括：

1. 日志记录；
2. 权限校验；
3. 事务控制；
4. 接口限流；
5. 性能统计；
6. 异常处理；
7. 参数校验；
8. 操作审计。

---

## 3. 没有 AOP 的问题

没有 AOP 时，公共逻辑会散落在各个业务方法中。

示例：

```java
public void createOrder() {
    System.out.println("记录日志");

    // 创建订单业务逻辑

    System.out.println("记录日志");
}
```

问题：

1. 代码重复；
2. 业务逻辑和非业务逻辑耦合；
3. 不方便统一维护；
4. 不方便扩展。

---

## 4. 使用 AOP 之后

AOP 可以把公共逻辑抽取成切面。

示例：

```java
@Aspect
@Component
public class LogAspect {

    @Before("execution(* com.example.service.*.*(..))")
    public void before() {
        System.out.println("方法执行前记录日志");
    }
}
```

这样业务代码只关注业务本身。

---

## 5. AOP 核心概念

| 概念 | 说明 |
|---|---|
| Join Point | 连接点，程序执行过程中的某个点，例如方法调用 |
| Pointcut | 切点，用于匹配哪些连接点需要增强 |
| Advice | 通知，具体增强逻辑 |
| Aspect | 切面，切点和通知的组合 |
| Target | 目标对象，被代理的对象 |
| Proxy | 代理对象，增强后的对象 |
| Weaving | 织入，将增强逻辑应用到目标对象的过程 |

---

## 6. 通知类型

Spring AOP 常见通知类型：

| 通知类型 | 注解 | 执行时机 |
|---|---|---|
| 前置通知 | `@Before` | 目标方法执行前 |
| 后置通知 | `@After` | 目标方法执行后，不管是否异常 |
| 返回通知 | `@AfterReturning` | 目标方法正常返回后 |
| 异常通知 | `@AfterThrowing` | 目标方法抛出异常后 |
| 环绕通知 | `@Around` | 目标方法执行前后都可以增强 |

---

## 7. 环绕通知示例

```java
@Aspect
@Component
public class TimeAspect {

    @Around("execution(* com.example.service.*.*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        try {
            return joinPoint.proceed();
        } finally {
            long end = System.currentTimeMillis();
            System.out.println("方法耗时：" + (end - start) + "ms");
        }
    }
}
```

---

## 8. Spring AOP 底层原理

Spring AOP 底层主要基于动态代理实现。

常见代理方式：

```text
1. JDK 动态代理
2. CGLIB 动态代理
```

---

## 9. JDK 动态代理

如果目标类实现了接口，Spring 默认使用 JDK 动态代理。

特点：

1. 基于接口代理；
2. 代理对象和目标对象实现相同接口；
3. 通过反射调用目标方法。

示例：

```java
public interface UserService {
    void save();
}

@Service
public class UserServiceImpl implements UserService {
    public void save() {
        System.out.println("save");
    }
}
```

---

## 10. CGLIB 动态代理

如果目标类没有实现接口，Spring 会使用 CGLIB 动态代理。

特点：

1. 基于继承生成子类；
2. 不能代理 `final` 类；
3. 不能增强 `final` 方法。

---

## 11. Spring AOP 和 AspectJ 的区别

| 对比项 | Spring AOP | AspectJ |
|---|---|---|
| 实现方式 | 动态代理 | 编译期、类加载期或运行期织入 |
| 功能强度 | 相对较弱 | 更强大 |
| 连接点 | 主要支持方法级别 | 支持字段、构造器、方法等 |
| 使用复杂度 | 简单 | 相对复杂 |
| Spring 集成 | 原生支持 | 可集成 |

---

## 12. 面试回答

Spring AOP 是面向切面编程，主要用于将日志、事务、权限、监控等横切逻辑从业务代码中抽离出来，降低代码耦合，提高复用性。

Spring AOP 底层主要通过动态代理实现。如果目标类实现了接口，默认使用 JDK 动态代理；如果目标类没有实现接口，则使用 CGLIB 动态代理。

---

# 三、Bean 生命周期

## 1. Bean 生命周期是什么？

Bean 生命周期指的是一个 Bean 从创建到销毁的完整过程。

主要包括：

```text
实例化 -> 属性赋值 -> 初始化 -> 使用 -> 销毁
```

---

## 2. Bean 生命周期完整流程

Spring Bean 的完整生命周期大致如下：

```text
1. 实例化 Bean
2. 属性赋值
3. 执行 Aware 接口回调
4. 执行 BeanPostProcessor 前置处理
5. 执行初始化方法
6. 执行 BeanPostProcessor 后置处理
7. Bean 可以被使用
8. 容器关闭时执行销毁方法
```

---

## 3. 第一步：实例化 Bean

Spring 根据 BeanDefinition 中的信息，通过反射创建 Bean 对象。

示例：

```java
UserService userService = new UserService();
```

这个阶段只是创建了对象，还没有完成依赖注入。

---

## 4. 第二步：属性赋值

Spring 会给 Bean 的属性注入依赖。

例如：

```java
@Service
public class UserService {

    @Autowired
    private UserDao userDao;
}
```

此时 Spring 会将 `UserDao` 注入到 `UserService` 中。

---

## 5. 第三步：Aware 接口回调

如果 Bean 实现了某些 Aware 接口，Spring 会回调对应方法。

常见 Aware 接口：

| 接口 | 作用 |
|---|---|
| `BeanNameAware` | 获取 Bean 名称 |
| `BeanFactoryAware` | 获取 BeanFactory |
| `ApplicationContextAware` | 获取 ApplicationContext |
| `EnvironmentAware` | 获取 Environment |

示例：

```java
@Component
public class MyBean implements BeanNameAware {

    @Override
    public void setBeanName(String name) {
        System.out.println("当前 Bean 名称：" + name);
    }
}
```

---

## 6. 第四步：BeanPostProcessor 前置处理

执行所有 `BeanPostProcessor` 的前置方法：

```java
postProcessBeforeInitialization()
```

示例：

```java
@Component
public class MyBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        System.out.println("初始化前处理：" + beanName);
        return bean;
    }
}
```

---

## 7. 第五步：初始化方法

初始化阶段常见方式有三种：

### 7.1 @PostConstruct

```java
@PostConstruct
public void init() {
    System.out.println("初始化方法");
}
```

---

### 7.2 InitializingBean

```java
@Component
public class MyBean implements InitializingBean {

    @Override
    public void afterPropertiesSet() {
        System.out.println("初始化方法");
    }
}
```

---

### 7.3 init-method

```java
@Bean(initMethod = "init")
public UserService userService() {
    return new UserService();
}
```

---

## 8. 第六步：BeanPostProcessor 后置处理

执行所有 `BeanPostProcessor` 的后置方法：

```java
postProcessAfterInitialization()
```

AOP 代理对象通常就是在这个阶段创建的。

例如 Spring 会在初始化后判断当前 Bean 是否需要被 AOP 增强，如果需要，就生成代理对象。

---

## 9. 第七步：Bean 可以被使用

初始化完成后，Bean 就可以被业务代码使用了。

对于单例 Bean，会存放到 Spring 单例池中。

---

## 10. 第八步：销毁方法

容器关闭时，会执行 Bean 的销毁方法。

常见方式有三种：

### 10.1 @PreDestroy

```java
@PreDestroy
public void destroy() {
    System.out.println("销毁方法");
}
```

---

### 10.2 DisposableBean

```java
@Component
public class MyBean implements DisposableBean {

    @Override
    public void destroy() {
        System.out.println("销毁方法");
    }
}
```

---

### 10.3 destroy-method

```java
@Bean(destroyMethod = "destroy")
public UserService userService() {
    return new UserService();
}
```

---

## 11. Bean 生命周期流程图

```text
实例化 Bean
    |
    v
属性赋值 / 依赖注入
    |
    v
Aware 接口回调
    |
    v
BeanPostProcessor 前置处理
    |
    v
初始化方法
    |
    v
BeanPostProcessor 后置处理
    |
    v
Bean 可以被使用
    |
    v
容器关闭，执行销毁方法
```

---

## 12. 面试回答

Spring Bean 生命周期大致包括实例化、属性赋值、初始化、使用和销毁。

具体流程是：Spring 先根据 BeanDefinition 创建 Bean 实例，然后进行属性注入。如果 Bean 实现了 Aware 接口，会回调对应方法。接着执行 BeanPostProcessor 的前置处理，然后执行初始化方法，例如 `@PostConstruct`、`InitializingBean` 或 `init-method`。初始化后会执行 BeanPostProcessor 的后置处理，AOP 代理通常在这个阶段生成。最后 Bean 可以被使用，容器关闭时执行销毁方法。

---

# 四、BeanFactory 和 ApplicationContext 区别

## 1. BeanFactory 是什么？

`BeanFactory` 是 Spring 最基础的 IoC 容器接口。

它提供了 Bean 的基本管理能力，例如：

```text
获取 Bean
创建 Bean
管理 Bean
```

示例：

```java
BeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("spring.xml"));
UserService userService = beanFactory.getBean(UserService.class);
```

---

## 2. ApplicationContext 是什么？

`ApplicationContext` 是 `BeanFactory` 的子接口。

它在 `BeanFactory` 基础上提供了更多企业级功能。

常见实现类：

```text
ClassPathXmlApplicationContext
FileSystemXmlApplicationContext
AnnotationConfigApplicationContext
WebApplicationContext
```

---

## 3. 核心区别

| 对比项 | BeanFactory | ApplicationContext |
|---|---|---|
| 定位 | 基础 IoC 容器 | 高级 IoC 容器 |
| 继承关系 | 顶层基础接口 | BeanFactory 子接口 |
| Bean 初始化 | 默认懒加载 | 默认启动时创建单例 Bean |
| 国际化 | 不支持 | 支持 |
| 事件发布 | 不支持 | 支持 |
| 资源访问 | 能力较弱 | 支持 Resource 访问 |
| AOP 集成 | 需要额外配置 | 更方便 |
| Web 集成 | 不方便 | 支持 WebApplicationContext |
| 使用场景 | 底层容器 | 实际开发常用 |

---

## 4. Bean 初始化时机不同

### BeanFactory

`BeanFactory` 默认是懒加载。

即：

```text
什么时候调用 getBean，什么时候创建 Bean。
```

---

### ApplicationContext

`ApplicationContext` 默认会在容器启动时创建所有非懒加载的单例 Bean。

这样可以提前发现配置问题。

---

## 5. ApplicationContext 提供的额外能力

ApplicationContext 额外提供：

1. 国际化支持；
2. 事件发布机制；
3. 资源加载；
4. 环境变量管理；
5. 自动识别 BeanPostProcessor；
6. 自动识别 BeanFactoryPostProcessor；
7. 更方便地集成 AOP；
8. Web 环境支持。

---

## 6. 面试回答

`BeanFactory` 是 Spring 最基础的 IoC 容器，提供 Bean 的创建和获取能力。

`ApplicationContext` 是 `BeanFactory` 的子接口，在它的基础上提供了更多企业级功能，比如国际化、事件发布、资源加载、环境管理和 Web 支持。

另外，`BeanFactory` 默认懒加载 Bean，而 `ApplicationContext` 默认在容器启动时创建非懒加载的单例 Bean。实际开发中一般使用 `ApplicationContext`。

---

# 五、Spring 如何解决循环依赖

## 1. 什么是循环依赖？

循环依赖是指两个或多个 Bean 之间相互依赖。

最常见的是 A 依赖 B，B 又依赖 A。

示例：

```java
@Component
public class A {

    @Autowired
    private B b;
}

@Component
public class B {

    @Autowired
    private A a;
}
```

依赖关系：

```text
A -> B
B -> A
```

这就是循环依赖。

---

## 2. Spring 能解决哪些循环依赖？

Spring 默认可以解决：

```text
单例 Bean 的 setter 注入或字段注入循环依赖
```

Spring 默认不能解决：

```text
1. 构造方法注入的循环依赖
2. prototype 作用域 Bean 的循环依赖
3. 多例 Bean 的循环依赖
```

---

## 3. 为什么构造方法循环依赖解决不了？

构造方法注入要求对象创建时必须先准备好依赖。

示例：

```java
@Component
public class A {

    private final B b;

    public A(B b) {
        this.b = b;
    }
}

@Component
public class B {

    private final A a;

    public B(A a) {
        this.a = a;
    }
}
```

创建 A 需要 B，创建 B 又需要 A。

此时 A 和 B 都无法先实例化出来，因此 Spring 无法通过提前暴露对象解决。

---

## 4. Spring 解决循环依赖的核心思想

Spring 解决循环依赖的核心思想是：

> 提前暴露还没有完成初始化的 Bean 引用。

也就是说：

```text
Bean 实例化之后，还没有完成属性赋值和初始化之前，
Spring 会提前把这个半成品 Bean 暴露出来，
供其他 Bean 引用。
```

---

## 5. Spring 解决循环依赖依赖什么机制？

Spring 主要依赖：

```text
三级缓存
```

三级缓存用于保存 Bean 创建过程中的不同状态。

---

## 6. 单例循环依赖解决流程

以 A 依赖 B，B 依赖 A 为例。

### 6.1 创建 A

Spring 开始创建 A：

```text
1. 实例化 A
2. 将 A 的 ObjectFactory 放入三级缓存
3. 开始给 A 填充属性
4. 发现 A 依赖 B
```

---

### 6.2 创建 B

Spring 开始创建 B：

```text
1. 实例化 B
2. 将 B 的 ObjectFactory 放入三级缓存
3. 开始给 B 填充属性
4. 发现 B 依赖 A
```

---

### 6.3 B 获取 A

B 需要 A，于是 Spring 去缓存中查找 A：

```text
一级缓存没有 A
二级缓存没有 A
三级缓存有 A 的 ObjectFactory
```

Spring 通过 ObjectFactory 获取 A 的早期引用，并放入二级缓存。

此时 B 拿到的是 A 的早期引用。

---

### 6.4 B 创建完成

B 拿到 A 后，完成属性赋值、初始化，然后放入一级缓存。

---

### 6.5 A 创建完成

A 继续属性赋值，拿到 B。

A 完成初始化后，放入一级缓存。

---

## 7. 流程图

```text
创建 A
  |
  v
实例化 A
  |
  v
A 的 ObjectFactory 放入三级缓存
  |
  v
A 属性注入，发现需要 B
  |
  v
创建 B
  |
  v
实例化 B
  |
  v
B 的 ObjectFactory 放入三级缓存
  |
  v
B 属性注入，发现需要 A
  |
  v
从三级缓存拿到 A 的早期引用
  |
  v
B 完成初始化，进入一级缓存
  |
  v
A 注入 B
  |
  v
A 完成初始化，进入一级缓存
```

---

## 8. 为什么 prototype 不能解决循环依赖？

prototype Bean 每次获取都会创建新对象，不会放入单例池缓存。

Spring 没有办法通过三级缓存提前暴露 prototype Bean。

所以 prototype 循环依赖无法解决。

---

## 9. 面试回答

Spring 通过三级缓存解决单例 Bean 的循环依赖，核心思想是提前暴露还没有初始化完成的 Bean 引用。

当 A 依赖 B，B 又依赖 A 时，Spring 创建 A 后会先实例化 A，并将 A 的 ObjectFactory 放入三级缓存。然后 A 注入 B 时创建 B，B 又需要 A，此时 Spring 可以从三级缓存中拿到 A 的早期引用注入给 B。B 创建完成后，A 再继续完成创建。

不过 Spring 只能解决单例 Bean 的 setter 注入或字段注入循环依赖，不能解决构造方法注入和 prototype Bean 的循环依赖。

---

# 六、三级缓存是什么

## 1. 三级缓存的作用

三级缓存是 Spring 用来解决单例 Bean 循环依赖的重要机制。

它们位于 `DefaultSingletonBeanRegistry` 中。

---

## 2. 三级缓存分别是什么？

三级缓存分别是：

```text
一级缓存：singletonObjects
二级缓存：earlySingletonObjects
三级缓存：singletonFactories
```

---

## 3. 一级缓存：singletonObjects

一级缓存保存的是已经完全初始化好的单例 Bean。

```java
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
```

特点：

```text
保存完整 Bean
可以直接使用
生命周期已经完成初始化阶段
```

---

## 4. 二级缓存：earlySingletonObjects

二级缓存保存的是提前暴露的 Bean 早期引用。

```java
private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);
```

特点：

```text
保存早期 Bean 引用
Bean 还没有完成完整初始化
用于解决循环依赖
```

---

## 5. 三级缓存：singletonFactories

三级缓存保存的是 ObjectFactory。

```java
private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);
```

特点：

```text
保存创建早期引用的工厂
可以生成 Bean 的早期引用
可以支持 AOP 代理提前暴露
```

---

## 6. 三级缓存对比

| 缓存 | 名称 | 存储内容 | 作用 |
|---|---|---|---|
| 一级缓存 | `singletonObjects` | 完整初始化后的 Bean | 正常使用的单例池 |
| 二级缓存 | `earlySingletonObjects` | 提前暴露的早期 Bean 引用 | 解决循环依赖 |
| 三级缓存 | `singletonFactories` | ObjectFactory | 创建早期 Bean 引用，支持代理对象 |

---

## 7. 为什么需要三级缓存？

如果只是普通对象循环依赖，二级缓存理论上也可以解决。

但 Spring 还要考虑 AOP。

也就是说：

> 如果某个 Bean 最终需要被 AOP 代理，那么其他 Bean 注入它时，应该注入代理对象，而不是原始对象。

三级缓存中的 `ObjectFactory` 可以在需要时生成早期代理对象。

---

## 8. 三级缓存解决了什么问题？

三级缓存主要解决两个问题：

```text
1. 单例 Bean 的循环依赖
2. AOP 代理对象的提前暴露
```

---

## 9. 为什么不能直接把原始 Bean 放入二级缓存？

因为这个 Bean 后续可能会被 AOP 代理。

如果直接把原始对象放入二级缓存，那么其他 Bean 注入的就是原始对象。

但 Bean 初始化完成后，Spring 容器中保存的可能是代理对象。

这样就会出现：

```text
某些地方引用原始对象；
某些地方引用代理对象；
导致对象不一致。
```

---

## 10. 三级缓存工作流程

```text
1. Bean 实例化后，将 ObjectFactory 放入三级缓存
2. 如果其他 Bean 需要当前 Bean
3. Spring 先查一级缓存
4. 一级缓存没有，查二级缓存
5. 二级缓存没有，查三级缓存
6. 通过 ObjectFactory 获取早期引用
7. 将早期引用放入二级缓存
8. 删除三级缓存中的 ObjectFactory
9. Bean 初始化完成后，放入一级缓存
10. 删除二级缓存
```

---

## 11. 面试回答

Spring 三级缓存分别是一级缓存 `singletonObjects`、二级缓存 `earlySingletonObjects` 和三级缓存 `singletonFactories`。

一级缓存保存完整初始化后的单例 Bean；二级缓存保存提前暴露的早期 Bean 引用；三级缓存保存 ObjectFactory，用于生成 Bean 的早期引用。

三级缓存的核心作用是解决单例 Bean 的循环依赖，并且支持 AOP 场景下提前暴露代理对象，避免注入原始对象和最终代理对象不一致的问题。

---

# 七、@Autowired 和 @Resource 区别

## 1. @Autowired 是什么？

`@Autowired` 是 Spring 提供的依赖注入注解。

默认按照类型注入。

示例：

```java
@Autowired
private UserService userService;
```

---

## 2. @Resource 是什么？

`@Resource` 是 Java/Jakarta 规范提供的注解，不是 Spring 独有的。

在 Spring 中也支持使用 `@Resource` 进行依赖注入。

示例：

```java
@Resource
private UserService userService;
```

---

## 3. 核心区别

| 对比项 | @Autowired | @Resource |
|---|---|---|
| 来源 | Spring 提供 | Java/Jakarta 规范提供 |
| 默认注入方式 | 按类型注入 | 按名称注入 |
| 按名称指定 | 配合 `@Qualifier` | 使用 `name` 属性 |
| 是否支持 required | 支持 `required=false` | 不直接支持 required |
| 构造器注入 | 支持 | 一般不用 |
| Setter 注入 | 支持 | 支持 |
| 字段注入 | 支持 | 支持 |

---

## 4. @Autowired 按类型注入

```java
@Autowired
private UserService userService;
```

Spring 会根据 `UserService` 类型去容器中找 Bean。

如果只有一个 Bean，直接注入。

---

## 5. @Autowired 类型冲突

如果同一个类型有多个 Bean：

```java
@Service
public class UserServiceImpl1 implements UserService {
}

@Service
public class UserServiceImpl2 implements UserService {
}
```

此时：

```java
@Autowired
private UserService userService;
```

会报错，因为 Spring 不知道注入哪一个。

---

## 6. 使用 @Qualifier 指定名称

```java
@Autowired
@Qualifier("userServiceImpl1")
private UserService userService;
```

这样可以指定注入哪个 Bean。

---

## 7. 使用 @Primary 指定默认 Bean

```java
@Primary
@Service
public class UserServiceImpl1 implements UserService {
}
```

当有多个同类型 Bean 时，Spring 会优先注入带有 `@Primary` 的 Bean。

---

## 8. @Resource 默认按名称注入

```java
@Resource
private UserService userService;
```

`@Resource` 会优先根据字段名 `userService` 查找 Bean。

如果找不到，再根据类型查找。

---

## 9. @Resource 指定名称

```java
@Resource(name = "userServiceImpl1")
private UserService userService;
```

---

## 10. @Autowired 和 @Resource 示例对比

### @Autowired

```java
@Autowired
@Qualifier("orderServiceImpl")
private OrderService orderService;
```

---

### @Resource

```java
@Resource(name = "orderServiceImpl")
private OrderService orderService;
```

---

## 11. 面试回答

`@Autowired` 是 Spring 提供的注解，默认按照类型注入。如果容器中有多个相同类型的 Bean，可以配合 `@Qualifier` 或 `@Primary` 使用。

`@Resource` 是 Java/Jakarta 规范提供的注解，默认按照名称注入，如果找不到对应名称的 Bean，再按照类型注入。它可以通过 `name` 属性指定 Bean 名称。

简单来说，`@Autowired` 更偏向按类型注入，`@Resource` 更偏向按名称注入。

---

# 八、Spring 事务传播行为有哪些

## 1. 什么是事务传播行为？

事务传播行为指的是：

> 一个事务方法调用另一个事务方法时，事务应该如何传播。

例如：

```java
@Transactional
public void methodA() {
    methodB();
}

@Transactional
public void methodB() {
}
```

当 `methodA()` 调用 `methodB()` 时：

```text
methodB 是加入 methodA 的事务？
还是新建一个事务？
还是不使用事务？
```

这就是事务传播行为要解决的问题。

---

## 2. Spring 七种事务传播行为

Spring 中事务传播行为有七种：

```text
1. REQUIRED
2. SUPPORTS
3. MANDATORY
4. REQUIRES_NEW
5. NOT_SUPPORTED
6. NEVER
7. NESTED
```

---

## 3. REQUIRED

`REQUIRED` 是默认传播行为。

含义：

```text
如果当前存在事务，就加入当前事务；
如果当前没有事务，就新建一个事务。
```

示例：

```java
@Transactional(propagation = Propagation.REQUIRED)
public void save() {
}
```

使用场景：

```text
大多数业务方法都使用 REQUIRED。
```

---

## 4. SUPPORTS

含义：

```text
如果当前存在事务，就加入当前事务；
如果当前没有事务，就以非事务方式执行。
```

示例：

```java
@Transactional(propagation = Propagation.SUPPORTS)
public void query() {
}
```

使用场景：

```text
查询方法可以使用 SUPPORTS。
```

---

## 5. MANDATORY

含义：

```text
必须在事务中运行；
如果当前没有事务，则抛出异常。
```

示例：

```java
@Transactional(propagation = Propagation.MANDATORY)
public void update() {
}
```

使用场景：

```text
要求调用方必须已经开启事务。
```

---

## 6. REQUIRES_NEW

含义：

```text
无论当前是否存在事务，都会新建一个事务；
如果当前存在事务，则挂起当前事务。
```

示例：

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void saveLog() {
}
```

使用场景：

```text
日志保存、审计记录、消息记录等希望独立提交的场景。
```

---

## 7. NOT_SUPPORTED

含义：

```text
以非事务方式执行；
如果当前存在事务，则挂起当前事务。
```

示例：

```java
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public void queryRemoteData() {
}
```

使用场景：

```text
不希望在事务中执行的耗时操作。
```

---

## 8. NEVER

含义：

```text
必须以非事务方式执行；
如果当前存在事务，则抛出异常。
```

示例：

```java
@Transactional(propagation = Propagation.NEVER)
public void executeWithoutTransaction() {
}
```

---

## 9. NESTED

含义：

```text
如果当前存在事务，则在嵌套事务中执行；
如果当前没有事务，则新建一个事务。
```

嵌套事务通常基于数据库保存点 Savepoint 实现。

特点：

```text
内层事务可以单独回滚到保存点；
外层事务回滚时，内层事务也会回滚。
```

示例：

```java
@Transactional(propagation = Propagation.NESTED)
public void nestedMethod() {
}
```

---

## 10. 七种传播行为对比

| 传播行为 | 当前有事务 | 当前无事务 | 说明 |
|---|---|---|---|
| REQUIRED | 加入当前事务 | 新建事务 | 默认行为 |
| SUPPORTS | 加入当前事务 | 非事务执行 | 有就用，没有就不用 |
| MANDATORY | 加入当前事务 | 抛异常 | 必须有事务 |
| REQUIRES_NEW | 挂起当前事务，新建事务 | 新建事务 | 独立事务 |
| NOT_SUPPORTED | 挂起当前事务，非事务执行 | 非事务执行 | 不使用事务 |
| NEVER | 抛异常 | 非事务执行 | 禁止有事务 |
| NESTED | 嵌套事务 | 新建事务 | 基于保存点 |

---

## 11. REQUIRED 和 REQUIRES_NEW 区别

| 对比项 | REQUIRED | REQUIRES_NEW |
|---|---|---|
| 当前有事务 | 加入当前事务 | 挂起当前事务，新建事务 |
| 当前无事务 | 新建事务 | 新建事务 |
| 是否独立提交 | 不独立 | 独立 |
| 回滚影响 | 内外互相影响 | 相对独立 |
| 使用场景 | 普通业务 | 日志、审计、消息记录 |

---

## 12. NESTED 和 REQUIRES_NEW 区别

| 对比项 | NESTED | REQUIRES_NEW |
|---|---|---|
| 实现方式 | 保存点 Savepoint | 新事务 |
| 是否挂起外部事务 | 不挂起 | 挂起 |
| 内层提交 | 依赖外层最终提交 | 可以独立提交 |
| 外层回滚影响内层 | 影响 | 通常不影响已经提交的内层事务 |
| 内层回滚影响外层 | 可回滚到保存点 | 一般不影响外层 |

---

## 13. 面试回答

Spring 事务传播行为用于控制一个事务方法调用另一个事务方法时，事务如何传播。

常见传播行为有七种：`REQUIRED`、`SUPPORTS`、`MANDATORY`、`REQUIRES_NEW`、`NOT_SUPPORTED`、`NEVER`、`NESTED`。

最常用的是 `REQUIRED`，表示有事务就加入，没有事务就新建。`REQUIRES_NEW` 表示无论是否存在事务，都会新建一个独立事务。`NESTED` 表示嵌套事务，通常基于数据库保存点实现。

---

# 九、Spring 事务失效场景有哪些

## 1. Spring 事务为什么会失效？

Spring 声明式事务底层基于 AOP 动态代理实现。

因此事务失效的核心原因通常是：

```text
方法没有被 Spring 代理对象调用。
```

或者：

```text
事务配置不满足生效条件。
```

---

## 2. 方法内部自调用导致事务失效

### 2.1 错误示例

```java
@Service
public class UserService {

    public void methodA() {
        methodB();
    }

    @Transactional
    public void methodB() {
        // 数据库操作
    }
}
```

这里 `methodA()` 直接调用当前类的 `methodB()`，属于 `this.methodB()`。

没有经过 Spring 代理对象，所以事务不会生效。

---

### 2.2 解决方式

方式一：拆到另一个 Service 中。

```java
@Service
public class UserService {

    @Autowired
    private OrderService orderService;

    public void methodA() {
        orderService.methodB();
    }
}
```

方式二：通过代理对象调用。

```java
@Service
public class UserService {

    @Autowired
    private UserService userService;

    public void methodA() {
        userService.methodB();
    }

    @Transactional
    public void methodB() {
        // 数据库操作
    }
}
```

---

## 3. 方法不是 public

Spring AOP 默认只对 public 方法生效。

错误示例：

```java
@Transactional
private void save() {
    // 数据库操作
}
```

或者：

```java
@Transactional
protected void save() {
    // 数据库操作
}
```

这些方法可能不会被代理增强。

正确写法：

```java
@Transactional
public void save() {
    // 数据库操作
}
```

---

## 4. 异常被捕获没有抛出

### 4.1 错误示例

```java
@Transactional
public void save() {
    try {
        userMapper.insert(user);
        int i = 1 / 0;
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

异常被 catch 掉了，没有抛给 Spring 事务管理器。

Spring 认为方法正常执行完成，就会提交事务。

---

### 4.2 解决方式

继续抛出异常：

```java
@Transactional
public void save() {
    try {
        userMapper.insert(user);
        int i = 1 / 0;
    } catch (Exception e) {
        throw e;
    }
}
```

或者手动设置回滚：

```java
TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
```

---

## 5. 抛出检查异常但未配置 rollbackFor

默认情况下，Spring 事务只会对：

```text
RuntimeException
Error
```

进行回滚。

对于受检异常，也就是 checked exception，默认不会回滚。

错误示例：

```java
@Transactional
public void save() throws Exception {
    userMapper.insert(user);
    throw new Exception(" checked exception ");
}
```

---

### 5.1 解决方式

配置 `rollbackFor`：

```java
@Transactional(rollbackFor = Exception.class)
public void save() throws Exception {
    userMapper.insert(user);
    throw new Exception(" checked exception ");
}
```

---

## 6. 数据库本身不支持事务

例如 MySQL 使用 MyISAM 存储引擎时，不支持事务。

即使加了 `@Transactional`，也不会真正回滚。

解决方式：

```text
使用 InnoDB 存储引擎。
```

---

## 7. 没有被 Spring 管理

如果类不是 Spring Bean，事务不会生效。

错误示例：

```java
public class UserService {

    @Transactional
    public void save() {
    }
}
```

如果没有加：

```java
@Service
@Component
```

或者没有通过配置注册到 Spring 容器，事务不会生效。

---

## 8. new 对象调用方法

错误示例：

```java
UserService userService = new UserService();
userService.save();
```

通过 `new` 创建的对象不是 Spring 容器中的代理对象。

所以事务不会生效。

---

## 9. final 方法或 final 类

如果使用 CGLIB 代理，目标类或方法不能是 `final`。

错误示例：

```java
@Service
public final class UserService {

    @Transactional
    public void save() {
    }
}
```

或者：

```java
@Transactional
public final void save() {
}
```

CGLIB 基于继承生成代理子类，`final` 类不能被继承，`final` 方法不能被重写，因此无法增强。

---

## 10. 事务传播行为配置不当

例如：

```java
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public void save() {
}
```

`NOT_SUPPORTED` 表示以非事务方式执行。

所以不会开启事务。

再比如：

```java
@Transactional(propagation = Propagation.NEVER)
public void save() {
}
```

表示禁止在事务中执行。

---

## 11. 多线程调用导致事务失效

事务信息是和当前线程绑定的。

如果在事务方法中开启新线程，新线程中的数据库操作不会自动加入当前事务。

错误示例：

```java
@Transactional
public void save() {
    userMapper.insert(user);

    new Thread(() -> {
        orderMapper.insert(order);
    }).start();
}
```

`orderMapper.insert(order)` 在新线程中执行，不属于当前事务。

---

## 12. 异步方法导致事务不一致

类似地，`@Async` 方法通常在另一个线程中执行。

```java
@Transactional
public void save() {
    asyncService.saveLog();
}
```

如果 `saveLog()` 是异步方法，它不会自动加入当前线程事务。

---

## 13. 事务管理器配置错误

如果项目中有多个数据源，但事务管理器配置错误，也会导致事务不生效。

例如：

```text
操作的是 A 数据源；
事务管理器管理的是 B 数据源。
```

---

## 14. rollbackFor 配置错误

错误示例：

```java
@Transactional(rollbackFor = RuntimeException.class)
public void save() throws Exception {
    throw new Exception();
}
```

这里抛出的是 `Exception`，但配置的是 `RuntimeException`，不会回滚。

正确写法：

```java
@Transactional(rollbackFor = Exception.class)
public void save() throws Exception {
    throw new Exception();
}
```

---

## 15. 常见事务失效场景总结

| 场景 | 原因 | 解决方案 |
|---|---|---|
| 同类方法自调用 | 没经过代理对象 | 通过代理对象调用或拆分 Service |
| 方法不是 public | Spring AOP 默认无法增强 | 改成 public |
| 异常被捕获 | Spring 感知不到异常 | 继续抛出或手动回滚 |
| 抛 checked 异常 | 默认不回滚 | 配置 `rollbackFor = Exception.class` |
| 类没有被 Spring 管理 | 不是 Bean | 加 `@Service` 等注解 |
| new 对象调用 | 不是代理对象 | 从 Spring 容器获取 Bean |
| final 类或 final 方法 | CGLIB 无法代理 | 去掉 final |
| 数据库不支持事务 | 如 MyISAM | 使用 InnoDB |
| 传播行为配置错误 | 如 NOT_SUPPORTED | 使用正确传播行为 |
| 多线程调用 | 事务绑定当前线程 | 避免跨线程共享事务 |
| 多数据源事务管理器错误 | 管理器和数据源不匹配 | 指定正确事务管理器 |

---

## 16. 面试回答

Spring 事务失效最常见的原因是声明式事务基于 AOP 代理实现，方法调用没有经过代理对象，事务就不会生效。

常见失效场景包括：同类方法自调用、方法不是 public、异常被捕获没有抛出、抛出 checked 异常但没有配置 `rollbackFor`、类没有被 Spring 管理、通过 new 创建对象调用、final 方法无法被代理、多线程或异步调用导致事务上下文丢失、数据库不支持事务，以及事务传播行为或事务管理器配置错误。

---

# 十、面试速记版

## 1. Spring IoC 是什么？

IoC 是控制反转，指对象创建和依赖关系维护不再由程序员手动控制，而是交给 Spring 容器管理。

DI 是依赖注入，是 IoC 的具体实现。

---

## 2. Spring AOP 是什么？

AOP 是面向切面编程，用于把日志、权限、事务、监控等横切逻辑从业务代码中抽离出来。

Spring AOP 底层主要基于动态代理实现，包括 JDK 动态代理和 CGLIB 动态代理。

---

## 3. Bean 生命周期

Bean 生命周期：

```text
实例化 -> 属性赋值 -> Aware 回调 -> BeanPostProcessor 前置处理 -> 初始化 -> BeanPostProcessor 后置处理 -> 使用 -> 销毁
```

AOP 代理通常在 BeanPostProcessor 后置处理阶段生成。

---

## 4. BeanFactory 和 ApplicationContext 区别

`BeanFactory` 是基础 IoC 容器。

`ApplicationContext` 是高级 IoC 容器，继承 BeanFactory，支持国际化、事件发布、资源加载、环境管理和 Web 集成。

实际开发中一般使用 `ApplicationContext`。

---

## 5. Spring 如何解决循环依赖？

Spring 通过三级缓存解决单例 Bean 的 setter 注入或字段注入循环依赖。

核心思想是提前暴露还没有初始化完成的 Bean 引用。

构造方法注入和 prototype Bean 循环依赖无法解决。

---

## 6. 三级缓存是什么？

三级缓存分别是：

```text
一级缓存 singletonObjects：完整 Bean
二级缓存 earlySingletonObjects：早期 Bean 引用
三级缓存 singletonFactories：ObjectFactory
```

三级缓存既用于解决循环依赖，也用于支持 AOP 代理对象提前暴露。

---

## 7. @Autowired 和 @Resource 区别

`@Autowired` 是 Spring 提供的，默认按类型注入，可以配合 `@Qualifier` 指定名称。

`@Resource` 是 Java/Jakarta 规范提供的，默认按名称注入，找不到名称时再按类型注入。

---

## 8. Spring 事务传播行为有哪些？

Spring 有七种事务传播行为：

```text
REQUIRED
SUPPORTS
MANDATORY
REQUIRES_NEW
NOT_SUPPORTED
NEVER
NESTED
```

最常用的是 `REQUIRED`，默认行为，有事务就加入，没有事务就新建。

---

## 9. Spring 事务失效场景有哪些？

常见事务失效场景：

```text
同类方法自调用
方法不是 public
异常被捕获
checked 异常未配置 rollbackFor
类没有被 Spring 管理
new 对象调用
final 方法或 final 类
数据库不支持事务
传播行为配置错误
多线程或异步调用
事务管理器配置错误
```

---

# 十一、总览表

| 问题 | 核心结论 |
|---|---|
| Spring IoC 是什么 | 控制反转，对象创建和依赖维护交给 Spring 容器 |
| Spring AOP 是什么 | 面向切面编程，底层主要基于动态代理 |
| Bean 生命周期 | 实例化、属性赋值、Aware、前置处理、初始化、后置处理、使用、销毁 |
| BeanFactory 和 ApplicationContext 区别 | BeanFactory 是基础容器，ApplicationContext 是高级容器 |
| Spring 如何解决循环依赖 | 通过三级缓存提前暴露单例 Bean 早期引用 |
| 三级缓存是什么 | 一级完整 Bean，二级早期引用，三级 ObjectFactory |
| @Autowired 和 @Resource 区别 | @Autowired 默认按类型，@Resource 默认按名称 |
| Spring 事务传播行为 | REQUIRED、SUPPORTS、MANDATORY、REQUIRES_NEW、NOT_SUPPORTED、NEVER、NESTED |
| Spring 事务失效场景 | 自调用、非 public、异常被捕获、checked 异常、非 Spring Bean、多线程等 |

---

# 十二、完整面试回答模板

Spring IoC 是控制反转，它是一种设计思想，指对象的创建和依赖关系不再由程序员手动控制，而是交给 Spring 容器统一管理。DI 是依赖注入，是 IoC 的具体实现方式，Spring 容器会负责创建 Bean，并将 Bean 所依赖的对象注入进去，从而降低类与类之间的耦合。

Spring AOP 是面向切面编程，主要用于将日志、事务、权限、监控等横切逻辑从业务代码中抽离出来。Spring AOP 底层主要基于动态代理实现，如果目标类实现了接口，默认使用 JDK 动态代理；如果目标类没有实现接口，则使用 CGLIB 动态代理。

Spring Bean 生命周期大致包括实例化、属性赋值、初始化、使用和销毁。具体流程是：Spring 根据 BeanDefinition 创建 Bean 实例，然后进行属性注入。如果 Bean 实现了 Aware 接口，会回调对应方法。接着执行 BeanPostProcessor 前置处理，然后执行初始化方法，例如 `@PostConstruct`、`InitializingBean` 或 `init-method`。初始化完成后执行 BeanPostProcessor 后置处理，AOP 代理通常在这个阶段生成。最后 Bean 可以被使用，容器关闭时执行销毁方法。

`BeanFactory` 是 Spring 最基础的 IoC 容器，提供 Bean 的创建和获取能力。`ApplicationContext` 是 `BeanFactory` 的子接口，在它的基础上提供了更多企业级功能，比如国际化、事件发布、资源加载、环境管理和 Web 支持。实际开发中一般使用 `ApplicationContext`。

Spring 通过三级缓存解决单例 Bean 的循环依赖。核心思想是提前暴露还没有初始化完成的 Bean 引用。当 A 依赖 B，B 又依赖 A 时，Spring 创建 A 后会先实例化 A，并将 A 的 ObjectFactory 放入三级缓存。创建 B 时发现需要 A，就可以通过三级缓存拿到 A 的早期引用并注入给 B。B 创建完成后，A 再继续完成创建。

三级缓存分别是一级缓存 `singletonObjects`、二级缓存 `earlySingletonObjects` 和三级缓存 `singletonFactories`。一级缓存保存完整初始化后的单例 Bean，二级缓存保存提前暴露的早期 Bean 引用，三级缓存保存 ObjectFactory，用于生成 Bean 的早期引用。三级缓存不仅解决循环依赖，也支持 AOP 场景下提前暴露代理对象。

`@Autowired` 是 Spring 提供的注解，默认按照类型注入。如果容器中有多个相同类型的 Bean，可以配合 `@Qualifier` 或 `@Primary` 使用。`@Resource` 是 Java/Jakarta 规范提供的注解，默认按照名称注入，如果找不到对应名称的 Bean，再按照类型注入。简单来说，`@Autowired` 更偏向按类型注入，`@Resource` 更偏向按名称注入。

Spring 事务传播行为用于控制一个事务方法调用另一个事务方法时事务如何传播。常见传播行为有七种：`REQUIRED`、`SUPPORTS`、`MANDATORY`、`REQUIRES_NEW`、`NOT_SUPPORTED`、`NEVER`、`NESTED`。最常用的是 `REQUIRED`，表示有事务就加入，没有事务就新建。`REQUIRES_NEW` 表示无论当前是否存在事务，都会新建一个独立事务。`NESTED` 表示嵌套事务，通常基于数据库保存点实现。

Spring 事务失效最常见的原因是声明式事务基于 AOP 代理实现，方法调用没有经过代理对象，事务就不会生效。常见失效场景包括：同类方法自调用、方法不是 public、异常被捕获没有抛出、抛出 checked 异常但没有配置 `rollbackFor`、类没有被 Spring 管理、通过 new 创建对象调用、final 方法无法被代理、多线程或异步调用导致事务上下文丢失、数据库不支持事务，以及事务传播行为或事务管理器配置错误。
