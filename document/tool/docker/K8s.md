# Kubernetes / K8s 总结

Kubernetes，简称 K8s，是一个 **容器编排平台**，用于自动化部署、扩缩容和管理容器化应用。官方定义中，Kubernetes 是用于管理容器化工作负载和服务的可移植、可扩展开源平台，支持声明式配置和自动化。:contentReference[oaicite:0]{index=0}

核心作用：

```text
管理容器
自动部署
自动扩缩容
故障自愈
服务发现
负载均衡
滚动发布
配置管理
```

---

## 1. 为什么需要 K8s

如果只有 Docker，只能解决：

```text
应用打包和运行环境一致
```

但生产环境还需要解决：

```text
容器部署在哪台机器
容器挂了怎么办
流量怎么转发
服务如何发现
如何扩容缩容
如何滚动发布
配置如何管理
数据如何持久化
```

K8s 就是用来统一管理这些问题的。

---

## 2. K8s 核心架构

K8s 集群由两部分组成：

```text
Control Plane 控制平面
Worker Node 工作节点
```

官方文档也将 K8s 集群划分为控制平面和一个或多个工作节点。:contentReference[oaicite:1]{index=1}

```text
用户 / kubectl
  ↓
API Server
  ↓
Scheduler / Controller Manager
  ↓
Worker Node
  ↓
Pod / Container
```

---

## 3. Control Plane 组件

| 组件 | 作用 |
|---|---|
| API Server | 集群统一入口，接收 kubectl / 控制器请求 |
| etcd | 保存集群状态和配置 |
| Scheduler | 负责把 Pod 调度到合适 Node |
| Controller Manager | 维护期望状态，例如副本数、节点状态 |
| Cloud Controller Manager | 对接云厂商资源 |

---

## 4. Worker Node 组件

| 组件 | 作用 |
|---|---|
| kubelet | 管理本节点 Pod 生命周期 |
| kube-proxy | 负责 Service 网络转发 |
| Container Runtime | 运行容器，例如 containerd |
| Pod | K8s 最小调度单位 |

---

## 5. Pod

Pod 是 K8s 中最小的部署和调度单位。一个 Pod 可以包含一个或多个容器，这些容器共享网络命名空间和存储卷。:contentReference[oaicite:2]{index=2}

```text
Pod
├── Container A
├── Container B
└── Volume
```

特点：

```text
一个 Pod 有一个 IP
Pod 内容器共享网络
Pod 内容器可以通过 localhost 通信
Pod 通常是临时的，可能被重建
```

---

## 6. Deployment

Deployment 用于管理无状态应用。

作用：

```text
创建 Pod
维持副本数量
滚动发布
版本回滚
扩容缩容
```

示例：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
    spec:
      containers:
        - name: user-service
          image: user-service:1.0
          ports:
            - containerPort: 8080
```

---

## 7. Service

Pod IP 会变化，Service 用来提供稳定访问入口。Kubernetes 官方文档说明，Service 用于暴露运行在一个或多个 Pod 上的网络应用，并提供统一访问端点。:contentReference[oaicite:3]{index=3}

```text
Client
  ↓
Service
  ↓
Pod1 / Pod2 / Pod3
```

常见类型：

| 类型 | 说明 |
|---|---|
| ClusterIP | 集群内部访问，默认类型 |
| NodePort | 通过节点端口暴露服务 |
| LoadBalancer | 通过云负载均衡暴露服务 |
| ExternalName | 映射外部域名 |

---

## 8. Ingress

Ingress 用于管理外部 HTTP / HTTPS 访问。

```text
外部请求
  ↓
Ingress
  ↓
Service
  ↓
Pod
```

适合：

```text
域名路由
路径路由
HTTPS 证书
统一入口
```

示例：

```text
api.demo.com/user  -> user-service
api.demo.com/order -> order-service
```

---

## 9. ConfigMap 和 Secret

### ConfigMap

用于保存普通配置：

```text
配置文件
环境变量
应用参数
```

### Secret

用于保存敏感数据，例如密码、Token、密钥；官方文档也说明 Secret 用于保存少量敏感数据，避免直接写入 Pod 配置或镜像中。:contentReference[oaicite:4]{index=4}

```text
数据库密码
Access Token
证书密钥
```

区别：

| 对比项 | ConfigMap | Secret |
|---|---|---|
| 数据类型 | 普通配置 | 敏感配置 |
| 常见内容 | 配置项、开关 | 密码、Token、证书 |
| 使用方式 | 环境变量 / 文件挂载 | 环境变量 / 文件挂载 |

---

## 10. StatefulSet

StatefulSet 用于管理有状态应用。官方文档说明，StatefulSet 会管理一组 Pod，并为每个 Pod 保持稳定、唯一的身份，适合需要持久存储或稳定网络标识的应用。:contentReference[oaicite:5]{index=5}

适合：

```text
MySQL
Redis
Kafka
ZooKeeper
Elasticsearch
```

特点：

```text
Pod 名称稳定
网络标识稳定
存储稳定
按顺序创建和删除
```

---

## 11. DaemonSet

DaemonSet 用于保证每个 Node 上都运行一个 Pod。

适合：

```text
日志采集 Agent
监控 Agent
网络插件
存储插件
```

例如：

```text
每台机器都部署 Filebeat
每台机器都部署 node-exporter
```

---

## 12. Job 和 CronJob

### Job

用于执行一次性任务：

```text
数据迁移
批处理
初始化任务
```

### CronJob

用于定时任务：

```text
每天凌晨清理数据
每小时同步报表
定时备份
```

---

## 13. K8s 发布流程

```text
开发提交代码
  ↓
