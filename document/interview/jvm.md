# JVM 常见面试题总结

## 目录

- [一、JVM 内存结构](#一jvm-内存结构)
- [二、堆和栈的区别](#二堆和栈的区别)
- [三、对象创建过程](#三对象创建过程)
- [四、对象什么时候进入老年代](#四对象什么时候进入老年代)
- [五、GC Roots 有哪些](#五gc-roots-有哪些)
- [六、Minor GC、Major GC、Full GC 区别](#六minor-gcmajor-gcfull-gc-区别)
- [七、常见垃圾回收器有哪些](#七常见垃圾回收器有哪些)
- [八、CMS 和 G1 区别](#八cms-和-g1-区别)
- [九、如何排查 OOM](#九如何排查-oom)
- [十、如何排查 CPU 飙高](#十如何排查-cpu-飙高)
- [十一、类加载机制是什么](#十一类加载机制是什么)
- [十二、双亲委派模型是什么](#十二双亲委派模型是什么)
- [十三、面试速记版](#十三面试速记版)
- [十四、总览表](#十四总览表)
- [十五、完整面试回答模板](#十五完整面试回答模板)

---

# 一、JVM 内存结构

## 1. JVM 运行时数据区

JVM 运行时内存结构主要分为两大类：

```text
线程共享区域：
1. 堆
2. 方法区

线程私有区域：
1. 程序计数器
2. Java 虚拟机栈
3. 本地方法栈
```

结构示意：

```text
JVM Runtime Data Area

├── 线程共享
│   ├── 堆 Heap
│   └── 方法区 Method Area
│
└── 线程私有
    ├── 程序计数器 Program Counter Register
    ├── Java 虚拟机栈 JVM Stack
    └── 本地方法栈 Native Method Stack
```

---

## 2. 程序计数器

程序计数器是线程私有的。

它可以理解为：

> 当前线程正在执行的字节码行号指示器。

每个线程都有自己的程序计数器，用来记录当前线程执行到哪一条字节码指令。

### 特点

1. 线程私有；
2. 生命周期与线程一致；
3. 占用内存很小；
4. 是 JVM 规范中唯一一个不会出现 `OutOfMemoryError` 的区域；
5. 如果执行的是 Java 方法，记录字节码指令地址；
6. 如果执行的是 Native 方法，值可能为空。

---

## 3. Java 虚拟机栈

Java 虚拟机栈也是线程私有的。

每个线程创建时，都会创建一个对应的虚拟机栈。

虚拟机栈中存放的是一个个 **栈帧**。

每调用一个方法，就会创建一个栈帧并入栈；方法执行结束后，栈帧出栈。

---

## 4. 栈帧中包含什么？

一个栈帧主要包含：

```text
1. 局部变量表
2. 操作数栈
3. 动态链接
4. 方法返回地址
```

---

### 4.1 局部变量表

局部变量表用于存放：

1. 方法参数；
2. 方法内部定义的局部变量；
3. 基本数据类型；
4. 对象引用；
5. returnAddress 类型。

示例：

```java
public void test(int age) {
    String name = "Tom";
    int count = 10;
}
```

其中 `age`、`name`、`count` 都会存放在当前方法栈帧的局部变量表中。

---

### 4.2 操作数栈

操作数栈用于字节码指令执行过程中的临时计算。

例如：

```java
int c = a + b;
```

JVM 在执行加法时，会将 `a` 和 `b` 压入操作数栈，然后执行加法指令，再将结果保存到局部变量表。

---

### 4.3 动态链接

每个栈帧都包含一个指向运行时常量池中该方法所属类的引用。

动态链接用于支持方法调用过程中的符号引用解析。

---

### 4.4 方法返回地址

方法执行完成后，需要知道返回到调用方的哪一条指令继续执行。

方法返回地址就是用来记录这个位置的。

---

## 5. 本地方法栈

本地方法栈也是线程私有的。

它和 Java 虚拟机栈类似，只不过 Java 虚拟机栈服务于 Java 方法，而本地方法栈服务于 Native 方法。

Native 方法通常是用 C、C++ 等语言实现的方法。

---

## 6. 堆

堆是 JVM 中最大的一块内存区域。

它是线程共享的。

主要用于存放：

```text
对象实例
数组
```

例如：

```java
User user = new User();
int[] arr = new int[10];
```

其中 `new User()` 创建的对象实例、`new int[10]` 创建的数组都存放在堆中。

堆是垃圾回收器管理的主要区域。

---

## 7. 方法区

方法区是线程共享的。

主要用于存放：

1. 类信息；
2. 常量；
3. 静态变量；
4. 即时编译器编译后的代码；
5. 运行时常量池。

---

## 8. 方法区、永久代、元空间的关系

方法区是 JVM 规范中的概念。

HotSpot JVM 对方法区有不同实现：

| JDK 版本 | 方法区实现 |
|---|---|
| JDK 7 及以前 | 永久代 PermGen |
| JDK 8 及以后 | 元空间 Metaspace |

---

## 9. 永久代和元空间区别

### 永久代

永久代使用的是 JVM 堆内存的一部分。

容易出现：

```text
java.lang.OutOfMemoryError: PermGen space
```

---

### 元空间

元空间使用的是本地内存，也就是操作系统内存。

JDK 8 以后，永久代被移除，类元数据主要存放在元空间中。

常见异常：

```text
java.lang.OutOfMemoryError: Metaspace
```

---

## 10. 直接内存

直接内存不是 JVM 运行时数据区的一部分，但在实际排查问题时非常重要。

例如 NIO 中的 `DirectByteBuffer` 会使用直接内存。

示例：

```java
ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
```

直接内存不受 Java 堆大小直接限制，但会受到本机内存限制，也可以通过参数限制：

```bash
-XX:MaxDirectMemorySize=256m
```

---

## 11. 面试回答

JVM 内存结构主要分为线程共享区域和线程私有区域。

线程共享区域包括堆和方法区。堆主要存放对象实例和数组，是垃圾回收的主要区域；方法区主要存放类信息、常量、静态变量和即时编译后的代码。HotSpot 在 JDK 8 以后使用元空间实现方法区。

线程私有区域包括程序计数器、Java 虚拟机栈和本地方法栈。程序计数器记录当前线程执行的字节码位置；虚拟机栈存放方法调用产生的栈帧；本地方法栈服务于 Native 方法。

---

# 二、堆和栈的区别

## 1. 堆是什么？

堆是线程共享的内存区域，主要用于存放对象实例和数组。

示例：

```java
User user = new User();
```

其中：

```text
user 这个引用变量在栈中；
new User() 创建的对象实例在堆中。
```

---

## 2. 栈是什么？

栈是线程私有的内存区域，主要用于存放方法调用产生的栈帧。

每个方法执行时都会创建栈帧，方法执行结束后栈帧销毁。

---

## 3. 堆和栈的区别

| 对比项 | 堆 | 栈 |
|---|---|---|
| 线程共享 | 线程共享 | 线程私有 |
| 存储内容 | 对象实例、数组 | 栈帧、局部变量、操作数栈 |
| 生命周期 | 对象由 GC 回收 | 方法调用结束后自动出栈 |
| 空间大小 | 通常较大 | 通常较小 |
| 访问速度 | 相对较慢 | 相对较快 |
| 是否 GC 管理 | 是 | 一般不需要 GC |
| 常见异常 | `OutOfMemoryError` | `StackOverflowError`、`OutOfMemoryError` |
| 参数控制 | `-Xms`、`-Xmx` | `-Xss` |

---

## 4. 示例说明

```java
public void test() {
    User user = new User();
}
```

内存分布：

```text
栈：
    user 引用变量

堆：
    User 对象实例
```

当 `test()` 方法执行结束后：

```text
user 引用从栈中销毁；
如果堆中的 User 对象没有其他引用指向它，则会成为可回收对象。
```

---

## 5. 面试回答

堆是线程共享的，主要存放对象实例和数组，是垃圾回收的主要区域。

栈是线程私有的，主要存放方法调用产生的栈帧，包括局部变量表、操作数栈、动态链接和方法返回地址。

堆中的对象生命周期由 GC 管理；栈中的栈帧随着方法调用创建，随着方法结束销毁。

---

# 三、对象创建过程

## 1. 对象创建整体流程

当执行：

```java
User user = new User();
```

JVM 创建对象大致经历以下步骤：

```text
1. 类加载检查
2. 分配内存
3. 初始化零值
4. 设置对象头
5. 执行构造方法
6. 引用指向对象
```

---

## 2. 第一步：类加载检查

当 JVM 遇到 `new` 指令时，会先检查这个类是否已经被加载、解析和初始化。

如果类还没有被加载，则先执行类加载过程。

类加载过程包括：

```text
加载 -> 验证 -> 准备 -> 解析 -> 初始化
```

---

## 3. 第二步：分配内存

类加载完成后，JVM 已经知道对象需要多大内存。

接下来会在堆中为对象分配内存。

常见分配方式有两种：

```text
1. 指针碰撞
2. 空闲列表
```

---

## 4. 指针碰撞

如果堆内存是规整的，已使用内存放在一边，空闲内存放在另一边，中间有一个指针作为分界点。

分配对象时，只需要移动指针即可。

这种方式叫：

```text
指针碰撞
```

优点是速度快。

---

## 5. 空闲列表

如果堆内存不是规整的，JVM 需要维护一个列表，记录哪些内存块是空闲的。

分配对象时，从空闲列表中找一块足够大的空间。

这种方式叫：

```text
空闲列表
```

---

## 6. TLAB

为了避免多线程并发分配对象时频繁加锁，HotSpot JVM 使用了 TLAB。

TLAB 全称：

```text
Thread Local Allocation Buffer
```

即线程本地分配缓冲区。

每个线程可以在自己的 TLAB 中分配对象，从而减少竞争。

---

## 7. 第三步：初始化零值

内存分配完成后，JVM 会将分配到的内存空间初始化为零值。

例如：

| 类型 | 零值 |
|---|---|
| int | 0 |
| long | 0L |
| boolean | false |
| 引用类型 | null |

---

## 8. 第四步：设置对象头

对象头中主要包含：

```text
1. Mark Word
2. 类型指针
3. 数组长度，只有数组对象有
```

---

### 8.1 Mark Word

Mark Word 中保存对象运行时数据，例如：

1. 哈希码；
2. GC 分代年龄；
3. 锁状态标志；
4. 偏向线程 ID；
5. Monitor 指针。

---

### 8.2 类型指针

类型指针指向该对象所属类的元数据。

JVM 通过类型指针知道这个对象是哪个类的实例。

---

## 9. 第五步：执行构造方法

对象头设置完成后，JVM 会执行对象的构造方法，也就是 `<init>` 方法。

这一步会按照代码中的逻辑给字段赋值。

示例：

```java
public class User {
    private String name = "Tom";

    public User() {
        System.out.println("构造方法执行");
    }
}
```

---

## 10. 第六步：引用指向对象

最后，将对象地址赋值给引用变量。

```java
User user = new User();
```

此时：

```text
user 引用指向堆中的 User 对象。
```

---

## 11. 面试回答

对象创建过程大致分为：类加载检查、分配内存、初始化零值、设置对象头、执行构造方法、引用指向对象。

JVM 遇到 `new` 指令时，会先检查类是否已经加载。如果没有加载，则先进行类加载。然后在堆中为对象分配内存，分配方式可能是指针碰撞或空闲列表。内存分配完成后，JVM 会把对象内存初始化为零值，设置对象头，最后执行构造方法。

---

# 四、对象什么时候进入老年代

## 1. 堆的分代模型

传统分代模型中，堆通常分为：

```text
年轻代 Young Generation
老年代 Old Generation
```

年轻代又分为：

```text
Eden 区
Survivor From 区
Survivor To 区
```

结构示意：

```text
Heap
├── Young Generation
│   ├── Eden
│   ├── Survivor From
│   └── Survivor To
└── Old Generation
```

---

## 2. 大多数对象先进入 Eden 区

大部分新对象会优先分配在 Eden 区。

```java
User user = new User();
```

通常会进入 Eden 区。

当 Eden 区空间不足时，会触发 Minor GC。

---

## 3. 对象年龄达到阈值

对象在年轻代中每经历一次 Minor GC 并存活下来，年龄就会加 1。

当年龄达到一定阈值时，对象会晋升到老年代。

相关参数：

```bash
-XX:MaxTenuringThreshold=15
```

默认最大年龄通常是 15，但实际晋升并不一定非要等到 15。

---

## 4. 动态年龄判断

JVM 会根据 Survivor 区的使用情况动态判断对象是否提前进入老年代。

如果某个年龄及以上的对象大小总和超过 Survivor 区的一半，那么大于等于这个年龄的对象可能直接进入老年代。

---

## 5. 大对象直接进入老年代

大对象是指需要大量连续内存空间的对象，例如：

```java
byte[] data = new byte[10 * 1024 * 1024];
```

大对象可能直接进入老年代。

常见于：

1. 大数组；
2. 大字符串；
3. 大集合；
4. 大对象图。

---

## 6. Survivor 空间不足

Minor GC 后，如果存活对象无法放入 Survivor 区，则这些对象会通过空间分配担保机制进入老年代。

---

## 7. 长期存活对象进入老年代

如果一个对象经过多次 Minor GC 后仍然存活，说明它可能是长期存活对象，适合晋升到老年代。

---

## 8. G1 中的情况

G1 仍然有年轻代和老年代的逻辑，但它将堆划分为多个大小相等的 Region，而不是物理上连续的新生代和老年代。

G1 会根据 Region 的角色，将 Region 标记为 Eden、Survivor、Old 或 Humongous。

---

## 9. 面试回答

对象进入老年代主要有几种情况：

第一，对象年龄达到晋升阈值。对象每经历一次 Minor GC 并存活下来，年龄就会加 1，达到阈值后会进入老年代。

第二，大对象可能直接进入老年代。

第三，动态年龄判断可能让部分对象提前进入老年代。

第四，Minor GC 后 Survivor 区放不下存活对象时，对象会通过空间分配担保进入老年代。

---

# 五、GC Roots 有哪些

## 1. 什么是 GC Roots？

GC Roots 是垃圾回收时判断对象是否存活的起点。

JVM 会从 GC Roots 出发，沿着引用链向下搜索。

如果一个对象可以从 GC Roots 到达，说明对象仍然存活。

如果一个对象无法从 GC Roots 到达，说明它是不可达对象，可能被回收。

---

## 2. 可达性分析算法

Java 判断对象是否可回收，主要使用可达性分析算法。

过程如下：

```text
GC Roots
   |
   v
引用链
   |
   v
可达对象：存活
不可达对象：可能被回收
```

---

## 3. 常见 GC Roots

常见可以作为 GC Roots 的对象包括：

```text
1. 虚拟机栈中引用的对象
2. 本地方法栈中 JNI 引用的对象
3. 方法区中类静态属性引用的对象
4. 方法区中常量引用的对象
5. 被 synchronized 锁持有的对象
6. JVM 内部引用
7. 活跃线程对象
```

---

## 4. 虚拟机栈中引用的对象

示例：

```java
public void test() {
    User user = new User();
}
```

`user` 是局部变量，存放在栈帧的局部变量表中。

只要当前方法还没有执行结束，`user` 引用指向的对象就可以作为可达对象。

---

## 5. 静态变量引用的对象

示例：

```java
public class UserHolder {
    public static User user = new User();
}
```

`user` 是类静态变量，只要类没有被卸载，它引用的对象通常就是可达的。

---

## 6. 常量引用的对象

示例：

```java
public static final String NAME = "Tom";
```

运行时常量池、字符串常量池中的引用也可能作为 GC Roots 的一部分参与可达性判断。

---

## 7. JNI 引用对象

Native 方法中引用的 Java 对象，也可以作为 GC Roots。

---

## 8. 被锁持有的对象

如果一个对象正在被 `synchronized` 持有，也可能作为 GC Roots 相关的对象。

---

## 9. 面试回答

GC Roots 是可达性分析的起点。JVM 判断对象是否存活时，会从 GC Roots 出发向下搜索，如果对象能被 GC Roots 直接或间接引用，就认为对象存活；否则对象可能被回收。

常见 GC Roots 包括虚拟机栈中引用的对象、方法区中静态变量引用的对象、常量引用的对象、本地方法栈中 JNI 引用的对象、被锁持有的对象以及 JVM 内部引用等。

---

# 六、Minor GC、Major GC、Full GC 区别

## 1. Minor GC

Minor GC 也叫 Young GC。

它主要回收年轻代。

触发场景通常是：

```text
Eden 区空间不足
```

---

## 2. Minor GC 特点

1. 发生频率较高；
2. 回收年轻代；
3. 通常速度较快；
4. 大多数对象会在年轻代被回收；
5. 会产生 Stop The World。

---

## 3. Major GC

Major GC 一般指老年代 GC。

不过需要注意：

> Major GC 这个术语在不同 JVM、不同垃圾回收器、不同日志中含义可能不完全一致。

有些场景中，Major GC 指老年代回收；有些资料中，也会把 Full GC 称为 Major GC。

---

## 4. Full GC

Full GC 通常指回收整个 Java 堆，可能还会涉及方法区、元空间、类卸载等。

Full GC 通常开销较大，停顿时间较长。

---

## 5. 三者对比

| 对比项 | Minor GC | Major GC | Full GC |
|---|---|---|---|
| 回收区域 | 年轻代 | 通常指老年代 | 整个堆，可能包含方法区/元空间 |
| 触发频率 | 较高 | 较低 | 较低 |
| 停顿时间 | 较短 | 较长 | 通常最长 |
| 触发原因 | Eden 空间不足 | 老年代空间不足 | 老年代不足、元空间不足、显式 System.gc 等 |
| 是否 STW | 是 | 通常是 | 通常是 |

---

## 6. Full GC 常见触发原因

Full GC 常见触发原因包括：

1. 老年代空间不足；
2. 元空间不足；
3. 调用 `System.gc()`；
4. Minor GC 晋升失败；
5. 空间分配担保失败；
6. 大对象分配失败；
7. CMS 出现 Concurrent Mode Failure；
8. G1 中出现 Evacuation Failure 或 Humongous 对象分配压力过大。

---

## 7. 面试回答

Minor GC 主要回收年轻代，通常在 Eden 区空间不足时触发，频率较高，停顿时间较短。

Major GC 一般指老年代 GC，但这个术语在不同场景下可能有不同含义。

Full GC 通常回收整个堆，并可能涉及方法区或元空间，停顿时间较长，对系统影响较大。线上应重点关注 Full GC 频率和耗时。

---

# 七、常见垃圾回收器有哪些

## 1. 常见垃圾回收器分类

常见垃圾回收器包括：

```text
1. Serial GC
2. ParNew GC
3. Parallel GC
4. CMS GC
5. G1 GC
6. ZGC
7. Shenandoah GC
8. Epsilon GC
```

---

## 2. Serial GC

Serial GC 是单线程垃圾回收器。

特点：

1. 单线程回收；
2. 简单高效；
3. Stop The World 时间可能较长；
4. 适合客户端、小内存、单核或资源受限环境。

启动参数：

```bash
-XX:+UseSerialGC
```

---

## 3. ParNew GC

ParNew 是 Serial 的多线程版本。

特点：

1. 多线程回收年轻代；
2. 常与 CMS 搭配使用；
3. JDK 8 中较常见；
4. 现在新项目中较少主动选择。

---

## 4. Parallel GC

Parallel GC 也叫吞吐量优先收集器。

特点：

1. 多线程垃圾回收；
2. 关注吞吐量；
3. 适合后台计算、批处理任务；
4. 停顿时间不一定最低。

启动参数：

```bash
-XX:+UseParallelGC
```

---

## 5. CMS GC

CMS 全称：

```text
Concurrent Mark Sweep
```

即并发标记清除收集器。

特点：

1. 以低停顿为目标；
2. 主要回收老年代；
3. 使用标记-清除算法；
4. 会产生内存碎片；
5. 对 CPU 资源敏感；
6. 可能出现 Concurrent Mode Failure；
7. 已在 JDK 14 被移除。

---

## 6. G1 GC

G1 全称：

```text
Garbage First
```

特点：

1. 面向服务端应用；
2. 低停顿；
3. 可预测停顿时间；
4. 将堆划分为多个 Region；
5. 支持年轻代回收和混合回收；
6. 整体使用标记-整理思想，局部使用复制算法；
7. JDK 9 以后成为默认垃圾回收器。

启动参数：

```bash
-XX:+UseG1GC
```

---

## 7. ZGC

ZGC 是低延迟垃圾回收器。

特点：

1. 超低停顿；
2. 支持大堆；
3. 大部分工作并发执行；
4. 适合对延迟敏感的服务；
5. 对 CPU 和内存资源要求相对更高。

启动参数：

```bash
-XX:+UseZGC
```

---

## 8. Shenandoah GC

Shenandoah 也是低停顿垃圾回收器。

特点：

1. 低暂停时间；
2. 并发整理；
3. 停顿时间与堆大小关系较弱；
4. 在部分 OpenJDK 发行版中可用。

启动参数：

```bash
-XX:+UseShenandoahGC
```

---

## 9. Epsilon GC

Epsilon 是无操作垃圾回收器。

特点：

1. 只分配内存，不回收内存；
2. 适合性能测试、内存压力测试、短生命周期程序；
3. 生产环境一般不使用。

启动参数：

```bash
-XX:+UseEpsilonGC
```

---

## 10. 常见垃圾回收器对比

| 垃圾回收器 | 目标 | 特点 | 适用场景 |
|---|---|---|---|
| Serial | 简单、低资源 | 单线程，STW | 小应用、客户端 |
| ParNew | 年轻代多线程 | 常配 CMS | JDK 8 老系统 |
| Parallel | 吞吐量优先 | 多线程，高吞吐 | 批处理、后台任务 |
| CMS | 低停顿 | 并发标记清除，会碎片化 | JDK 8 老系统 |
| G1 | 可预测停顿 | Region、混合回收 | 服务端应用 |
| ZGC | 超低延迟 | 并发、低停顿、大堆 | 低延迟服务 |
| Shenandoah | 低延迟 | 并发整理 | 低停顿场景 |
| Epsilon | 不回收 | 只分配不回收 | 测试场景 |

---

## 11. 面试回答

常见垃圾回收器有 Serial、ParNew、Parallel、CMS、G1、ZGC、Shenandoah 和 Epsilon。

Serial 是单线程收集器；Parallel 关注吞吐量；CMS 是低停顿的老年代收集器，但会产生内存碎片，并且已经在 JDK 14 被移除；G1 是面向服务端的低停顿收集器，使用 Region 管理堆；ZGC 和 Shenandoah 都是低延迟垃圾回收器，适合对停顿时间要求很高的场景。

---

# 八、CMS 和 G1 区别

## 1. CMS 是什么？

CMS 是 JDK 8 时代常见的老年代低停顿垃圾回收器。

全称：

```text
Concurrent Mark Sweep
```

它的核心目标是：

```text
尽量减少用户线程停顿时间
```

---

## 2. CMS 回收过程

CMS 主要分为四个阶段：

```text
1. 初始标记
2. 并发标记
3. 重新标记
4. 并发清除
```

---

### 2.1 初始标记

标记 GC Roots 能直接关联到的对象。

这个阶段需要 Stop The World，但时间较短。

---

### 2.2 并发标记

从 GC Roots 直接关联对象开始遍历整个对象图。

这个阶段可以和用户线程并发执行。

---

### 2.3 重新标记

修正并发标记期间因用户线程继续运行而导致的标记变动。

这个阶段需要 Stop The World。

---

### 2.4 并发清除

清除不可达对象。

这个阶段可以和用户线程并发执行。

---

## 3. CMS 的问题

CMS 主要问题包括：

1. 使用标记-清除算法，会产生内存碎片；
2. 对 CPU 资源敏感；
3. 无法处理浮动垃圾；
4. 可能出现 Concurrent Mode Failure；
5. Full GC 代价较高；
6. 已经从 JDK 14 移除。

---

## 4. G1 是什么？

G1 是面向服务端应用的垃圾回收器。

G1 全称：

```text
Garbage First
```

它的核心目标是：

```text
在可控停顿时间内，优先回收垃圾最多的 Region
```

---

## 5. G1 的 Region 结构

G1 不再将堆简单划分为连续的新生代和老年代，而是将堆划分为多个大小相等的 Region。

每个 Region 可以扮演不同角色：

```text
Eden Region
Survivor Region
Old Region
Humongous Region
```

---

## 6. G1 回收方式

G1 常见回收类型：

```text
1. Young GC
2. Mixed GC
3. Full GC
```

Mixed GC 不只回收年轻代，也会回收部分老年代 Region。

---

## 7. CMS 和 G1 对比

| 对比项 | CMS | G1 |
|---|---|---|
| 出现背景 | JDK 8 常用低停顿收集器 | JDK 9 后默认收集器 |
| 堆布局 | 年轻代 + 老年代连续空间 | Region 分区 |
| 回收目标 | 降低老年代停顿 | 可预测停顿、整体回收效率 |
| 算法 | 标记-清除 | 标记-整理 + 复制 |
| 是否整理内存 | 默认不整理 | 会进行整理/复制 |
| 内存碎片 | 容易产生碎片 | 碎片问题较少 |
| 回收范围 | 主要老年代 | 年轻代 + 部分老年代 |
| 停顿控制 | 较弱 | 支持暂停时间目标 |
| 并发能力 | 并发标记、并发清除 | 并发标记、并行回收 |
| JDK 状态 | JDK 14 已移除 | 主流服务端 GC |

---

## 8. G1 为什么比 CMS 更适合大堆？

原因包括：

1. G1 使用 Region 管理堆；
2. 可以优先回收垃圾比例高的 Region；
3. 支持 Mixed GC；
4. 可以设置停顿时间目标；
5. 不容易产生严重内存碎片；
6. 在大堆场景下更容易控制停顿。

---

## 9. 面试回答

CMS 是一种以低停顿为目标的老年代垃圾回收器，主要使用标记-清除算法，回收过程包括初始标记、并发标记、重新标记和并发清除。它的问题是会产生内存碎片，对 CPU 敏感，并且可能出现 Concurrent Mode Failure。

G1 是面向服务端应用的垃圾回收器，它将堆划分为多个 Region，可以优先回收垃圾最多的 Region，并支持可预测停顿时间。G1 使用复制和整理算法，碎片问题比 CMS 少，并且可以进行年轻代回收和混合回收。

---

# 九、如何排查 OOM

## 1. OOM 是什么？

OOM 全称：

```text
OutOfMemoryError
```

表示 JVM 或进程内存不足，无法继续分配内存。

常见 OOM 类型：

```text
java.lang.OutOfMemoryError: Java heap space
java.lang.OutOfMemoryError: GC overhead limit exceeded
java.lang.OutOfMemoryError: Metaspace
java.lang.OutOfMemoryError: Direct buffer memory
java.lang.OutOfMemoryError: unable to create new native thread
```

---

## 2. OOM 排查整体思路

OOM 排查核心流程：

```text
1. 确认 OOM 类型
2. 保留现场
3. 获取 dump 文件
4. 分析对象占用
5. 找到引用链
6. 定位代码
7. 修复问题
8. 验证和监控
```

---

## 3. 第一步：确认 OOM 类型

不同 OOM 对应不同方向。

| OOM 类型 | 常见原因 |
|---|---|
| Java heap space | 堆内存不足、对象泄漏、大对象过多 |
| GC overhead limit exceeded | GC 频繁但回收效果差 |
| Metaspace | 类加载过多、动态代理过多、类加载器泄漏 |
| Direct buffer memory | 直接内存不足、NIO 使用不当 |
| unable to create new native thread | 线程数过多、系统资源不足 |

---

## 4. 第二步：配置自动 Dump

建议在线上配置：

```bash
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/data/dump/heap.hprof
```

这样发生 OOM 时，JVM 会自动生成堆转储文件。

---

## 5. 第三步：查看 JVM 参数

使用：

```bash
jcmd <pid> VM.flags
```

或：

```bash
jinfo -flags <pid>
```

重点关注：

```text
-Xms
-Xmx
-Xss
-XX:MaxMetaspaceSize
-XX:MaxDirectMemorySize
GC 类型
```

---

## 6. 第四步：查看 GC 日志

JDK 8 常见配置：

```bash
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-Xloggc:/data/logs/gc.log
```

JDK 9+ 推荐：

```bash
-Xlog:gc*:file=/data/logs/gc.log:time,uptime,level,tags
```

重点关注：

1. Full GC 是否频繁；
2. Full GC 后内存是否下降；
3. 老年代是否持续上涨；
4. 元空间是否持续上涨；
5. 单次 GC 耗时是否异常。

---

## 7. 第五步：分析 Dump 文件

常用工具：

```text
MAT
VisualVM
JProfiler
YourKit
jhat，不推荐线上大文件使用
```

重点看：

1. Dominator Tree；
2. Leak Suspects；
3. Histogram；
4. Retained Heap；
5. GC Roots 引用链。

---

## 8. 第六步：常用命令

### 查看堆使用情况

```bash
jmap -heap <pid>
```

### 查看对象直方图

```bash
jmap -histo:live <pid> | head -n 30
```

### 生成堆 Dump

```bash
jmap -dump:format=b,file=heap.hprof <pid>
```

### 使用 jcmd 生成 Dump

```bash
jcmd <pid> GC.heap_dump /data/dump/heap.hprof
```

---

## 9. Java heap space 排查

重点看：

```text
哪些对象占用内存最多？
这些对象为什么没有被回收？
这些对象被谁引用？
是否存在集合无限增长？
是否存在缓存没有淘汰？
是否存在大对象频繁创建？
```

常见问题：

1. `List`、`Map` 无限增长；
2. 本地缓存无淘汰策略；
3. 查询一次性加载大量数据；
4. MQ 消费堆积；
5. 文件导入导出占用大内存；
6. ThreadLocal 未 remove；
7. 静态集合持有大量对象。

---

## 10. Metaspace OOM 排查

常见原因：

1. 动态生成类过多；
2. CGLIB 动态代理过多；
3. Groovy、JSP、反射框架动态类过多；
4. 类加载器泄漏；
5. 热部署导致 ClassLoader 无法回收。

排查重点：

```text
类数量是否持续增长？
ClassLoader 是否无法释放？
是否存在频繁动态生成类？
```

---

## 11. Direct buffer memory 排查

常见原因：

1. NIO 直接内存使用过多；
2. Netty ByteBuf 未释放；
3. DirectByteBuffer 创建过多；
4. `MaxDirectMemorySize` 设置过小。

排查方向：

```text
检查 Netty 是否存在 ByteBuf 泄漏；
检查直接内存限制；
检查是否频繁 allocateDirect；
```

---

## 12. unable to create new native thread 排查

常见原因：

1. 创建线程过多；
2. 线程池参数不合理；
3. 每个线程栈太大；
4. 操作系统线程数限制；
5. 容器资源限制。

排查命令：

```bash
jstack <pid> | grep "java.lang.Thread.State" | wc -l
```

查看系统限制：

```bash
ulimit -u
```

---

## 13. 面试回答

排查 OOM 首先要确认 OOM 类型，比如堆 OOM、元空间 OOM、直接内存 OOM 或无法创建线程。

如果是堆 OOM，需要保留现场，获取 heap dump，结合 GC 日志分析。通过 MAT 等工具查看哪些对象占用内存最多、对象被谁引用、是否存在无法释放的引用链。

如果是 Metaspace OOM，要重点排查动态类生成和类加载器泄漏。如果是 Direct Memory OOM，要排查 NIO、Netty 或直接内存配置。如果是无法创建线程，要检查线程数量、线程池参数和系统资源限制。

---

# 十、如何排查 CPU 飙高

## 1. CPU 飙高常见原因

CPU 飙高常见原因包括：

1. 死循环；
2. 频繁 GC；
3. 线程池任务过多；
4. 锁竞争激烈；
5. 正则回溯严重；
6. 大量计算任务；
7. 序列化/反序列化开销大；
8. 日志打印过多；
9. 自旋等待；
10. 代码 Bug。

---

## 2. 排查整体流程

```text
1. top 找到 CPU 高的 Java 进程
2. top -Hp 找到进程中 CPU 高的线程
3. 将线程 ID 转为十六进制
4. jstack 导出线程栈
5. 根据 nid 找到对应线程
6. 分析线程正在执行的代码
7. 结合日志、监控、GC 日志定位原因
```

---

## 3. 第一步：找到 CPU 高的 Java 进程

```bash
top
```

找到 CPU 占用高的 Java 进程 PID。

---

## 4. 第二步：找到 CPU 高的线程

```bash
top -Hp <pid>
```

可以看到该进程下每个线程的 CPU 占用。

---

## 5. 第三步：线程 ID 转十六进制

假设线程 ID 是：

```text
12345
```

转换成十六进制：

```bash
printf "%x\n" 12345
```

输出：

```text
3039
```

---

## 6. 第四步：导出线程栈

```bash
jstack <pid> > thread.txt
```

或者：

```bash
jcmd <pid> Thread.print > thread.txt
```

---

## 7. 第五步：根据 nid 搜索线程

在线程栈中搜索：

```text
nid=0x3039
```

找到该线程正在执行的代码位置。

---

## 8. 第六步：分析线程状态

常见线程状态：

| 状态 | 含义 |
|---|---|
| RUNNABLE | 正在运行或等待 CPU |
| BLOCKED | 等待锁 |
| WAITING | 无限期等待 |
| TIMED_WAITING | 超时等待 |
| NEW | 新建状态 |
| TERMINATED | 已结束 |

CPU 飙高时重点关注：

```text
RUNNABLE 状态线程
```

---

## 9. 判断是否 GC 导致 CPU 高

查看 GC 情况：

```bash
jstat -gcutil <pid> 1000 10
```

如果发现：

```text
Full GC 频繁
Old 区使用率很高
GC 时间占比很高
```

可能是频繁 GC 导致 CPU 飙高。

---

## 10. 常见定位场景

### 10.1 死循环

线程栈中一直停留在某个业务方法。

示例：

```java
while (true) {
    // 没有退出条件
}
```

---

### 10.2 正则回溯

线程栈中出现大量正则相关方法：

```text
java.util.regex.Pattern
java.util.regex.Matcher
```

可能是复杂正则导致 CPU 高。

---

### 10.3 锁竞争

大量线程处于：

```text
BLOCKED
```

说明可能有锁竞争。

---

### 10.4 频繁 GC

如果 GC 线程占用 CPU 高，且 GC 日志显示 Full GC 频繁，说明可能是内存不足或对象创建过快。

---

## 11. Arthas 排查

可以使用 Arthas 辅助排查。

查看最忙线程：

```bash
thread -n 5
```

查看线程栈：

```bash
thread <threadId>
```

查看方法耗时：

```bash
trace com.xxx.Service methodName
```

查看方法调用情况：

```bash
watch com.xxx.Service methodName '{params, returnObj}' -x 2
```

---

## 12. 面试回答

排查 CPU 飙高，首先用 `top` 找到 CPU 高的 Java 进程，再用 `top -Hp pid` 找到进程中 CPU 高的线程。然后把线程 ID 转成十六进制，用 `jstack` 导出线程栈，根据 `nid` 找到对应线程，查看它正在执行的代码。

如果线程一直处于 RUNNABLE，可能是死循环、复杂计算或正则回溯。如果大量线程 BLOCKED，可能是锁竞争。如果 GC 线程占用高，需要结合 `jstat` 和 GC 日志判断是否频繁 GC。

---

# 十一、类加载机制是什么

## 1. 类加载是什么？

类加载是指 JVM 将 `.class` 字节码文件加载到内存，并对类数据进行校验、转换、解析和初始化，最终形成可以被 JVM 使用的 Class 对象的过程。

---

## 2. 类加载过程

类加载过程包括五个阶段：

```text
加载 -> 验证 -> 准备 -> 解析 -> 初始化
```

其中：

```text
验证、准备、解析 统称为连接 Linking
```

完整过程：

```text
加载 Loading
   |
   v
连接 Linking
   ├── 验证 Verification
   ├── 准备 Preparation
   └── 解析 Resolution
   |
   v
初始化 Initialization
```

---

## 3. 加载

加载阶段主要做三件事：

```text
1. 通过类的全限定名获取二进制字节流
2. 将字节流转换为方法区中的运行时数据结构
3. 在堆中生成一个 java.lang.Class 对象
```

例如：

```java
Class<?> clazz = Class.forName("com.example.User");
```

---

## 4. 验证

验证阶段用于保证 class 文件的字节流符合 JVM 规范，不会危害 JVM 安全。

验证内容包括：

1. 文件格式验证；
2. 元数据验证；
3. 字节码验证；
4. 符号引用验证。

---

## 5. 准备

准备阶段会为类变量分配内存，并设置默认初始值。

注意：

> 这里是给静态变量设置默认值，不是代码中写的最终值。

示例：

```java
public static int count = 10;
```

准备阶段：

```text
count = 0
```

初始化阶段：

```text
count = 10
```

---

## 6. 特殊情况：static final 常量

如果是编译期常量：

```java
public static final int COUNT = 10;
```

在准备阶段可能就会直接赋值为 10。

---

## 7. 解析

解析阶段会将常量池中的符号引用转换为直接引用。

例如：

```text
符号引用：com.example.User
直接引用：内存地址或句柄
```

---

## 8. 初始化

初始化阶段才真正执行类中的 Java 代码。

主要执行：

```text
类构造器 <clinit>() 方法
```

`<clinit>()` 由以下内容合并生成：

1. 静态变量赋值语句；
2. 静态代码块。

示例：

```java
public class User {
    public static int count = 10;

    static {
        System.out.println("User 初始化");
    }
}
```

---

## 9. 类初始化触发时机

类初始化常见触发时机包括：

1. 创建类实例；
2. 访问类的静态变量；
3. 调用类的静态方法；
4. 使用反射调用类；
5. 初始化子类时，父类会先初始化；
6. JVM 启动时包含 `main()` 方法的主类会被初始化。

---

## 10. 不会触发初始化的情况

以下情况通常不会触发类初始化：

### 10.1 通过子类引用父类静态字段

```java
System.out.println(Child.parentValue);
```

如果 `parentValue` 定义在父类中，只会初始化父类，不会初始化子类。

---

### 10.2 创建对象数组

```java
User[] users = new User[10];
```

只创建数组对象，不会初始化 `User` 类。

---

### 10.3 访问编译期常量

```java
public static final String NAME = "Tom";
```

访问编译期常量通常不会触发类初始化，因为常量在编译期已经进入调用类的常量池。

---

## 11. 类加载器有哪些？

常见类加载器：

| 类加载器 | 作用 |
|---|---|
| Bootstrap ClassLoader | 加载 Java 核心类库 |
| Platform ClassLoader | 加载平台相关类库，JDK 9+ |
| Extension ClassLoader | 加载扩展类库，JDK 8 |
| Application ClassLoader | 加载应用 classpath 下的类 |
| Custom ClassLoader | 自定义类加载器 |

JDK 9 以后模块化系统引入后，平台类加载器取代了 JDK 8 中的扩展类加载器概念。

---

## 12. 面试回答

类加载机制是 JVM 将 class 字节码加载到内存，并经过验证、准备、解析和初始化，最终形成 JVM 可以使用的 Class 对象的过程。

类加载过程包括加载、验证、准备、解析、初始化五个阶段。其中验证、准备、解析统称为连接。

加载阶段读取 class 字节流；验证阶段确保字节码安全；准备阶段为静态变量分配内存并设置默认值；解析阶段将符号引用转换为直接引用；初始化阶段执行静态变量赋值和静态代码块。

---

# 十二、双亲委派模型是什么

## 1. 什么是双亲委派模型？

双亲委派模型是 Java 类加载器的一种类加载机制。

它的核心思想是：

> 一个类加载器收到类加载请求后，先把请求委托给父类加载器加载，父类加载器无法加载时，子类加载器才会尝试自己加载。

---

## 2. 类加载器层次结构

典型层次结构：

```text
Bootstrap ClassLoader
        ↑
Platform ClassLoader / Extension ClassLoader
        ↑
Application ClassLoader
        ↑
Custom ClassLoader
```

JDK 8 中通常是：

```text
Bootstrap ClassLoader
        ↑
Extension ClassLoader
        ↑
Application ClassLoader
        ↑
Custom ClassLoader
```

JDK 9+ 中通常是：

```text
Bootstrap ClassLoader
        ↑
Platform ClassLoader
        ↑
Application ClassLoader
        ↑
Custom ClassLoader
```

---

## 3. 双亲委派加载流程

加载一个类时，流程大致如下：

```text
1. 当前 ClassLoader 收到类加载请求
2. 先检查这个类是否已经被加载
3. 如果没有加载，则委托父类加载器加载
4. 父类加载器继续向上委托
5. 直到 Bootstrap ClassLoader
6. 如果父类加载器无法加载
7. 子类加载器才尝试自己加载
```

---

## 4. loadClass 流程

伪代码：

```java
protected Class<?> loadClass(String name, boolean resolve) {
    // 1. 检查类是否已经加载
    Class<?> c = findLoadedClass(name);

    if (c == null) {
        try {
            // 2. 委托父类加载器
            if (parent != null) {
                c = parent.loadClass(name, false);
            } else {
                c = findBootstrapClassOrNull(name);
            }
        } catch (ClassNotFoundException e) {
            // 父类加载器加载失败
        }

        if (c == null) {
            // 3. 当前类加载器自己加载
            c = findClass(name);
        }
    }

    return c;
}
```

---

## 5. 为什么需要双亲委派？

双亲委派的作用包括：

```text
1. 避免类重复加载
2. 保护 Java 核心类库安全
3. 保证类加载的一致性
```

---

## 6. 避免核心类被篡改

例如用户自己定义一个类：

```java
package java.lang;

public class String {
}
```

按照双亲委派模型，加载 `java.lang.String` 时会先交给 Bootstrap ClassLoader。

Bootstrap ClassLoader 会加载 JDK 自带的 `java.lang.String`，而不是加载用户自定义的 String。

这样可以防止核心类库被篡改。

---

## 7. 类的唯一性

在 JVM 中，判断两个类是否相同，不仅看类的全限定名，还要看加载它们的类加载器是否相同。

也就是说：

```text
类唯一性 = 类全限定名 + 类加载器
```

即使两个类的包名和类名完全一样，如果由不同类加载器加载，也会被认为是不同的类。

---

## 8. 双亲委派是否可以打破？

可以。

常见打破双亲委派的场景：

1. 自定义类加载器重写 `loadClass()`；
2. SPI 机制；
3. JDBC 驱动加载；
4. Tomcat WebAppClassLoader；
5. OSGi 模块化；
6. 热部署框架。

---

## 9. 为什么 Tomcat 要打破双亲委派？

Tomcat 中不同 Web 应用可能依赖不同版本的同一个类库。

例如：

```text
应用 A 使用 Spring 5
应用 B 使用 Spring 6
```

如果完全遵守双亲委派，类可能被父加载器统一加载，无法实现应用隔离。

所以 Tomcat 的 WebAppClassLoader 会优先加载 Web 应用自己的类，从而实现不同应用之间的类隔离。

---

## 10. 面试回答

双亲委派模型是指类加载器收到类加载请求后，不会先自己加载，而是先委托父类加载器加载。父类加载器无法加载时，子类加载器才会尝试自己加载。

它的好处是可以避免类重复加载，保证 Java 核心类库安全，并保证类加载的一致性。

例如 `java.lang.String` 会优先由 Bootstrap ClassLoader 加载，避免用户自定义同名类替换 JDK 核心类。

不过双亲委派也可以被打破，例如 Tomcat 为了实现不同 Web 应用之间的类隔离，会采用自己的类加载机制。

---

# 十三、面试速记版

## 1. JVM 内存结构

JVM 内存分为线程共享和线程私有。

线程共享包括：

```text
堆、方法区
```

线程私有包括：

```text
程序计数器、Java 虚拟机栈、本地方法栈
```

---

## 2. 堆和栈区别

堆是线程共享的，主要存放对象实例和数组，由 GC 管理。

栈是线程私有的，主要存放方法调用产生的栈帧，包括局部变量表、操作数栈、动态链接和返回地址。

---

## 3. 对象创建过程

对象创建过程：

```text
类加载检查 -> 分配内存 -> 初始化零值 -> 设置对象头 -> 执行构造方法 -> 引用指向对象
```

---

## 4. 对象什么时候进入老年代

对象进入老年代常见情况：

```text
年龄达到阈值
大对象直接进入老年代
动态年龄判断
Survivor 空间不足
```

---

## 5. GC Roots 有哪些

常见 GC Roots：

```text
虚拟机栈引用的对象
静态变量引用的对象
常量引用的对象
JNI 引用的对象
被 synchronized 持有的对象
JVM 内部引用
活跃线程对象
```

---

## 6. Minor GC、Major GC、Full GC 区别

```text
Minor GC：回收年轻代
Major GC：通常指回收老年代
Full GC：回收整个堆，可能包含方法区/元空间
```

---

## 7. 常见垃圾回收器

常见 GC：

```text
Serial
ParNew
Parallel
CMS
G1
ZGC
Shenandoah
Epsilon
```

---

## 8. CMS 和 G1 区别

CMS 基于标记-清除，低停顿，但会产生内存碎片，JDK 14 已移除。

G1 基于 Region，支持可预测停顿，能进行年轻代回收和混合回收，碎片问题更少。

---

## 9. 如何排查 OOM

排查 OOM：

```text
确认 OOM 类型
查看 GC 日志
获取 heap dump
用 MAT 分析对象占用和引用链
定位代码
修复并验证
```

---

## 10. 如何排查 CPU 飙高

排查 CPU 飙高：

```bash
top
top -Hp <pid>
printf "%x\n" <tid>
jstack <pid>
搜索 nid
```

重点看高 CPU 线程正在执行什么代码。

---

## 11. 类加载机制

类加载过程：

```text
加载 -> 验证 -> 准备 -> 解析 -> 初始化
```

验证、准备、解析合称连接。

---

## 12. 双亲委派模型

双亲委派模型是：

```text
类加载请求先委托父加载器，父加载器无法加载时，子加载器才自己加载。
```

作用：

```text
避免重复加载
保护核心类库
保证类加载一致性
```

---

# 十四、总览表

| 问题 | 核心结论 |
|---|---|
| JVM 内存结构 | 堆、方法区线程共享；程序计数器、虚拟机栈、本地方法栈线程私有 |
| 堆和栈区别 | 堆存对象实例，线程共享；栈存栈帧，线程私有 |
| 对象创建过程 | 类加载检查、分配内存、初始化零值、设置对象头、执行构造方法 |
| 对象什么时候进入老年代 | 年龄达到阈值、大对象、动态年龄判断、Survivor 放不下 |
| GC Roots 有哪些 | 栈引用、静态变量、常量、JNI 引用、锁对象、JVM 内部引用等 |
| Minor GC、Major GC、Full GC 区别 | Minor 回收年轻代；Major 通常回收老年代；Full 回收整个堆 |
| 常见垃圾回收器 | Serial、ParNew、Parallel、CMS、G1、ZGC、Shenandoah、Epsilon |
| CMS 和 G1 区别 | CMS 标记清除会碎片化；G1 Region 化、可预测停顿 |
| 如何排查 OOM | 看类型、GC 日志、dump、MAT 分析引用链 |
| 如何排查 CPU 飙高 | top 找进程，top -Hp 找线程，jstack 定位代码 |
| 类加载机制 | 加载、验证、准备、解析、初始化 |
| 双亲委派模型 | 先父加载器，后子加载器，保护核心类库 |

---

# 十五、完整面试回答模板

JVM 内存结构主要分为线程共享区域和线程私有区域。线程共享区域包括堆和方法区，堆主要存放对象实例和数组，是垃圾回收的主要区域；方法区主要存放类信息、常量、静态变量和 JIT 编译后的代码。线程私有区域包括程序计数器、Java 虚拟机栈和本地方法栈。

堆和栈的区别是，堆是线程共享的，主要存放对象实例，由 GC 管理；栈是线程私有的，主要存放方法调用产生的栈帧，包括局部变量表、操作数栈、动态链接和方法返回地址。

对象创建过程大致是：JVM 遇到 `new` 指令后，先检查类是否已经加载，如果没有加载则先进行类加载。然后在堆中为对象分配内存，初始化零值，设置对象头，最后执行构造方法，将引用指向创建好的对象。

对象进入老年代主要有几种情况：对象年龄达到晋升阈值，大对象直接进入老年代，动态年龄判断导致提前晋升，或者 Minor GC 后 Survivor 区放不下存活对象。

GC Roots 是可达性分析的起点，常见 GC Roots 包括虚拟机栈中引用的对象、静态变量引用的对象、常量引用的对象、本地方法栈中 JNI 引用的对象、被锁持有的对象和 JVM 内部引用等。

Minor GC 主要回收年轻代，通常在 Eden 区空间不足时触发。Major GC 一般指老年代 GC，但不同场景下含义可能不同。Full GC 通常回收整个堆，并可能涉及方法区或元空间，停顿时间较长。

常见垃圾回收器有 Serial、ParNew、Parallel、CMS、G1、ZGC、Shenandoah 和 Epsilon。CMS 是低停顿的老年代收集器，使用标记-清除算法，会产生内存碎片，并且已经在 JDK 14 被移除。G1 使用 Region 管理堆，可以优先回收垃圾最多的 Region，并支持可预测停顿时间。

排查 OOM 时，首先确认 OOM 类型，然后查看 GC 日志和 JVM 参数，获取 heap dump，使用 MAT 等工具分析对象占用和 GC Roots 引用链，最终定位到代码中的内存泄漏、大对象分配或资源未释放问题。

排查 CPU 飙高时，先用 `top` 找到 CPU 高的 Java 进程，再用 `top -Hp pid` 找到高 CPU 线程，将线程 ID 转为十六进制，然后用 `jstack` 导出线程栈，通过 `nid` 找到对应线程，分析它正在执行的代码。如果是 GC 导致 CPU 高，还需要结合 `jstat` 和 GC 日志分析。

类加载机制包括加载、验证、准备、解析和初始化五个阶段。其中验证、准备、解析属于连接。加载阶段读取 class 字节流，验证阶段保证字节码安全，准备阶段为静态变量分配内存并设置默认值，解析阶段将符号引用转换为直接引用，初始化阶段执行静态变量赋值和静态代码块。

双亲委派模型是指类加载器收到类加载请求后，先委托父类加载器加载，父类加载器无法加载时，子类加载器才会尝试自己加载。它可以避免类重复加载，保护 Java 核心类库，并保证类加载的一致性。例如 `java.lang.String` 会优先由 Bootstrap ClassLoader 加载，避免被用户自定义的同名类替换。
