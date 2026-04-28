# AWK 常用命令

## 1. AWK 是什么

`awk` 是 Linux 下常用的文本处理工具，适合处理**按行、按列**的数据。

常用于：

- 日志分析
- 文本切割
- 字段提取
- 求和统计
- 条件过滤
- 格式化输出

基本格式：

```bash
awk '条件 {动作}' 文件
```

示例：

```bash
awk '{print $1}' access.log
```

表示打印每一行的第 1 列。

---

## 2. AWK 基本语法

```bash
awk 'pattern { action }' file
```

| 部分 | 说明 |
|---|---|
| `pattern` | 匹配条件 |
| `action` | 执行动作 |
| `file` | 处理的文件 |

如果省略条件，表示处理每一行：

```bash
awk '{print $1}' file.txt
```

如果省略动作，默认打印整行：

```bash
awk '/error/' file.txt
```

等价于：

```bash
awk '/error/ {print $0}' file.txt
```

---

## 3. AWK 内置变量

| 变量 | 说明 |
|---|---|
| `$0` | 当前整行内容 |
| `$1` | 第 1 列 |
| `$2` | 第 2 列 |
| `$NF` | 最后一列 |
| `NF` | 当前行字段数量 |
| `NR` | 当前处理的行号 |
| `FNR` | 当前文件的行号 |
| `FS` | 输入字段分隔符 |
| `OFS` | 输出字段分隔符 |
| `RS` | 输入记录分隔符 |
| `ORS` | 输出记录分隔符 |

---

## 4. 打印指定列

### 打印第 1 列

```bash
awk '{print $1}' file.txt
```

### 打印第 1 列和第 3 列

```bash
awk '{print $1, $3}' file.txt
```

### 打印整行

```bash
awk '{print $0}' file.txt
```

### 打印最后一列

```bash
awk '{print $NF}' file.txt
```

### 打印倒数第二列

```bash
awk '{print $(NF-1)}' file.txt
```

---

## 5. 指定分隔符

默认情况下，`awk` 使用空格或 Tab 作为分隔符。

### 使用逗号分隔

```bash
awk -F ',' '{print $1, $2}' file.csv
```

### 使用冒号分隔

```bash
awk -F ':' '{print $1}' /etc/passwd
```

### 使用多个分隔符

```bash
awk -F '[,:]' '{print $1, $2}' file.txt
```

表示用逗号或冒号作为分隔符。

---

## 6. 设置输出分隔符

默认输出字段之间用空格分隔。

```bash
awk -F ',' '{print $1, $2}' file.csv
```

如果想用逗号输出：

```bash
awk -F ',' 'BEGIN{OFS=","} {print $1, $2}' file.csv
```

---

## 7. BEGIN 和 END

### BEGIN

`BEGIN` 在读取文件前执行。

```bash
awk 'BEGIN{print "start"} {print $0}' file.txt
```

### END

`END` 在所有行处理完后执行。

```bash
awk '{print $0} END{print "end"}' file.txt
```

### 示例：统计总行数

```bash
awk 'END{print NR}' file.txt
```

---

## 8. 条件过滤

### 打印第 3 列大于 100 的行

```bash
awk '$3 > 100 {print $0}' file.txt
```

### 打印第 1 列等于 `Tom` 的行

```bash
awk '$1 == "Tom" {print $0}' file.txt
```

### 打印第 2 列不等于 `success` 的行

```bash
awk '$2 != "success" {print $0}' file.txt
```

### 打印字段数大于 5 的行

```bash
awk 'NF > 5 {print $0}' file.txt
```

---

## 9. 正则匹配

### 匹配包含 error 的行

```bash
awk '/error/ {print $0}' app.log
```

### 第 3 列匹配 error

```bash
awk '$3 ~ /error/ {print $0}' app.log
```

### 第 3 列不匹配 error

```bash
awk '$3 !~ /error/ {print $0}' app.log
```

### 忽略大小写匹配

```bash
awk 'BEGIN{IGNORECASE=1} /error/ {print $0}' app.log
```

---

## 10. 多条件判断

### 与条件

```bash
awk '$3 > 100 && $4 == "success" {print $0}' file.txt
```

### 或条件

```bash
awk '$3 > 100 || $4 == "fail" {print $0}' file.txt
```

### 非条件

```bash
awk '!/error/ {print $0}' app.log
```

---

## 11. 求和统计

### 统计第 3 列总和

```bash
awk '{sum += $3} END{print sum}' file.txt
```

### 统计第 3 列平均值

```bash
awk '{sum += $3} END{print sum / NR}' file.txt
```

### 只统计第 3 列大于 100 的总和

```bash
awk '$3 > 100 {sum += $3} END{print sum}' file.txt
```

---

## 12. 计数统计

### 统计行数

```bash
awk 'END{print NR}' file.txt
```

### 统计包含 error 的行数

```bash
awk '/error/ {count++} END{print count}' app.log
```

### 按第 1 列分组计数

```bash
awk '{count[$1]++} END{for (i in count) print i, count[i]}' file.txt
```

---

## 13. 分组求和

### 按第 1 列分组，对第 3 列求和

```bash
awk '{sum[$1] += $3} END{for (i in sum) print i, sum[i]}' file.txt
```

示例数据：

