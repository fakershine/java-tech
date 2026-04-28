# Linux find 命令

## 1. find 是什么

`find` 用于在指定目录下查找文件或目录。

基本语法：

```bash
find 查找路径 查找条件 执行动作
```

示例：

```bash
find /home -name "*.log"
```

表示在 `/home` 目录下查找所有 `.log` 文件。

---

## 2. 按文件名查找

### 精确匹配文件名

```bash
find /home -name "test.txt"
```

### 忽略大小写

```bash
find /home -iname "test.txt"
```

可以匹配：

```text
test.txt
Test.txt
TEST.txt
```

### 使用通配符

```bash
find /var/log -name "*.log"
```

查找所有 `.log` 文件。

---

## 3. 按文件类型查找

| 类型 | 说明 |
|---|---|
| `f` | 普通文件 |
| `d` | 目录 |
| `l` | 软链接 |
| `b` | 块设备 |
| `c` | 字符设备 |
| `s` | socket 文件 |
| `p` | 管道文件 |

### 查找普通文件

```bash
find /home -type f
```

### 查找目录

```bash
find /home -type d
```

### 查找软链接

```bash
find /home -type l
```

---

## 4. 按文件大小查找

| 写法 | 说明 |
|---|---|
| `+100M` | 大于 100MB |
| `-100M` | 小于 100MB |
| `100M` | 等于 100MB |

常用单位：

| 单位 | 说明 |
|---|---|
| `c` | 字节 |
| `k` | KB |
| `M` | MB |
| `G` | GB |

### 查找大于 100MB 的文件

```bash
find / -type f -size +100M
```

### 查找小于 10KB 的文件

```bash
find /home -type f -size -10k
```

### 查找大于 1GB 的文件

```bash
find / -type f -size +1G
```

---

## 5. 按修改时间查找

### 常用时间参数

| 参数 | 说明 |
|---|---|
| `-mtime` | 按文件内容修改时间，单位：天 |
| `-mmin` | 按文件内容修改时间，单位：分钟 |
| `-atime` | 按访问时间，单位：天 |
| `-amin` | 按访问时间，单位：分钟 |
| `-ctime` | 按状态变更时间，单位：天 |
| `-cmin` | 按状态变更时间，单位：分钟 |

### 查找 7 天前修改过的文件

```bash
find /home -type f -mtime +7
```

### 查找 7 天内修改过的文件

```bash
find /home -type f -mtime -7
```

### 查找最近 30 分钟修改过的文件

```bash
find /home -type f -mmin -30
```

### 查找 30 分钟前修改过的文件

```bash
find /home -type f -mmin +30
```

---

## 6. 按权限查找

### 查找权限为 777 的文件

```bash
find /home -type f -perm 777
```

### 查找有执行权限的文件

```bash
find /home -type f -perm /111
```

### 查找用户有写权限的文件

```bash
find /home -type f -perm /200
```

---

## 7. 按用户和用户组查找

### 查找属于某个用户的文件

```bash
find /home -user tom
```

### 查找属于某个用户组的文件

```bash
find /home -group dev
```

### 查找没有所属用户的文件

```bash
find / -nouser
```

### 查找没有所属用户组的文件

```bash
find / -nogroup
```

---

## 8. 多条件查找

### 与条件

默认就是与条件：

```bash
find /home -type f -name "*.log" -size +100M
```

表示查找 `.log` 文件，并且大小大于 100MB。

---

### 或条件

```bash
find /home \( -name "*.log" -o -name "*.txt" \)
```

表示查找 `.log` 或 `.txt` 文件。

---

### 非条件

```bash
find /home -type f ! -name "*.log"
```

表示查找不是 `.log` 的普通文件。

---

## 9. 查找后删除

### 删除 `.log` 文件

```bash
find /home -type f -name "*.log" -delete
```

### 删除 7 天前的日志

```bash
find /var/log -type f -name "*.log" -mtime +7 -delete
```

更安全的写法是先查看：

```bash
find /var/log -type f -name "*.log" -mtime +7
```

确认无误后再删除。

---

## 10. 查找后执行命令

### 使用 `-exec`

```bash
find /home -type f -name "*.log" -exec ls -lh {} \;
```

说明：

| 符号 | 说明 |
|---|---|
| `{}` | 表示 find 找到的文件 |
| `\;` | 表示命令结束 |

---

### 删除查找到的文件

