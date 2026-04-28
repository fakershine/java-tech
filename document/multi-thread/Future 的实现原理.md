# Future 的实现原理

## 1. Future 是什么

`Future` 是 Java 并发包中的一个接口，位于：

```java
java.util.concurrent.Future
```

它表示：

> 一个异步任务的执行结果。

简单理解：

```text
提交一个任务
   ↓
立即返回 Future
   ↓
任务在线程池中执行
   ↓
通过 Future 获取结果
```

示例：

```java
ExecutorService executorService = Executors.newFixedThreadPool(10);

Future<String> future = executorService.submit(() -> {
    return "hello";
});

String result = future.get();
```

---

## 2. Future 的核心作用

`Future` 主要用于：

```text
获取异步任务结果
判断任务是否完成
取消任务
判断任务是否取消
阻塞等待任务执行完成
```

---

## 3. Future 核心方法

```java
public interface Future<V> {

    boolean cancel(boolean mayInterruptIfRunning);

    boolean isCancelled();

    boolean isDone();

    V get() throws InterruptedException, ExecutionException;

    V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException;
}
```

| 方法 | 说明 |
|---|---|
| `cancel()` | 取消任务 |
| `isCancelled()` | 判断任务是否被取消 |
| `isDone()` | 判断任务是否完成 |
| `get()` | 阻塞等待任务结果 |
| `get(timeout)` | 限时等待任务结果 |

---

## 4. Future 本身只是接口

需要注意：

> `Future` 只是接口，本身没有具体实现逻辑。

真正常见的实现类是：

```java
FutureTask
```

线程池 `submit()` 方法返回的 `Future`，底层通常就是 `FutureTask`。

---

## 5. FutureTask 是什么

`FutureTask` 是 `Future` 的核心实现类。

它实现了：

```java
RunnableFuture<V>
```

而 `RunnableFuture` 又继承了：

```java
Runnable
Future
```

结构：

```text
Future
  ↑
RunnableFuture
  ↑
FutureTask
```

源码关系大致是：

```java
public class FutureTask<V> implements RunnableFuture<V> {
}
```

```java
public interface RunnableFuture<V> extends Runnable, Future<V> {
    void run();
}
```

所以 `FutureTask` 既是：

```text
一个可以被线程执行的任务
也是一个可以获取执行结果的 Future
```

---

## 6. FutureTask 的核心思想

`FutureTask` 的核心原理可以理解为：

> 用一个状态变量记录任务状态，用一个结果变量保存任务结果，用等待队列保存调用 `get()` 阻塞的线程，任务完成后唤醒等待线程。

核心结构：

```text
FutureTask
├── state       任务状态
├── callable    真正要执行的任务
├── outcome     执行结果或异常
├── runner      执行任务的线程
└── waiters     等待结果的线程队列
```

---

## 7. FutureTask 核心字段

简化理解：

```java
private volatile int state;

private Callable<V> callable;

private Object outcome;

private volatile Thread runner;

private volatile WaitNode waiters;
```

| 字段 | 作用 |
|---|---|
| `state` | 任务状态 |
| `callable` | 真正执行的任务 |
| `outcome` | 保存正常结果或异常 |
| `runner` | 当前执行任务的线程 |
| `waiters` | 等待结果的线程链表 |

---

# 一、FutureTask 状态设计

## 8. FutureTask 的状态

`FutureTask` 内部用 `state` 表示任务状态。

常见状态：

```java
private static final int NEW          = 0;
private static final int COMPLETING   = 1;
private static final int NORMAL       = 2;
private static final int EXCEPTIONAL  = 3;
private static final int CANCELLED    = 4;
private static final int INTERRUPTING = 5;
private static final int INTERRUPTED  = 6;
```

---

## 9. 状态含义

