# Linux grep 命令

## 1. grep 是什么

`grep` 用于在文件或输入内容中搜索匹配指定模式的行。

常用于：

- 查找日志关键字
- 过滤命令输出
- 搜索代码内容
- 统计匹配行数
- 排查线上问题

基本语法：

```bash
grep [选项] "匹配内容" 文件
```

示例：

```bash
grep "error" app.log
```

表示在 `app.log` 中查找包含 `error` 的行。

---

## 2. 基本使用

### 查找包含关键字的行

```bash
grep "error" app.log
```

### 在多个文件中查找

```bash
grep "error" app.log server.log
```

### 在当前目录所有文件中查找

```bash
grep "error" *
```

### 忽略大小写

```bash
grep -i "error" app.log
```

可以匹配：

```text
error
Error
ERROR
```

---

## 3. 显示行号

```bash
grep -n "error" app.log
```

输出示例：

```text
12:error occurred
35:error timeout
```

其中 `12`、`35` 是行号。

---

## 4. 显示文件名

### 多文件搜索时显示文件名

```bash
grep -H "error" *.log
```

### 不显示文件名

```bash
grep -h "error" *.log
```

---

## 5. 递归搜索

### 递归查找目录下所有文件

```bash
grep -r "error" /var/log
```

### 递归查找并显示行号

```bash
grep -rn "error" /var/log
```

### 递归查找当前目录

```bash
grep -rn "userService" .
```

---

## 6. 只显示匹配文件名

### 显示包含关键字的文件名

```bash
grep -l "error" *.log
```

### 显示不包含关键字的文件名

```bash
grep -L "error" *.log
```

---

## 7. 反向匹配

### 查找不包含 error 的行

```bash
grep -v "error" app.log
```

### 查找不包含 error 和 warn 的行

```bash
grep -v "error" app.log | grep -v "warn"
```

---

## 8. 统计匹配行数

```bash
grep -c "error" app.log
```

统计包含 `error` 的行数。

### 递归统计

```bash
grep -rc "error" /var/log
```

---

## 9. 只输出匹配内容

```bash
grep -o "error" app.log
```

如果一行中有多个匹配，会分别输出。

示例：

```bash
echo "error error warn" | grep -o "error"
```

输出：

```text
error
error
```

---

## 10. 精确匹配整行

```bash
grep -x "success" app.log
```

只匹配整行等于 `success` 的行。

---

## 11. 匹配完整单词

```bash
grep -w "error" app.log
```

可以匹配：

```text
error happened
```

不会匹配：

```text
myerror
errorCode
```

---

## 12. 使用正则表达式

### 匹配 error 或 warn

```bash
grep -E "error|warn" app.log
```

或者：

```bash
egrep "error|warn" app.log
```

---

### 匹配数字

```bash
grep -E "[0-9]+" app.log
```

---

### 匹配手机号示例

```bash
grep -E "1[3-9][0-9]{9}" file.txt
```

---

### 匹配 IP 地址

```bash
grep -E "([0-9]{1,3}\.){3}[0-9]{1,3}" access.log
```

---

## 13. 固定字符串匹配

如果不想让 grep 把内容当正则，可以使用 `-F`。

```bash
grep -F "a.b" file.txt
```

这会匹配普通字符串 `a.b`，而不是正则含义中的任意字符。

也可以使用：

```bash
fgrep "a.b" file.txt
```

---

## 14. 显示匹配上下文

### 显示匹配行后 3 行

```bash
grep -A 3 "error" app.log
```

`A` 表示 After。

---

### 显示匹配行前 3 行

```bash
grep -B 3 "error" app.log
```

`B` 表示 Before。

---

### 显示匹配行前后各 3 行

```bash
grep -C 3 "error" app.log
```

`C` 表示 Context。

---

## 15. 多关键字匹配

### 匹配 error 或 warn

```bash
grep -E "error|warn" app.log
```

### 使用多个 `-e`

```bash
grep -e "error" -e "warn" app.log
```

### 从文件读取关键字

```bash
grep -f keywords.txt app.log
```

`keywords.txt` 内容示例：

```text
error
warn
timeout
```

---

## 16. 排除文件或目录

### 排除某个文件

```bash
grep -rn "error" . --exclude="debug.log"
```

### 排除某类文件

```bash
grep -rn "error" . --exclude="*.class"
```

### 排除目录

```bash
grep -rn "error" . --exclude-dir="target"
```

### 排除多个目录

```bash
grep -rn "error" . --exclude-dir={target,node_modules,.git}
```

---

## 17. 配合管道使用

