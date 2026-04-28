# `@Valid` 和 `@Validated` 的区别

---

## 1. 核心区别

| 对比项 | `@Valid` | `@Validated` |
|---|---|---|
| 来源 | Java 标准规范 | Spring 提供 |
| 包名 | `jakarta.validation.Valid` / `javax.validation.Valid` | `org.springframework.validation.annotation.Validated` |
| 是否支持分组校验 | 不支持 | 支持 |
| 是否支持嵌套校验 | 支持 | 支持 |
| 常用位置 | Controller 参数、实体字段 | Controller 参数、类、方法、分组校验 |
| 典型场景 | 普通参数校验 | 分组校验、方法级校验 |

---

## 2. `@Valid` 是什么

`@Valid` 是 Bean Validation 标准提供的注解。

常用于 Controller 接收参数时校验对象字段。

```java
@PostMapping("/user")
public String saveUser(@RequestBody @Valid UserDTO userDTO) {
    return "success";
}
```

DTO：

```java
public class UserDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotNull(message = "年龄不能为空")
    private Integer age;
}
```

如果参数不符合规则，就会抛出校验异常。

---

## 3. `@Validated` 是什么

`@Validated` 是 Spring 提供的注解，是对 `@Valid` 的增强。

它最大的特点是：

> 支持分组校验。

示例：

```java
@PostMapping("/user")
public String saveUser(@RequestBody @Validated UserDTO userDTO) {
    return "success";
}
```

普通场景下，`@Validated` 和 `@Valid` 效果差不多。

---

## 4. 分组校验区别

### 4.1 定义分组

```java
public interface AddGroup {
}

public interface UpdateGroup {
}
```

---

### 4.2 DTO

```java
public class UserDTO {

    @NotNull(message = "更新时 id 不能为空", groups = UpdateGroup.class)
    private Long id;

    @NotBlank(message = "用户名不能为空", groups = {AddGroup.class, UpdateGroup.class})
    private String username;

    @NotNull(message = "年龄不能为空", groups = AddGroup.class)
    private Integer age;
}
```

---

### 4.3 新增接口

```java
@PostMapping("/user")
public String addUser(@RequestBody @Validated(AddGroup.class) UserDTO userDTO) {
    return "success";
}
```

新增时校验：

```text
username 不能为空
age 不能为空
id 不强制校验
```

---

### 4.4 更新接口

```java
@PutMapping("/user")
public String updateUser(@RequestBody @Validated(UpdateGroup.class) UserDTO userDTO) {
    return "success";
}
```

更新时校验：

```text
id 不能为空
username 不能为空
age 不强制校验
```

---

## 5. 为什么 `@Valid` 不适合分组校验

`@Valid` 主要用于标准 Bean Validation 校验。

它本身不能像下面这样指定分组：

```java
@Valid(AddGroup.class) // 错误写法
```

而 `@Validated` 可以：

```java
@Validated(AddGroup.class)
```

所以：

```text
普通校验：@Valid / @Validated 都可以
分组校验：使用 @Validated
```

---

## 6. 嵌套校验

如果对象里还有对象，需要在字段上加 `@Valid`。

```java
public class OrderDTO {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @Valid
    @NotNull(message = "用户信息不能为空")
    private UserDTO user;
}
```

Controller：

```java
@PostMapping("/order")
public String createOrder(@RequestBody @Validated OrderDTO orderDTO) {
    return "success";
}
```

注意：

```text
嵌套对象校验一般用 @Valid 标在字段上。
```

---

## 7. 集合嵌套校验

如果请求参数中有集合对象，也可以使用 `@Valid` 做嵌套校验。

```java
public class OrderDTO {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @Valid
    @NotEmpty(message = "商品列表不能为空")
    private List<OrderItemDTO> items;
}
```

```java
public class OrderItemDTO {

    @NotNull(message = "商品 id 不能为空")
    private Long productId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于 0")
    private Integer quantity;
}
```

Controller：

```java
@PostMapping("/order")
public String createOrder(@RequestBody @Validated OrderDTO orderDTO) {
    return "success";
}
```

---

## 8. 方法参数校验

如果要校验普通方法参数，通常需要在类上加 `@Validated`。

```java
@RestController
@Validated
public class UserController {

    @GetMapping("/user")
    public String getUser(@NotNull(message = "id 不能为空") Long id) {
        return "success";
    }
}
```

这里类上的 `@Validated` 用来开启方法级参数校验。

---

## 9. 常见校验注解

| 注解 | 说明 |
|---|---|
| `@NotNull` | 不能为 `null` |
| `@NotBlank` | 字符串不能为 `null`，且去空格后不能为空 |
| `@NotEmpty` | 集合、数组、字符串不能为空 |
| `@Size` | 长度或集合大小限制 |
| `@Min` | 最小值 |
| `@Max` | 最大值 |
| `@DecimalMin` | 最小小数值 |
| `@DecimalMax` | 最大小数值 |
| `@Pattern` | 正则校验 |
| `@Email` | 邮箱格式 |
| `@Positive` | 必须为正数 |
| `@PositiveOrZero` | 必须为正数或 0 |
| `@Negative` | 必须为负数 |
| `@Past` | 必须是过去时间 |
| `@Future` | 必须是未来时间 |