| 状态 | 说明 |
|---|---|
| `NEW` | 初始状态，任务还没完成 |
| `COMPLETING` | 正在设置结果的中间状态 |
| `NORMAL` | 正常执行完成 |
| `EXCEPTIONAL` | 执行过程中抛异常 |
| `CANCELLED` | 任务被取消 |
| `INTERRUPTING` | 正在中断执行线程 |
| `INTERRUPTED` | 已经中断执行线程 |

---

## 10. 状态流转

### 正常完成

```text
NEW
 ↓
COMPLETING
 ↓
NORMAL
```

---

### 异常完成

```text
NEW
 ↓
COMPLETING
 ↓
EXCEPTIONAL
```

---

### 取消任务，不中断线程

```text
NEW
 ↓
CANCELLED
```

---

### 取消任务，并尝试中断线程

```text
NEW
 ↓
INTERRUPTING
 ↓
INTERRUPTED
```

---

## 11. 为什么需要状态

因为异步任务可能处于不同阶段：

```text
还没执行
正在执行
执行成功
执行失败
被取消
被中断
```

`FutureTask` 通过 `state` 控制：

```text
是否可以执行任务
是否可以设置结果
get() 是否需要阻塞
cancel() 是否能成功
是否需要抛异常
是否需要唤醒等待线程
```

---

# 二、FutureTask 执行原理

## 12. submit() 后发生了什么

示例：

```java
Future<String> future = executorService.submit(() -> {
    return "hello";
});
```

线程池内部大致流程：

```text
1. 把 Callable 包装成 FutureTask
2. 把 FutureTask 提交到工作队列
3. 工作线程从队列中取出 FutureTask
4. 调用 FutureTask.run()
5. run() 执行 Callable.call()
6. 执行完成后保存结果
7. 唤醒调用 get() 等待的线程
```

---

## 13. submit() 简化源码理解

线程池的 `submit()` 大致类似：

```java
public <T> Future<T> submit(Callable<T> task) {
    RunnableFuture<T> futureTask = newTaskFor(task);
    execute(futureTask);
    return futureTask;
}
```

其中：

```java
newTaskFor(task)
```

通常会创建：

```java
new FutureTask<>(task)
```

---

## 14. FutureTask.run() 原理

`FutureTask` 的 `run()` 是任务执行入口。

简化逻辑：

```java
public void run() {
    if (state != NEW) {
        return;
    }

    try {
        V result = callable.call();
        set(result);
    } catch (Throwable e) {
        setException(e);
    }
}
```

真实逻辑中还会用 CAS 设置 `runner`，防止任务被多个线程重复执行。

---

## 15. run() 的核心流程

```text
1. 判断任务状态是否为 NEW
2. 使用 CAS 设置 runner 为当前线程
3. 执行 callable.call()
4. 如果正常返回，调用 set(result)
5. 如果抛出异常，调用 setException(exception)
6. 设置最终状态
7. 唤醒等待 get() 的线程
```

流程图：

```text
工作线程执行 FutureTask.run()
   ↓
CAS 设置 runner
   ↓
执行 Callable.call()
   ↓
成功：保存结果
失败：保存异常
   ↓
修改 state
   ↓
唤醒等待线程
```

---

# 三、结果保存原理

## 16. outcome 保存结果

`FutureTask` 用 `outcome` 保存任务结果。

如果任务正常完成：

```text
outcome = 返回值
state = NORMAL
```

如果任务异常：

```text
outcome = 异常对象
state = EXCEPTIONAL
```

例如：

```java
Future<String> future = executor.submit(() -> "hello");
```

执行完成后：

```text
outcome = "hello"
state = NORMAL
```

---

## 17. set(result) 原理

任务正常完成时，会调用：

```java
set(result)
```

简化逻辑：

```java
protected void set(V v) {
    if (CAS state NEW -> COMPLETING) {
        outcome = v;
        state = NORMAL;
        finishCompletion();
    }
}
```

流程：

```text
1. CAS 把 state 从 NEW 改成 COMPLETING
2. 把结果保存到 outcome
3. 把 state 改成 NORMAL
4. 唤醒等待线程
```

---

