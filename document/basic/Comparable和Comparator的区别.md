# Comparable 和 Comparator 的区别

## 1. 核心区别

| 对比项 | Comparable | Comparator |
|---|---|---|
| 所在包 | `java.lang` | `java.util` |
| 排序方式 | 自然排序 | 自定义排序 |
| 方法 | `compareTo()` | `compare()` |
| 是否侵入类 | 侵入，需要修改类本身 | 不侵入，可以写在类外部 |
| 排序规则数量 | 通常只有一种 | 可以有多种 |
| 使用场景 | 类本身有固定排序规则 | 不同场景需要不同排序规则 |

---

## 2. Comparable

`Comparable` 表示对象本身具备比较能力。

简单理解：

> Comparable 是“自己和别人比”。

### 示例：按年龄升序排序

```java
public class User implements Comparable<User> {

    private String name;
    private Integer age;

    public User(String name, Integer age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public int compareTo(User other) {
        return Integer.compare(this.age, other.age);
    }

    public String getName() {
        return name;
    }

    public Integer getAge() {
        return age;
    }
}
```

使用：

```java
List<User> users = new ArrayList<>();

users.add(new User("Tom", 20));
users.add(new User("Jerry", 18));
users.add(new User("Jack", 25));

Collections.sort(users);
```

排序规则由 `User` 类自己决定。

---

## 3. Comparator

`Comparator` 表示外部比较器。

简单理解：

> Comparator 是“找一个外部裁判来比”。

### 示例：按年龄升序排序

```java
users.sort(new Comparator<User>() {
    @Override
    public int compare(User u1, User u2) {
        return Integer.compare(u1.getAge(), u2.getAge());
    }
});
```

Lambda 写法：

```java
users.sort((u1, u2) -> Integer.compare(u1.getAge(), u2.getAge()));
```

更推荐写法：

```java
users.sort(Comparator.comparing(User::getAge));
```

### 按年龄倒序

```java
users.sort(Comparator.comparing(User::getAge).reversed());
```

### 先按年龄排序，年龄相同再按姓名排序

```java
users.sort(
    Comparator.comparing(User::getAge)
              .thenComparing(User::getName)
);
```

---

## 4. 返回值含义

无论是 `compareTo()` 还是 `compare()`，返回值规则都一样：

| 返回值 | 含义 |
|---|---|
| 负数 | 当前对象排在前面 |
| `0` | 两个对象相等 |
| 正数 | 当前对象排在后面 |

例如：

```java
return Integer.compare(this.age, other.age);
```

表示年龄升序。

```java
return Integer.compare(other.age, this.age);
```

表示年龄降序。

不推荐直接写：

```java
return this.age - other.age;
```

原因是可能出现整数溢出。

---

## 5. 使用建议

| 场景 | 推荐 |
|---|---|
| 类有天然排序规则 | `Comparable` |
| 排序规则不固定 | `Comparator` |
| 不想修改原类 | `Comparator` |
| 需要多个排序规则 | `Comparator` |
| `TreeSet` 自定义排序 | `Comparator` |
| `TreeMap` 自定义排序 | `Comparator` |

---

## 6. 总结

`Comparable` 和 `Comparator` 都可以用于对象排序。

`Comparable` 是自然排序，需要类本身实现 `Comparable` 接口，并重写 `compareTo()` 方法。排序规则写在类内部，所以对类有侵入性，通常适合对象本身有固定排序规则的场景。

`Comparator` 是外部比较器，需要实现 `compare()` 方法。排序规则写在类外部，不需要修改对象本身，灵活性更高。一个类可以根据不同业务场景定义多个 `Comparator`，比如按年龄排序、按姓名排序、按创建时间排序等。

---

## 7. 一句话总结

> `Comparable` 是对象自己定义排序规则，适合固定自然排序；`Comparator` 是外部定义排序规则，适合多种排序场景，更灵活。
