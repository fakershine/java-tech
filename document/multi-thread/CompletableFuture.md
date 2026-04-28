# CompletableFuture

## 1. CompletableFuture 是什么

`CompletableFuture` 是 Java 8 引入的异步编程工具类，位于：

```java
java.util.concurrent.CompletableFuture
```

它主要用于：

```text
异步执行任务
任务编排
任务串行执行
任务并行执行
任务结果合并
异常处理
超时控制
替代 Future 的阻塞等待
```

一句话理解：

> `CompletableFuture` 是 Java 提供的异步任务编排工具，可以让多个异步任务像流水线一样组合执行。

---

## 2. 为什么需要 CompletableFuture

传统 `Future` 可以获取异步任务结果：

```java
Future<String> future = executorService.submit(() -> {
    return "hello";
});

String result = future.get();
```

但是 `Future` 有几个问题：

```text
get() 会阻塞
不方便做任务编排
不方便处理多个任务结果
不方便做异常链式处理
不方便任务完成后自动回调
```

`CompletableFuture` 解决了这些问题。

---

## 3. Future 和 CompletableFuture 对比

| 对比项 | Future | CompletableFuture |
|---|---|---|
| 异步执行 | 支持 | 支持 |
| 获取结果 | `get()` 阻塞 | `get()` / `join()`，也支持回调 |
| 任务编排 | 弱 | 强 |
| 串行执行 | 不方便 | 支持 |
| 并行执行 | 不方便 | 支持 |
| 异常处理 | 不方便 | 支持链式异常处理 |
| 回调机制 | 不支持 | 支持 |
| 手动完成 | 不支持 | 支持 `complete()` |

---

## 4. 创建 CompletableFuture

常见方式有两种：

```text
runAsync()
supplyAsync()
```

---

## 5. runAsync

`runAsync()` 用于执行没有返回值的异步任务。

```java
CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
    System.out.println("异步任务执行：" + Thread.currentThread().getName());
});
```

特点：

```text
没有返回值
返回 CompletableFuture<Void>
```

---

## 6. supplyAsync

`supplyAsync()` 用于执行有返回值的异步任务。

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    return "hello";
});
```

获取结果：

```java
String result = future.join();

System.out.println(result);
```

输出：

```text
hello
```

---

## 7. 默认线程池

如果不指定线程池：

```java
CompletableFuture.supplyAsync(() -> {
    return "hello";
});
```

默认使用：

```text
ForkJoinPool.commonPool()
```

注意：

```text
默认线程池是公共线程池
不建议在生产环境大量使用默认线程池
```

因为可能导致：

```text
线程资源不可控
不同业务互相影响
阻塞任务拖垮 commonPool
排查问题困难
```

---

## 8. 推荐指定自定义线程池

实际项目中推荐这样写：

```java
ExecutorService executorService = new ThreadPoolExecutor(
        10,
        20,
        60,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1000),
        new ThreadFactory() {
            private final AtomicInteger index = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "biz-pool-" + index.getAndIncrement());
            }
        },
        new ThreadPoolExecutor.CallerRunsPolicy()
);
```

使用：

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    return "hello";
}, executorService);
```

推荐原因：

```text
线程数可控
队列可控
拒绝策略可控
方便监控
方便排查问题
不同业务线程池隔离
```

---

# 一、获取结果

## 9. get()

```java
String result = future.get();
```

特点：

```text
阻塞等待结果
需要处理 checked exception
```

示例：

```java
try {
    String result = future.get();
} catch (InterruptedException | ExecutionException e) {
    e.printStackTrace();
}
```

---

## 10. join()

```java
String result = future.join();
```

特点：

```text
阻塞等待结果
不需要显式捕获 checked exception
异常会包装成 CompletionException
```

实际开发中，链式调用里常用 `join()`。

---

## 11. get 和 join 区别

| 对比项 | get | join |
|---|---|---|
| 是否阻塞 | 是 | 是 |
| 异常类型 | checked exception | unchecked exception |
| 是否必须 try-catch | 是 | 否 |
| 常见场景 | 传统 Future 风格 | CompletableFuture 链式调用 |

示例：

```java
future.get();
```

可能抛出：

```text
InterruptedException
ExecutionException
```

示例：

```java
future.join();
```

可能抛出：

```text
CompletionException
```

---

# 二、任务串行编排

## 12. thenApply