## 18. setException(exception) 原理

任务抛异常时，会调用：

```java
setException(Throwable t)
```

简化逻辑：

```java
protected void setException(Throwable t) {
    if (CAS state NEW -> COMPLETING) {
        outcome = t;
        state = EXCEPTIONAL;
        finishCompletion();
    }
}
```

流程：

```text
1. CAS 把 state 从 NEW 改成 COMPLETING
2. 把异常保存到 outcome
3. 把 state 改成 EXCEPTIONAL
4. 唤醒等待线程
```

---

# 四、get() 阻塞等待原理

## 19. get() 做了什么

代码：

```java
String result = future.get();
```

如果任务已经完成：

```text
直接返回结果
```

如果任务还没完成：

```text
当前线程阻塞等待
```

---

## 20. get() 简化逻辑

```java
public V get() throws InterruptedException, ExecutionException {
    int s = state;

    if (s <= COMPLETING) {
        s = awaitDone(false, 0L);
    }

    return report(s);
}
```

可以理解为：

```text
1. 先看任务是否完成
2. 没完成就进入等待
3. 完成后根据状态返回结果或抛异常
```

---

## 21. awaitDone() 原理

如果任务没完成，调用 `get()` 的线程会进入等待队列。

简化流程：

```text
1. 创建 WaitNode，保存当前线程
2. 把 WaitNode 加入 waiters 链表
3. 使用 LockSupport.park() 挂起当前线程
4. 等任务完成后被唤醒
5. 被唤醒后检查状态
6. 返回最终状态
```

---

## 22. waiters 是什么

`waiters` 是等待线程链表。

例如多个线程都调用：

```java
future.get();
```

如果任务还没完成，这些线程都会被包装成 `WaitNode`，挂到 `waiters` 上。

结构类似：

```text
waiters
  ↓
WaitNode(thread-1)
  ↓
WaitNode(thread-2)
  ↓
WaitNode(thread-3)
```

---

## 23. LockSupport.park()

`FutureTask` 阻塞线程主要依赖：

```java
LockSupport.park()
```

作用：

```text
挂起当前线程
让线程进入等待状态
不再占用 CPU 执行
```

任务完成后，会调用：

```java
LockSupport.unpark(thread)
```

唤醒等待线程。

---

## 24. finishCompletion() 原理

任务完成后，会调用：

```java
finishCompletion()
```

它的作用是：

```text
唤醒所有等待 get() 的线程
```

简化逻辑：

```java
private void finishCompletion() {
    for (WaitNode q; (q = waiters) != null;) {
        if (CAS waiters -> null) {
            while (q != null) {
                Thread t = q.thread;
                if (t != null) {
                    LockSupport.unpark(t);
                }
                q = q.next;
            }
            break;
        }
    }
}
```

流程：

```text
1. 取出 waiters 等待队列
2. 遍历所有 WaitNode
3. 对每个等待线程调用 LockSupport.unpark()
4. 所有 get() 阻塞线程被唤醒
```

---

## 25. get() 返回结果原理

等待结束后，`get()` 会调用：

```java
report(state)
```

根据状态决定返回什么。

简化逻辑：

```java
private V report(int s) throws ExecutionException {
    Object x = outcome;

    if (s == NORMAL) {
        return (V) x;
    }

    if (s >= CANCELLED) {
        throw new CancellationException();
    }

    throw new ExecutionException((Throwable) x);
}
```

---

## 26. get() 的三种结果

| 状态 | get() 行为 |
|---|---|
| `NORMAL` | 返回正常结果 |
| `EXCEPTIONAL` | 抛出 `ExecutionException` |
| `CANCELLED / INTERRUPTED` | 抛出 `CancellationException` |

---

# 五、cancel() 取消原理

## 27. cancel() 的作用

```java
future.cancel(true);
```

用于取消任务。

参数：

```java
mayInterruptIfRunning
```

表示：

```text
如果任务正在执行，是否尝试中断执行线程
```

---

