# Class.forName 和 ClassLoader 的区别

## 1. 核心区别

| 对比项 | `Class.forName()` | `ClassLoader.loadClass()` |
|---|---|---|
| 所属类 | `java.lang.Class` | `java.lang.ClassLoader` |
| 主要作用 | 加载类，并默认初始化类 | 加载类，默认不初始化类 |
| 是否执行静态代码块 | 默认会执行 | 默认不会执行 |
| 是否触发类初始化 | 会 | 不会 |
| 常见用途 | JDBC 驱动加载、主动触发类初始化 | 框架类加载、延迟加载、自定义类加载 |
| 返回值 | `Class<?>` | `Class<?>` |

---

## 2. Class.forName 是什么

`Class.forName()` 用于根据类的全限定名加载类。

示例：

```java
Class<?> clazz = Class.forName("com.example.User");
```

默认情况下，`Class.forName()` 不只是加载类，还会触发类初始化。

也就是说，它会执行：

```java
static {
    System.out.println("静态代码块执行");
}
```

---

## 3. ClassLoader.loadClass 是什么

`ClassLoader.loadClass()` 是通过类加载器加载类。

示例：

```java
ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

Class<?> clazz = classLoader.loadClass("com.example.User");
```

默认情况下，`loadClass()` 只完成类加载，不会触发类初始化。

也就是说，它不会执行静态代码块。

---

## 4. 示例验证

### User 类

```java
public class User {

    static {
        System.out.println("User 静态代码块执行");
    }

    public User() {
        System.out.println("User 构造方法执行");
    }
}
```

---

### 使用 Class.forName

```java
public class Test {
    public static void main(String[] args) throws Exception {
        Class.forName("com.example.User");
    }
}
```

输出：

```text
User 静态代码块执行
```

说明：

```text
Class.forName 默认会触发类初始化
```

---

### 使用 ClassLoader.loadClass

```java
public class Test {
    public static void main(String[] args) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        classLoader.loadClass("com.example.User");
    }
}
```

输出：

```text
无输出
```

说明：

```text
ClassLoader.loadClass 默认只加载类，不初始化类
```

---

## 5. 类加载过程

Java 类加载大致分为：

```text
加载 Loading
   ↓
验证 Verification
   ↓
准备 Preparation
   ↓
解析 Resolution
   ↓
初始化 Initialization
```

其中：

```text
验证、准备、解析 合称为连接 Linking
```

完整流程：

```text
加载 -> 连接 -> 初始化
```

---

## 6. 两者对应阶段

| 方法 | 执行阶段 |
|---|---|
| `ClassLoader.loadClass()` | 加载阶段 |
| `Class.forName()` | 加载 + 连接 + 初始化 |

简单理解：

```text
ClassLoader.loadClass：把 class 加载进 JVM，但不主动初始化
Class.forName：把 class 加载进 JVM，并默认初始化
```

---

## 7. Class.forName 的重载方法

`Class.forName()` 有一个重载方法：

```java
Class.forName(String className, boolean initialize, ClassLoader loader)
```

参数说明：

| 参数 | 说明 |
|---|---|
| `className` | 类全限定名 |
| `initialize` | 是否初始化类 |
| `loader` | 指定类加载器 |

---

### 不初始化类

```java
Class<?> clazz = Class.forName(
        "com.example.User",
        false,
        Thread.currentThread().getContextClassLoader()
);
```

这样不会执行静态代码块。

---

### 初始化类

```java
Class<?> clazz = Class.forName(
        "com.example.User",
        true,
        Thread.currentThread().getContextClassLoader()
);
```

这样会执行静态代码块。

---

## 8. Class.forName 默认等价于什么

下面这个：

```java
Class.forName("com.example.User");
```

大致等价于：

```java
Class.forName(
        "com.example.User",
        true,
        当前类的类加载器
);
```

重点是：

```text
initialize = true
```

所以默认会初始化类。

---

## 9. ClassLoader.loadClass 默认等价于什么

```java
classLoader.loadClass("com.example.User");
```

默认不会初始化类。

底层类似：

```java
loadClass(name, false);
```

重点是：

```text
resolve = false
```

它只负责加载类，不会主动执行初始化。

---

## 10. 什么是类初始化

类初始化主要做这些事：

```text
执行 static 变量赋值
执行 static 静态代码块
```

例如：

```java
public class User {

    private static String name = initName();

    static {
        System.out.println("静态代码块");
    }

    private static String initName() {
        System.out.println("静态变量初始化");
        return "Tom";
    }
}
```

类初始化时会执行：

```text
静态变量初始化
静态代码块
```

---

## 11. 什么时候会触发类初始化

除了 `Class.forName()`，这些情况也会触发类初始化：

```text
new 创建对象
访问类的静态变量
调用类的静态方法
反射调用类方法
初始化子类时先初始化父类
main 方法所在类启动
```

示例：

```java
new User();
```

会触发初始化。

```java
User.name;
```

如果 `name` 是非 final 静态变量，也会触发初始化。