`thenApply()` 用于接收上一个任务的结果，并返回新结果。

```java
CompletableFuture<String> future = CompletableFuture
        .supplyAsync(() -> "hello")
        .thenApply(result -> result + " world");

System.out.println(future.join());
```

输出：

```text
hello world
```

特点：

```text
有入参
有返回值
```

---

## 13. thenAccept

`thenAccept()` 用于接收上一个任务结果，但不返回新结果。

```java
CompletableFuture<Void> future = CompletableFuture
        .supplyAsync(() -> "hello")
        .thenAccept(result -> {
            System.out.println("结果：" + result);
        });
```

特点：

```text
有入参
无返回值
```

---

## 14. thenRun

`thenRun()` 不接收上一个任务结果，也没有返回值。

```java
CompletableFuture<Void> future = CompletableFuture
        .supplyAsync(() -> "hello")
        .thenRun(() -> {
            System.out.println("任务执行完成");
        });
```

特点：

```text
无入参
无返回值
```

---

## 15. thenApply、thenAccept、thenRun 对比

| 方法 | 是否接收上一步结果 | 是否有返回值 | 适合场景 |
|---|---|---|---|
| `thenApply` | 是 | 是 | 转换结果 |
| `thenAccept` | 是 | 否 | 消费结果 |
| `thenRun` | 否 | 否 | 执行后续动作 |

---

## 16. 串行执行示例

```java
CompletableFuture<String> future = CompletableFuture
        .supplyAsync(() -> {
            System.out.println("查询用户");
            return "user";
        })
        .thenApply(user -> {
            System.out.println("查询订单");
            return user + "-order";
        })
        .thenApply(order -> {
            System.out.println("计算金额");
            return order + "-amount";
        });

System.out.println(future.join());
```

执行顺序：

```text
查询用户
查询订单
计算金额
```

---

# 三、Async 后缀区别

## 17. thenApply 和 thenApplyAsync

```java
thenApply()
thenApplyAsync()
```

区别：

| 方法 | 执行线程 |
|---|---|
| `thenApply` | 可能使用上一个任务的线程 |
| `thenApplyAsync` | 使用线程池异步执行 |

示例：

```java
CompletableFuture<String> future = CompletableFuture
        .supplyAsync(() -> {
            System.out.println("task1: " + Thread.currentThread().getName());
            return "hello";
        }, executorService)
        .thenApply(result -> {
            System.out.println("task2: " + Thread.currentThread().getName());
            return result + " world";
        });
```

`thenApply()` 可能在 task1 同一个线程执行。

---

## 18. thenApplyAsync 指定线程池

```java
CompletableFuture<String> future = CompletableFuture
        .supplyAsync(() -> "hello", executorService)
        .thenApplyAsync(result -> result + " world", executorService);
```

推荐：

```text
生产环境使用 Async 方法时，尽量指定自定义线程池
```

---

# 四、任务合并

## 19. thenCombine

`thenCombine()` 用于合并两个独立任务的结果。

```java
CompletableFuture<String> userFuture = CompletableFuture.supplyAsync(() -> {
    return "user";
});

CompletableFuture<String> orderFuture = CompletableFuture.supplyAsync(() -> {
    return "order";
});

CompletableFuture<String> resultFuture = userFuture.thenCombine(
        orderFuture,
        (user, order) -> user + "-" + order
);

System.out.println(resultFuture.join());
```

输出：

```text
user-order
```

适合：

```text
两个任务互不依赖
两个任务都完成后合并结果
```

---

## 20. thenAcceptBoth

接收两个任务的结果，但不返回新结果。

```java
userFuture.thenAcceptBoth(orderFuture, (user, order) -> {
    System.out.println(user);
    System.out.println(order);
});
```

特点：

```text
两个任务都完成
消费两个结果
无返回值
```

---

## 21. runAfterBoth

两个任务都执行完成后，再执行一个动作。

```java
userFuture.runAfterBoth(orderFuture, () -> {
    System.out.println("两个任务都完成了");
});
```

特点：

```text
不关心两个任务结果
只关心两个任务都完成
```

---

## 22. 两任务合并方法对比

| 方法 | 是否接收结果 | 是否返回结果 |
|---|---|---|
| `thenCombine` | 接收两个结果 | 有返回值 |
| `thenAcceptBoth` | 接收两个结果 | 无返回值 |
| `runAfterBoth` | 不接收结果 | 无返回值 |

