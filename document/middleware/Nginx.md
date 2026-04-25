# Nginx 总结

Nginx 是一个高性能的 **Web 服务器 / 反向代理服务器 / 负载均衡器**。

常见用途：

```text
静态资源服务
反向代理
负载均衡
网关入口
HTTPS 终止
限流
动静分离
```

---

## 1. Nginx 是什么

Nginx 可以直接处理 HTTP 请求，也可以把请求转发给后端服务。

典型架构：

```text
客户端
  ↓
Nginx
  ↓
Tomcat / Spring Boot / 微服务
```

简单理解：

```text
Nginx 负责接收请求和转发请求
后端服务负责处理业务逻辑
```

---

## 2. Nginx 核心功能

| 功能 | 说明 |
|---|---|
| 静态资源服务 | 直接返回 HTML、CSS、JS、图片 |
| 反向代理 | 代理请求到后端服务 |
| 负载均衡 | 将请求分发到多个后端实例 |
| HTTPS 终止 | 处理 SSL/TLS，加密解密 |
| 动静分离 | 静态资源走 Nginx，动态请求走后端 |
| 限流限速 | 控制访问频率和连接数 |
| 缓存 | 缓存后端响应，降低服务压力 |
| 灰度发布 | 按权重、Header、Cookie 分流 |
| 日志记录 | 记录访问日志和错误日志 |

---

## 3. 正向代理和反向代理

### 正向代理

代理的是客户端。

```text
客户端
  ↓
代理服务器
  ↓
目标服务器
```

特点：

```text
目标服务器不知道真实客户端是谁
```

常见场景：

```text
访问外网
客户端代理
VPN
```

---

### 反向代理

代理的是服务端。

```text
客户端
  ↓
Nginx
  ↓
后端服务器
```

特点：

```text
客户端不知道真实后端服务器是谁
```

常见场景：

```text
网关入口
负载均衡
隐藏后端服务
```

---

## 4. Nginx 为什么性能高

核心原因：

```text
事件驱动
异步非阻塞
IO 多路复用
多进程模型
sendfile 零拷贝
```

Nginx 使用：

```text
Master + Worker 多进程模型
```

Worker 通过 `epoll` 等机制处理大量连接。

```text
少量 Worker
  ↓
处理大量并发连接
```

---

## 5. Nginx 进程模型

```text
Master 进程
  ↓
Worker 进程 1
Worker 进程 2
Worker 进程 3
```

### Master

负责：

```text
读取配置
管理 Worker
平滑重启
信号处理
```

### Worker

负责：

```text
接收连接
处理请求
反向代理
返回响应
```

---

## 6. 常见配置结构

```nginx
worker_processes auto;

events {
    worker_connections 10240;
}

http {
    server {
        listen 80;
        server_name www.demo.com;

        location / {
            proxy_pass http://backend;
        }
    }

    upstream backend {
        server 127.0.0.1:8080;
        server 127.0.0.1:8081;
    }
}
```

---

## 7. 反向代理配置

```nginx
server {
    listen 80;
    server_name api.demo.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
    }
}
```

含义：

```text
客户端访问 api.demo.com
Nginx 将请求转发到 127.0.0.1:8080
```

常用代理头：

```nginx
proxy_set_header Host $host;
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
```

---

## 8. 负载均衡配置

```nginx
upstream backend {
    server 192.168.1.10:8080;
    server 192.168.1.11:8080;
    server 192.168.1.12:8080;
}

server {
    listen 80;

    location / {
        proxy_pass http://backend;
    }
}
```

---

## 9. 负载均衡策略

| 策略 | 说明 |
|---|---|
| 轮询 | 默认策略，请求依次分发 |
| 加权轮询 | 权重越高，分配请求越多 |
| `ip_hash` | 同一 IP 尽量分配到同一后端 |
| `least_conn` | 分配给连接数最少的后端 |
| `hash` | 根据指定 key 做 Hash 分流 |

---

### 加权轮询

```nginx
upstream backend {
    server 192.168.1.10:8080 weight=3;
    server 192.168.1.11:8080 weight=1;
}
```

含义：

```text
10 号机器承接更多流量
```

---

### ip_hash

```nginx
upstream backend {
    ip_hash;
    server 192.168.1.10:8080;
    server 192.168.1.11:8080;
}
```

适合：

```text
会话粘滞
用户固定访问同一台后端
```

---

### least_conn

```nginx
upstream backend {
    least_conn;
    server 192.168.1.10:8080;
    server 192.168.1.11:8080;
}
```

适合：

```text
请求耗时差异较大的场景
```

---

## 10. 静态资源服务

```nginx
server {
    listen 80;

    location /static/ {
        root /data/www;
    }
}
```

如果请求：

```text
/static/a.png
```

实际访问：

```text
/data/www/static/a.png
```

---

### alias

```nginx
location /static/ {
    alias /data/static/;
}
```

如果请求：

```text
/static/a.png
```