## 28. cancel(false)

```java
future.cancel(false);
```

表示：

```text
取消任务
但不尝试中断正在执行的线程
```

状态变化：

```text
NEW -> CANCELLED
```

如果任务还没开始执行，后续不会再执行。

如果任务已经在执行，不一定能阻止执行。

---

## 29. cancel(true)

```java
future.cancel(true);
```

表示：

```text
取消任务
如果任务正在执行，尝试中断执行线程
```

状态变化：

```text
NEW -> INTERRUPTING -> INTERRUPTED
```

内部会调用：

```java
runner.interrupt();
```

---

## 30. cancel() 简化逻辑

```java
public boolean cancel(boolean mayInterruptIfRunning) {
    if (state != NEW) {
        return false;
    }

    if (mayInterruptIfRunning) {
        if (CAS state NEW -> INTERRUPTING) {
            Thread t = runner;
            if (t != null) {
                t.interrupt();
            }
            state = INTERRUPTED;
        }
    } else {
        CAS state NEW -> CANCELLED;
    }

    finishCompletion();
    return true;
}
```

---

## 31. cancel() 一定能停止任务吗？

不一定。

如果任务已经在线程中运行：

```java
future.cancel(true);
```

只是调用执行线程的：

```java
interrupt()
```

它只是发出中断信号。

任务是否停止，取决于任务代码是否响应中断。

---

## 32. 正确响应中断示例

```java
Future<?> future = executor.submit(() -> {
    while (!Thread.currentThread().isInterrupted()) {
        // 执行业务
    }
});
```

或者：

```java
Future<?> future = executor.submit(() -> {
    try {
        Thread.sleep(10000);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
    }
});
```

---

## 33. 不响应中断的任务

```java
Future<?> future = executor.submit(() -> {
    while (true) {
        // 死循环，不检查中断
    }
});
```

即使调用：

```java
future.cancel(true);
```

任务也可能继续执行。

---

# 六、isDone 和 isCancelled 原理

## 34. isDone()

```java
future.isDone();
```

判断任务是否完成。

简化逻辑：

```java
public boolean isDone() {
    return state != NEW;
}
```

只要不是 `NEW` 状态，就表示任务已经结束或正在结束。

包括：

```text
正常完成
异常完成
取消
中断取消
```

---

## 35. isCancelled()

```java
future.isCancelled();
```

判断任务是否被取消。

简化逻辑：

```java
public boolean isCancelled() {
    return state >= CANCELLED;
}
```

只要状态是：

```text
CANCELLED
INTERRUPTING
INTERRUPTED
```

就认为任务被取消。

---

# 七、FutureTask 为什么线程安全

## 36. volatile 保证可见性

核心状态字段：

```java
volatile int state;
```

保证一个线程修改状态后，其他线程可以立即看到。

例如：

```text
工作线程设置 state = NORMAL
调用 get() 的线程能看到任务已完成
```

---

## 37. CAS 保证原子性

`FutureTask` 使用 CAS 修改状态。

例如：

```text
NEW -> COMPLETING
NEW -> CANCELLED
NEW -> INTERRUPTING
```

CAS 保证：

```text
同一时刻只有一个线程能成功完成或取消任务
```

避免：

```text
任务被重复完成
结果被覆盖
取消和完成冲突
```

---

## 38. LockSupport 实现阻塞唤醒

调用 `get()` 的线程：

```text
任务未完成 -> park 阻塞
```

任务完成后：

```text
finishCompletion -> unpark 唤醒
```

---

# 八、Future 和线程池的关系

## 39. execute 和 submit 的区别

### execute

```java
executorService.execute(() -> {
    System.out.println("task");
});
```

特点：

```text
没有返回值
无法通过 Future 获取结果
异常直接由线程处理
```

---

### submit

```java
Future<String> future = executorService.submit(() -> {
    return "hello";
});
```

特点：

```text
返回 Future
可以获取结果
可以捕获任务异常
可以取消任务
```

---