---

# 五、谁先完成用谁

## 23. applyToEither

两个任务谁先完成，就用谁的结果继续处理。

```java
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
    sleep(1000);
    return "A";
});

CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
    sleep(500);
    return "B";
});

CompletableFuture<String> resultFuture = future1.applyToEither(
        future2,
        result -> result + "-done"
);

System.out.println(resultFuture.join());
```

输出：

```text
B-done
```

---

## 24. acceptEither

谁先完成，就消费谁的结果。

```java
future1.acceptEither(future2, result -> {
    System.out.println("最快结果：" + result);
});
```

---

## 25. runAfterEither

任意一个任务完成后，执行动作。

```java
future1.runAfterEither(future2, () -> {
    System.out.println("有一个任务完成了");
});
```

---

## 26. Either 系列对比

| 方法 | 是否接收结果 | 是否返回结果 |
|---|---|---|
| `applyToEither` | 接收最快任务结果 | 有返回值 |
| `acceptEither` | 接收最快任务结果 | 无返回值 |
| `runAfterEither` | 不接收结果 | 无返回值 |

---

# 六、多个任务编排

## 27. allOf

`allOf()` 等待所有任务完成。

```java
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> "A");
CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> "B");
CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> "C");

CompletableFuture<Void> allFuture = CompletableFuture.allOf(
        future1,
        future2,
        future3
);

allFuture.join();

List<String> result = Arrays.asList(
        future1.join(),
        future2.join(),
        future3.join()
);

System.out.println(result);
```

输出：

```text
[A, B, C]
```

特点：

```text
等待所有任务完成
返回 CompletableFuture<Void>
结果需要自己从每个 Future 取
```

---

## 28. anyOf

`anyOf()` 等待任意一个任务完成。

```java
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
    sleep(1000);
    return "A";
});

CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
    sleep(500);
    return "B";
});

CompletableFuture<Object> anyFuture = CompletableFuture.anyOf(
        future1,
        future2
);

System.out.println(anyFuture.join());
```

输出：

```text
B
```

---

## 29. allOf 和 anyOf 对比

| 方法 | 说明 | 返回值 |
|---|---|---|
| `allOf` | 等待所有任务完成 | `CompletableFuture<Void>` |
| `anyOf` | 任意一个任务完成即可 | `CompletableFuture<Object>` |

---

# 七、异常处理

## 30. exceptionally

`exceptionally()` 用于异常兜底，返回默认结果。

```java
CompletableFuture<String> future = CompletableFuture
        .supplyAsync(() -> {
            int i = 1 / 0;
            return "success";
        })
        .exceptionally(e -> {
            System.out.println("异常：" + e.getMessage());
            return "default";
        });

System.out.println(future.join());
```

输出：

```text
default
```

特点：

```text
只能处理异常
可以返回兜底值
```

---

## 31. handle

`handle()` 无论成功还是失败都会执行，并且可以返回新结果。

```java
CompletableFuture<String> future = CompletableFuture
        .supplyAsync(() -> {
            int i = 1 / 0;
            return "success";
        })
        .handle((result, exception) -> {
            if (exception != null) {
                return "default";
            }
            return result;
        });

System.out.println(future.join());
```

特点：

```text
成功执行
失败也执行
可以返回新结果
```

---

## 32. whenComplete

`whenComplete()` 无论成功还是失败都会执行，但通常不改变结果。

```java
CompletableFuture<String> future = CompletableFuture
        .supplyAsync(() -> "success")
        .whenComplete((result, exception) -> {
            if (exception != null) {
                System.out.println("异常：" + exception.getMessage());
            } else {
                System.out.println("结果：" + result);
            }
        });
```

特点：

```text
成功失败都执行
适合记录日志
通常不修改最终结果
```

---

## 33. 异常处理对比

| 方法 | 成功是否执行 | 异常是否执行 | 是否能返回新结果 | 常见用途 |
|---|---|---|---|---|
| `exceptionally` | 否 | 是 | 是 | 异常兜底 |
| `handle` | 是 | 是 | 是 | 成功/失败统一转换 |
| `whenComplete` | 是 | 是 | 不改变原结果 | 日志记录、清理资源 |

---

# 八、超时控制

## 34. orTimeout

Java 9 引入。

如果超时未完成，则抛出异常。

