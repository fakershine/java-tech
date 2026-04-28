# CompletableFuture 的实现原理

## 1. 一句话总结

> `CompletableFuture` 的核心原理是：  
> **用一个 volatile 变量保存任务结果，用一个 Completion 链表保存后续回调任务，当前任务完成后通过 CAS 设置结果，并触发依赖它的回调任务继续执行。**

简单流程：

```text
创建 CompletableFuture
   ↓
提交异步任务到线程池
   ↓
任务执行完成
   ↓
CAS 设置 result
   ↓
触发依赖当前 Future 的回调任务
   ↓
回调任务继续完成自己的 CompletableFuture
```

---

## 2. CompletableFuture 解决了什么问题

传统 `Future` 只能这样用：

```java
Future<String> future = executor.submit(() -> "hello");

String result = future.get(); // 阻塞
```

问题：

```text
只能阻塞等待
不能方便地做回调
不能方便地串联任务
不能方便地合并多个任务
异常处理不方便
```

`CompletableFuture` 在 `Future` 基础上增加了：

```text
任务完成回调
任务串行编排
任务并行合并
异常传播
结果主动完成
非阻塞式任务链
```

---

# 一、CompletableFuture 的核心结构

## 3. 核心字段

`CompletableFuture` 内部最核心的字段可以简化理解为两个：

```java
volatile Object result;

volatile Completion stack;
```

含义：

| 字段 | 作用 |
|---|---|
| `result` | 保存当前任务的执行结果 |
| `stack` | 保存依赖当前任务的回调任务链 |

---

## 4. result 保存什么

`result` 用来保存任务最终状态。

它可能保存：

```text
正常结果
null 结果
异常结果
取消结果
```

由于 `null` 也可能是正常结果，所以 JDK 内部会用特殊对象包装。

简化理解：

```text
result == null       表示任务未完成
result != null       表示任务已完成
```

正常结果：

```text
result = "hello"
```

异常结果：

```text
result = AltResult(exception)
```

取消结果本质上也是一种异常结果。

---

## 5. stack 保存什么

`stack` 保存的是依赖当前 `CompletableFuture` 的后续任务。

例如：

```java
CompletableFuture<String> future = CompletableFuture
        .supplyAsync(() -> "hello")
        .thenApply(s -> s + " world")
        .thenAccept(System.out::println);
```

这里会形成一条任务依赖链：

```text
future1：执行 supplyAsync
   ↓
future2：执行 thenApply
   ↓
future3：执行 thenAccept
```

每一个 `thenApply`、`thenAccept` 都会被包装成一个内部的 `Completion` 节点，挂到前一个 `CompletableFuture` 的 `stack` 上。

---

# 二、CompletableFuture 执行流程

## 6. supplyAsync 的执行原理

代码：

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    return "hello";
});
```

大致流程：

```text
1. 创建一个 CompletableFuture 对象
2. 把 Supplier 包装成异步任务
3. 提交到线程池
4. 线程池执行 Supplier
5. 执行成功后，把结果 CAS 设置到 result
6. 触发后续 Completion 回调
```

简化伪代码：

```java
CompletableFuture<T> future = new CompletableFuture<>();

executor.execute(() -> {
    try {
        T value = supplier.get();
        future.complete(value);
    } catch (Throwable e) {
        future.completeExceptionally(e);
    }
});

return future;
```

---

## 7. complete 的核心逻辑

```java
future.complete("hello");
```

内部核心逻辑类似：

```text
1. 判断 result 是否为空
2. 如果为空，CAS 设置 result
3. 设置成功后，触发 stack 中的回调任务
4. 如果 result 已经有值，说明已经完成，不能重复完成
```

伪代码：

```java
boolean completeValue(Object value) {
    if (result == null) {
        if (CAS(result, null, value)) {
            postComplete();
            return true;
        }
    }
    return false;
}
```

重点：

```text
CompletableFuture 只能完成一次
后续再次 complete 不会覆盖第一次结果
```

---

## 8. thenApply 的执行原理

代码：

```java
CompletableFuture<String> future2 = future1.thenApply(result -> {
    return result + " world";
});
```

大致流程：

```text
1. 创建新的 CompletableFuture future2
2. 创建 Completion 节点，保存：
   - 上一个 future1
   - 新的 future2
   - 回调函数 fn
3. 如果 future1 还没完成，把 Completion 挂到 future1.stack
4. 如果 future1 已经完成，直接触发该 Completion
5. Completion 执行后，把结果设置到 future2.result
```

简化理解：

```text
thenApply 不会直接执行
它只是注册一个回调任务
等上一个 CompletableFuture 完成后再触发
```

---

## 9. thenApply 的依赖关系

```java
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> "hello");

