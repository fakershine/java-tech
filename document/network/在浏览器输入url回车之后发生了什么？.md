# 浏览器输入 URL 回车后发生了什么？

浏览器输入 URL 回车后，本质流程是：

```text
URL 解析
  ↓
浏览器缓存检查
  ↓
DNS 域名解析
  ↓
建立 TCP 连接
  ↓
TLS 握手
  ↓
发送 HTTP 请求
  ↓
服务器处理请求
  ↓
返回 HTTP 响应
  ↓
浏览器解析渲染页面
  ↓
关闭或复用连接
```

---

## 1. URL 解析

浏览器首先解析输入的 URL。

例如：

```text
https://www.example.com:443/index.html?name=tom#top
```

会解析出：

| 部分 | 说明 |
|---|---|
| `https` | 协议 |
| `www.example.com` | 域名 |
| `443` | 端口 |
| `/index.html` | 请求路径 |
| `name=tom` | 查询参数 |
| `#top` | 页面锚点，不会发送给服务器 |

如果没有写协议，浏览器可能会自动补全：

```text
http:// 或 https://
```

---

## 2. 浏览器缓存检查

浏览器会先判断是否可以直接使用缓存。

常见缓存：

```text
强缓存
协商缓存
```

### 强缓存

如果缓存未过期，浏览器直接使用本地缓存，不请求服务器。

相关响应头：

```text
Cache-Control
Expires
```

---

### 协商缓存

如果强缓存失效，浏览器会向服务器确认资源是否变化。

相关请求头：

```text
If-None-Match
If-Modified-Since
```

服务器如果返回：

```text
304 Not Modified
```

浏览器继续使用本地缓存。

---

## 3. DNS 域名解析

如果需要请求服务器，浏览器要先把域名解析成 IP。

```text
www.example.com -> 93.184.216.34
```

DNS 查询顺序一般是：

```text
浏览器 DNS 缓存
  ↓
操作系统 DNS 缓存
  ↓
本地 hosts 文件
  ↓
本地 DNS 服务器
  ↓
根域名服务器
  ↓
顶级域名服务器
  ↓
权威域名服务器
```

---

## 4. 建立 TCP 连接

拿到服务器 IP 后，浏览器会和服务器建立 TCP 连接。

TCP 三次握手：

```text
客户端发送 SYN
  ↓
服务端返回 SYN + ACK
  ↓
客户端发送 ACK
```

三次握手完成后，TCP 连接建立成功。

---

## 5. TLS 握手

如果是 HTTPS，还需要进行 TLS 握手。

主要做几件事：

```text
协商加密算法
验证服务器证书
生成会话密钥
建立加密通道
```

HTTPS 的核心作用：

```text
加密传输
身份认证
防止数据篡改
```

---

## 6. 发送 HTTP 请求

连接建立后，浏览器会发送 HTTP 请求。

请求内容包括：

```text
请求行
请求头
请求体
```

示例：

```http
GET /index.html HTTP/1.1
Host: www.example.com
User-Agent: Chrome
Accept: text/html
Cookie: token=xxx
```

如果是 POST 请求，还会携带请求体。

---

## 7. 服务器处理请求

服务器收到请求后，会进行处理。

常见流程：

```text
Nginx / 网关接收请求
  ↓
路由到后端服务
  ↓
后端接口处理业务
  ↓
查询缓存 / 数据库
  ↓
生成响应结果
```

如果是静态资源，可能直接由 Nginx 返回。

如果是动态接口，会进入后端应用，例如：

```text
Tomcat
Spring MVC
Controller
Service
DAO
Database
```

---

## 8. 返回 HTTP 响应

服务器处理完成后，返回 HTTP 响应。

响应内容包括：

```text
状态行
响应头
响应体
```

示例：

```http
HTTP/1.1 200 OK
Content-Type: text/html
Cache-Control: max-age=3600

<html>...</html>
```

常见状态码：

| 状态码 | 说明 |
|---|---|
| 200 | 请求成功 |
| 301 / 302 | 重定向 |
| 304 | 使用缓存 |
| 400 | 请求参数错误 |
| 401 | 未登录 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器异常 |
| 502 | 网关错误 |
| 504 | 网关超时 |

---

## 9. 浏览器解析 HTML

浏览器拿到 HTML 后开始解析。

```text
HTML -> DOM 树
CSS -> CSSOM 树
DOM + CSSOM -> Render Tree
```

流程：

```text
解析 HTML
  ↓
构建 DOM 树
  ↓
解析 CSS
  ↓
构建 CSSOM 树
  ↓
合成渲染树
```

---

## 10. 加载静态资源

浏览器解析 HTML 时，如果遇到：

```html
<link rel="stylesheet" href="style.css">
<script src="main.js"></script>
<img src="logo.png">
```

会继续发起请求加载：

```text
CSS
JavaScript
图片
字体
视频
```

这些资源也会经历：

```text
缓存检查 -> DNS -> TCP/TLS -> HTTP 请求 -> 响应
```

---

## 11. 页面渲染

浏览器渲染页面主要包括：

```text
构建 DOM
构建 CSSOM
生成 Render Tree
布局 Layout
绘制 Paint
合成 Composite
```

### Layout

计算元素的位置和大小。

```text
这个元素多宽、多高、在哪个位置
```

### Paint

把元素绘制成像素。

```text
颜色、边框、文字、阴影
```

### Composite

把不同图层合成最终页面。

---

## 12. JavaScript 执行

如果页面中有 JavaScript，浏览器会执行 JS。

JS 可能会：

```text
修改 DOM
修改 CSS
发送 Ajax 请求
绑定事件
操作本地存储
```

如果 JS 修改了页面结构或样式，可能触发：

```text
重排 Reflow
重绘 Repaint
```

---

## 13. 连接关闭或复用

请求完成后，连接不一定马上关闭。

HTTP/1.1 默认支持：

```text
Keep-Alive
```

可以复用 TCP 连接，避免频繁建立连接。

HTTP/2 还支持：

```text
多路复用
```

一个 TCP 连接上可以并发传输多个请求。

---

## 14. 总结

在浏览器输入 URL 后，浏览器首先会解析 URL，然后检查本地缓存。如果缓存不能直接使用，就会进行 DNS 解析，把域名解析成 IP 地址。

拿到 IP 后，浏览器会和服务器建立 TCP 连接；如果是 HTTPS，还会进行 TLS 握手，建立安全加密通道。

连接建立后，浏览器发送 HTTP 请求，服务器接收请求并处理业务逻辑，最后返回 HTTP 响应。

浏览器拿到响应后，会解析 HTML，构建 DOM 树，解析 CSS 构建 CSSOM 树，然后生成渲染树，经过布局、绘制和合成，最终展示页面。如果页面中包含 CSS、JS、图片等资源，浏览器还会继续发起请求加载这些资源。

一句话总结：

```text
输入 URL 回车 = URL 解析 + 缓存判断 + DNS 解析 + TCP/TLS 连接 + HTTP 请求响应 + 浏览器解析渲染。
```
