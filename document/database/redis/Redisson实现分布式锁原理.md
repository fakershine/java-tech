# Redisson 实现分布式锁原理

Redisson 分布式锁是基于 Redis 实现的 **可重入分布式锁**，常用实现类是 `RLock`。  
它实现了 Java `Lock` 接口，只有持有锁的线程才能释放锁，否则会抛出 `IllegalMonitorStateException`。:contentReference[oaicite:0]{index=0}

---

## 1. 核心特点

| 能力 | 说明 |
|---|---|
| 互斥 | 同一时刻只有一个线程能获取锁 |
| 可重入 | 同一线程可以重复获取同一把锁 |
| 自动续期 | WatchDog 自动延长锁过期时间 |
| 自动释放 | 指定 `leaseTime` 后，到期自动释放 |
| 原子操作 | 加锁、解锁通过 Lua 脚本保证原子性 |
| 等待通知 | 获取失败时订阅释放锁消息，避免频繁自旋 |

---

## 2. 基本使用

```java
RLock lock = redissonClient.getLock("order:lock:1001");

try {
    lock.lock();

    // 执行业务逻辑
} finally {
    lock.unlock();
}
```

也可以指定锁自动释放时间：

```java
lock.lock(10, TimeUnit.SECONDS);
```

如果指定了 `leaseTime`，锁会在指定时间后自动释放。:contentReference[oaicite:1]{index=1}

---

## 3. 底层数据结构

Redisson 分布式锁底层主要使用 Redis 的：

```text
Hash
```

结构可以理解为：

```text
key   = 锁名称
field = Redisson 客户端 ID + 线程 ID
value = 重入次数
```

示例：

```text
order:lock:1001
  └── clientId:threadId -> 1
```

为什么用 Hash？

```text
需要记录当前锁属于哪个线程
需要记录同一线程重入次数
```

---

## 4. 加锁原理

加锁时，Redisson 会执行 Lua 脚本，保证判断和加锁是原子操作。

### 加锁逻辑

```text
1. 判断锁 Key 是否存在。
2. 如果不存在，说明没人持有锁。
3. 创建 Hash 结构。
4. field 设置为 当前客户端 ID + 当前线程 ID。
5. value 设置为 1。
6. 设置锁过期时间。
7. 加锁成功。
```

伪逻辑：

```lua
if lock_key 不存在 then
    hset lock_key thread_id 1
    pexpire lock_key expire_time
    return success
end
```

---

## 5. 可重入原理

如果当前线程已经持有这把锁，再次加锁时不会阻塞，而是将重入次数加 1。

### 重入逻辑

```text
1. 判断锁是否存在。
2. 如果锁存在，判断 field 是否是当前线程。
3. 如果是当前线程，说明是重入。
4. 重入次数 +1。
5. 重新设置过期时间。
6. 加锁成功。
```

示例：

```text
第一次 lock：
clientId:threadId -> 1

第二次 lock：
clientId:threadId -> 2

第三次 lock：
clientId:threadId -> 3
```

简单理解：

```text
Redisson 通过 Hash 的 value 记录重入次数。
```

---

## 6. 加锁失败怎么办

如果锁已经被其他线程持有：

```text
当前线程加锁失败
  ↓
返回锁剩余过期时间
  ↓
当前线程订阅锁释放消息
  ↓
等待锁释放或等待超时
  ↓
再次尝试加锁
```

Redisson 不是一直死循环疯狂重试，而是结合：

```text
Pub/Sub 发布订阅机制
```

当持锁线程释放锁时，会发布通知，等待锁的线程收到通知后再尝试竞争锁。

---

## 7. WatchDog 自动续期

### 为什么需要 WatchDog

如果业务执行时间超过锁过期时间，锁可能提前释放。

```text
线程 A 获取锁，过期时间 30 秒
  ↓
业务执行 60 秒
  ↓
30 秒时锁过期
  ↓
线程 B 获取锁
  ↓
A、B 同时执行业务
```

这会破坏互斥性。

---

### WatchDog 原理

如果加锁时没有指定 `leaseTime`，Redisson 会启动 WatchDog 自动续期。

默认锁过期时间是：

```text
30 秒
```

WatchDog 会在锁快过期前自动续期，只要持有锁的 Redisson 实例还活着，就会持续延长锁过期时间。官方文档说明，默认 watchdog 超时时间是 30 秒，可以通过 `Config.lockWatchdogTimeout` 修改。:contentReference[oaicite:2]{index=2}

流程：

```text
获取锁成功
  ↓
设置默认过期时间 30 秒
  ↓
启动 WatchDog 定时任务
  ↓
定期检查当前线程是否仍持有锁
  ↓
如果仍持有锁，重新设置过期时间为 30 秒
  ↓
直到业务执行完成 unlock
```

---

## 8. 指定 leaseTime 时不会自动续期

如果显式指定锁时间：

```java
lock.lock(10, TimeUnit.SECONDS);
```

