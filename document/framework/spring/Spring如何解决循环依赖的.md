# Spring 如何解决循环依赖

Spring 解决循环依赖主要依赖 **三级缓存 + 提前暴露对象引用**。

但需要注意：Spring 主要只能解决 **单例 Bean 的 setter / field 注入循环依赖**，不能解决所有循环依赖。

---

## 1. 什么是循环依赖

两个或多个 Bean 之间互相依赖。

例如：

```java
@Service
public class AService {
    @Autowired
    private BService bService;
}

@Service
public class BService {
    @Autowired
    private AService aService;
}
```

依赖关系：

```text
AService -> BService -> AService
```

---

## 2. Spring 能解决哪种循环依赖

| 场景 | 是否能解决 |
|---|---|
| 单例 Bean + setter 注入 | 可以 |
| 单例 Bean + field 注入 | 可以 |
| 单例 Bean + 构造器注入 | 不可以 |
| prototype Bean 循环依赖 | 不可以 |
| 多例 Bean 循环依赖 | 不可以 |

---

## 3. Spring 三级缓存

Spring 通过三级缓存解决单例 Bean 循环依赖。

| 缓存 | 名称 | 作用 |
|---|---|---|
| 一级缓存 | `singletonObjects` | 存放完全初始化完成的 Bean |
| 二级缓存 | `earlySingletonObjects` | 存放提前暴露的半成品 Bean |
| 三级缓存 | `singletonFactories` | 存放 Bean 的工厂对象，用于提前暴露 Bean 引用 |

可以简单理解为：

```text
一级缓存：成品 Bean
二级缓存：半成品 Bean
三级缓存：可以生成半成品 Bean 的工厂
```

---

## 4. 创建 Bean 的大致流程

Spring 创建单例 Bean 的流程：

```text
实例化 Bean
  ↓
提前暴露 Bean 引用到三级缓存
  ↓
属性填充
  ↓
初始化 Bean
  ↓
放入一级缓存
```

也就是说，Spring 在 Bean 还没有完全初始化完成时，就会提前暴露它的引用。

---

## 5. 循环依赖解决流程

以 `AService` 和 `BService` 为例：

```text
1. Spring 开始创建 AService。
2. 实例化 AService，此时 AService 还没有完成属性注入。
3. Spring 将 AService 的 ObjectFactory 放入三级缓存。
4. AService 需要注入 BService，于是开始创建 BService。
5. 实例化 BService。
6. BService 需要注入 AService。
7. Spring 先查一级缓存，没找到 AService。
8. 再查二级缓存，没找到 AService。
9. 再查三级缓存，找到 AService 的 ObjectFactory。
10. 通过 ObjectFactory 获取 AService 的早期引用，并放入二级缓存。
11. BService 注入 AService 成功。
12. BService 初始化完成，放入一级缓存。
13. AService 继续注入 BService。
14. AService 初始化完成，放入一级缓存。
```

简化流程：

```text
创建 A
  ↓
A 提前暴露到三级缓存
  ↓
A 依赖 B，开始创建 B
  ↓
B 依赖 A
  ↓
B 从三级缓存拿到 A 的早期引用
  ↓
B 创建完成
  ↓
A 注入 B
  ↓
A 创建完成
```

---

## 6. 为什么需要三级缓存

如果没有 AOP，其实二级缓存也能解决普通循环依赖。

但 Spring 中可能存在 AOP 代理对象。

例如：

```java
@Transactional
public void method() {
}
```

这种 Bean 最终注入的对象可能不是原始对象，而是代理对象。

三级缓存的作用是：

```text
延迟生成早期引用
必要时提前生成代理对象
保证其他 Bean 注入的是正确的代理对象
```

如果直接把原始对象放入二级缓存，可能导致其他 Bean 注入的是原始对象，而不是 AOP 代理对象。

---

## 7. 为什么构造器注入无法解决

构造器注入要求 Bean 在实例化时就必须拿到依赖对象。

例如：

```java
@Service
public class AService {

    private final BService bService;

    public AService(BService bService) {
        this.bService = bService;
    }
}
```

如果 A 和 B 互相构造器依赖：

```text
创建 A 需要 B
创建 B 又需要 A
```

此时 A 还没有实例化完成，无法提前暴露引用，所以 Spring 无法解决。

---

## 8. 为什么 prototype 无法解决

prototype Bean 每次获取都会创建新对象，不会放入单例池。

三级缓存主要针对单例 Bean：

```text
singleton Bean 可以提前暴露引用
prototype Bean 不会缓存，无法提前暴露
```

所以 prototype 循环依赖无法解决。

---

## 9. 如何解决无法自动处理的循环依赖

### 1. 优化设计，拆分依赖

如果两个类互相依赖，通常说明职责边界不清晰。

推荐：

```text
拆出第三个服务
重新划分职责
减少双向依赖
```

---

### 2. 使用 setter 注入

构造器循环依赖可以改成 setter 注入。

```java
@Autowired
public void setBService(BService bService) {
    this.bService = bService;
}
```

---

### 3. 使用 @Lazy

可以延迟注入依赖对象。

```java
public AService(@Lazy BService bService) {
    this.bService = bService;
}
```

`@Lazy` 会注入一个代理对象，等真正使用时再获取目标 Bean。

---

### 4. 使用 ObjectProvider

```java
@Autowired
private ObjectProvider<BService> bServiceProvider;

public void method() {
    BService bService = bServiceProvider.getObject();
}
```

这样可以延迟获取依赖对象。

---

## 10. 总结

Spring 通过三级缓存解决单例 Bean 的循环依赖。

三级缓存分别是：一级缓存 `singletonObjects`，存放初始化完成的 Bean；二级缓存 `earlySingletonObjects`，存放提前暴露的早期 Bean；三级缓存 `singletonFactories`，存放可以生成早期 Bean 引用的工厂对象。

Spring 创建 Bean 时，会先实例化对象，然后把生成早期引用的工厂放入三级缓存，再进行属性注入。如果属性注入时发现循环依赖，就可以从三级缓存中拿到对方的早期引用，从而打破循环依赖。

Spring 主要能解决单例 Bean 的 setter 注入和 field 注入循环依赖，不能解决构造器循环依赖和 prototype Bean 循环依赖。

一句话总结：

```text
Spring 解决循环依赖 = 实例化后提前暴露 Bean 引用 + 三级缓存 + 必要时提前生成 AOP 代理对象。
```
