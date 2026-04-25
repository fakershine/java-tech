# Java 中的异常总结

Java 异常用于表示程序运行过程中出现的错误或异常情况。  
异常机制的核心作用是：**将正常业务逻辑和异常处理逻辑分离，提高程序健壮性。**

---

## 1. Java 异常体系

Java 异常体系的顶层是 `Throwable`。

```text
Throwable
├── Error
└── Exception
    ├── RuntimeException
    └── Checked Exception
```

---

## 2. Error

### 说明

`Error` 表示 JVM 层面的严重错误，一般程序无法处理。

常见 Error：

```text
OutOfMemoryError
StackOverflowError
NoClassDefFoundError
```

### 特点

- 通常由 JVM 抛出。
- 一般不建议捕获。
- 多数属于严重系统问题。

### 示例

```java
java.lang.OutOfMemoryError: Java heap space
```

---

## 3. Exception

### 说明

`Exception` 表示程序可以处理的异常。

主要分为：

```text
编译时异常 Checked Exception
运行时异常 RuntimeException
```

---

## 4. 编译时异常

### 说明

编译时异常也叫受检异常，必须显式处理。

处理方式：

```text
try-catch 捕获
throws 向上抛出
```

常见异常：

```text
IOException
SQLException
ClassNotFoundException
FileNotFoundException
InterruptedException
```

示例：

```java
public void readFile() throws IOException {
    FileInputStream inputStream = new FileInputStream("a.txt");
}
```

### 特点

- 编译器强制要求处理。
- 通常是外部资源异常。
- 适合调用方可以恢复或补救的场景。

---

## 5. 运行时异常

### 说明

运行时异常继承自 `RuntimeException`，编译器不强制处理。

常见异常：

```text
NullPointerException
IndexOutOfBoundsException
ClassCastException
IllegalArgumentException
ArithmeticException
NumberFormatException
```

示例：

```java
String str = null;
str.length(); // NullPointerException
```

### 特点

- 编译器不强制捕获。
- 多数是代码逻辑问题。
- 应该通过代码校验避免，而不是大量捕获。

---

## 6. Checked Exception 和 RuntimeException 区别

| 对比项 | Checked Exception | RuntimeException |
|---|---|---|
| 是否强制处理 | 是 | 否 |
| 发生阶段 | 编译期要求处理 | 运行期发生 |
| 常见原因 | 外部资源异常 | 代码逻辑错误 |
| 处理方式 | catch 或 throws | 通常通过代码规避 |
| 示例 | IOException、SQLException | NPE、数组越界、类型转换异常 |

---

## 7. try-catch-finally

### 基本用法

```java
try {
    // 可能出现异常的代码
} catch (Exception e) {
    // 异常处理
} finally {
    // 一定会执行的代码
}
```

### finally 的作用

通常用于释放资源：

```text
关闭文件
关闭数据库连接
释放锁
关闭网络连接
```

### 注意

```text
finally 通常会执行，但如果 JVM 退出或进程被 kill，则不会执行。
```

例如：

```java
System.exit(0);
```

会导致 `finally` 不执行。

---

## 8. throws 和 throw

### throws

`throws` 用在方法声明上，表示当前方法不处理异常，交给调用方处理。

```java
public void readFile() throws IOException {
}
```

---

### throw

`throw` 用在方法体中，表示主动抛出一个异常对象。

```java
throw new IllegalArgumentException("参数不能为空");
```

---

### 区别

| 对比项 | throw | throws |
|---|---|---|
| 位置 | 方法体内 | 方法声明上 |
| 作用 | 主动抛出异常对象 | 声明方法可能抛异常 |
| 后面跟什么 | 异常对象 | 异常类型 |
| 数量 | 一次抛一个对象 | 可以声明多个类型 |

---

## 9. 自定义异常

### 实现方式

业务中经常自定义异常。

```java
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
```

使用：

```java
if (user == null) {
    throw new BusinessException("USER_NOT_FOUND", "用户不存在");
}
```

### 建议

业务异常一般继承：

```text
RuntimeException
```

这样调用链不用层层 `throws`。

---

## 10. 异常处理原则

### 1. 不要吞异常

不推荐：

```java
try {
    doSomething();
} catch (Exception e) {
}
```

问题：

```text
异常被隐藏，后续难以排查。
```

---

### 2. 不要捕获过大的异常范围

不推荐：

```java
catch (Throwable e) {
}
```

原因：

```text
Error 也会被捕获，可能掩盖严重问题。
```

---

### 3. 异常日志要有上下文

推荐：

```java
log.error("创建订单失败，userId={}, orderNo={}", userId, orderNo, e);
```

日志中要包含：

```text
关键参数
业务场景
异常堆栈
```

---

### 4. 不要用异常控制正常流程

不推荐：

```java
try {
    Integer.parseInt(value);
} catch (Exception e) {
    // 用异常判断是否数字
}
```

异常处理成本较高，不适合作为正常业务分支。

---

### 5. finally 中不要 return

不推荐：

```java
try {
    return 1;
} finally {
    return 2;
}
```

因为 `finally` 中的 `return` 会覆盖 `try` 中的返回值。

---

## 11. try-with-resources

### 作用

用于自动关闭资源，避免手动写 `finally`。

资源类需要实现：

```text
AutoCloseable
```

示例：

```java
try (FileInputStream inputStream = new FileInputStream("a.txt")) {
    // 使用 inputStream
} catch (IOException e) {
    e.printStackTrace();
}
```

执行完成后会自动调用：

```java
inputStream.close();
```

适合：

```text
文件流
数据库连接
网络连接
```

---

## 12. 常见异常场景

| 异常 | 常见原因 |
|---|---|
| `NullPointerException` | 对 null 对象调用方法 |
| `ArrayIndexOutOfBoundsException` | 数组下标越界 |
| `ClassCastException` | 类型强转失败 |
| `NumberFormatException` | 字符串转数字失败 |
| `IllegalArgumentException` | 参数不合法 |
| `IOException` | 文件或网络 IO 异常 |
| `SQLException` | 数据库异常 |
| `StackOverflowError` | 递归过深 |
| `OutOfMemoryError` | 内存不足或泄漏 |

---

## 13. 总结

Java 异常体系的顶层是 `Throwable`，下面分为 `Error` 和 `Exception`。

`Error` 表示 JVM 层面的严重错误，例如 `OutOfMemoryError`、`StackOverflowError`，通常不建议业务代码捕获。

`Exception` 表示程序可以处理的异常，又分为编译时异常和运行时异常。编译时异常必须显式捕获或抛出，例如 `IOException`、`SQLException`；运行时异常继承自 `RuntimeException`，编译器不强制处理，例如空指针、数组越界、类型转换异常等。

实际开发中，业务异常通常自定义为 `RuntimeException`。异常处理时要避免吞异常，日志要打印上下文和堆栈，资源释放推荐使用 `try-with-resources`。

一句话总结：

```text
Java 异常体系 = Throwable -> Error + Exception；
Exception 又分为编译时异常和运行时异常。
```