### 过滤 ps 结果

```bash
ps -ef | grep java
```

### 避免 grep 匹配自身

```bash
ps -ef | grep java | grep -v grep
```

或者：

```bash
ps -ef | grep "[j]ava"
```

---

### 查看端口

```bash
netstat -tunlp | grep 8080
```

或者：

```bash
ss -tunlp | grep 8080
```

---

### 查看日志关键字

```bash
tail -f app.log | grep "error"
```

---

## 18. 常用日志排查命令

### 查找错误日志

```bash
grep -n "ERROR" app.log
```

### 忽略大小写查找 error

```bash
grep -in "error" app.log
```

### 查看 error 前后 5 行

```bash
grep -C 5 "ERROR" app.log
```

### 查看 timeout 后 10 行

```bash
grep -A 10 "timeout" app.log
```

### 查看 exception 前 20 行

```bash
grep -B 20 "Exception" app.log
```

### 查找多个关键字

```bash
grep -E "ERROR|Exception|timeout" app.log
```

### 实时查看错误日志

```bash
tail -f app.log | grep -E "ERROR|Exception|timeout"
```

---

## 19. 和 find 组合使用

### 查找所有 log 文件中包含 error 的内容

```bash
find /var/log -type f -name "*.log" -exec grep -Hn "error" {} \;
```

### 使用 xargs

```bash
find /var/log -type f -name "*.log" | xargs grep -Hn "error"
```

### 处理带空格文件名

```bash
find /var/log -type f -name "*.log" -print0 | xargs -0 grep -Hn "error"
```

---

## 20. 和 awk 组合使用

### 查找 error 行并打印第 1 列

```bash
grep "error" app.log | awk '{print $1}'
```

### 统计 error 日志中 IP 出现次数

假设 IP 在第 1 列：

```bash
grep "error" access.log | awk '{count[$1]++} END{for (ip in count) print ip, count[ip]}'
```

---

## 21. 和 sort、uniq 组合使用

### 统计关键字出现最多的内容

```bash
grep "error" app.log | sort | uniq -c | sort -nr
```

### 统计访问最多的 IP

假设 IP 在日志第 1 列：

```bash
grep "GET" access.log | awk '{print $1}' | sort | uniq -c | sort -nr | head
```

---

## 22. 二进制文件处理

有时候 grep 会提示：

```text
Binary file xxx matches
```

可以使用：

```bash
grep -a "error" file.log
```

`-a` 表示把二进制文件当作文本处理。

---

## 23. 常用参数总结

| 参数 | 说明 |
|---|---|
| `-i` | 忽略大小写 |
| `-n` | 显示行号 |
| `-r` | 递归搜索目录 |
| `-R` | 递归搜索并跟随软链接 |
| `-v` | 反向匹配 |
| `-c` | 统计匹配行数 |
| `-l` | 只显示匹配的文件名 |
| `-L` | 只显示不匹配的文件名 |
| `-o` | 只输出匹配内容 |
| `-w` | 匹配完整单词 |
| `-x` | 匹配整行 |
| `-E` | 使用扩展正则 |
| `-F` | 固定字符串匹配，不按正则解析 |
| `-A n` | 显示匹配行后 n 行 |
| `-B n` | 显示匹配行前 n 行 |
| `-C n` | 显示匹配行前后各 n 行 |
| `-H` | 显示文件名 |
| `-h` | 不显示文件名 |
| `--exclude` | 排除文件 |
| `--exclude-dir` | 排除目录 |

---

## 24. 工作常用命令

| 场景 | 命令 |
|---|---|
| 查找关键字 | `grep "error" app.log` |
| 显示行号 | `grep -n "error" app.log` |
| 忽略大小写 | `grep -i "error" app.log` |
| 递归查找 | `grep -rn "keyword" .` |
| 查找多个关键字 | `grep -E "error|warn|timeout" app.log` |
| 查看上下文 | `grep -C 5 "Exception" app.log` |
| 反向过滤 | `grep -v "debug" app.log` |
| 统计行数 | `grep -c "ERROR" app.log` |
| 只显示文件名 | `grep -l "error" *.log` |
| 固定字符串匹配 | `grep -F "a.b" file.txt` |
| 实时过滤日志 | `tail -f app.log | grep "ERROR"` |

---

## 25. 一句话总结

> `grep` 是 Linux 中最常用的文本搜索命令，核心作用是按照关键字或正则表达式过滤文本内容，常与 `tail`、`find`、`awk`、`sort`、`uniq` 等命令组合用于日志排查和文本分析。