构建 Docker 镜像
  ↓
推送镜像仓库
  ↓
更新 Deployment 镜像版本
  ↓
K8s 滚动创建新 Pod
  ↓
旧 Pod 逐步下线
  ↓
Service 自动转发到新 Pod
```

---

## 14. 滚动发布和回滚

Deployment 支持滚动发布：

```text
逐步启动新版本 Pod
逐步停止旧版本 Pod
```

查看发布状态：

```bash
kubectl rollout status deployment user-service
```

回滚：

```bash
kubectl rollout undo deployment user-service
```

---

## 15. 扩容缩容

手动扩容：

```bash
kubectl scale deployment user-service --replicas=5
```

自动扩容通常用 HPA：

```text
Horizontal Pod Autoscaler
```

可以根据：

```text
CPU
内存
自定义指标
```

自动调整 Pod 数量。

---

## 16. 存储

容器本身是临时的，Pod 重建后本地数据可能丢失。

K8s 常用：

| 概念 | 说明 |
|---|---|
| Volume | Pod 使用的存储卷 |
| PV | PersistentVolume，集群中的持久化存储 |
| PVC | PersistentVolumeClaim，应用申请存储 |
| StorageClass | 动态创建存储的模板 |

常见场景：

```text
数据库数据目录
日志目录
上传文件
中间件数据
```

---

## 17. 网络模型

K8s 网络核心要求：

```text
Pod 和 Pod 可以直接通信
Node 和 Pod 可以通信
Service 提供稳定访问入口
```

常见网络插件：

```text
Calico
Flannel
Cilium
```

---

## 18. 常用 kubectl 命令

```bash
# 查看节点
kubectl get nodes

# 查看 Pod
kubectl get pods

# 查看所有资源
kubectl get all

# 查看指定命名空间 Pod
kubectl get pods -n default

# 查看 Pod 详情
kubectl describe pod pod-name

# 查看日志
kubectl logs pod-name

# 进入容器
kubectl exec -it pod-name -- /bin/sh

# 应用配置
kubectl apply -f deployment.yaml

# 删除资源
kubectl delete -f deployment.yaml

# 查看 Deployment
kubectl get deployment

# 扩容
kubectl scale deployment user-service --replicas=5

# 查看 Service
kubectl get svc

# 查看 Ingress
kubectl get ingress
```

---

## 19. K8s 和 Docker 的关系

| 对比项 | Docker | Kubernetes |
|---|---|---|
| 定位 | 容器运行和镜像构建 | 容器编排和集群管理 |
| 解决问题 | 应用打包、容器运行 | 部署、调度、扩缩容、自愈 |
| 管理范围 | 单机容器 | 集群容器 |
| 典型命令 | `docker run` | `kubectl apply` |

简单理解：

```text
Docker 负责把应用装进容器
K8s 负责管理大量容器
```

---

## 20. 总结

Kubernetes 是容器编排平台，用于自动化部署、扩缩容和管理容器化应用。

K8s 集群由控制平面和工作节点组成。控制平面包括 API Server、etcd、Scheduler、Controller Manager；工作节点包括 kubelet、kube-proxy、Container Runtime 和 Pod。

Pod 是 K8s 最小调度单位；Deployment 用于管理无状态应用和滚动发布；Service 提供稳定访问入口；Ingress 管理外部 HTTP/HTTPS 流量；ConfigMap 和 Secret 用于配置管理；StatefulSet 用于有状态应用；DaemonSet 用于每个节点部署一个守护进程。

一句话总结：

```text
K8s = 容器编排平台；
核心能力 = Pod 调度 + 服务发现 + 负载均衡 + 自动扩缩容 + 故障自愈 + 滚动发布。
```
