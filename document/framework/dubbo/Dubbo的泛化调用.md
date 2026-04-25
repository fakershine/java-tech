# Dubbo 泛化调用总结

Dubbo 泛化调用是指：**调用方不依赖服务提供方的接口 Jar，也可以调用远程 Dubbo 服务**。

核心接口：

```java
org.apache.dubbo.rpc.service.GenericService
```

核心方法：

```java
Object $invoke(String methodName, String[] parameterTypes, Object[] args)
```

Dubbo 官方说明，泛化调用适合调用方没有服务方 API/SDK 的场景，只要知道接口全限定名、方法名和参数类型，就可以发起调用。:contentReference[oaicite:0]{index=0}

---

## 1. 为什么需要泛化调用

普通 Dubbo 调用需要依赖接口 Jar：

```java
@DubboReference
private UserService userService;
```

如果调用方没有 `UserService` 接口和相关 DTO，就无法直接调用。

泛化调用解决的问题是：

```text
不依赖接口 Jar
不依赖 DTO 类
只通过接口名、方法名、参数类型、参数值调用远程服务
```

---

## 2. 适用场景

常见场景：

- RPC 网关。
- 接口测试平台。
- 运维调用平台。
- 通用 Mock 平台。
- 动态服务调用。
- 低代码 / 开放平台调用 Dubbo 服务。

官方文档也提到，网关服务和 RPC 测试平台是泛化调用的典型场景，因为这些平台不应该随着每个服务 API 发布而重新依赖接口 Jar 或重新部署。:contentReference[oaicite:1]{index=1}

---

## 3. 泛化调用核心原理

普通调用：

```text
调用接口方法
  ↓
Dubbo 代理对象
  ↓
远程调用
```

泛化调用：

```text
调用 GenericService.$invoke()
  ↓
传入接口名、方法名、参数类型、参数值
  ↓
Dubbo 封装 Invocation
  ↓
走正常 Dubbo 调用链路
  ↓
Provider 执行真实方法
  ↓
返回结果
```

本质上：

```text
泛化调用只是把强类型接口调用，变成弱类型动态调用。
```

---

## 4. 客户端泛化调用

### API 方式

```java
ReferenceConfig<GenericService> reference = new ReferenceConfig<>();

reference.setInterface("com.demo.UserService");
reference.setVersion("1.0.0");
reference.setGeneric("true");

GenericService genericService = reference.get();

Object result = genericService.$invoke(
        "getUser",
        new String[]{"java.lang.Long"},
        new Object[]{1001L}
);
```

说明：

```text
methodName：方法名
parameterTypes：参数类型数组
args：参数值数组
```

Dubbo 文档中说明，`GenericService` 只有 `$invoke` 一个核心方法，三个参数分别是方法名、方法参数类型数组和参数值数组。:contentReference[oaicite:2]{index=2}

---

## 5. 参数类型怎么传

### 基本类型

```java
new String[]{"int"}
```

或：

```java
new String[]{int.class.getName()}
```

---

### 包装类型

```java
new String[]{"java.lang.Integer"}
```

---

### 字符串

```java
new String[]{"java.lang.String"}
```

---

### 对象类型

```java
new String[]{"com.demo.UserDTO"}
```

如果调用方没有 DTO 类，可以用 `Map` 表示对象参数。

```java
Map<String, Object> user = new HashMap<>();
user.put("id", 1001L);
user.put("name", "Tom");

Object result = genericService.$invoke(
        "saveUser",
        new String[]{"com.demo.UserDTO"},
        new Object[]{user}
);
```

Dubbo 泛化实现文档也说明，参数和返回值中的 POJO 通常用 `Map` 表示，适合框架集成场景。:contentReference[oaicite:3]{index=3}

---

## 6. 泛化调用返回值

如果返回值是普通类型：

```text
String
Integer
Boolean
Long
```

可以直接强转。

如果返回值是对象类型，通常会返回：

```text
Map
```

例如返回 `UserDTO`：

```java
Object result = genericService.$invoke(...);

Map<String, Object> userMap = (Map<String, Object>) result;
```

---

## 7. 服务端泛化实现

除了客户端泛化调用，Dubbo 也支持服务端泛化实现。

也就是服务端不提供具体接口实现，而是实现 `GenericService`。

```java
public class MyGenericService implements GenericService {

    @Override
    public Object $invoke(
            String methodName,
            String[] parameterTypes,
            Object[] args
    ) {
        if ("sayHello".equals(methodName)) {
            return "Hello, " + args[0];
        }

        throw new UnsupportedOperationException(methodName);
    }
}
```

暴露服务时：

```java
ServiceConfig<GenericService> service = new ServiceConfig<>();

service.setInterface("com.demo.HelloService");
service.setGeneric("true");
service.setRef(new MyGenericService());

service.export();
```

服务端泛化实现要求 `ServiceConfig` 开启 `generic`，并且 `setRef` 指定的是 `GenericService` 对象，而不是真实服务实现类对象。:contentReference[oaicite:4]{index=4}

---

## 8. 泛化调用和普通调用区别

| 对比项 | 普通调用 | 泛化调用 |
|---|---|---|
| 是否依赖接口 Jar | 需要 | 不需要 |
| 是否强类型 | 是 | 否 |
| 调用方式 | 接口方法调用 | `$invoke()` |
| 参数对象 | DTO 对象 | Map / 基础类型 |
| 使用体验 | 更简单 | 更灵活但复杂 |
| 适用场景 | 业务系统调用 | 网关、测试平台、通用调用平台 |

---

## 9. 注意事项

- 必须知道服务接口全限定名。
- 必须知道方法名和参数类型。
- 方法重载时参数类型必须准确。
- POJO 参数通常用 `Map` 表示。
- 返回对象可能也是 `Map`。
- 泛化调用是弱类型调用，编译期无法校验。
- 参数组装错误会在运行时报错。
- 适合平台型系统，不建议普通业务代码大量使用。
- Dubbo 3.3 之后如果使用 Triple 协议，官方更建议直接使用 Triple 的 HTTP `application/json` 能力发起调用。:contentReference[oaicite:5]{index=5}

---

## 10. 总结

Dubbo 泛化调用是 Dubbo 提供的一种弱类型调用方式，调用方不需要依赖服务提供方的接口 Jar 和 DTO 类，只需要知道接口全限定名、方法名、参数类型和参数值，就可以通过 `GenericService.$invoke()` 发起远程调用。

泛化调用常用于 RPC 网关、接口测试平台、Mock 平台、运维调用平台等场景。它的核心是把普通的强类型接口调用转换成基于 `GenericService` 的动态调用。

一句话总结：

```text
Dubbo 泛化调用 = 不依赖接口 Jar，通过 GenericService.$invoke 动态调用远程服务。
```
