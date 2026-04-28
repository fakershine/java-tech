# Servlet 是什么

## 1. 一句话解释

> Servlet 是 Java Web 服务器端的一种组件规范，用来接收 HTTP 请求、处理业务逻辑、返回 HTTP 响应。

简单理解：

```text
浏览器请求
   ↓
Tomcat
   ↓
Servlet
   ↓
处理请求
   ↓
返回响应
```

---

## 2. Servlet 解决什么问题

没有 Servlet 之前，如果要用 Java 处理 HTTP 请求，需要自己处理很多底层细节：

```text
监听端口
解析 HTTP 请求
读取请求参数
生成 HTTP 响应
管理连接
处理并发
```

Servlet 把这些底层工作封装起来，让开发者只关注：

```text
请求进来后怎么处理
响应返回什么内容
```

---

## 3. Servlet 和 Tomcat 的关系

Servlet 是一种**规范**。

Tomcat 是 Servlet 规范的**实现容器**。

可以这样理解：

```text
Servlet：接口规范
Tomcat：实现规范的服务器
我们写的 Servlet：业务处理类
```

请求流程：

```text
浏览器
   ↓
Tomcat
   ↓
调用我们写的 Servlet
   ↓
返回响应
```

Tomcat 负责：

```text
接收 HTTP 请求
解析请求
创建 Request / Response 对象
调用 Servlet 方法
管理 Servlet 生命周期
返回响应给客户端
```

---

## 4. Servlet 核心接口

Servlet 最核心接口是：

```java
javax.servlet.Servlet
```

Spring Boot 3 / Jakarta EE 中是：

```java
jakarta.servlet.Servlet
```

常用的是它的子类：

```java
HttpServlet
```

---

## 5. 一个简单 Servlet 示例

```java
public class HelloServlet extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        response.setContentType("text/plain;charset=utf-8");
        response.getWriter().write("Hello Servlet");
    }
}
```

当浏览器发送 GET 请求时，会执行：

```java
doGet()
```

---

## 6. Servlet 常用方法

| 方法 | 说明 |
|---|---|
| `doGet()` | 处理 GET 请求 |
| `doPost()` | 处理 POST 请求 |
| `doPut()` | 处理 PUT 请求 |
| `doDelete()` | 处理 DELETE 请求 |
| `service()` | 根据请求方法分发到 `doGet()`、`doPost()` 等 |
| `init()` | Servlet 初始化 |
| `destroy()` | Servlet 销毁 |

---

## 7. Servlet 生命周期

Servlet 生命周期由 Servlet 容器管理，比如 Tomcat。

```text
加载 Servlet 类
   ↓
创建 Servlet 实例
   ↓
调用 init()
   ↓
处理请求 service()
   ↓
调用 destroy()
```

详细流程：

```text
1. Tomcat 启动或第一次请求到来
2. 创建 Servlet 对象
3. 调用 init() 初始化
4. 每次请求调用 service()
5. service() 根据请求方式调用 doGet() / doPost()
6. 容器关闭时调用 destroy()
```

---

## 8. Servlet 是单例还是多例

通常情况下：

> 一个 Servlet 在容器中只有一个实例。

多个请求会并发调用同一个 Servlet 对象的 `service()` 方法。

所以 Servlet 中不要随便定义可变成员变量。

错误示例：

```java
public class UserServlet extends HttpServlet {

    private String username;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        username = request.getParameter("username");
    }
}
```

问题：

```text
多个请求共享同一个 username
可能出现线程安全问题
```

推荐写法：

```java
protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    String username = request.getParameter("username");
}
```

局部变量是线程安全的。

---

## 9. Servlet 请求处理流程

```text
客户端发送 HTTP 请求
   ↓
Tomcat 接收请求
   ↓
根据 URL 找到对应 Servlet
   ↓
创建 HttpServletRequest
   ↓
创建 HttpServletResponse
   ↓
调用 Servlet.service()
   ↓
service() 调用 doGet() / doPost()
   ↓
业务处理
   ↓
写入 Response
   ↓
Tomcat 返回响应
```

---

## 10. Servlet、Filter、Listener 的关系

Java Web 中常见三大组件：

| 组件 | 作用 |
|---|---|
| Servlet | 处理请求并返回响应 |
| Filter | 请求进入 Servlet 前后做过滤 |
| Listener | 监听 Web 应用中的事件 |

---