CompletableFuture<String> f2 = f1.thenApply(s -> s + " world");
```

内部关系：

```text
f1
 ├── result = null
 └── stack = UniApply(f2, fn)
```

当 `f1` 完成：

```text
f1.result = "hello"
   ↓
触发 UniApply
   ↓
执行 fn.apply("hello")
   ↓
得到 "hello world"
   ↓
设置 f2.result = "hello world"
```

---

# 三、Completion 是什么

## 10. Completion 节点

`Completion` 可以理解为：

> 一个等待被触发的回调任务节点。

常见内部实现类型：

| 内部类 | 对应方法 |
|---|---|
| `UniApply` | `thenApply` |
| `UniAccept` | `thenAccept` |
| `UniRun` | `thenRun` |
| `BiApply` | `thenCombine` |
| `BiAccept` | `thenAcceptBoth` |
| `BiRun` | `runAfterBoth` |
| `OrApply` | `applyToEither` |
| `OrAccept` | `acceptEither` |
| `OrRun` | `runAfterEither` |

命名规律：

```text
Uni：依赖一个 CompletableFuture
Bi：依赖两个 CompletableFuture
Or：两个任务谁先完成用谁
```

---

## 11. Completion 本质

每个 Completion 节点里大概会保存：

```text
依赖的 CompletableFuture
要完成的 CompletableFuture
用户传入的函数
执行器 Executor
```

例如 `thenApply`：

```java
f1.thenApply(x -> x + " world");
```

对应内部节点保存：

```text
source = f1
dependent = f2
function = x -> x + " world"
executor = null 或指定线程池
```

---

# 四、任务完成后的触发机制

## 12. postComplete

当一个 `CompletableFuture` 完成后，会调用类似：

```java
postComplete();
```

它的作用是：

```text
从 stack 中取出 Completion 节点
依次触发这些 Completion
让依赖当前 Future 的任务继续执行
```

流程：

```text
当前 Future 完成
   ↓
弹出 stack 中的 Completion
   ↓
执行 Completion.tryFire()
   ↓
Completion 计算出结果
   ↓
完成下一个 CompletableFuture
   ↓
继续触发下一个 CompletableFuture 的回调
```

---

## 13. 为什么是链式触发

例如：

```java
CompletableFuture
        .supplyAsync(() -> "A")
        .thenApply(a -> a + "B")
        .thenApply(b -> b + "C")
        .thenAccept(System.out::println);
```

执行链：

```text
f1 完成，结果 A
   ↓
触发 f2，结果 AB
   ↓
触发 f3，结果 ABC
   ↓
触发 f4，打印 ABC
```

每个阶段完成后，都会触发下一个阶段。

---

# 五、同步方法和 Async 方法区别

## 14. thenApply 和 thenApplyAsync 的区别

```java
thenApply()
thenApplyAsync()
```

区别在于：

| 方法 | 执行线程 |
|---|---|
| `thenApply` | 可能由完成上一个任务的线程直接执行 |
| `thenApplyAsync` | 提交到线程池异步执行 |

---

## 15. thenApply 的执行线程

```java
CompletableFuture.supplyAsync(() -> {
    System.out.println("task1: " + Thread.currentThread().getName());
    return "hello";
}).thenApply(result -> {
    System.out.println("task2: " + Thread.currentThread().getName());
    return result + " world";
});
```

`thenApply` 可能由执行 `task1` 的线程继续执行。

也就是说：

```text
上一个任务完成后，当前线程顺手执行下一个回调
```

---

## 16. thenApplyAsync 的执行线程

```java
CompletableFuture.supplyAsync(() -> {
    return "hello";
}).thenApplyAsync(result -> {
    return result + " world";
});
```

`thenApplyAsync` 会把回调任务提交到线程池。

如果没有指定线程池，默认使用：

```text
ForkJoinPool.commonPool()
```

推荐指定自定义线程池：

```java
thenApplyAsync(result -> result + " world", executor)
```

---

## 17. Async 方法底层区别

普通方法：

```text
当前线程直接执行 Completion
```

Async 方法：

```text
把 Completion 包装成异步任务
提交到 Executor
由线程池线程执行
```

所以：

```text
thenApply：可能同步执行
thenApplyAsync：一定异步调度
```

---

# 六、线程池原理

## 18. 默认线程池

如果没有指定线程池：

```java
CompletableFuture.supplyAsync(() -> "hello");
```

默认使用：

```text
ForkJoinPool.commonPool()
```

它是 JVM 级别的公共线程池。

---

## 19. 为什么不建议大量使用默认线程池

因为 `ForkJoinPool.commonPool()` 是公共资源。

如果多个业务都用它：

```text
业务 A 慢任务占满线程
业务 B 异步任务也受影响
```

风险：

```text
线程资源不可控
任务互相影响
阻塞任务拖垮 commonPool
不好监控
不好排查
```

生产建议：

```java
CompletableFuture.supplyAsync(() -> {
    return queryRemote();
}, bizExecutor);
```

---

# 七、多个任务合并的实现原理

## 20. thenCombine 原理

代码：

```java
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> "A");
CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> "B");

