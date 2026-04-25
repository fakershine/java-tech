# Dockerfile 总结

Dockerfile 是用来 **构建 Docker 镜像的脚本文件**。

核心作用：

```text
定义基础环境
安装依赖
复制应用文件
暴露端口
指定容器启动命令
```

---

## 1. Dockerfile 基本示例

Spring Boot 项目常见 Dockerfile：

```dockerfile
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/demo.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

构建镜像：

```bash
docker build -t demo-app:1.0 .
```

运行容器：

```bash
docker run -d \
  --name demo-app \
  -p 8080:8080 \
  demo-app:1.0
```

---

## 2. 常用指令

| 指令 | 作用 |
|---|---|
| `FROM` | 指定基础镜像 |
| `WORKDIR` | 指定工作目录 |
| `COPY` | 复制文件到镜像 |
| `ADD` | 复制文件，支持自动解压和 URL |
| `RUN` | 构建镜像时执行命令 |
| `CMD` | 容器默认启动命令 |
| `ENTRYPOINT` | 容器入口命令 |
| `EXPOSE` | 声明容器端口 |
| `ENV` | 设置环境变量 |
| `ARG` | 构建参数 |
| `VOLUME` | 声明数据卷 |
| `USER` | 指定运行用户 |
| `LABEL` | 镜像元信息 |

---

## 3. FROM

指定基础镜像。

```dockerfile
FROM nginx:latest
```

Java 项目示例：

```dockerfile
FROM eclipse-temurin:17-jre
```

注意：

```text
尽量不要在线上使用 latest
建议指定明确版本
```

例如：

```dockerfile
FROM eclipse-temurin:17.0.10_7-jre
```

---

## 4. WORKDIR

指定容器中的工作目录。

```dockerfile
WORKDIR /app
```

之后的命令都会基于该目录执行。

等价于：

```bash
cd /app
```

如果目录不存在，会自动创建。

---

## 5. COPY

复制宿主机构建上下文中的文件到镜像。

```dockerfile
COPY target/demo.jar app.jar
```

推荐优先使用 `COPY`，语义清晰。

---

## 6. ADD

`ADD` 也可以复制文件，但功能更多：

```dockerfile
ADD app.tar.gz /app/
```

特点：

```text
可以自动解压 tar 文件
可以从 URL 下载文件
```

一般建议：

```text
普通复制用 COPY
需要自动解压时再用 ADD
```

---

## 7. RUN

`RUN` 是构建镜像时执行命令。

例如：

```dockerfile
RUN apt-get update && apt-get install -y curl
```

注意：

```text
RUN 执行结果会写入镜像层
```

优化写法：

```dockerfile
RUN apt-get update \
    && apt-get install -y curl \
    && rm -rf /var/lib/apt/lists/*
```

---

## 8. CMD

`CMD` 指定容器默认启动命令。

```dockerfile
CMD ["java", "-jar", "app.jar"]
```

特点：

```text
docker run 后面指定命令时，会覆盖 CMD
```

例如：

```bash
docker run demo-app echo hello
```

会覆盖原来的 CMD。

---

## 9. ENTRYPOINT

`ENTRYPOINT` 指定容器入口命令。

```dockerfile
ENTRYPOINT ["java", "-jar", "app.jar"]
```

特点：

```text
通常不会被 docker run 后面的参数直接覆盖
```

适合：

```text
固定启动程序
```

Java 服务更常用：

```dockerfile
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 10. CMD 和 ENTRYPOINT 区别

| 对比项 | CMD | ENTRYPOINT |
|---|---|---|
| 作用 | 默认命令或参数 | 容器入口命令 |
| 是否容易被覆盖 | 容易 | 不容易 |
| 常见用途 | 提供默认参数 | 固定启动程序 |
| Java 服务 | 可用 | 更常用 |

组合使用：

```dockerfile
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--spring.profiles.active=prod"]
```

运行时可以覆盖参数：

```bash
docker run demo-app --spring.profiles.active=test
```

---

## 11. EXPOSE

声明容器内部监听端口。

```dockerfile
EXPOSE 8080
```

注意：

```text
EXPOSE 只是声明，不会自动映射端口
```

运行时仍需要：

```bash
docker run -p 8080:8080 demo-app
```

---

## 12. ENV

设置环境变量。

```dockerfile
ENV APP_ENV=prod
ENV JAVA_OPTS="-Xms512m -Xmx512m"
```

使用：

```dockerfile
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

运行时覆盖：

```bash
docker run -e APP_ENV=test demo-app
```

---

## 13. ARG

`ARG` 是构建阶段参数。

```dockerfile
ARG JAR_FILE=target/demo.jar

COPY ${JAR_FILE} app.jar
```

构建时传参：

```bash
docker build \
  --build-arg JAR_FILE=target/app.jar \
  -t demo-app:1.0 .
```

区别：

```text
ARG：构建镜像时使用
ENV：容器运行时也存在
```

---

## 14. VOLUME

声明数据卷。

```dockerfile
VOLUME /data
```

作用：

```text
提示该目录用于持久化数据
```

实际项目更常在 `docker run` 或 `docker-compose.yml` 中挂载：

```bash
docker run -v /host/data:/data demo-app
```

---

## 15. USER

指定容器运行用户。

```dockerfile
USER app
```

生产环境建议：

```text
不要使用 root 用户运行容器
```

示例：

```dockerfile
RUN addgroup --system app && adduser --system --ingroup app app
USER app
```

---

## 16. LABEL

添加镜像元信息。

```dockerfile
LABEL maintainer="team@example.com"
LABEL version="1.0"
LABEL description="demo app"
```

查看：

```bash
docker inspect demo-app:1.0
```

---

## 17. .dockerignore

`.dockerignore` 用于排除不需要发送到 Docker 构建上下文的文件。

示例：

```text
.git
target/
logs/
*.log
.idea
node_modules
```

作用：

```text
减少构建上下文体积
提升构建速度
避免敏感文件进入镜像
```

如果要复制 jar，需要不要忽略目标 jar：

```text
target/*
!target/*.jar
```

---

## 18. 多阶段构建

多阶段构建可以减少最终镜像体积。

Maven 项目示例：

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /build/target/demo.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

好处：

```text
构建阶段包含 Maven
运行阶段只保留 JRE 和 jar
最终镜像更小
```

---

## 19. Java 服务推荐 Dockerfile

```dockerfile
FROM eclipse-temurin:17-jre

WORKDIR /app

RUN addgroup --system app && adduser --system --ingroup app app

COPY target/demo.jar app.jar

USER app

EXPOSE 8080

ENV JAVA_OPTS="-Xms512m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

---

## 20. Dockerfile 优化建议

- 基础镜像指定明确版本，不用 `latest`。
- 优先使用小镜像，例如 `jre` 而不是 `jdk`。
- 使用 `.dockerignore` 减少构建上下文。
- 多个 `RUN` 合并，减少镜像层。
- 清理无用缓存，减小镜像体积。
- 优先使用 `COPY`，少用 `ADD`。
- 不要把密码、密钥写进镜像。
- 生产环境不要用 root 用户运行。
- 使用多阶段构建。
- 应用日志输出到 stdout/stderr。
- 一个容器只运行一个主进程。

---

## 21. 常见问题

### 1. Dockerfile 中 CMD 和 ENTRYPOINT 怎么选？

Java 服务通常用：

```dockerfile
ENTRYPOINT ["java", "-jar", "app.jar"]
```

如果需要默认参数，可以配合：

```dockerfile
CMD ["--spring.profiles.active=prod"]
```

---

### 2. EXPOSE 后为什么访问不了？

因为 `EXPOSE` 只是声明端口。

还需要运行时映射：

```bash
docker run -p 8080:8080 demo-app
```

---

### 3. 为什么镜像很大？

常见原因：

```text
基础镜像太大
把源码、target、日志都复制进去了
没有使用 .dockerignore
没有清理安装缓存
没有使用多阶段构建
```

---

### 4. 为什么容器启动后立刻退出？

常见原因：

```text
启动命令执行完了
主进程退出
jar 路径错误
启动参数错误
端口冲突
配置文件缺失
```

查看日志：

```bash
docker logs 容器名
```

---

## 22. 总结

Dockerfile 是构建 Docker 镜像的脚本文件，通过一系列指令描述镜像如何构建。

常用指令包括：`FROM` 指定基础镜像，`WORKDIR` 指定工作目录，`COPY` 复制文件，`RUN` 构建时执行命令，`EXPOSE` 声明端口，`ENV` 设置环境变量，`CMD` 和 `ENTRYPOINT` 指定容器启动命令。

生产环境中，Dockerfile 要注意镜像体积、安全性和可维护性。建议使用明确版本的基础镜像，配置 `.dockerignore`，使用多阶段构建，不把密钥写进镜像，不使用 root 用户运行容器。

一句话总结：

```text
Dockerfile = 镜像构建说明书；
核心流程 = 选基础镜像 + 复制应用 + 配置环境 + 指定启动命令。
```