## 40. submit 为什么能返回 Future

因为线程池会把任务包装成：

```java
FutureTask
```

然后提交执行。

流程：

```text
submit(Callable)
   ↓
new FutureTask(callable)
   ↓
execute(FutureTask)
   ↓
返回 FutureTask
```

所以：

```text
FutureTask 既能被线程池执行
也能作为 Future 返回给调用方
```

---

## 41. Callable 和 Runnable 的区别

| 对比项 | Runnable | Callable |
|---|---|---|
| 方法 | `run()` | `call()` |
| 返回值 | 无 | 有 |
| 异常 | 不能直接抛 checked exception | 可以抛 checked exception |
| 配合 Future | 间接支持 | 直接支持 |

示例：

```java
Callable<String> callable = () -> "hello";
```

---

# 九、Future 的局限性

## 42. get() 会阻塞

```java
String result = future.get();
```

如果任务没完成，当前线程会一直阻塞。

问题：

```text
容易造成线程等待
不适合复杂异步编排
```

---

## 43. 不支持回调

Future 不能方便地写：

```text
任务完成后自动执行某个逻辑
```

只能：

```java
future.get();
```

阻塞等结果。

---

## 44. 不方便任务编排

Future 不方便实现：

```text
任务 A 完成后执行任务 B
任务 A 和任务 B 并行执行后合并结果
多个任务全部完成后执行下一步
任意一个任务完成后返回
统一异常处理
```

这些是 `CompletableFuture` 更擅长的事情。

---

# 十、Future 和 CompletableFuture 区别

| 对比项 | Future | CompletableFuture |
|---|---|---|
| 异步结果 | 支持 | 支持 |
| 阻塞获取 | `get()` | `get()` / `join()` |
| 回调 | 不支持 | 支持 |
| 链式编排 | 不支持 | 支持 |
| 多任务合并 | 不方便 | 支持 |
| 异常处理 | 不方便 | 支持 |
| 主动完成 | 不支持 | 支持 `complete()` |
| 实现类 | FutureTask | CompletableFuture |

一句话：

```text
Future 只能表示一个异步结果；
CompletableFuture 可以对异步结果进行编排和组合。
```

---

# 十一、完整执行流程总结

## 45. FutureTask 正常执行流程

```text
1. 调用 executorService.submit(callable)
2. 线程池把 callable 包装成 FutureTask
3. FutureTask 作为 Runnable 被放入线程池队列
4. 工作线程取出 FutureTask
5. 调用 FutureTask.run()
6. run() 执行 callable.call()
7. 执行成功，把结果保存到 outcome
8. state 改为 NORMAL
9. 唤醒所有调用 get() 阻塞的线程
10. get() 返回 outcome
```

---

## 46. FutureTask 异常执行流程

```text
1. 工作线程执行 callable.call()
2. callable 抛出异常
3. FutureTask 捕获异常
4. 把异常保存到 outcome
5. state 改为 EXCEPTIONAL
6. 唤醒等待线程
7. get() 抛出 ExecutionException
```

---

## 47. FutureTask 取消流程

```text
1. 调用 future.cancel(true)
2. CAS 修改 state
3. 如果任务正在执行，尝试中断 runner
4. state 改为 INTERRUPTED
5. 唤醒等待线程
6. get() 抛出 CancellationException
```

---

# 十二、面试常问问题

## 48. Future 的实现原理是什么？

```text
Future 本身是接口，常见实现是 FutureTask。

FutureTask 内部通过 state 表示任务状态，通过 callable 保存实际任务，通过 outcome 保存任务结果或异常，通过 waiters 保存调用 get() 阻塞等待的线程。

任务提交到线程池后，线程池执行 FutureTask.run()，run() 中调用 callable.call()。如果执行成功，就把结果保存到 outcome，并把 state 改为 NORMAL；如果执行异常，就把异常保存到 outcome，并把 state 改为 EXCEPTIONAL。任务完成后会调用 finishCompletion() 唤醒所有等待 get() 的线程。

get() 方法如果发现任务还没完成，会把当前线程包装成 WaitNode 放入 waiters 队列，并通过 LockSupport.park() 阻塞。等任务完成后，finishCompletion() 会通过 LockSupport.unpark() 唤醒等待线程，然后 get() 根据任务状态返回结果或抛异常。
```