```java
CompletableFuture<String> future = CompletableFuture
        .supplyAsync(() -> {
            sleep(3000);
            return "success";
        })
        .orTimeout(1, TimeUnit.SECONDS);

future.join();
```

超时后抛出：

```text
TimeoutException
```

---

## 35. completeOnTimeout

Java 9 引入。

如果超时未完成，则返回默认值。

```java
CompletableFuture<String> future = CompletableFuture
        .supplyAsync(() -> {
            sleep(3000);
            return "success";
        })
        .completeOnTimeout("default", 1, TimeUnit.SECONDS);

System.out.println(future.join());
```

输出：

```text
default
```

---

## 36. 超时方法对比

| 方法 | 超时后行为 |
|---|---|
| `orTimeout` | 抛出超时异常 |
| `completeOnTimeout` | 返回默认值 |

---

# 九、手动完成任务

## 37. complete

可以手动设置结果。

```java
CompletableFuture<String> future = new CompletableFuture<>();

future.complete("manual result");

System.out.println(future.join());
```

输出：

```text
manual result
```

---

## 38. completeExceptionally

可以手动设置异常结果。

```java
CompletableFuture<String> future = new CompletableFuture<>();

future.completeExceptionally(new RuntimeException("error"));

future.join();
```

会抛出异常。

---

## 39. 使用场景

```text
异步回调转 CompletableFuture
事件监听结果封装
异步框架适配
手动控制任务完成
```

---

# 十、常见业务场景

## 40. 并行查询多个接口

例如商品详情页需要查询：

```text
商品信息
库存信息
价格信息
优惠信息
评价信息
```

这些查询互不依赖，可以并行执行。

```java
CompletableFuture<Product> productFuture =
        CompletableFuture.supplyAsync(() -> productService.getProduct(id), executor);

CompletableFuture<Stock> stockFuture =
        CompletableFuture.supplyAsync(() -> stockService.getStock(id), executor);

CompletableFuture<Price> priceFuture =
        CompletableFuture.supplyAsync(() -> priceService.getPrice(id), executor);

CompletableFuture<Void> allFuture = CompletableFuture.allOf(
        productFuture,
        stockFuture,
        priceFuture
);

allFuture.join();

ProductDetailVO vo = new ProductDetailVO();
vo.setProduct(productFuture.join());
vo.setStock(stockFuture.join());
vo.setPrice(priceFuture.join());
```

优点：

```text
串行耗时 = A + B + C
并行耗时 ≈ max(A, B, C)
```

---

## 41. 任务有依赖关系

例如：

```text
先查询用户
再根据用户查询订单
再根据订单计算金额
```

可以用：

```java
CompletableFuture<String> future = CompletableFuture
        .supplyAsync(() -> userService.getUser(userId), executor)
        .thenApplyAsync(user -> orderService.getOrder(user), executor)
        .thenApplyAsync(order -> amountService.calculate(order), executor);
```

---

## 42. 两个任务结果合并

例如：

```text
查询用户信息
查询积分信息
合并成用户详情
```

```java
CompletableFuture<User> userFuture =
        CompletableFuture.supplyAsync(() -> userService.getUser(userId), executor);

CompletableFuture<Point> pointFuture =
        CompletableFuture.supplyAsync(() -> pointService.getPoint(userId), executor);

CompletableFuture<UserDetail> detailFuture = userFuture.thenCombine(
        pointFuture,
        (user, point) -> {
            UserDetail detail = new UserDetail();
            detail.setUser(user);
            detail.setPoint(point);
            return detail;
        }
);
```

---

# 十一、注意事项

## 43. 不要滥用默认线程池

不推荐：

```java
CompletableFuture.supplyAsync(() -> {
    return remoteCall();
});
```

推荐：

```java
CompletableFuture.supplyAsync(() -> {
    return remoteCall();
}, bizExecutor);
```

原因：

```text
默认 commonPool 不好隔离
阻塞任务会影响其他异步任务
线程数不可控
线上问题不好排查
```

---

## 44. 不要在异步任务里吞异常

错误示例：

```java
CompletableFuture.supplyAsync(() -> {
    try {
        return remoteCall();
    } catch (Exception e) {
        return null;
    }
});
```

问题：

```text
异常被吞掉
调用方不知道失败原因
可能导致空指针
```

推荐：

```java
CompletableFuture.supplyAsync(() -> {
    return remoteCall();
}).exceptionally(e -> {
    log.error("调用失败", e);
    return defaultValue;
});
```