```bash
find /home -type f -name "*.tmp" -exec rm -f {} \;
```

---

### 批量修改权限

```bash
find /home -type f -name "*.sh" -exec chmod +x {} \;
```

---

### 批量移动文件

```bash
find /home -type f -name "*.log" -exec mv {} /tmp/logs/ \;
```

---

## 11. 使用 xargs

`xargs` 可以把 `find` 的结果传给其他命令。

### 删除文件

```bash
find /home -type f -name "*.tmp" | xargs rm -f
```

### 查看文件大小

```bash
find /home -type f -name "*.log" | xargs ls -lh
```

### 更安全地处理带空格文件名

```bash
find /home -type f -name "*.log" -print0 | xargs -0 ls -lh
```

---

## 12. 查找空文件和空目录

### 查找空文件

```bash
find /home -type f -empty
```

### 查找空目录

```bash
find /home -type d -empty
```

### 删除空目录

```bash
find /home -type d -empty -delete
```

---

## 13. 限制查找深度

### 只查找当前目录，不递归子目录

```bash
find /home -maxdepth 1 -type f
```

### 最多查找 2 层目录

```bash
find /home -maxdepth 2 -type f
```

### 至少从第 2 层开始查找

```bash
find /home -mindepth 2 -type f
```

---

## 14. 按文件内容配合 grep 查找

### 查找包含 error 的日志文件

```bash
find /var/log -type f -name "*.log" -exec grep -n "error" {} \;
```

### 显示文件名

```bash
find /var/log -type f -name "*.log" -exec grep -Hn "error" {} \;
```

### 使用 xargs

```bash
find /var/log -type f -name "*.log" | xargs grep -n "error"
```

---

## 15. 常用实战命令

### 查找当前目录下所有 Java 文件

```bash
find . -name "*.java"
```

### 查找当前目录下所有目录

```bash
find . -type d
```

### 查找大于 500MB 的文件

```bash
find / -type f -size +500M
```

### 查找最近 1 天修改过的文件

```bash
find . -type f -mtime -1
```

### 查找最近 10 分钟修改过的文件

```bash
find . -type f -mmin -10
```

### 查找 30 天前的日志并删除

```bash
find /var/log -type f -name "*.log" -mtime +30 -delete
```

### 查找所有空文件

```bash
find . -type f -empty
```

### 查找所有空目录

```bash
find . -type d -empty
```

### 查找权限为 777 的文件

```bash
find / -type f -perm 777
```

### 查找并批量修改权限

```bash
find . -type f -name "*.sh" -exec chmod +x {} \;
```

### 查找文件并显示详细信息

```bash
find . -type f -name "*.log" -exec ls -lh {} \;
```

---

## 16. find 常用参数总结

| 参数 | 说明 |
|---|---|
| `-name` | 按名称查找，区分大小写 |
| `-iname` | 按名称查找，忽略大小写 |
| `-type f` | 查找普通文件 |
| `-type d` | 查找目录 |
| `-size` | 按文件大小查找 |
| `-mtime` | 按修改时间查找，单位天 |
| `-mmin` | 按修改时间查找，单位分钟 |
| `-user` | 按所属用户查找 |
| `-group` | 按所属用户组查找 |
| `-perm` | 按权限查找 |
| `-empty` | 查找空文件或空目录 |
| `-delete` | 删除查找到的文件 |
| `-exec` | 对查找到的文件执行命令 |
| `-maxdepth` | 限制最大查找深度 |
| `-mindepth` | 限制最小查找深度 |

---

## 17. 工作常用命令

| 场景 | 命令 |
|---|---|
| 查找文件名 | `find . -name "app.log"` |
| 查找某类文件 | `find . -name "*.java"` |
| 查找目录 | `find . -type d -name "logs"` |
| 查找大文件 | `find / -type f -size +1G` |
| 查找最近修改文件 | `find . -type f -mtime -1` |
| 查找最近几分钟修改 | `find . -type f -mmin -10` |
| 删除过期日志 | `find /var/log -name "*.log" -mtime +30 -delete` |
| 查找空文件 | `find . -type f -empty` |
| 查找并执行命令 | `find . -name "*.sh" -exec chmod +x {} \;` |

---

## 18. 一句话总结

> `find` 是 Linux 中最常用的文件查找命令，可以按照文件名、类型、大小、时间、权限、用户等条件查找文件，并支持删除、移动、修改权限、配合 grep 搜索内容等操作。