CompletableFuture<String> f3 = f1.thenCombine(f2, (a, b) -> a + b);
```

原理：

```text
1. 创建 f3
2. 创建 BiApply Completion
3. 这个 Completion 依赖 f1 和 f2
4. f1 或 f2 任意一个完成时，都会尝试触发 BiApply
5. BiApply 检查另一个任务是否也完成
6. 如果两个都完成，执行合并函数
7. 设置 f3.result
```

流程：

```text
f1 完成
   ↓
尝试触发 BiApply
   ↓
发现 f2 未完成
   ↓
暂不执行

f2 完成
   ↓
再次尝试触发 BiApply
   ↓
发现 f1、f2 都完成
   ↓
执行 (a, b) -> a + b
   ↓
完成 f3
```

---

## 21. applyToEither 原理

代码：

```java
f1.applyToEither(f2, result -> result + " done");
```

原理：

```text
1. f1 和 f2 都注册同一个 OrApply 逻辑
2. 谁先完成，谁触发回调
3. 回调执行后，完成新的 CompletableFuture
4. 另一个任务后完成时，发现目标 Future 已完成，不再覆盖结果
```

关键点：

```text
CAS 保证只有第一个完成的结果生效
```

---

## 22. allOf 原理

```java
CompletableFuture<Void> all = CompletableFuture.allOf(f1, f2, f3);
```

原理：

```text
1. 创建一个新的 CompletableFuture<Void>
2. 给多个 Future 注册完成监听
3. 每完成一个任务，就检查是否全部完成
4. 全部完成后，设置 all 的 result
5. 如果有任务异常，all 最终异常完成
```

注意：

```text
allOf 本身不保存每个任务的结果
结果需要从原来的 Future 中取
```

所以：

```java
all.join();

String r1 = f1.join();
String r2 = f2.join();
String r3 = f3.join();
```

---

## 23. anyOf 原理

```java
CompletableFuture<Object> any = CompletableFuture.anyOf(f1, f2, f3);
```

原理：

```text
1. 创建新的 CompletableFuture<Object>
2. 给每个 Future 注册完成监听
3. 任意一个 Future 完成后，CAS 设置 any.result
4. 其他 Future 后续完成时无法覆盖结果
```

关键点：

```text
谁先完成，谁 CAS 成功
```

---

# 八、异常处理原理

## 24. 异常如何保存

异步任务抛异常：

```java
CompletableFuture.supplyAsync(() -> {
    throw new RuntimeException("error");
});
```

内部不会直接把异常抛给调用线程。

而是把异常包装后保存到：

```text
result
```

类似：

```text
result = AltResult(Throwable)
```

所以异常也是一种完成状态。

---

## 25. exceptionally 原理

```java
future.exceptionally(e -> "default");
```

原理：

```text
1. 注册一个异常处理 Completion
2. 当前 Future 正常完成时，不执行兜底逻辑
3. 当前 Future 异常完成时，执行 exceptionally 函数
4. 返回兜底值
5. 完成新的 CompletableFuture
```

---

## 26. handle 原理

```java
future.handle((result, ex) -> {
    if (ex != null) {
        return "default";
    }
    return result;
});
```

原理：

```text
无论上一个 Future 是正常完成还是异常完成
都会触发 handle 对应的 Completion
然后由 handle 返回一个新结果
```

所以它可以：

```text
处理成功结果
处理异常结果
转换最终结果
```

---

## 27. whenComplete 原理

```java
future.whenComplete((result, ex) -> {
    log.info("done");
});
```

原理：

```text
成功或失败都会执行
但通常不改变原始结果
```

如果上一个任务成功：

```text
下一个 Future 仍然成功
```

如果上一个任务失败：

```text
下一个 Future 默认仍然失败
```

---

# 九、join 和 get 的原理

## 28. join / get 都会阻塞等待

```java
future.join();
future.get();
```

如果任务还没完成，当前线程会等待。

内部大致逻辑：

```text
1. 先检查 result 是否已经有值
2. 如果已经完成，直接返回结果或抛异常
3. 如果未完成，当前线程进入等待
4. Future 完成后唤醒等待线程
5. 返回结果
```

---

## 29. join 和 get 区别

| 对比项 | get | join |
|---|---|---|
| 是否阻塞 | 是 | 是 |
| 异常类型 | checked exception | unchecked exception |
| 异常包装 | `ExecutionException` | `CompletionException` |
| 是否必须 try-catch | 是 | 否 |

---

# 十、CompletableFuture 为什么能链式调用

## 30. 每个阶段都会返回新的 CompletableFuture

例如：

```java
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> "A");