---

## 49. FutureTask 为什么既能执行又能获取结果？

因为 `FutureTask` 实现了：

```java
RunnableFuture
```

而 `RunnableFuture` 同时继承：

```java
Runnable
Future
```

所以它既可以作为任务被线程执行，也可以作为 `Future` 返回给调用方获取结果。

---

## 50. FutureTask 如何保证任务只执行一次？

主要依靠：

```text
state 状态控制
CAS 设置 runner
CAS 修改任务状态
```

当任务已经不是 `NEW` 状态时，后续执行或取消操作都会失败。

---

## 51. Future.get() 为什么会阻塞？

因为任务未完成时，`get()` 会把当前线程加入等待队列，并调用：

```java
LockSupport.park()
```

挂起当前线程。

任务完成后，通过：

```java
LockSupport.unpark(thread)
```

唤醒等待线程。

---

## 52. Future.cancel(true) 一定能停止任务吗？

不一定。

`cancel(true)` 只是尝试中断正在执行任务的线程，也就是调用：

```java
thread.interrupt()
```

但 Java 的中断是协作式机制。

如果任务代码不检查中断状态，也不响应 `InterruptedException`，任务可能不会停止。

---

## 53. submit 和 execute 有什么区别？

```text
execute 只提交任务，没有返回值。
submit 会把任务包装成 FutureTask，并返回 Future，可以通过 Future 获取结果、捕获异常或取消任务。
```

---

## 54. Future 有什么缺点？

```text
get() 会阻塞
不支持回调
不支持链式任务编排
不方便组合多个异步任务
异常处理不够灵活
```

所以 Java 8 引入了：

```java
CompletableFuture
```

---

# 十三、总结

`Future` 是 Java 并发包中表示异步任务结果的接口，它本身只是接口，常见实现类是 `FutureTask`。线程池的 `submit()` 方法底层通常会把 `Callable` 或 `Runnable` 包装成 `FutureTask`，然后提交到线程池执行，并把这个 `FutureTask` 作为 `Future` 返回给调用方。

`FutureTask` 同时实现了 `Runnable` 和 `Future`，所以它既可以被线程执行，也可以被调用方用来获取结果。它内部主要有几个核心字段：`state` 表示任务状态，`callable` 表示真正要执行的任务，`outcome` 保存执行结果或异常，`runner` 表示执行任务的线程，`waiters` 保存调用 `get()` 阻塞等待结果的线程。

当线程池中的工作线程执行 `FutureTask.run()` 时，会调用 `callable.call()`。如果执行成功，就把结果保存到 `outcome`，并把状态改为 `NORMAL`；如果执行失败，就把异常保存到 `outcome`，并把状态改为 `EXCEPTIONAL`。任务完成后会唤醒所有因为调用 `get()` 而阻塞的线程。

`get()` 方法如果发现任务未完成，会把当前线程加入等待队列，并通过 `LockSupport.park()` 挂起线程；任务完成后通过 `LockSupport.unpark()` 唤醒等待线程，然后根据状态返回结果、抛出 `ExecutionException` 或 `CancellationException`。

`cancel(true)` 会尝试取消任务，如果任务正在运行，会调用执行线程的 `interrupt()`，但它不一定能真正停止任务，因为 Java 中断是协作式的，需要任务代码主动响应中断。

---

## 55. 一句话总结

> `Future` 是异步任务结果接口，核心实现是 `FutureTask`；`FutureTask` 通过 `state` 管理任务状态，通过 `outcome` 保存结果或异常，通过 `waiters + LockSupport` 实现 `get()` 阻塞等待和任务完成后的线程唤醒。