---

## 10. `@NotNull`、`@NotEmpty`、`@NotBlank` 区别

| 注解 | 适用类型 | 校验规则 |
|---|---|---|
| `@NotNull` | 任意对象 | 不能为 `null` |
| `@NotEmpty` | 字符串、集合、数组、Map | 不能为 `null`，且长度/大小不能为 0 |
| `@NotBlank` | 字符串 | 不能为 `null`，且去除空格后不能为空 |

示例：

```java
@NotNull
private Integer age;

@NotEmpty
private List<String> tags;

@NotBlank
private String username;
```

---

## 11. 常见异常

### 11.1 `@RequestBody` 对象校验失败

通常抛出：

```text
MethodArgumentNotValidException
```

示例：

```java
@PostMapping("/user")
public String saveUser(@RequestBody @Valid UserDTO userDTO) {
    return "success";
}
```

---

### 11.2 普通参数校验失败

例如：

```java
@GetMapping("/user")
public String getUser(@NotNull Long id) {
    return "success";
}
```

通常抛出：

```text
ConstraintViolationException
```

---

### 11.3 表单参数对象校验失败

例如：

```java
@PostMapping("/user")
public String saveUser(@Validated UserDTO userDTO) {
    return "success";
}
```

通常可能抛出：

```text
BindException
```

---

## 12. 全局异常处理示例

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        return e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ":" + error.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public String handleConstraintViolationException(ConstraintViolationException e) {
        return e.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ":" + violation.getMessage())
                .findFirst()
                .orElse("参数校验失败");
    }

    @ExceptionHandler(BindException.class)
    public String handleBindException(BindException e) {
        return e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ":" + error.getDefaultMessage())
                .findFirst()
                .orElse("参数绑定失败");
    }
}
```

---

## 13. Spring Boot 依赖

Spring Boot 2.3 之后，参数校验需要显式引入依赖。

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

---

## 14. Spring Boot 2 和 Spring Boot 3 包名区别

### Spring Boot 2 常见

```java
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
```

---

### Spring Boot 3 常见

```java
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
```

---

### `@Validated`

Spring Boot 2 和 Spring Boot 3 都是：

```java
import org.springframework.validation.annotation.Validated;
```

---

## 15. 使用建议

| 场景 | 推荐 |
|---|---|
| 普通对象参数校验 | `@Valid` 或 `@Validated` |
| 分组校验 | `@Validated` |
| 嵌套对象校验 | 字段上使用 `@Valid` |
| Controller 普通参数校验 | 类上加 `@Validated` |
| 新增、修改不同校验规则 | `@Validated(AddGroup.class)` / `@Validated(UpdateGroup.class)` |
| Spring 项目统一写法 | 常用 `@Validated`，嵌套字段配合 `@Valid` |

---

## 16. 常见用法总结

### 16.1 普通对象校验

```java
@PostMapping("/user")
public String saveUser(@RequestBody @Valid UserDTO userDTO) {
    return "success";
}
```

或者：

```java
@PostMapping("/user")
public String saveUser(@RequestBody @Validated UserDTO userDTO) {
    return "success";
}
```

---

### 16.2 分组校验

```java
@PostMapping("/user")
public String addUser(@RequestBody @Validated(AddGroup.class) UserDTO userDTO) {
    return "success";
}
```

```java
@PutMapping("/user")
public String updateUser(@RequestBody @Validated(UpdateGroup.class) UserDTO userDTO) {
    return "success";
}
```

---

### 16.3 嵌套校验

```java
public class OrderDTO {

    @Valid
    private UserDTO user;
}
```

---

### 16.4 普通参数校验

```java
@RestController
@Validated
public class UserController {

    @GetMapping("/user")
    public String getUser(@NotNull(message = "id 不能为空") Long id) {
        return "success";
    }
}
```

---

## 17. 总结

`@Valid` 和 `@Validated` 都可以用于参数校验。

`@Valid` 是 Java Bean Validation 标准提供的注解，主要用于普通对象校验和嵌套对象校验。比如 Controller 中对 `@RequestBody` 参数进行校验，或者在对象字段上标注 `@Valid` 触发嵌套校验。

`@Validated` 是 Spring 提供的注解，是对 `@Valid` 的增强。它除了可以做普通校验外，还支持分组校验，也常用于类上开启方法级参数校验。

如果只是普通参数校验，`@Valid` 和 `@Validated` 基本都可以；如果是新增、修改等不同场景需要不同校验规则，就应该使用 `@Validated` 的分组校验。嵌套对象校验通常在字段上使用 `@Valid`。

---

## 18. 一句话总结

> `@Valid` 是标准校验注解，适合普通校验和嵌套校验；`@Validated` 是 Spring 提供的增强版，支持分组校验和方法级校验。实际开发中，普通校验两者都可以，分组校验用 `@Validated`。
