# String 为什么定义成 final

`String` 被定义成 `final`，核心原因是：**保证字符串不可被继承修改，从而维护不可变性、安全性和性能优化**。

---

## 1. 防止被继承破坏不可变性

`String` 是不可变对象。

```java
String s = "abc";
```

一旦创建，内容不能被修改。

如果 `String` 不是 `final`，别人可以继承它并重写方法，破坏不可变性。

例如理论上可能出现：

```java
class MyString extends String {
    // 重写 equals、hashCode、charAt 等方法
}
```

这样会导致：

```text
String 行为不再稳定
字符串内容可能被伪造
不可变性被破坏
```

所以 `String` 被设计成 `final`，禁止继承。

---

## 2. 保证安全性

`String` 在 Java 中被广泛用于敏感场景，例如：

```text
类名
方法名
文件路径
网络地址
数据库连接信息
用户名
密码
权限标识
反射调用
ClassLoader 加载类
```

例如：

```java
Class.forName("com.demo.UserService");
```

如果 `String` 可以被继承并篡改行为，就可能导致安全问题。

比如：

```text
传入时看起来是 A 类名
实际使用时变成 B 类名
```

因此 `String` 必须保证行为稳定，不允许子类改变。

---

## 3. 支持字符串常量池

Java 中有字符串常量池：

```java
String a = "abc";
String b = "abc";
```

`a` 和 `b` 可以指向同一个常量池对象。

```text
a -> "abc"
b -> "abc"
```

如果 `String` 可变或可被子类破坏，就不能安全地共享同一个字符串对象。

`String` 不可变后，多个引用共享同一个字符串才是安全的。

---

## 4. 支持 hashCode 缓存

`String` 经常作为 `HashMap` 的 key。

```java
Map<String, Object> map = new HashMap<>();
map.put("userId", 1001);
```

`String` 不可变后，它的 `hashCode` 也不会变化。

所以 `String` 可以缓存 hash 值，提高性能。

如果字符串内容可变，会出现问题：

```text
put 时 hashCode = 100
修改后 hashCode = 200
再 get 时找不到原来的 key
```

所以 `String` 不可变对 HashMap 非常重要。

---

## 5. 保证多线程安全

因为 `String` 不可变，所以多个线程可以安全共享同一个字符串对象。

```java
String s = "hello";
```

多个线程同时读取不会有线程安全问题。

```text
不需要加锁
不会被修改
天然线程安全
```

这也是 `String` 被大量共享使用的重要原因。

---

## 6. 提高性能

`String` 定义为 `final` 并保持不可变，可以带来很多优化：

- 字符串常量池复用对象。
- hashCode 可以缓存。
- 多线程共享不需要加锁。
- JVM 可以做更多优化。
- 方法调用行为稳定。

---

## 7. final 修饰 String 的含义

`String` 类声明如下：

```java
public final class String
        implements java.io.Serializable, Comparable<String>, CharSequence {
}
```

`final` 修饰类表示：

```text
String 不能被继承
不能有子类
不能被重写方法破坏行为
```

注意：

```text
final 修饰 String 类，不是说 String 变量不能重新赋值。
```

例如：

```java
String s = "abc";
s = "def";
```

这不是修改 `"abc"` 对象，而是让变量 `s` 指向新的字符串对象。

---

## 8. String 不可变的真正原因

`String` 不可变主要依赖两点：

### 1. 类被 final 修饰

```java
public final class String
```

防止子类破坏不可变性。

### 2. 内部存储不可直接修改

JDK 8 中：

```java
private final char[] value;
```

JDK 9 以后：

```java
private final byte[] value;
```

并且没有提供修改内部数组内容的方法。

所以 `String` 一旦创建，内容不能被改变。

---

## 9. 总结

`String` 被定义成 `final`，主要是为了保证不可变性。

如果 `String` 可以被继承，子类可能重写方法或改变行为，从而破坏字符串不可变、安全和稳定的特性。

`String` 不可变后，可以安全用于字符串常量池共享、HashMap 的 key、类加载路径、文件路径、网络地址等场景。同时它天然线程安全，可以缓存 hashCode，提高性能。

一句话总结：

```text
String 定义成 final，是为了防止被继承破坏不可变性，从而保证安全性、线程安全、常量池复用和 hashCode 缓存等优化。
```