```text
A 10
A 20
B 30
B 40
```

命令：

```bash
awk '{sum[$1] += $2} END{for (i in sum) print i, sum[i]}' file.txt
```

输出：

```text
A 30
B 70
```

---

## 14. 格式化输出 printf

`print` 会自动换行，`printf` 不会自动换行。

```bash
awk '{printf "%s %s\n", $1, $2}' file.txt
```

### 格式化对齐

```bash
awk '{printf "%-10s %-10s\n", $1, $2}' file.txt
```

| 格式 | 说明 |
|---|---|
| `%s` | 字符串 |
| `%d` | 整数 |
| `%f` | 浮点数 |
| `%.2f` | 保留 2 位小数 |
| `%-10s` | 左对齐，占 10 位 |

---

## 15. if 判断

```bash
awk '{
    if ($3 >= 60) {
        print $1, "pass"
    } else {
        print $1, "fail"
    }
}' file.txt
```

单行写法：

```bash
awk '{if ($3 >= 60) print $1, "pass"; else print $1, "fail"}' file.txt
```

---

## 16. for 循环

### 打印每一列

```bash
awk '{
    for (i = 1; i <= NF; i++) {
        print $i
    }
}' file.txt
```

### 打印每行字段数量

```bash
awk '{print NR, NF}' file.txt
```

---

## 17. 数组

### 按 IP 统计访问次数

```bash
awk '{count[$1]++} END{for (ip in count) print ip, count[ip]}' access.log
```

### 按状态码统计数量

```bash
awk '{count[$9]++} END{for (code in count) print code, count[code]}' access.log
```

---

## 18. 常用日志分析示例

### 统计访问量最高的 IP

```bash
awk '{count[$1]++} END{for (ip in count) print ip, count[ip]}' access.log | sort -k2 -nr | head
```

### 统计 HTTP 状态码数量

```bash
awk '{count[$9]++} END{for (code in count) print code, count[code]}' access.log
```

### 统计 500 错误请求

```bash
awk '$9 == 500 {print $0}' access.log
```

### 统计接口访问次数

假设接口在第 7 列：

```bash
awk '{count[$7]++} END{for (url in count) print url, count[url]}' access.log | sort -k2 -nr | head
```

### 统计响应时间大于 1 秒的请求

假设响应时间在最后一列：

```bash
awk '$NF > 1 {print $0}' access.log
```

---

## 19. 和 grep、sort、uniq 组合

### 查找 error 并打印第 1 列

```bash
grep 'error' app.log | awk '{print $1}'
```

### 统计第 1 列出现次数

```bash
awk '{print $1}' file.txt | sort | uniq -c | sort -nr
```

### 找出访问次数最多的前 10 个 IP

```bash
awk '{print $1}' access.log | sort | uniq -c | sort -nr | head -10
```

---

## 20. 修改字段内容

### 将第 2 列改为 success

```bash
awk '{$2="success"; print $0}' file.txt
```

### 删除第 2 列

```bash
awk '{$2=""; print $0}' file.txt
```

---

## 21. 打印指定行

### 打印第 10 行

```bash
awk 'NR == 10 {print $0}' file.txt
```

### 打印第 10 到 20 行

```bash
awk 'NR >= 10 && NR <= 20 {print $0}' file.txt
```

### 打印奇数行

```bash
awk 'NR % 2 == 1 {print $0}' file.txt
```

### 打印偶数行

```bash
awk 'NR % 2 == 0 {print $0}' file.txt
```

---

## 22. 常见实战命令

### 查看 `/etc/passwd` 用户名

```bash
awk -F ':' '{print $1}' /etc/passwd
```

### 查看用户名和 shell

```bash
awk -F ':' '{print $1, $7}' /etc/passwd
```

### 统计 nginx access.log 中 IP 访问次数

```bash
awk '{ip[$1]++} END{for (i in ip) print i, ip[i]}' access.log | sort -k2 -nr
```

### 统计 nginx 状态码

```bash
awk '{code[$9]++} END{for (i in code) print i, code[i]}' access.log
```

### 统计大于 100ms 的请求

```bash
awk '$NF > 0.1 {print $0}' access.log
```

### 打印文件第 1 列去重

```bash
awk '{print $1}' file.txt | sort | uniq
```

---

## 23. AWK 常用命令总结

| 命令 | 说明 |
|---|---|
| `awk '{print $0}' file` | 打印整行 |
| `awk '{print $1}' file` | 打印第 1 列 |
| `awk '{print $NF}' file` | 打印最后一列 |
| `awk -F ':' '{print $1}' file` | 指定冒号分隔 |
| `awk 'NR==10{print}' file` | 打印第 10 行 |
| `awk 'NR>=10&&NR<=20{print}' file` | 打印第 10 到 20 行 |
| `awk '/error/{print}' file` | 打印包含 error 的行 |
| `awk '$3>100{print}' file` | 打印第 3 列大于 100 的行 |
| `awk '{sum+=$3}END{print sum}' file` | 第 3 列求和 |
| `awk '{count[$1]++}END{for(i in count)print i,count[i]}' file` | 按第 1 列计数 |

---

## 24. 一句话总结

> `awk` 适合处理结构化文本，核心就是按行读取、按列切分，然后根据条件过滤、统计和格式化输出。
