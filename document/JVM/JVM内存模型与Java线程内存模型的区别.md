# JVM 内存模型与 Java 线程内存模型的区别

这两个概念很容易混淆：

```text
JVM 内存模型：讲 JVM 运行时内存区域怎么划分
Java 线程内存模型 / JMM：讲多线程之间变量可见性、有序性、原子性
```

---

## 1. 核心区别

| 对比项 | JVM 内存模型 | Java 线程内存模型 / JMM |
|---|---|---|
| 关注点 | JVM 内存区域划分 | 多线程并发访问共享变量的规则 |
| 解决问题 | 对象、方法、栈帧、类信息放哪里 | 线程之间变量如何同步、是否可见 |
| 主要内容 | 堆、方法区、虚拟机栈、本地方法栈、程序计数器 | 主内存、工作内存、happens-before、volatile、synchronized |
| 面向对象 | JVM 运行时结构 | Java 并发语义 |
| 常见问题 | OOM、StackOverflow、GC | 可见性、指令重排、线程安全 |
| 典型工具 | jmap、MAT、GC 日志 | volatile、锁、CAS、JUC |

---

## 2. JVM 内存模型是什么

JVM 内存模型更准确叫：

```text
JVM 运行时数据区
```

它描述 JVM 运行 Java 程序时，内存怎么划分。

主要包括：

```text
堆
方法区
虚拟机栈
本地方法栈
程序计数器
```

---

## 3. JVM 运行时数据区

```text
JVM Runtime Data Areas

线程共享：
  堆
  方法区

线程私有：
  虚拟机栈
  本地方法栈
  程序计数器
```

---

## 4. 堆 Heap

### 作用

堆主要存放：

```text
对象实例
数组
```

特点：

```text
线程共享
GC 主要管理区域
容易发生 Java heap space OOM
```

示例：

```java
User user = new User();
```

`new User()` 创建的对象主要在堆中。

---

## 5. 方法区 Method Area

### 作用

方法区主要存放：

```text
类元信息
常量
静态变量
即时编译后的代码
```

JDK 8 以后，方法区实现主要是：

```text
Metaspace 元空间
```

常见问题：

```text
java.lang.OutOfMemoryError: Metaspace
```

---

## 6. 虚拟机栈 JVM Stack

### 作用

每个线程都有自己的虚拟机栈。

方法调用时会创建：

```text
栈帧 Stack Frame
```

栈帧中包含：

```text
局部变量表
操作数栈
动态链接
方法返回地址
```

常见问题：

```text
StackOverflowError
```

例如递归太深。

---

## 7. 本地方法栈 Native Method Stack

用于执行 Native 方法。

例如：

```text
C / C++ 实现的本地方法
```

和虚拟机栈类似，只是服务对象不同。

---

## 8. 程序计数器 Program Counter Register

每个线程私有。

作用：

```text
记录当前线程正在执行的字节码行号
```

线程切换后，JVM 可以通过程序计数器恢复执行位置。

---

# 二、Java 线程内存模型 / JMM

## 9. JMM 是什么

JMM 全称：

```text
Java Memory Model
```

它不是 JVM 内存区域划分，而是 Java 并发规范。

核心解决：

```text
多个线程读写共享变量时，如何保证可见性、有序性、原子性
```

---

## 10. JMM 中的主内存和工作内存

JMM 抽象出：

```text
主内存 Main Memory
工作内存 Working Memory
```

可以理解为：

```text
共享变量存放在主内存
每个线程有自己的工作内存副本
线程操作变量时，先操作自己的副本
再同步回主内存
```

示意：

```text
主内存：共享变量 count = 0

线程 A 工作内存：count 副本
线程 B 工作内存：count 副本
```

---

## 11. 为什么会有可见性问题

例如：

```java
boolean flag = true;

线程 A:
flag = false;

线程 B:
while (flag) {
}
```

问题：

```text
线程 A 修改了 flag
线程 B 不一定马上看到
```

原因：

```text
线程 B 可能一直读自己的工作内存副本
```

---

## 12. JMM 解决的三大问题

| 问题 | 说明 | 解决方式 |
|---|---|---|
| 原子性 | 操作不可被中断 | synchronized、Lock、Atomic |
| 可见性 | 一个线程修改，其他线程能看到 | volatile、synchronized、Lock |
| 有序性 | 禁止不安全的指令重排 | volatile、synchronized、happens-before |

---

## 13. volatile 和 JMM

`volatile` 主要保证：

```text
可见性
有序性
```

不保证：

```text
复合操作原子性
```

例如：

```java
volatile int count = 0;
count++;
```

`count++` 不是原子操作，仍然线程不安全。

---

## 14. synchronized 和 JMM

`synchronized` 可以保证：

```text
原子性
可见性
有序性
```

进入同步块：

```text
从主内存读取最新值
```

退出同步块：

```text
把修改刷新回主内存
```

---

## 15. happens-before

JMM 通过 happens-before 规则定义操作之间的可见性关系。

常见规则：

```text
程序顺序规则
锁规则
volatile 规则
线程启动规则
线程终止规则
传递性规则
```

例如：

```text
对 volatile 变量的写
happens-before
后续对该 volatile 变量的读
```

---

# 三、二者容易混淆的点

## 16. 主内存是不是堆？

不是完全等价。

JMM 的主内存是一个抽象概念。

可以粗略理解为：

```text
共享变量主要在堆中
```

但不能简单说：

```text
JMM 主内存 = JVM 堆
```

因为 JMM 是并发语义模型，JVM 内存区域是运行时数据结构。

---

## 17. 工作内存是不是虚拟机栈？

也不是完全等价。

JMM 的工作内存是抽象概念。

可以粗略理解为包括：

```text
CPU 缓存
寄存器
线程本地变量副本
编译器优化后的临时数据
```

而 JVM 虚拟机栈是具体运行时区域，用来保存栈帧、局部变量表等。

---

## 18. 对象在堆中，为什么线程还有副本？

对象本体在堆中，但线程执行时可能把字段值加载到：

```text
CPU 缓存
寄存器
工作内存副本
```

所以会出现：

```text
一个线程改了变量
另一个线程暂时看不到
```

这就是 JMM 要解决的问题。

---

## 19. 一个例子理解区别

```java
public class Demo {
    private static boolean flag = true;

    public static void main(String[] args) {
        new Thread(() -> {
            while (flag) {
            }
        }).start();

        flag = false;
    }
}
```

从 JVM 内存区域看：

```text
Demo 类信息在方法区
flag 静态变量在方法区/堆相关区域
线程有自己的虚拟机栈和程序计数器
```

从 JMM 看：

```text
flag 是共享变量
线程可能读取自己的工作内存副本
main 线程修改 flag 后，子线程不一定可见
需要 volatile 保证可见性
```

修改：

```java
private static volatile boolean flag = true;
```

---

## 20. 总结

JVM 内存模型和 Java 线程内存模型不是一个东西。

JVM 内存模型更准确叫 JVM 运行时数据区，主要讲 JVM 内存怎么划分，包括堆、方法区、虚拟机栈、本地方法栈和程序计数器。它主要用于理解对象分配、方法调用、GC、OOM、栈溢出等问题。

Java 线程内存模型通常指 JMM，它是 Java 并发规范，主要讲多线程之间共享变量如何读写，如何保证可见性、原子性和有序性。它主要用于理解 volatile、synchronized、Lock、CAS、happens-before、指令重排等并发问题。

一句话总结：

```text
JVM 内存模型 = JVM 运行时内存区域划分；
JMM = Java 多线程共享变量的读写规则。
```