```java
User.staticMethod();
```

调用静态方法也会触发初始化。

---

## 12. JDBC 中为什么以前常用 Class.forName

以前 JDBC 常见写法：

```java
Class.forName("com.mysql.jdbc.Driver");

Connection connection = DriverManager.getConnection(
        "jdbc:mysql://localhost:3306/test",
        "root",
        "123456"
);
```

原因是：

```text
Class.forName 会加载并初始化 Driver 类
Driver 类的静态代码块会把驱动注册到 DriverManager
```

类似：

```java
static {
    DriverManager.registerDriver(new Driver());
}
```

所以 `Class.forName()` 可以触发驱动注册。

---

## 13. 为什么 ClassLoader.loadClass 不适合这个场景

如果使用：

```java
ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
classLoader.loadClass("com.mysql.jdbc.Driver");
```

默认只加载类，不初始化类。

也就是说：

```text
Driver 的 static 代码块不会执行
驱动不会注册到 DriverManager
```

所以早期 JDBC 使用 `Class.forName()`，而不是 `ClassLoader.loadClass()`。

---

## 14. 现在 JDBC 还需要 Class.forName 吗

通常不需要。

JDBC 4.0 以后支持 SPI 自动加载驱动。

只要驱动 jar 包里有：

```text
META-INF/services/java.sql.Driver
```

`DriverManager` 就可以自动发现驱动。

所以现在通常可以直接写：

```java
Connection connection = DriverManager.getConnection(url, username, password);
```

不过有些老项目或特殊环境中，仍然可能看到：

```java
Class.forName("com.mysql.cj.jdbc.Driver");
```

---

## 15. ClassLoader 的作用更底层

`ClassLoader` 是类加载机制的核心。

它负责：

```text
根据类名查找 class 文件
读取 class 字节码
把字节码加载到 JVM
生成 Class 对象
```

常见类加载器：

| 类加载器 | 说明 |
|---|---|
| Bootstrap ClassLoader | 启动类加载器，加载 JDK 核心类 |
| Extension ClassLoader | 扩展类加载器，JDK 8 中常见 |
| Platform ClassLoader | 平台类加载器，JDK 9+ |
| Application ClassLoader | 应用类加载器，加载 classpath 下的类 |
| Custom ClassLoader | 自定义类加载器 |

---

## 16. Class.forName 本质上也依赖 ClassLoader

`Class.forName()` 不是自己直接读取 class 文件。

它底层仍然需要通过类加载器加载类。

也就是说：

```text
Class.forName 是更上层的便捷入口
ClassLoader 是真正负责类加载的组件
```

可以简单理解：

```text
Class.forName = 使用某个 ClassLoader 加载类 + 可选择是否初始化
```

---

## 17. 什么时候用 Class.forName

适合场景：

```text
需要根据类名动态加载类
需要触发类初始化
需要执行 static 静态代码块
JDBC 旧式驱动注册
简单反射创建对象
```

示例：

```java
Class<?> clazz = Class.forName("com.example.User");

Object obj = clazz.getDeclaredConstructor().newInstance();
```

---

## 18. 什么时候用 ClassLoader.loadClass

适合场景：

```text
只想加载类，不想立即初始化
框架做延迟加载
需要自定义类加载器
插件化系统
热部署
隔离不同版本 jar
容器加载类
```

示例：

```java
ClassLoader classLoader = new MyClassLoader();

Class<?> clazz = classLoader.loadClass("com.example.Plugin");
```

---

## 19. 面试常问：Class.forName 和 ClassLoader.loadClass 区别

可以这样回答：

```text
Class.forName 默认会加载类并触发类初始化，因此会执行静态变量初始化和静态代码块。

ClassLoader.loadClass 默认只完成类加载，不会主动触发类初始化，所以不会执行 static 静态代码块。

Class.forName 本质上也是通过 ClassLoader 完成类加载，只是它提供了是否初始化的参数。ClassLoader 更底层，主要负责类加载过程，常用于框架、自定义类加载器、插件化和延迟加载场景。
```

---

## 20. 代码对比

### Class.forName

```java
Class<?> clazz = Class.forName("com.example.User");
```

特点：

```text
加载类
连接类
初始化类
执行 static 代码块
```

---

### Class.forName 不初始化

```java
Class<?> clazz = Class.forName(
        "com.example.User",
        false,
        Thread.currentThread().getContextClassLoader()
);
```

特点：

```text
加载类
不初始化类
不执行 static 代码块
```

---

### ClassLoader.loadClass

```java
ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

Class<?> clazz = classLoader.loadClass("com.example.User");
```

特点：

```text
加载类
默认不初始化类
不执行 static 代码块
```

---

## 21. 一句话总结

> `Class.forName()` 默认会加载并初始化类，会执行静态代码块；`ClassLoader.loadClass()` 默认只加载类，不会初始化类。`Class.forName()` 更像是反射加载类的便捷入口，而 `ClassLoader` 是 Java 类加载机制的底层核心。