---

## 45. 注意事务问题

`CompletableFuture` 开启新线程后，不会自动继承当前 Spring 事务。

错误理解：

```text
主线程有 @Transactional
异步线程里的数据库操作也在同一个事务中
```

实际：

```text
Spring 事务基于 ThreadLocal
异步线程拿不到主线程事务上下文
```

示例：

```java
@Transactional
public void createOrder() {
    CompletableFuture.runAsync(() -> {
        orderMapper.insertLog(); // 不在 createOrder 的事务中
    }, executor);
}
```

注意：

```text
异步任务中的数据库操作需要独立考虑事务
```

---

## 46. 注意 ThreadLocal 上下文丢失

例如：

```text
用户上下文
TraceId
MDC 日志上下文
租户上下文
安全上下文
```

这些通常存在线程的 `ThreadLocal` 中。

切换线程后会丢失。

解决方式：

```text
手动传递上下文
使用 TaskDecorator
使用 TransmittableThreadLocal
在线程池中包装 Runnable / Callable
```

---

## 47. 注意任务阻塞

虽然 `CompletableFuture` 是异步编排工具，但如果任务本身是阻塞的：

```text
慢 SQL
远程 HTTP
RPC 调用
文件 IO
```

还是会占用线程池线程。

所以要合理设置：

```text
核心线程数
最大线程数
队列大小
超时时间
拒绝策略
```

---

## 48. allOf 中一个任务异常怎么办

如果 `allOf()` 中某个任务异常，`allOf().join()` 会抛异常。

示例：

```java
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> "A");

CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> {
    throw new RuntimeException("error");
});

CompletableFuture<Void> all = CompletableFuture.allOf(f1, f2);

all.join(); // 会抛 CompletionException
```

可以给每个任务单独兜底：

```java
CompletableFuture<String> f2 = CompletableFuture
        .supplyAsync(() -> {
            throw new RuntimeException("error");
        })
        .exceptionally(e -> "default");
```

---

# 十二、常见方法总结

## 49. 创建任务

| 方法 | 说明 |
|---|---|
| `runAsync` | 无返回值异步任务 |
| `supplyAsync` | 有返回值异步任务 |

---

## 50. 串行编排

| 方法 | 说明 |
|---|---|
| `thenApply` | 接收结果并返回新结果 |
| `thenAccept` | 接收结果，无返回值 |
| `thenRun` | 不接收结果，无返回值 |
| `thenCompose` | 扁平化串联异步任务 |

---

## 51. 结果合并

| 方法 | 说明 |
|---|---|
| `thenCombine` | 合并两个任务结果 |
| `thenAcceptBoth` | 消费两个任务结果 |
| `runAfterBoth` | 两个任务都完成后执行 |

---

## 52. 谁先完成

| 方法 | 说明 |
|---|---|
| `applyToEither` | 谁先完成，处理谁的结果 |
| `acceptEither` | 谁先完成，消费谁的结果 |
| `runAfterEither` | 任意一个完成后执行 |

---

## 53. 多任务

| 方法 | 说明 |
|---|---|
| `allOf` | 等待所有任务完成 |
| `anyOf` | 等待任意一个任务完成 |

---

## 54. 异常处理

| 方法 | 说明 |
|---|---|
| `exceptionally` | 异常兜底 |
| `handle` | 成功失败都处理，并返回新结果 |
| `whenComplete` | 成功失败都执行，通常用于日志 |

---

## 55. 超时控制

| 方法 | 说明 |
|---|---|
| `orTimeout` | 超时抛异常 |
| `completeOnTimeout` | 超时返回默认值 |

---

# 十三、thenApply 和 thenCompose 区别

## 56. thenApply

`thenApply()` 用于普通结果转换。

```java
CompletableFuture<String> future = CompletableFuture
        .supplyAsync(() -> "hello")
        .thenApply(result -> result + " world");
```

结果类型：

```java
CompletableFuture<String>
```

---

## 57. thenCompose

`thenCompose()` 用于连接两个异步任务，避免嵌套。

错误/不优雅写法：

```java
CompletableFuture<CompletableFuture<String>> future =
        CompletableFuture.supplyAsync(() -> "user")
                .thenApply(user -> CompletableFuture.supplyAsync(() -> "order"));
```

使用 `thenCompose()`：

