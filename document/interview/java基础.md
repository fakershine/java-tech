# Java 面向对象与常见基础问题总结

## 目录

1. [Java 面向对象三大特性](#一java-面向对象三大特性)
2. [重载和重写的区别](#二重载和重写的区别)
3. [接口和抽象类的区别](#三接口和抽象类的区别)
4. [String、StringBuilder、StringBuffer 区别](#四stringstringbuilderstringbuffer-区别)
5. [final、finally、finalize 区别](#五finalfinallyfinalize-区别)
6. [== 和 equals 的区别](#六-和-equals-的区别)
7. [hashCode 和 equals 的关系](#七hashcode-和-equals-的关系)
8. [总结表](#八总结表)

---

# 一、Java 面向对象三大特性

Java 面向对象的三大特性是：

> **封装、继承、多态**

---

## 1. 封装

### 1.1 什么是封装？

封装是指将对象的 **属性和行为** 包装在类中，并通过访问控制符限制外部直接访问对象内部数据。

简单来说：

> 把数据隐藏起来，只暴露必要的方法给外部使用。

---

### 1.2 封装的作用

封装的主要作用有：

1. 提高代码安全性
2. 提高代码可维护性
3. 降低类与类之间的耦合
4. 隐藏对象内部实现细节

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