含义是：

```text
这把锁最多持有 10 秒
10 秒后自动释放
```

这种情况下通常不会走 WatchDog 自动续期逻辑。

所以：

```text
不确定业务耗时时：使用 lock()
明确知道最大耗时时：使用 lock(leaseTime)
```

---

## 9. 解锁原理

解锁时也会执行 Lua 脚本，保证原子性。

### 解锁逻辑

```text
1. 判断锁是否存在。
2. 判断当前线程是否持有锁。
3. 如果不是当前线程，抛出异常。
4. 如果是当前线程，重入次数 -1。
5. 如果重入次数 > 0，只更新次数，不释放锁。
6. 如果重入次数 = 0，删除锁 Key。
7. 发布锁释放消息，通知等待线程。
8. 取消 WatchDog 续期任务。
```

示例：

```text
当前重入次数 = 3

第一次 unlock -> 2，不释放
第二次 unlock -> 1，不释放
第三次 unlock -> 0，真正释放锁
```

---

## 10. 为什么要用 Lua 脚本

因为加锁和解锁都包含多个 Redis 操作。

例如加锁：

```text
判断 Key 是否存在
写入 Hash
设置过期时间
```

如果不用 Lua，多个命令之间可能被其他客户端插入操作。

Lua 可以保证：

```text
多个 Redis 操作原子执行
```

---

## 11. Redisson 和普通 Redis 锁区别

普通 Redis 锁常见写法：

```bash
SET lock_key unique_value NX PX 30000
```

释放锁用 Lua 判断 value 后删除。

Redisson 在此基础上增强了：

| 能力 | 普通 Redis 锁 | Redisson |
|---|---|---|
| 可重入 | 不支持 | 支持 |
| 自动续期 | 不支持 | 支持 WatchDog |
| 等待通知 | 一般自旋重试 | Pub/Sub 通知 |
| 解锁安全 | 需要自己写 Lua | 内置 Lua |
| 使用方式 | 自己封装 | 类似 Java Lock |
| 锁类型 | 简单互斥锁 | 支持公平锁、读写锁、联锁等 |

---

## 12. 主从架构下的问题

Redis 主从复制默认是异步的。

可能出现：

```text
线程 A 在 Master 获取锁成功
  ↓
锁还没同步到 Slave
  ↓
Master 宕机
  ↓
Slave 被提升为新 Master
  ↓
线程 B 又获取锁成功
```

结果：

```text
A 和 B 同时认为自己持有锁
```

这就是 Redis 单主锁在主从切换下的安全风险。

---

## 13. RedLock / MultiLock

为了解决单 Redis 节点或主从切换带来的风险，可以使用多节点锁。

Redisson 支持：

```text
MultiLock
RedLock
```

基本思想：

```text
同时向多个 Redis 节点加锁
只有多数节点加锁成功，才认为获取锁成功
```

适合：

```text
对锁安全性要求更高的场景
```

但代价是：

```text
实现复杂
性能下降
依赖多个 Redis 节点
可用性和一致性需要权衡
```

---

## 14. 使用注意点

- 加锁后必须在 `finally` 中解锁。
- 解锁前建议判断当前线程是否持有锁。
- 不要用 `isLocked()` 判断后直接解锁，建议用 `isHeldByCurrentThread()`。
- 锁粒度不要太大。
- 锁内业务逻辑要尽量短。
- 不要在锁内调用慢接口。
- 写操作要配合幂等。
- 主从切换场景要评估锁安全性。
- 核心强一致场景可以考虑 ZooKeeper 锁或数据库事务。

示例：

```java
RLock lock = redissonClient.getLock("order:lock:1001");

try {
    lock.lock();

    // 业务逻辑
} finally {
    if (lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}
```

---

## 15. 总结

Redisson 分布式锁底层基于 Redis 实现，常用 `RLock`。它使用 Redis Hash 保存锁信息，Key 是锁名称，field 是客户端 ID 加线程 ID，value 是重入次数。

加锁时通过 Lua 脚本保证原子性：如果锁不存在，则创建 Hash 并设置过期时间；如果锁已存在但持有者是当前线程，则重入次数加一并刷新过期时间；如果锁被其他线程持有，则加锁失败并等待释放通知。

Redisson 支持可重入，原因是它在 Hash 中记录了当前线程的加锁次数。解锁时重入次数减一，只有减到零才真正删除锁，并通过 Pub/Sub 通知其他等待线程。

为了防止业务执行时间过长导致锁提前过期，Redisson 提供 WatchDog 自动续期机制。默认锁过期时间是 30 秒，只要持锁客户端还活着，就会定期延长锁过期时间。如果加锁时显式指定了 `leaseTime`，则到期后自动释放，不再依赖 WatchDog 续期。

一句话总结：

```text
Redisson 分布式锁 = Redis Hash 可重入 + Lua 原子加解锁 + WatchDog 自动续期 + Pub/Sub 等待通知。
```
