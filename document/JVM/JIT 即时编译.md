# JIT 即时编译

## 1. JIT 是什么

JIT 全称是 **Just-In-Time Compilation**，中文叫 **即时编译**。

在 Java 中，JIT 是 JVM 的一项优化技术：

> 程序运行过程中，JVM 会把频繁执行的热点代码编译成本地机器码，从而提高执行效率。

Java 程序不是一开始就直接变成机器码执行，而是先编译成字节码：

```text
Java 源码
   ↓ javac
字节码 .class
   ↓ JVM
解释执行 + JIT 编译
   ↓
机器码
```

---

## 2. 为什么需要 JIT

Java 代码编译后生成的是字节码：

```java
public class Test {
    public static void main(String[] args) {
        System.out.println("hello");
    }
}
```

经过 `javac` 编译后变成：

```text
Test.class
```

`.class` 文件不能直接被 CPU 执行，需要 JVM 执行。

JVM 执行字节码有两种方式：

| 执行方式 | 说明 |
|---|---|
| 解释执行 | 一行一行把字节码解释成机器指令执行 |
| JIT 编译 | 把热点代码编译成本地机器码后执行 |

解释执行启动快，但长期运行性能一般。

JIT 编译前期有编译成本，但热点代码编译后执行很快。

---

## 3. 解释执行和 JIT 编译区别

### 解释执行

```text
字节码
   ↓
解释器逐条解释
   ↓
CPU 执行
```

特点：

- 启动快
- 不需要等待编译
- 执行效率相对低
- 适合只执行一次或很少执行的代码

---

### JIT 编译

```text
热点字节码
   ↓
JIT 编译器
   ↓
机器码
   ↓
CPU 直接执行
```

特点：

- 编译后执行快
- 适合频繁执行的代码
- 有一定编译成本
- 可以根据运行时信息做优化

---

## 4. 什么是热点代码

JIT 不会把所有代码都编译成本地机器码。

它主要编译 **热点代码**。

热点代码包括：

- 被频繁调用的方法
- 被频繁执行的循环体
- 执行次数很多的代码路径

例如：

```java
for (int i = 0; i < 10000000; i++) {
    sum += i;
}
```

这个循环执行很多次，就可能成为热点代码。

---

## 5. JVM 如何判断热点代码

JVM 会通过计数器统计代码执行频率。

主要有两类计数器：

| 计数器 | 说明 |
|---|---|
| 方法调用计数器 | 统计方法被调用多少次 |
| 回边计数器 | 统计循环执行多少次 |

当计数达到一定阈值后，JVM 就认为这段代码是热点代码，会触发 JIT 编译。

---

## 6. JIT 编译流程

整体流程：

```text
Java 源码
   ↓
javac 编译
   ↓
字节码
   ↓
JVM 解释执行
   ↓
发现热点代码
   ↓
JIT 编译成本地机器码
   ↓
后续直接执行机器码
```

更具体：

```text
1. JVM 加载 class 文件
2. 解释器开始执行字节码
3. JVM 统计方法调用次数和循环次数
4. 发现热点代码
5. JIT 编译器将热点字节码编译成本地机器码
6. 机器码缓存起来
7. 后续再执行该代码时直接执行机器码
```

---

## 7. JIT 编译器类型

HotSpot JVM 中主要有两种 JIT 编译器：

| 编译器 | 说明 |
|---|---|
| C1 编译器 | Client Compiler，编译速度快，优化较少 |
| C2 编译器 | Server Compiler，编译速度慢，优化更强 |

---

## 8. C1 和 C2 区别

| 对比项 | C1 | C2 |
|---|---|---|
| 目标 | 快速编译 | 深度优化 |
| 编译速度 | 快 | 慢 |
| 优化程度 | 较低 | 较高 |
| 适合场景 | 启动阶段、客户端程序 | 长时间运行的服务端程序 |
| 性能 | 一般 | 更高 |

简单理解：

```text
C1：先快速优化一下，让程序尽快跑起来
C2：后面发现代码很热，再做更深层优化
```

---

## 9. 分层编译

现代 JVM 默认使用 **分层编译**。

分层编译就是：

> JVM 会先用解释器执行代码，然后根据热点程度逐步使用 C1、C2 编译器优化。

大致流程：

```text
解释执行
   ↓
C1 简单编译
   ↓
C1 带性能采集编译
   ↓
C2 深度优化编译
```

这样可以兼顾：

- 启动速度
- 编译效率
- 运行性能

