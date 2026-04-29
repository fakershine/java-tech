# Java 面向对象与常见基础面试题总结

## 目录

- [一、Java 面向对象三大特性是什么？](#一java-面向对象三大特性是什么)
- [二、重载和重写的区别](#二重载和重写的区别)
- [三、接口和抽象类的区别](#三接口和抽象类的区别)
- [四、String、StringBuilder、StringBuffer 区别](#四stringstringbuilderstringbuffer-区别)
- [五、final、finally、finalize 区别](#五finalfinallyfinalize-区别)
- [六、== 和 equals 的区别](#六-和-equals-的区别)
- [七、hashCode 和 equals 的关系](#七hashcode-和-equals-的关系)
- [八、面试速记版](#八面试速记版)

---

# 一、Java 面向对象三大特性是什么？

Java 面向对象的三大特性是：

> **封装、继承、多态**

---

## 1. 封装

### 1.1 什么是封装？

封装是指将对象的属性和行为封装到类中，并通过访问控制符限制外部直接访问对象内部数据。

简单来说：

> 封装就是把对象的内部细节隐藏起来，只对外暴露必要的访问方法。

---

### 1.2 封装的作用

封装的主要作用包括：

1. 提高代码安全性
2. 隐藏实现细节
3. 提高代码可维护性
4. 降低类与类之间的耦合度
5. 方便对属性进行统一校验和控制

---

### 1.3 示例

```java
public class User {

    private String name;
    private int age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        if (age < 0) {
            throw new IllegalArgumentException("年龄不能小于 0");
        }
        this.age = age;
    }
}
```

在上面的代码中：

- `name` 和 `age` 使用 `private` 修饰，外部不能直接访问；
- 外部只能通过 `getName()`、`setName()`、`getAge()`、`setAge()` 方法访问；
- 在 `setAge()` 方法中可以对年龄进行校验，避免非法数据进入对象。

---

## 2. 继承

### 2.1 什么是继承？

继承是指子类可以继承父类的属性和方法，从而实现代码复用。

Java 中使用 `extends` 关键字实现继承。

---

### 2.2 继承的作用

继承的主要作用包括：

1. 提高代码复用性
2. 建立类与类之间的层次关系
3. 为多态提供基础
4. 方便代码扩展和维护

---

### 2.3 示例

```java
public class Animal {

    public void eat() {
        System.out.println("动物吃东西");
    }
}

public class Dog extends Animal {

    public void bark() {
        System.out.println("狗叫");
    }
}
```

使用：

```java
public class Test {
    public static void main(String[] args) {
        Dog dog = new Dog();
        dog.eat();
        dog.bark();
    }
}
```

输出结果：

```text
动物吃东西
狗叫
```

`Dog` 类继承了 `Animal` 类，所以 `Dog` 对象可以调用父类中的 `eat()` 方法。

---

### 2.4 Java 继承的特点

Java 中继承有以下特点：

1. Java 类只支持单继承；
2. 一个类只能直接继承一个父类；
3. 一个类可以实现多个接口；
4. 所有类默认继承 `Object` 类；
5. 子类可以继承父类的非私有成员；
6. 父类的 `private` 成员不能被子类直接访问，但可以通过父类提供的方法间接访问。

---

## 3. 多态

### 3.1 什么是多态？

多态是指同一个行为在不同对象上具有不同的表现形式。

简单来说：

> 父类引用指向子类对象，调用方法时执行的是子类重写后的方法。

---

### 3.2 多态的前提条件

多态需要满足以下条件：

1. 有继承关系或者实现关系；
2. 子类重写父类方法；
3. 父类引用指向子类对象。

---

### 3.3 示例

```java
public class Animal {

    public void eat() {
        System.out.println("动物吃东西");
    }
}

public class Dog extends Animal {

    @Override
    public void eat() {
        System.out.println("狗吃骨头");
    }
}

public class Cat extends Animal {

    @Override
    public void eat() {
        System.out.println("猫吃鱼");
    }
}
```

测试代码：

```java
public class Test {
    public static void main(String[] args) {
        Animal animal1 = new Dog();
        Animal animal2 = new Cat();

        animal1.eat();
        animal2.eat();
    }
}
```

输出结果：

```text
狗吃骨头
猫吃鱼
```

---

### 3.4 多态的优点

多态的优点包括：

1. 提高代码扩展性；
2. 降低代码耦合度；
3. 方便程序维护；
4. 符合开闭原则；
5. 可以通过父类或接口统一管理不同子类对象。

---

### 3.5 面试回答

Java 面向对象三大特性是封装、继承和多态。

封装是隐藏对象内部实现细节，只对外暴露必要的方法；继承是子类复用父类的属性和方法；多态是父类引用指向子类对象，在运行时根据实际对象类型调用对应的方法。

---

# 二、重载和重写的区别

## 1. 方法重载

### 1.1 什么是重载？

方法重载指的是在同一个类中，多个方法的方法名相同，但参数列表不同。

参数列表不同包括：

1. 参数个数不同；
2. 参数类型不同；
3. 参数顺序不同。

---

### 1.2 示例

```java
public class Calculator {

    public int add(int a, int b) {
        return a + b;
    }

    public int add(int a, int b, int c) {
        return a + b + c;
    }

    public double add(double a, double b) {
        return a + b;
    }

    public double add(int a, double b) {
        return a + b;
    }

    public double add(double a, int b) {
        return a + b;
    }
}
```

以上这些 `add()` 方法都属于方法重载。

---

### 1.3 重载注意点

方法重载与返回值类型无关。

下面这种写法不构成重载，会编译报错：

```java
public int test(int a) {
    return a;
}

public double test(int a) {
    return a;
}
```

原因是：

```text
方法名相同，参数列表也相同，仅返回值类型不同，Java 无法区分调用哪个方法。
```

---

## 2. 方法重写

### 2.1 什么是重写？

方法重写指的是子类重新实现父类中已有的方法。

---

### 2.2 示例

```java
public class Animal {

    public void eat() {
        System.out.println("动物吃东西");
    }
}

public class Dog extends Animal {

    @Override
    public void eat() {
        System.out.println("狗吃骨头");
    }
}
```

`Dog` 类中的 `eat()` 方法重写了父类 `Animal` 中的 `eat()` 方法。

---

### 2.3 重写的要求

方法重写需要满足以下条件：

1. 方法名必须相同；
2. 参数列表必须相同；
3. 返回值类型必须相同，或者是父类方法返回值类型的子类型；
4. 子类方法访问权限不能比父类方法更小；
5. 子类方法抛出的异常不能比父类方法更宽泛；
6. `private` 方法不能被重写；
7. `final` 方法不能被重写；
8. `static` 方法不能被真正重写，只能被隐藏。

---

## 3. 重载和重写的区别

| 对比项 | 重载 Overload | 重写 Override |
|---|---|---|
| 发生位置 | 同一个类中 | 父子类之间 |
| 方法名 | 必须相同 | 必须相同 |
| 参数列表 | 必须不同 | 必须相同 |
| 返回值类型 | 可以不同，但不能仅靠返回值区分 | 相同，或者是父类返回值类型的子类型 |
| 访问修饰符 | 没有限制 | 子类访问权限不能更小 |
| 异常限制 | 没有限制 | 子类异常不能比父类更宽泛 |
| 多态类型 | 编译期多态 | 运行期多态 |
| 绑定时期 | 编译期确定 | 运行期确定 |

---

## 4. 面试回答

重载是同一个类中方法名相同、参数列表不同的方法，与返回值类型无关，属于编译期多态。

重写是子类对父类方法的重新实现，要求方法名和参数列表相同，返回值类型兼容，访问权限不能更小，属于运行期多态。

---

# 三、接口和抽象类的区别

## 1. 抽象类

### 1.1 什么是抽象类？

使用 `abstract` 修饰的类叫抽象类。

抽象类不能直接创建对象，通常用于被子类继承。

---

### 1.2 示例

```java
public abstract class Animal {

    private String name;

    public Animal(String name) {
        this.name = name;
    }

    public void sleep() {
        System.out.println(name + " 正在睡觉");
    }

    public abstract void eat();
}
```

抽象类中可以包含：

1. 成员变量；
2. 构造方法；
3. 普通方法；
4. 抽象方法；
5. 静态方法；
6. final 方法。

---

## 2. 接口

### 2.1 什么是接口？

接口是一种规范或者能力的抽象，使用 `interface` 定义。

类通过 `implements` 关键字实现接口。

---

### 2.2 示例

```java
public interface Flyable {

    void fly();
}
```

实现接口：

```java
public class Bird implements Flyable {

    @Override
    public void fly() {
        System.out.println("鸟会飞");
    }
}
```

---

## 3. 接口和抽象类的区别

| 对比项 | 接口 | 抽象类 |
|---|---|---|
| 定义关键字 | `interface` | `abstract class` |
| 实现方式 | `implements` | `extends` |
| 多继承能力 | 一个类可以实现多个接口 | 一个类只能继承一个抽象类 |
| 构造方法 | 没有构造方法 | 可以有构造方法 |
| 成员变量 | 默认是 `public static final` 常量 | 可以有普通成员变量 |
| 方法 | 可以有抽象方法、默认方法、静态方法、私有方法 | 可以有抽象方法、普通方法、静态方法 |
| 访问修饰符 | 接口方法默认是 `public` | 可以使用多种访问修饰符 |
| 是否保存状态 | 通常不保存对象状态 | 可以保存对象状态 |
| 设计目的 | 定义规范、能力、行为约束 | 抽取公共属性和公共行为 |
| 使用场景 | 表示某种能力 | 表示一类对象的公共父类 |

---

## 4. 接口的特点

接口的特点包括：

1. 接口不能直接创建对象；
2. 接口中的变量默认是 `public static final`；
3. 接口中的抽象方法默认是 `public abstract`；
4. 一个类可以实现多个接口；
5. 接口可以继承多个接口；
6. Java 8 之后接口可以有默认方法和静态方法；
7. Java 9 之后接口可以有私有方法。

---

## 5. 抽象类的特点

抽象类的特点包括：

1. 抽象类不能直接创建对象；
2. 抽象类可以有构造方法；
3. 抽象类可以有普通成员变量；
4. 抽象类可以有普通方法和抽象方法；
5. 抽象类的子类必须实现父类中的抽象方法，除非子类也是抽象类；
6. 一个类只能继承一个抽象类。

---

## 6. 什么时候使用接口？

如果强调的是某个类具备某种能力，优先使用接口。

例如：

```java
public interface Runnable {
    void run();
}

public interface Serializable {
}
```

接口更像是一种能力约束，例如：

- 能运行；
- 能飞；
- 能序列化；
- 能支付；
- 能发送消息。

---

## 7. 什么时候使用抽象类？

如果强调的是一类对象的共同属性和行为，优先使用抽象类。

例如：

```java
public abstract class Animal {

    protected String name;

    public void sleep() {
        System.out.println("动物睡觉");
    }

    public abstract void eat();
}
```

抽象类更像是一类对象的公共父类，例如：

- 动物；
- 用户；
- 订单；
- 支付模板；
- 消息处理模板。

---

## 8. 面试回答

接口强调行为规范和能力，一个类可以实现多个接口；抽象类强调代码复用和共性抽取，一个类只能继承一个抽象类。

如果只是定义能力或规范，通常使用接口；如果需要抽取公共属性、公共方法或模板逻辑，通常使用抽象类。

---

# 四、String、StringBuilder、StringBuffer 区别

## 1. String

### 1.1 String 的特点

`String` 是不可变字符串。

一旦创建，字符串内容不能被修改。

---

### 1.2 示例

```java
String str = "hello";
str = str + " world";
```

表面上看 `str` 的内容发生了变化，但实际上是创建了一个新的字符串对象，然后让 `str` 指向新对象。

---

### 1.3 String 不可变的好处

`String` 不可变的好处包括：

1. 线程安全；
2. 可以被字符串常量池复用；
3. 适合作为 `HashMap` 的 key；
4. 避免字符串内容被恶意修改；
5. 方便缓存 hashCode。

---

## 2. StringBuilder

### 2.1 StringBuilder 的特点

`StringBuilder` 是可变字符串，线程不安全。

它适合在单线程环境下进行大量字符串拼接。

---

### 2.2 示例

```java
StringBuilder builder = new StringBuilder();

builder.append("hello");
builder.append(" ");
builder.append("world");

System.out.println(builder.toString());
```

输出结果：

```text
hello world
```

---

## 3. StringBuffer

### 3.1 StringBuffer 的特点

`StringBuffer` 也是可变字符串，但是线程安全。

它的大部分方法使用了 `synchronized` 修饰。

---

### 3.2 示例

```java
StringBuffer buffer = new StringBuffer();

buffer.append("hello");
buffer.append(" ");
buffer.append("world");

System.out.println(buffer.toString());
```

输出结果：

```text
hello world
```

---

## 4. String、StringBuilder、StringBuffer 对比

| 对比项 | String | StringBuilder | StringBuffer |
|---|---|---|---|
| 是否可变 | 不可变 | 可变 | 可变 |
| 线程安全 | 安全 | 不安全 | 安全 |
| 性能 | 频繁拼接性能较低 | 性能最高 | 性能低于 StringBuilder |
| 是否加锁 | 不加锁 | 不加锁 | 方法大多加 synchronized |
| 适用场景 | 少量字符串操作 | 单线程大量字符串拼接 | 多线程大量字符串拼接 |

---

## 5. 性能比较

一般情况下，性能排序为：

```text
StringBuilder > StringBuffer > String
```

原因：

1. `StringBuilder` 不加锁，性能最高；
2. `StringBuffer` 加锁保证线程安全，所以性能比 `StringBuilder` 低；
3. `String` 不可变，频繁拼接可能产生大量临时对象。

---

## 6. 面试回答

`String` 是不可变字符串，适合少量字符串操作。

`StringBuilder` 是可变字符串，线程不安全，但效率高，适合单线程环境下大量字符串拼接。

`StringBuffer` 是可变字符串，线程安全，适合多线程环境下字符串拼接，但性能比 `StringBuilder` 低。

---

# 五、final、finally、finalize 区别

## 1. final

### 1.1 final 是什么？

`final` 是 Java 中的关键字，可以修饰：

1. 类；
2. 方法；
3. 变量。

---

### 1.2 final 修饰类

被 `final` 修饰的类不能被继承。

```java
public final class MyClass {
}
```

例如，`String` 类就是 `final` 类。

---

### 1.3 final 修饰方法

被 `final` 修饰的方法不能被子类重写。

```java
public class Parent {

    public final void test() {
        System.out.println("final 方法");
    }
}
```

---

### 1.4 final 修饰变量

被 `final` 修饰的变量只能赋值一次。

```java
final int count = 10;
```

如果是基本数据类型，值不能改变：

```java
final int age = 18;
// age = 20; // 编译报错
```

如果是引用数据类型，引用地址不能改变，但对象内部属性可以改变：

```java
final User user = new User();

user.setName("Tom"); // 可以修改对象内部属性

// user = new User(); // 不可以重新指向新对象
```

---

## 2. finally

### 2.1 finally 是什么？

`finally` 是 Java 异常处理机制中的代码块，通常和 `try-catch` 一起使用。

无论是否发生异常，`finally` 中的代码通常都会执行。

---

### 2.2 示例

```java
try {
    int result = 10 / 0;
} catch (Exception e) {
    System.out.println("发生异常：" + e.getMessage());
} finally {
    System.out.println("finally 代码块执行");
}
```

---

### 2.3 finally 的使用场景

`finally` 通常用于释放资源，例如：

1. 关闭 IO 流；
2. 关闭数据库连接；
3. 关闭网络连接；
4. 释放锁资源。

---

### 2.4 finally 一定会执行吗？

大多数情况下 `finally` 会执行，但不是绝对的。

以下情况可能不会执行：

1. JVM 退出，例如调用 `System.exit(0)`；
2. 程序所在进程被强制杀死；
3. 服务器断电或系统崩溃；
4. 线程被强制终止；
5. 程序一直卡在 `try` 或 `catch` 中无法继续执行。

---

## 3. finalize

### 3.1 finalize 是什么？

`finalize()` 是 `Object` 类中的一个方法。

它曾经用于对象被垃圾回收前执行清理操作。

```java
@Override
protected void finalize() throws Throwable {
    System.out.println("对象被垃圾回收前执行");
}
```

---

### 3.2 finalize 的问题

`finalize()` 不推荐使用，原因包括：

1. 调用时间不确定；
2. 不保证一定会执行；
3. 可能影响垃圾回收性能；
4. 可能导致对象复活；
5. 逻辑不可控，容易引发问题。

---

## 4. final、finally、finalize 对比

| 对比项 | 类型 | 作用 |
|---|---|---|
| final | 关键字 | 修饰类、方法、变量 |
| finally | 异常处理代码块 | 通常用于释放资源 |
| finalize | Object 类中的方法 | 对象被回收前可能被调用 |

---

## 5. 面试回答

`final` 是关键字，可以修饰类、方法和变量。修饰类表示不能被继承，修饰方法表示不能被重写，修饰变量表示只能赋值一次。

`finally` 是异常处理中的代码块，通常用于释放资源，一般情况下都会执行。

`finalize()` 是 `Object` 类中的方法，对象被垃圾回收前可能会被调用，但不保证执行，现在已经不推荐使用。

---

# 六、== 和 equals 的区别

## 1. == 的作用

`==` 是 Java 中的运算符。

它的比较规则取决于比较的数据类型。

---

## 2. 基本数据类型使用 ==

如果比较的是基本数据类型，`==` 比较的是值。

```java
int a = 10;
int b = 10;

System.out.println(a == b);
```

输出结果：

```text
true
```

---

## 3. 引用数据类型使用 ==

如果比较的是引用数据类型，`==` 比较的是对象的内存地址。

```java
User user1 = new User();
User user2 = new User();

System.out.println(user1 == user2);
```

输出结果：

```text
false
```

虽然 `user1` 和 `user2` 的内容可能相同，但它们是两个不同的对象，所以地址不同。

---

## 4. equals 的作用

`equals()` 是 `Object` 类中的方法。

默认情况下，`Object` 类中的 `equals()` 方法比较的也是对象地址。

源码逻辑类似：

```java
public boolean equals(Object obj) {
    return this == obj;
}
```

---

## 5. 为什么 String 的 equals 比较内容？

因为 `String` 类重写了 `equals()` 方法。

示例：

```java
String s1 = new String("hello");
String s2 = new String("hello");

System.out.println(s1 == s2);
System.out.println(s1.equals(s2));
```

输出结果：

```text
false
true
```

原因：

1. `s1 == s2` 比较的是对象地址；
2. `s1.equals(s2)` 比较的是字符串内容。

---

## 6. == 和 equals 对比

| 对比项 | == | equals |
|---|---|---|
| 类型 | 运算符 | 方法 |
| 基本数据类型 | 比较值 | 不能直接使用 |
| 引用数据类型 | 比较对象地址 | 默认比较地址，可重写为比较内容 |
| 是否可重写 | 不可以 | 可以 |
| 常见使用场景 | 比较基本类型、判断是否同一个对象 | 判断对象内容是否相等 |

---

## 7. 面试回答

`==` 如果比较基本数据类型，比较的是值；如果比较引用数据类型，比较的是对象地址。

`equals()` 是 `Object` 类中的方法，默认比较对象地址，但很多类会重写它，比如 `String` 重写后比较的是字符串内容。

---

# 七、hashCode 和 equals 的关系

## 1. hashCode 是什么？

`hashCode()` 是 `Object` 类中的方法，用于返回对象的哈希值。

哈希值主要用于哈希结构，例如：

1. `HashMap`
2. `HashSet`
3. `Hashtable`
4. `ConcurrentHashMap`

---

## 2. equals 是什么？

`equals()` 用于判断两个对象是否相等。

默认情况下，`equals()` 比较的是对象地址，但可以被重写为比较对象内容。

---

## 3. hashCode 和 equals 的核心关系

Java 中有一个重要约定：

> 如果两个对象通过 `equals()` 比较相等，那么它们的 `hashCode()` 必须相等。

但是：

> 如果两个对象的 `hashCode()` 相等，它们的 `equals()` 不一定相等。

---

## 4. 为什么 equals 相等，hashCode 必须相等？

因为 `HashMap`、`HashSet` 等哈希结构在判断对象是否重复时，通常会先使用 `hashCode()` 定位桶的位置，再使用 `equals()` 判断对象是否真正相等。

如果两个对象 `equals()` 相等，但 `hashCode()` 不相等，就会导致它们被放到不同的桶中，从而导致集合判断错误。

---

## 5. 错误示例：只重写 equals，不重写 hashCode

```java
public class User {

    private String name;
    private int age;

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        User user = (User) obj;

        return age == user.age && name.equals(user.name);
    }
}
```

测试：

```java
import java.util.HashSet;
import java.util.Set;

public class Test {
    public static void main(String[] args) {
        Set<User> set = new HashSet<>();

        set.add(new User("Tom", 18));
        set.add(new User("Tom", 18));

        System.out.println(set.size());
    }
}
```

由于没有重写 `hashCode()`，即使两个对象通过 `equals()` 判断相等，也可能被 `HashSet` 当成两个不同对象。

---

## 6. 正确示例：同时重写 equals 和 hashCode

```java
import java.util.Objects;

public class User {

    private String name;
    private int age;

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        User user = (User) o;

        return age == user.age &&
                Objects.equals(name, user.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }
}
```

测试：

```java
import java.util.HashSet;
import java.util.Set;

public class Test {
    public static void main(String[] args) {
        Set<User> set = new HashSet<>();

        set.add(new User("Tom", 18));
        set.add(new User("Tom", 18));

        System.out.println(set.size());
    }
}
```

输出结果：

```text
1
```

---

## 7. hashCode 和 equals 的规则

需要记住以下规则：

1. 如果两个对象 `equals()` 相等，那么它们的 `hashCode()` 必须相等；
2. 如果两个对象 `equals()` 不相等，它们的 `hashCode()` 可以相等，也可以不相等；
3. 如果两个对象 `hashCode()` 相等，它们的 `equals()` 不一定相等；
4. 如果两个对象 `hashCode()` 不相等，它们的 `equals()` 一定不相等；
5. 重写 `equals()` 时必须重写 `hashCode()`。

---

## 8. 面试回答

`equals()` 用来判断两个对象是否相等，`hashCode()` 用来获取对象的哈希值。

如果两个对象通过 `equals()` 判断相等，那么它们的 `hashCode()` 必须相等。

但是两个对象的 `hashCode()` 相等，`equals()` 不一定相等，因为哈希冲突是可能存在的。

在 `HashMap`、`HashSet` 等哈希结构中，会先通过 `hashCode()` 定位存储位置，再通过 `equals()` 判断对象是否真正相等。

所以重写 `equals()` 时必须同时重写 `hashCode()`。

---

# 八、面试速记版

## 1. 面向对象三大特性

Java 面向对象三大特性是：

```text
封装、继承、多态
```

封装隐藏对象内部细节，继承实现代码复用，多态提高代码扩展性。

---

## 2. 重载和重写

重载发生在同一个类中，方法名相同，参数列表不同，属于编译期多态。

重写发生在父子类之间，子类重新实现父类方法，属于运行期多态。

---

## 3. 接口和抽象类

接口强调规范和能力，一个类可以实现多个接口。

抽象类强调共性和代码复用，一个类只能继承一个抽象类。

---

## 4. String、StringBuilder、StringBuffer

`String` 不可变。

`StringBuilder` 可变，线程不安全，效率高。

`StringBuffer` 可变，线程安全，效率比 `StringBuilder` 低。

---

## 5. final、finally、finalize

`final` 是关键字，可以修饰类、方法、变量。

`finally` 是异常处理代码块，通常用于释放资源。

`finalize()` 是对象回收前可能执行的方法，但不保证执行，现在不推荐使用。

---

## 6. == 和 equals

`==` 比较基本类型时比较值，比较引用类型时比较地址。

`equals()` 默认比较地址，但可以被重写，比如 `String` 的 `equals()` 比较的是字符串内容。

---

## 7. hashCode 和 equals

两个对象 `equals()` 相等，`hashCode()` 必须相等。

两个对象 `hashCode()` 相等，`equals()` 不一定相等。

重写 `equals()` 时必须重写 `hashCode()`。

---

# 九、总览表

| 问题 | 核心结论 |
|---|---|
| Java 面向对象三大特性 | 封装、继承、多态 |
| 重载和重写区别 | 重载是同类中方法名相同参数不同；重写是子类重新实现父类方法 |
| 接口和抽象类区别 | 接口强调规范和能力；抽象类强调共性和代码复用 |
| String、StringBuilder、StringBuffer 区别 | `String` 不可变；`StringBuilder` 可变但线程不安全；`StringBuffer` 可变且线程安全 |
| final、finally、finalize 区别 | `final` 是关键字；`finally` 是异常处理代码块；`finalize()` 是对象回收前可能调用的方法 |
| == 和 equals 区别 | `==` 比较值或地址；`equals()` 默认比较地址，可重写为比较内容 |
| hashCode 和 equals 关系 | `equals()` 相等则 `hashCode()` 必须相等；`hashCode()` 相等则 `equals()` 不一定相等 |

---

# 十、总结

Java 面向对象有三大特性：封装、继承和多态。

封装是把对象的属性隐藏起来，通过方法对外提供访问，从而提高安全性和可维护性。

继承是子类复用父类的属性和方法，减少重复代码。

多态是父类引用指向子类对象，程序在运行时根据实际对象类型调用对应方法，从而提高扩展性。

重载是同一个类中方法名相同但参数列表不同，属于编译期多态；重写是子类重新实现父类方法，属于运行期多态。

接口更强调规范和能力，抽象类更强调共性抽取和代码复用。一个类可以实现多个接口，但只能继承一个抽象类。

`String` 是不可变字符串，`StringBuilder` 和 `StringBuffer` 是可变字符串，其中 `StringBuilder` 线程不安全但效率高，`StringBuffer` 线程安全但效率相对低。

`final` 是关键字，可以修饰类、方法和变量；`finally` 是异常处理代码块，通常用于释放资源；`finalize()` 是对象回收前可能调用的方法，但不保证执行，现在不推荐使用。

`==` 比较基本类型时比较值，比较引用类型时比较地址。`equals()` 默认比较地址，但可以重写为比较内容，例如 `String` 的 `equals()` 比较的是字符串内容。

`hashCode()` 和 `equals()` 的关系是：如果两个对象 `equals()` 相等，那么它们的 `hashCode()` 必须相等；但 `hashCode()` 相等，`equals()` 不一定相等。重写 `equals()` 时必须同时重写 `hashCode()`。
