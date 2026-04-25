# HTTP 协议总结

HTTP 是应用层协议，全称：

```text
HyperText Transfer Protocol
```

核心作用：

```text
客户端和服务端之间传输超文本数据
```

常见场景：

```text
网页访问
接口调用
图片加载
文件下载
前后端通信
```

---

## 1. HTTP 在网络模型中的位置

```text
应用层：HTTP、HTTPS、DNS、FTP
传输层：TCP、UDP
网络层：IP
数据链路层：以太网、Wi-Fi
```

HTTP 通常基于 TCP。

```text
HTTP = 应用层协议
TCP = 传输层协议
IP = 网络层协议
```

---

## 2. HTTP 的特点

| 特点 | 说明 |
|---|---|
| 简单 | 请求和响应格式清晰 |
| 灵活 | 可以传输 HTML、JSON、图片、文件 |
| 无状态 | 每次请求相互独立 |
| 明文传输 | HTTP 默认不加密 |
| 请求响应模型 | 客户端请求，服务端响应 |
| 可扩展 | 通过 Header 扩展能力 |

---

## 3. HTTP 为什么是无状态

HTTP 无状态是指：

```text
服务端不会天然记住上一次请求是谁发的
```

例如：

```text
第一次请求登录
第二次请求订单
```

HTTP 本身不会自动关联这两次请求。

所以需要：

```text
Cookie
Session
Token
JWT
```

来维护登录态。

---

## 4. HTTP 请求结构

HTTP 请求一般由三部分组成：

```text
请求行
请求头
请求体
```

示例：

```http
POST /api/login HTTP/1.1
Host: www.demo.com
Content-Type: application/json
Authorization: Bearer token

{"username":"tom","password":"123456"}
```

---

## 5. 请求行

请求行包含：

```text
请求方法 + 请求路径 + 协议版本
```

示例：

```http
GET /user?id=1 HTTP/1.1
```

---

## 6. 请求头

请求头用于描述请求的附加信息。

常见 Header：

| Header | 说明 |
|---|---|
| `Host` | 目标主机 |
| `User-Agent` | 客户端信息 |
| `Content-Type` | 请求体类型 |
| `Accept` | 客户端能接收的数据类型 |
| `Authorization` | 认证信息 |
| `Cookie` | Cookie 信息 |
| `Referer` | 来源页面 |
| `Connection` | 连接控制 |

---

## 7. 请求体

请求体用于传输数据。

常见于：

```text
POST
PUT
PATCH
```

常见格式：

```text
application/json
application/x-www-form-urlencoded
multipart/form-data
```

例如 JSON：

```json
{
  "username": "tom",
  "password": "123456"
}
```

---

## 8. HTTP 响应结构

HTTP 响应一般由三部分组成：

```text
状态行
响应头
响应体
```

示例：

```http
HTTP/1.1 200 OK
Content-Type: application/json
Cache-Control: no-cache

{"code":0,"message":"success"}
```

---

## 9. 状态行

状态行包含：

```text
协议版本 + 状态码 + 状态描述
```

示例：

```http
HTTP/1.1 200 OK
```

---

## 10. 常见状态码

| 状态码 | 说明 |
|---|---|
| 200 | 请求成功 |
| 201 | 创建成功 |
| 204 | 成功但无响应体 |
| 301 | 永久重定向 |
| 302 | 临时重定向 |
| 304 | 资源未修改，使用缓存 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 405 | 请求方法不允许 |
| 408 | 请求超时 |
| 429 | 请求过多，被限流 |
| 500 | 服务端异常 |
| 502 | 网关错误 |
| 503 | 服务不可用 |
| 504 | 网关超时 |

---

## 11. HTTP 请求方法

| 方法 | 说明 |
|---|---|
| GET | 查询资源 |
| POST | 创建资源 / 提交数据 |
| PUT | 全量更新资源 |
| PATCH | 局部更新资源 |
| DELETE | 删除资源 |
| HEAD | 只获取响应头 |
| OPTIONS | 查询服务支持的方法，跨域预检常用 |

---

## 12. GET 和 POST 区别

| 对比项 | GET | POST |
|---|---|---|
| 语义 | 查询数据 | 提交数据 |
| 参数位置 | URL Query | 请求体 Body |
| 是否适合传敏感数据 | 不适合 | 相对更适合，但仍需 HTTPS |
| 幂等性 | 通常幂等 | 通常不幂等 |
| 缓存 | 更容易被缓存 | 默认不缓存 |
| 长度限制 | 受 URL 长度限制 | 理论上更大 |

注意：

```text
GET 也可以有 Body，但不推荐。
POST 参数也不是天然安全，必须配合 HTTPS。
```

---

## 13. 幂等性

幂等是指：

```text
一次请求和多次相同请求，对资源结果影响一致
```

常见幂等方法：

```text
GET
PUT
DELETE
```

通常非幂等：

```text
POST
```

例如：

```text
GET 查询订单，执行多次结果一样
POST 创建订单，执行多次可能创建多笔订单
```

---

## 14. HTTP 缓存

HTTP 缓存分为：

```text
强缓存
协商缓存
```

---

### 强缓存

浏览器直接使用本地缓存，不请求服务器。

常见响应头：

```http
Cache-Control: max-age=3600
Expires: Wed, 21 Oct 2026 07:28:00 GMT
```