---

## 10. JIT 常见优化手段

### 10.1 方法内联

方法内联是 JIT 最重要的优化之一。

原代码：

```java
public int add(int a, int b) {
    return a + b;
}

public int test() {
    return add(1, 2);
}
```

JIT 优化后类似：

```java
public int test() {
    return 1 + 2;
}
```

好处：

- 减少方法调用开销
- 方便继续做其他优化

---

### 10.2 逃逸分析

逃逸分析用于判断对象是否会逃出方法作用域。

示例：

```java
public void test() {
    User user = new User();
    user.setName("Tom");
}
```

如果 `user` 只在方法内部使用，没有被返回，也没有赋值给全局变量，那么它没有逃逸。

JIT 可以进一步优化：

- 栈上分配
- 标量替换
- 锁消除

---

### 10.3 锁消除

如果 JIT 判断某个锁对象不会被多个线程访问，就可以去掉锁。

示例：

```java
public void test() {
    StringBuffer sb = new StringBuffer();
    sb.append("a");
    sb.append("b");
}
```

`StringBuffer` 方法有同步锁，但 `sb` 是局部变量，不会被其他线程访问。

JIT 可能会消除锁，提高性能。

---

### 10.4 标量替换

原本创建对象：

```java
Point p = new Point(1, 2);
int x = p.x;
int y = p.y;
```

JIT 可能优化为：

```java
int x = 1;
int y = 2;
```

也就是不真正创建对象，而是把对象拆成几个普通变量。

---

### 10.5 公共子表达式消除

原代码：

```java
int a = x * y + 1;
int b = x * y + 2;
```

`x * y` 计算了两次。

JIT 可能优化为：

```java
int temp = x * y;
int a = temp + 1;
int b = temp + 2;
```

---

### 10.6 循环优化

JIT 对循环会做很多优化，例如：

- 循环展开
- 循环不变量外提
- 边界检查消除
- 回边计数优化

示例：

```java
for (int i = 0; i < list.size(); i++) {
    sum += list.get(i);
}
```

如果 `list.size()` 在循环中不变，JIT 可能把它提到循环外。

---

## 11. JIT 编译的优点

| 优点 | 说明 |
|---|---|
| 性能高 | 热点代码编译成本地机器码 |
| 动态优化 | 可以根据运行时信息优化 |
| 跨平台 | Java 字节码仍然保持跨平台 |
| 自动优化 | 大多数优化不需要开发者手动处理 |

---

## 12. JIT 编译的缺点

| 缺点 | 说明 |
|---|---|
| 启动有预热成本 | 程序刚启动时热点代码还没编译 |
| 占用 CPU | JIT 编译本身需要消耗 CPU |
| 占用内存 | 编译后的机器码需要 Code Cache 保存 |
| 性能有波动 | 编译、反优化可能导致短暂抖动 |

---

## 13. 什么是预热

Java 程序刚启动时，很多代码还没有被 JIT 编译。

这时主要靠解释执行，性能可能没有达到最佳状态。

运行一段时间后，热点代码被编译成本地机器码，性能会提升。

这个过程叫：

```text
JVM 预热
```

所以 Java 服务通常有：

```text
刚启动时性能一般
运行一段时间后性能更稳定
```

---

## 14. Code Cache 是什么

JIT 编译后的机器码会存放在 JVM 的 **Code Cache** 中。

如果 Code Cache 满了，JIT 可能无法继续编译新的热点代码，影响性能。

相关参数：

```bash
-XX:ReservedCodeCacheSize=256m
```

查看 Code Cache 使用情况：

```bash
jcmd <pid> Compiler.codecache
```

---

## 15. 查看 JIT 编译情况

### 打印编译日志

```bash
-XX:+PrintCompilation
```

示例：

```bash
java -XX:+PrintCompilation -jar app.jar
```

---

### 查看 JIT 编译统计

```bash
jstat -compiler <pid>
```

输出示例：

```text
Compiled Failed Invalid   Time
   12345      2       0   34.56
```

字段说明：

| 字段 | 说明 |
|---|---|
| `Compiled` | 编译次数 |
| `Failed` | 编译失败次数 |
| `Invalid` | 失效编译次数 |
| `Time` | 编译耗时 |

---

### 查看 Code Cache

```bash
jcmd <pid> Compiler.codecache
```

---

## 16. 常见 JIT 参数