CompletableFuture<String> f2 = f1.thenApply(a -> a + "B");

CompletableFuture<Void> f3 = f2.thenAccept(System.out::println);
```

每调用一次 `thenXxx`，都会返回一个新的 `CompletableFuture`。

关系：

```text
f1 保存第一个任务结果
f2 保存 thenApply 的结果
f3 保存 thenAccept 的结果
```

不是同一个 Future 被反复修改，而是：

```text
每一步都有自己的 CompletableFuture
前一个完成后，推动后一个完成
```

---

# 十一、完整链路示例

## 31. 示例代码

```java
CompletableFuture<Void> future = CompletableFuture
        .supplyAsync(() -> {
            return "A";
        })
        .thenApply(a -> {
            return a + "B";
        })
        .thenApplyAsync(ab -> {
            return ab + "C";
        })
        .thenAccept(result -> {
            System.out.println(result);
        });
```

---

## 32. 内部执行过程

```text
1. 创建 f1
2. supplyAsync 任务提交到线程池
3. 创建 f2，作为 f1 的回调
4. 创建 f3，作为 f2 的回调
5. 创建 f4，作为 f3 的回调

6. 线程池执行 supplyAsync，得到 A
7. CAS 设置 f1.result = A
8. 触发 f1.stack 中的 thenApply
9. thenApply 执行，得到 AB
10. CAS 设置 f2.result = AB
11. 触发 f2.stack 中的 thenApplyAsync
12. thenApplyAsync 提交到线程池执行
13. 线程池执行得到 ABC
14. CAS 设置 f3.result = ABC
15. 触发 f3.stack 中的 thenAccept
16. 打印 ABC
17. 设置 f4.result = null 对应的完成状态
```

---

# 十二、核心原理总结

## 33. CompletableFuture 核心机制

```text
1. result 保存当前任务结果
2. stack 保存依赖当前任务的回调链
3. Completion 表示一个待触发的回调节点
4. 当前任务完成后，通过 CAS 设置 result
5. 设置成功后调用 postComplete 触发后续回调
6. 每个 thenXxx 都会创建新的 CompletableFuture
7. 普通 thenXxx 可能由当前线程执行
8. thenXxxAsync 会提交到线程池执行
9. 异常也会包装成结果保存
10. allOf / anyOf 本质也是注册多个 Completion 并通过 CAS 控制完成
```

---

# 十三、总结

`CompletableFuture` 的实现原理可以从两个核心字段理解：一个是 `result`，一个是 `stack`。

`result` 是一个 `volatile Object`，用来保存当前任务的执行结果。如果 `result` 为空，说明任务还没有完成；如果不为空，说明任务已经完成。正常结果、空结果、异常结果都会被包装后放到 `result` 中。任务完成时会通过 CAS 设置 `result`，保证任务只能完成一次。

`stack` 是一个 Completion 链表，用来保存依赖当前任务的后续回调。比如调用 `thenApply`、`thenAccept`、`thenCombine` 时，都会创建一个对应的 Completion 节点，挂到前一个 `CompletableFuture` 的 stack 上。当上一个任务完成后，会调用类似 `postComplete` 的逻辑，从 stack 中取出 Completion 并触发执行。

每个 `thenXxx` 方法通常都会返回一个新的 `CompletableFuture`。前一个 Future 完成后，会触发回调函数，回调函数的执行结果会设置到新的 Future 中，这样就形成了链式任务编排。

对于 `thenApply` 这类不带 Async 的方法，回调可能由完成上一个任务的线程直接执行；而 `thenApplyAsync` 这类带 Async 的方法，会把回调任务提交到线程池执行。如果没有指定线程池，默认使用 `ForkJoinPool.commonPool()`。

异常处理方面，`CompletableFuture` 不会把异步线程中的异常直接抛给调用线程，而是把异常包装成异常结果保存到 `result` 中，后续通过 `exceptionally`、`handle`、`whenComplete` 等方法处理。

所以总结来说，`CompletableFuture` 的本质就是：用 `result` 保存任务状态，用 `Completion` 链表保存后续依赖任务，任务完成后通过 CAS 设置结果，并触发依赖任务继续执行。

---

## 34. 一句话总结

> `CompletableFuture` 底层通过 `volatile result` 保存完成结果，通过 `Completion stack` 保存依赖回调，通过 CAS 保证任务只完成一次；当前任务完成后触发 Completion 链表，从而实现异步任务的串行、并行、合并和异常传播。