优先级：

```text
Cache-Control > Expires
```

---

### 协商缓存

浏览器向服务器确认资源是否变化。

相关 Header：

```http
ETag
If-None-Match
Last-Modified
If-Modified-Since
```

如果资源没变，服务器返回：

```http
304 Not Modified
```

浏览器继续使用本地缓存。

---

## 15. Cookie

Cookie 是服务端发送给浏览器，并保存在浏览器本地的数据。

后续请求会自动携带：

```http
Cookie: token=abc123
```

服务端设置 Cookie：

```http
Set-Cookie: token=abc123; HttpOnly; Secure; SameSite=Lax
```

常见属性：

| 属性 | 说明 |
|---|---|
| `HttpOnly` | 禁止 JS 读取，降低 XSS 风险 |
| `Secure` | 仅 HTTPS 传输 |
| `SameSite` | 限制跨站携带，降低 CSRF 风险 |
| `Max-Age` | 有效时间 |
| `Domain` | 生效域名 |
| `Path` | 生效路径 |

---

## 16. Session / Token

由于 HTTP 无状态，登录态通常通过：

```text
Cookie + Session
Token
JWT
```

实现。

### Cookie + Session

```text
服务端保存 Session
浏览器 Cookie 保存 sessionId
```

### Token / JWT

```text
客户端保存 Token
请求时通过 Header 携带
服务端校验 Token
```

常见方式：

```http
Authorization: Bearer token
```

---

## 17. HTTP 长连接

HTTP/1.0 默认短连接。

HTTP/1.1 默认支持长连接：

```http
Connection: keep-alive
```

好处：

```text
多个请求复用同一个 TCP 连接
减少三次握手开销
提升性能
```

---

## 18. HTTP 队头阻塞

HTTP/1.1 中，一个 TCP 连接上的请求响应通常需要按顺序处理。

如果前一个响应很慢，后面的响应可能被阻塞。

这就是：

```text
HTTP/1.1 队头阻塞
```

HTTP/2 通过多路复用缓解了这个问题。

---

## 19. HTTP/1.0、HTTP/1.1、HTTP/2、HTTP/3

| 版本 | 特点 |
|---|---|
| HTTP/1.0 | 默认短连接 |
| HTTP/1.1 | 默认长连接，支持管道化 |
| HTTP/2 | 二进制分帧、多路复用、头部压缩 |
| HTTP/3 | 基于 QUIC / UDP，降低 TCP 队头阻塞影响 |

---

## 20. HTTP/2 特点

HTTP/2 主要优化：

```text
二进制分帧
多路复用
Header 压缩
服务端推送
```

### 多路复用

```text
一个 TCP 连接上可以并发多个请求和响应
```

减少连接数量，提高传输效率。

---

## 21. HTTP 和 HTTPS 区别

| 对比项 | HTTP | HTTPS |
|---|---|---|
| 是否加密 | 不加密 | 加密 |
| 默认端口 | 80 | 443 |
| 安全性 | 较低 | 高 |
| 协议组成 | HTTP + TCP | HTTP + TLS + TCP |
| 是否校验证书 | 否 | 是 |

HTTPS 解决：

```text
窃听
篡改
伪造
```

---

## 22. HTTP 和 TCP 的关系

```text
HTTP 负责应用层请求响应格式
TCP 负责可靠传输
```

访问一个网页：

```text
先建立 TCP 连接
再发送 HTTP 请求
最后接收 HTTP 响应
```

---

## 23. 常见 Content-Type

| Content-Type | 说明 |
|---|---|
| `application/json` | JSON 数据 |
| `application/x-www-form-urlencoded` | 表单键值对 |
| `multipart/form-data` | 文件上传 |
| `text/html` | HTML 页面 |
| `text/plain` | 普通文本 |
| `application/octet-stream` | 二进制流 |

---

## 24. 常见 Header

| Header | 说明 |
|---|---|
| `Content-Type` | 请求体 / 响应体类型 |
| `Content-Length` | 内容长度 |
| `Authorization` | 认证信息 |
| `Cookie` | 客户端 Cookie |
| `Set-Cookie` | 服务端设置 Cookie |
| `Cache-Control` | 缓存控制 |
| `ETag` | 资源版本标识 |
| `User-Agent` | 客户端信息 |
| `X-Forwarded-For` | 代理后的客户端 IP |
| `Referer` | 来源地址 |

---

## 25. 总结

HTTP 是应用层协议，采用请求响应模型，客户端发送请求，服务端返回响应。HTTP 本身是无状态的，所以需要 Cookie、Session、Token 或 JWT 维护用户登录状态。

HTTP 请求由请求行、请求头、请求体组成；响应由状态行、响应头、响应体组成。常见请求方法有 GET、POST、PUT、DELETE，常见状态码有 200、301、302、304、400、401、403、404、500、502、504。

HTTP/1.1 默认支持长连接，减少 TCP 建连开销；HTTP/2 支持二进制分帧、多路复用和头部压缩；HTTPS 则是在 HTTP 基础上加入 TLS，保证数据加密、防篡改和身份认证。

一句话总结：

```text
HTTP = 应用层请求响应协议；
特点是简单、灵活、无状态；
HTTPS = HTTP + TLS，解决传输安全问题。
```