实际访问：

```text
/data/static/a.png
```

---

## 11. root 和 alias 区别

| 配置 | 拼接方式 |
|---|---|
| `root` | root 路径 + location 路径 |
| `alias` | alias 路径替换 location 路径 |

简单理解：

```text
root 是追加路径
alias 是替换路径
```

---

## 12. 动静分离

```nginx
location /static/ {
    root /data/www;
}

location /api/ {
    proxy_pass http://backend;
}
```

含义：

```text
/static/ 静态资源由 Nginx 返回
/api/ 动态请求转发到后端服务
```

好处：

```text
减少后端压力
提升静态资源访问性能
```

---

## 13. HTTPS 配置

```nginx
server {
    listen 443 ssl;
    server_name www.demo.com;

    ssl_certificate /etc/nginx/cert/server.crt;
    ssl_certificate_key /etc/nginx/cert/server.key;

    location / {
        proxy_pass http://backend;
    }
}
```

HTTP 跳转 HTTPS：

```nginx
server {
    listen 80;
    server_name www.demo.com;

    return 301 https://$host$request_uri;
}
```

---

## 14. 限流配置

### 请求限流

```nginx
http {
    limit_req_zone $binary_remote_addr zone=req_limit:10m rate=10r/s;

    server {
        location /api/ {
            limit_req zone=req_limit burst=20 nodelay;
            proxy_pass http://backend;
        }
    }
}
```

含义：

```text
每个 IP 每秒最多 10 个请求
burst 允许突发 20 个请求
```

---

### 连接数限制

```nginx
http {
    limit_conn_zone $binary_remote_addr zone=conn_limit:10m;

    server {
        location /api/ {
            limit_conn conn_limit 10;
            proxy_pass http://backend;
        }
    }
}
```

含义：

```text
每个 IP 最多同时 10 个连接
```

---

## 15. 缓存配置

```nginx
proxy_cache_path /data/nginx/cache levels=1:2 keys_zone=my_cache:10m max_size=1g inactive=60m;

server {
    location /api/ {
        proxy_cache my_cache;
        proxy_cache_valid 200 302 10m;
        proxy_cache_valid 404 1m;

        proxy_pass http://backend;
    }
}
```

适合：

```text
热点接口
静态页面
低频变化数据
```

---

## 16. 常用超时配置

```nginx
proxy_connect_timeout 5s;
proxy_send_timeout 30s;
proxy_read_timeout 30s;
client_body_timeout 10s;
client_header_timeout 10s;
keepalive_timeout 65s;
```

含义：

| 配置 | 说明 |
|---|---|
| `proxy_connect_timeout` | 连接后端超时时间 |
| `proxy_send_timeout` | 向后端发送请求超时 |
| `proxy_read_timeout` | 等待后端响应超时 |
| `client_body_timeout` | 读取客户端请求体超时 |
| `keepalive_timeout` | 长连接保持时间 |

---

## 17. 常用命令

检查配置：

```bash
nginx -t
```

启动：

```bash
nginx
```

重载配置：

```bash
nginx -s reload
```

停止：

```bash
nginx -s stop
```

优雅停止：

```bash
nginx -s quit
```

查看进程：

```bash
ps -ef | grep nginx
```

---

## 18. 日志

### 访问日志

```text
access.log
```

记录：

```text
客户端 IP
请求路径
状态码
响应大小
耗时
User-Agent
Referer
```

### 错误日志

```text
error.log
```

记录：

```text
启动错误
配置错误
后端连接失败
超时
权限问题
```

---

## 19. Nginx 和 Tomcat 区别

| 对比项 | Nginx | Tomcat |
|---|---|---|
| 定位 | Web 服务器 / 反向代理 | Servlet 容器 |
| 主要能力 | 静态资源、代理、负载均衡 | 运行 Java Web 应用 |
| 是否执行 Java 代码 | 否 | 是 |
| 并发模型 | 事件驱动、异步非阻塞 | 线程池处理请求 |
| 静态资源性能 | 强 | 一般 |
| 动态业务处理 | 不负责业务 | 负责 Java 业务 |

常见组合：

```text
Nginx 处理入口流量和静态资源
Tomcat 处理 Java 动态请求
```

---

## 20. 总结

Nginx 是高性能 Web 服务器，常用于静态资源服务、反向代理、负载均衡、HTTPS 终止、限流、缓存和动静分离。

Nginx 性能高的原因主要是事件驱动、异步非阻塞、IO 多路复用、多进程模型和零拷贝。它通过 Master 管理 Worker，Worker 负责处理请求。

在实际项目中，Nginx 通常部署在后端服务前面，作为统一入口，将请求转发到多个 Tomcat 或 Spring Boot 实例，实现负载均衡和高可用。

一句话总结：

```text
Nginx = 高性能 Web 服务器 + 反向代理 + 负载均衡；
常用于入口流量转发、静态资源、HTTPS、限流和动静分离。
```
