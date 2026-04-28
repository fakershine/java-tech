# Docker 总结

Docker 是一种 **容器化技术**，用于把应用和运行环境一起打包，保证应用在不同机器上都能一致运行。

核心思想：

```text
一次构建
到处运行
```

---

## 1. Docker 解决什么问题

传统部署常见问题：

```text
开发环境能跑，测试环境不能跑
本地 JDK / MySQL / Redis 版本不一致
部署步骤复杂
服务迁移成本高
依赖冲突
```

Docker 通过容器解决：

```text
应用 + 依赖 + 配置 + 运行环境
统一打包成镜像
```

---

## 2. Docker 核心概念

| 概念 | 说明 |
|---|---|
| Image | 镜像，应用运行环境模板 |
| Container | 容器，镜像运行后的实例 |
| Dockerfile | 构建镜像的脚本文件 |
| Registry | 镜像仓库，如 Docker Hub |
| Volume | 数据卷，用于持久化数据 |
| Network | Docker 网络，用于容器通信 |

---

## 3. 镜像 Image

镜像可以理解为：

```text
应用运行所需环境的只读模板
```

例如：

```text
JDK 镜像
MySQL 镜像
Redis 镜像
Nginx 镜像
```

查看本地镜像：

```bash
docker images
```

拉取镜像：

```bash
docker pull nginx
```

删除镜像：

```bash
docker rmi nginx
```

---

## 4. 容器 Container

容器是镜像运行后的实例。

```text
镜像 = 类
容器 = 对象
```

启动容器：

```bash
docker run -d --name my-nginx -p 80:80 nginx
```

查看运行中的容器：

```bash
docker ps
```

查看所有容器：

```bash
docker ps -a
```

停止容器：

```bash
docker stop my-nginx
```

删除容器：

```bash
docker rm my-nginx
```

---

## 5. Dockerfile

Dockerfile 用来定义如何构建镜像。

示例：

```dockerfile
FROM openjdk:17

WORKDIR /app

COPY target/demo.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

构建镜像：

```bash
docker build -t demo-app:1.0 .
```

运行镜像：

```bash
docker run -d -p 8080:8080 --name demo-app demo-app:1.0
```

---

## 6. 常用 Dockerfile 指令

| 指令 | 说明 |
|---|---|
| `FROM` | 指定基础镜像 |
| `WORKDIR` | 指定工作目录 |
| `COPY` | 复制文件到镜像 |
| `RUN` | 构建镜像时执行命令 |
| `EXPOSE` | 声明容器端口 |
| `ENV` | 设置环境变量 |
| `CMD` | 容器默认启动命令 |
| `ENTRYPOINT` | 容器入口命令 |

---

## 7. 端口映射

容器内部端口默认不能直接被宿主机访问。

需要端口映射：

```bash
docker run -p 8080:80 nginx
```

含义：

```text
宿主机 8080 端口 -> 容器 80 端口
```

访问：

```text
http://宿主机IP:8080
```

---

## 8. 数据卷 Volume

容器删除后，容器内数据也可能丢失。

数据卷用于持久化数据。

```bash
docker run -d \
  --name mysql \
  -v /data/mysql:/var/lib/mysql \
  mysql:8
```

含义：

```text
宿主机 /data/mysql
映射到容器 /var/lib/mysql
```

适合：

```text
MySQL 数据
Redis 数据
日志文件
上传文件
```

---

## 9. Docker 网络

Docker 默认会创建网络，容器之间可以通信。

查看网络：

```bash
docker network ls
```

创建网络：

```bash
docker network create app-net
```

运行容器并加入网络：

```bash
docker run -d --name redis --network app-net redis
```

同一个网络下，容器可以通过容器名访问：

```text
redis:6379
```

---

## 10. Docker Compose

Docker Compose 用于管理多个容器。

例如：

```text
Spring Boot + MySQL + Redis + Nginx
```

可以用一个 `docker-compose.yml` 启动。

示例：

```yaml
version: "3.8"

services:
  app:
    image: demo-app:1.0
    ports:
      - "8080:8080"
    depends_on:
      - redis
      - mysql

  redis:
    image: redis:7
    ports:
      - "6379:6379"

  mysql:
    image: mysql:8
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: demo
    ports:
      - "3306:3306"
```

启动：

```bash
docker compose up -d
```

停止：

```bash
docker compose down
```

---

## 11. 常用命令

### 镜像命令

```bash
docker images
docker pull nginx
docker build -t app:1.0 .
docker rmi imageId
```

### 容器命令

```bash
docker ps
docker ps -a
docker run -d --name app app:1.0
docker stop app
docker start app
docker restart app
docker rm app
```

### 日志和进入容器

```bash
docker logs app
docker logs -f app
docker exec -it app /bin/bash
```

### 查看资源

```bash
docker stats
docker inspect app
```

---

## 12. Docker 和虚拟机区别

| 对比项 | Docker | 虚拟机 |
|---|---|---|
| 虚拟化层级 | 操作系统级 | 硬件级 |
| 启动速度 | 快，秒级 | 慢，分钟级 |
| 资源占用 | 小 | 大 |
| 隔离性 | 较强 | 更强 |
| 镜像大小 | 通常较小 | 通常较大 |
| 适合场景 | 微服务、快速部署 | 完整 OS 隔离 |

简单理解：

```text
虚拟机模拟一整套操作系统
Docker 复用宿主机内核，只隔离进程环境
```

---

## 13. Docker 底层原理

Docker 主要依赖 Linux 的：

```text
Namespace
Cgroups
UnionFS
```

### Namespace

用于资源隔离：

```text
进程隔离
网络隔离
文件系统隔离
用户隔离
```

### Cgroups

用于资源限制：

```text
CPU 限制
内存限制
磁盘 IO 限制
网络限制
```

### UnionFS

用于镜像分层。

```text
基础镜像层
依赖层
应用代码层
配置层
```

好处：

```text
镜像复用
构建更快
节省磁盘空间
```

---

## 14. Docker 优点

- 环境一致。
- 部署简单。
- 启动快。
- 资源占用低。
- 方便扩容。
- 方便 CI/CD。
- 适合微服务。
- 方便灰度和回滚。

---

## 15. Docker 缺点

- 容器隔离性弱于虚拟机。
- 需要学习镜像、网络、存储。
- 数据持久化需要额外设计。
- 容器过多需要编排工具。
- 生产环境通常需要 Kubernetes 管理。

---

## 16. 常见使用场景

```text
微服务部署
本地开发环境
测试环境隔离
CI/CD 自动化部署
中间件快速启动
灰度发布
弹性扩容
```

---

## 17. 总结

Docker 是容器化技术，可以把应用、依赖和运行环境一起打包成镜像，再通过镜像启动容器，从而解决环境不一致和部署复杂的问题。

Docker 的核心概念包括镜像、容器、Dockerfile、镜像仓库、数据卷和网络。镜像是运行模板，容器是镜像运行后的实例，Dockerfile 用来构建镜像，Volume 用来持久化数据，Network 用来支持容器通信。

Docker 底层依赖 Linux Namespace 做隔离，Cgroups 做资源限制，UnionFS 做镜像分层。

一句话总结：

```text
Docker = 镜像打包环境 + 容器运行应用；
核心价值是环境一致、部署简单、启动快、资源占用低。
```