```java
CompletableFuture<String> future =
        CompletableFuture.supplyAsync(() -> "user")
                .thenCompose(user -> CompletableFuture.supplyAsync(() -> "order"));
```

区别：

| 方法 | 返回结果 |
|---|---|
| `thenApply` | 可能产生嵌套 `CompletableFuture<CompletableFuture<T>>` |
| `thenCompose` | 扁平化为 `CompletableFuture<T>` |

一句话：

```text
thenApply 用于同步转换结果
thenCompose 用于串联另一个异步任务
```

---

# 十四、面试常问问题

## 58. CompletableFuture 是什么？

```text
CompletableFuture 是 Java 8 提供的异步编程工具类。
它可以创建异步任务、组合多个异步任务、处理任务结果、处理异常和超时。
相比 Future，它支持链式调用和任务编排。
```

---

## 59. CompletableFuture 和 Future 区别？

```text
Future 只能通过 get() 阻塞获取结果，不方便任务编排，也不支持回调。
CompletableFuture 支持异步回调、任务串行、任务并行、结果合并、异常处理和手动完成任务。
```

---

## 60. runAsync 和 supplyAsync 区别？

```text
runAsync 用于没有返回值的异步任务，返回 CompletableFuture<Void>。
supplyAsync 用于有返回值的异步任务，返回 CompletableFuture<T>。
```

---

## 61. thenApply、thenAccept、thenRun 区别？

```text
thenApply 接收上一步结果，并返回新结果。
thenAccept 接收上一步结果，但没有返回值。
thenRun 不接收上一步结果，也没有返回值。
```

---

## 62. thenCompose 和 thenCombine 区别？

```text
thenCompose 用于串联两个有依赖关系的异步任务，第二个任务依赖第一个任务的结果。
thenCombine 用于合并两个互不依赖的异步任务，两个任务都完成后合并结果。
```

---

## 63. allOf 和 anyOf 区别？

```text
allOf 等待所有任务完成。
anyOf 等待任意一个任务完成。
```

---

## 64. CompletableFuture 默认使用什么线程池？

```text
如果没有指定线程池，CompletableFuture 默认使用 ForkJoinPool.commonPool()。
生产环境不建议大量使用默认线程池，推荐传入自定义线程池。
```

---

## 65. CompletableFuture 会继承 Spring 事务吗？

```text
不会。

Spring 事务基于 ThreadLocal 保存事务上下文。
CompletableFuture 异步任务通常会切换线程，新线程拿不到主线程事务上下文。
所以异步任务中的数据库操作不会自动加入外层事务。
```

---

## 66. CompletableFuture 使用时要注意什么？

```text
指定自定义线程池
合理设置线程池大小和队列
做好异常处理
做好超时控制
注意事务上下文丢失
注意 ThreadLocal 上下文丢失
不要在异步任务中无限阻塞
allOf 中每个任务最好单独处理异常
```

---

# 十五、总结

`CompletableFuture` 是 Java 8 提供的异步编程工具类，用来创建异步任务并进行任务编排。相比传统 `Future`，它不仅可以异步执行任务，还支持链式调用、任务串行、任务并行、结果合并、异常处理和超时控制。

创建异步任务常用 `runAsync` 和 `supplyAsync`。`runAsync` 适合没有返回值的任务，`supplyAsync` 适合有返回值的任务。任务串行可以使用 `thenApply`、`thenAccept`、`thenRun`；两个任务结果合并可以使用 `thenCombine`；多个任务等待全部完成可以使用 `allOf`，等待任意一个完成可以使用 `anyOf`。

如果没有指定线程池，`CompletableFuture` 默认使用 `ForkJoinPool.commonPool()`，生产环境一般不建议直接使用默认线程池，而是应该传入自定义线程池，避免不同业务之间互相影响。

使用 `CompletableFuture` 时还要注意异常处理和超时控制，比如使用 `exceptionally`、`handle`、`whenComplete` 处理异常，使用 `orTimeout` 或 `completeOnTimeout` 控制超时。同时要注意异步任务会切换线程，Spring 事务、`ThreadLocal`、MDC、用户上下文等不会自动传递，需要单独处理。

---

## 67. 一句话总结

> `CompletableFuture` 是 Java 的异步任务编排工具，核心能力是异步执行、串行编排、并行合并、异常处理和超时控制；生产使用时重点关注自定义线程池、异常兜底、超时控制、事务和上下文传递问题。