| 参数 | 说明 |
|---|---|
| `-XX:+PrintCompilation` | 打印 JIT 编译日志 |
| `-XX:+TieredCompilation` | 开启分层编译，默认开启 |
| `-XX:-TieredCompilation` | 关闭分层编译 |
| `-XX:ReservedCodeCacheSize=256m` | 设置 Code Cache 大小 |
| `-XX:CompileThreshold=10000` | 设置触发 JIT 的方法调用阈值 |
| `-Xint` | 只解释执行，不使用 JIT |
| `-Xcomp` | 尽量全部编译执行 |
| `-Xmixed` | 混合模式，默认模式 |

---

## 17. `-Xint`、`-Xcomp`、`-Xmixed`

| 参数 | 说明 |
|---|---|
| `-Xint` | 只解释执行，不启用 JIT |
| `-Xcomp` | 尽量编译执行 |
| `-Xmixed` | 解释执行 + JIT 编译，默认模式 |

### `-Xint`

```bash
java -Xint -jar app.jar
```

特点：

- 启动快
- 不进行 JIT 编译
- 长期运行性能较差

---

### `-Xcomp`

```bash
java -Xcomp -jar app.jar
```

特点：

- 尽量编译执行
- 启动慢
- 不一定适合生产

---

### `-Xmixed`

```bash
java -Xmixed -jar app.jar
```

特点：

- 默认模式
- 解释执行和 JIT 编译结合
- 综合性能最好

---

## 18. JIT 和 AOT 的区别

| 对比项 | JIT | AOT |
|---|---|---|
| 全称 | Just-In-Time | Ahead-Of-Time |
| 编译时机 | 运行时编译 | 运行前编译 |
| 启动速度 | 需要预热 | 启动快 |
| 运行优化 | 可根据运行时信息优化 | 运行时优化较少 |
| 典型场景 | HotSpot JVM | GraalVM Native Image |
| 优点 | 峰值性能高 | 启动快、内存低 |
| 缺点 | 有预热和编译成本 | 动态能力弱一些 |

简单理解：

```text
JIT：边运行边优化
AOT：提前编译好
```

---

## 19. JIT 为什么能提升性能

JIT 能提升性能，主要因为它可以利用运行时信息。

例如：

```java
interface PayService {
    void pay();
}
```

运行时如果 JVM 发现大多数情况下实际类型都是：

```java
AliPayService
```

JIT 就可以针对这个实际类型进行优化。

这类优化是静态编译阶段很难做到的。

---

## 20. JIT 可能导致的问题

### 20.1 CPU 短暂升高

JIT 编译本身需要 CPU。

服务刚启动、流量刚进来时，可能出现：

```text
CPU 短暂升高
接口响应有波动
```

---

### 20.2 预热不足

刚启动的服务没有经过充分预热，热点代码还没编译完成。

可能表现为：

```text
刚上线时接口较慢
运行一段时间后变快
```

---

### 20.3 Code Cache 满

Code Cache 满后，新的热点代码无法继续被编译。

可能影响性能。

排查：

```bash
jcmd <pid> Compiler.codecache
```

---

### 20.4 反优化

JIT 会基于运行时假设做优化。

如果后续运行情况变化，原来的优化假设不成立，JVM 可能会进行反优化。

这可能导致性能短暂波动。

---

## 21. 总结

JIT 是 Just-In-Time Compilation，也就是即时编译。

Java 程序经过 `javac` 编译后生成字节码，字节码在 JVM 中执行。JVM 执行字节码有两种方式：解释执行和 JIT 编译。解释执行是逐条解释字节码，启动快但执行效率低；JIT 编译是 JVM 在运行过程中发现热点代码后，把热点字节码编译成本地机器码，后续直接执行机器码，从而提升性能。

HotSpot JVM 中主要有 C1 和 C2 两个 JIT 编译器。C1 编译速度快，适合快速启动；C2 优化能力更强，适合长期运行的服务端程序。现代 JVM 默认使用分层编译，先解释执行，再用 C1 快速编译，最后对更热的代码使用 C2 深度优化。

JIT 常见优化包括方法内联、逃逸分析、锁消除、标量替换、循环优化等。它的优点是可以根据运行时信息动态优化，提升长期运行性能；缺点是有预热成本，会占用一定 CPU 和 Code Cache。

---

## 22. 一句话总结

> JIT 即时编译就是 JVM 在程序运行过程中，把频繁执行的热点字节码编译成本地机器码，并通过方法内联、逃逸分析、锁消除等优化手段提升 Java 程序运行性能。