## 11. Filter 是什么

Filter 过滤器可以在请求到达 Servlet 前后做处理。

常见用途：

```text
登录校验
权限校验
编码处理
日志记录
跨域处理
请求耗时统计
```

流程：

```text
请求
 ↓
Filter
 ↓
Servlet
 ↓
Filter
 ↓
响应
```

---

## 12. Listener 是什么

Listener 用来监听 Web 应用事件。

常见监听：

```text
应用启动
应用销毁
Session 创建
Session 销毁
Request 创建
Request 销毁
```

适合：

```text
初始化资源
统计在线人数
应用启动加载配置
清理资源
```

---

## 13. Servlet 和 Spring MVC 的关系

Spring MVC 底层也是基于 Servlet 的。

核心 Servlet 是：

```text
DispatcherServlet
```

请求流程：

```text
浏览器请求
   ↓
Tomcat
   ↓
DispatcherServlet
   ↓
HandlerMapping 找 Controller
   ↓
Controller 方法执行
   ↓
返回结果
```

也就是说：

> 我们平时写的 `@Controller`、`@RestController`，底层请求入口其实是 Spring MVC 的 `DispatcherServlet`。

---

## 14. Spring MVC 为什么不用我们手写 Servlet

以前写 Java Web，经常手写 Servlet：

```java
public class UserServlet extends HttpServlet {
}
```

现在 Spring MVC 帮我们封装了。

我们只需要写：

```java
@RestController
public class UserController {

    @GetMapping("/user")
    public String getUser() {
        return "Tom";
    }
}
```

底层实际是：

```text
请求先到 DispatcherServlet
DispatcherServlet 再分发给 UserController#getUser()
```

---

## 15. Servlet 和 Controller 的区别

| 对比项 | Servlet | Controller |
|---|---|---|
| 所属体系 | Java Web 规范 | Spring MVC |
| 使用方式 | 继承 `HttpServlet` | 使用注解 |
| 请求映射 | `web.xml` 或注解配置 | `@RequestMapping` 等 |
| 参数获取 | 手动从 request 获取 | Spring 自动绑定 |
| 返回响应 | 手动写 response | 自动序列化 JSON |
| 开发效率 | 较低 | 高 |

Servlet 写法：

```java
String name = request.getParameter("name");
response.getWriter().write(name);
```

Controller 写法：

```java
@GetMapping("/hello")
public String hello(@RequestParam String name) {
    return name;
}
```

---

## 16. Servlet 和 Netty 的区别

| 对比项 | Servlet | Netty |
|---|---|---|
| 定位 | Java Web 规范 | 网络通信框架 |
| 常见容器 | Tomcat、Jetty、Undertow | Netty 自己处理网络 IO |
| 编程模型 | Servlet API | Channel、Handler |
| 常见应用 | Spring MVC、传统 Web 应用 | RPC、网关、IM、长连接 |
| IO 模型 | Servlet 3.1 支持异步，传统多为阻塞模型 | 典型异步非阻塞 |

简单理解：

```text
Servlet 更偏 Web 应用规范
Netty 更偏底层网络通信框架
```

---

## 17. 总结

Servlet 是 Java Web 的一种服务器端组件规范，用来处理 HTTP 请求和响应。它本质上是一个运行在 Servlet 容器中的 Java 类，常见容器有 Tomcat、Jetty、Undertow 等。

Servlet 的生命周期由容器管理，主要包括初始化 `init()`、处理请求 `service()`、销毁 `destroy()`。对于 HTTP 请求，通常继承 `HttpServlet`，然后重写 `doGet()`、`doPost()` 等方法。

Tomcat 接收到请求后，会根据 URL 找到对应的 Servlet，创建 `HttpServletRequest` 和 `HttpServletResponse` 对象，然后调用 Servlet 的 `service()` 方法进行处理。

Spring MVC 底层也是基于 Servlet 的，它的核心入口就是 `DispatcherServlet`。我们平时写的 `@Controller`、`@RestController` 并不是直接被 Tomcat 调用，而是由 `DispatcherServlet` 接收请求后再分发到具体 Controller 方法。

---

## 18. 一句话总结

> Servlet 是 Java Web 的核心规范，用来在服务器端接收请求、处理请求、返回响应；Tomcat 是 Servlet 容器，Spring MVC 的核心入口 `DispatcherServlet` 本质上也是一个 Servlet。
