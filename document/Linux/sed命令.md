# Linux sed 命令

## 1. sed 是什么

`sed` 是 Linux 下常用的文本流编辑工具，主要用于对文本进行：

- 查找
- 替换
- 删除
- 插入
- 追加
- 打印指定行
- 批量修改文件内容

`sed` 全称是 **Stream Editor**，即流编辑器。

基本语法：

```bash
sed [选项] '命令' 文件
```

示例：

```bash
sed 's/old/new/' file.txt
```

表示把每一行中第一个 `old` 替换成 `new`。

---

## 2. sed 基本格式

```bash
sed '地址 命令' 文件
```

| 部分 | 说明 |
|---|---|
| 地址 | 指定处理哪些行 |
| 命令 | 对匹配行执行什么操作 |
| 文件 | 要处理的文件 |

常见命令：

| 命令 | 说明 |
|---|---|
| `s` | 替换 |
| `p` | 打印 |
| `d` | 删除 |
| `a` | 在指定行后追加 |
| `i` | 在指定行前插入 |
| `c` | 替换整行 |
| `y` | 字符转换 |

---

## 3. 替换文本

### 替换每行第一个匹配

```bash
sed 's/old/new/' file.txt
```

### 替换每行所有匹配

```bash
sed 's/old/new/g' file.txt
```

### 只替换第 2 个匹配

```bash
sed 's/old/new/2' file.txt
```

### 替换并打印发生替换的行

```bash
sed -n 's/old/new/p' file.txt
```

---

## 4. 直接修改文件

默认情况下，`sed` 只输出结果，不会修改原文件。

### 直接修改原文件

```bash
sed -i 's/old/new/g' file.txt
```

### 修改前备份原文件

```bash
sed -i.bak 's/old/new/g' file.txt
```

执行后会生成：

```text
file.txt.bak
```

---

## 5. 指定行替换

### 只替换第 3 行

```bash
sed '3s/old/new/g' file.txt
```

### 替换第 3 到第 5 行

```bash
sed '3,5s/old/new/g' file.txt
```

### 从第 3 行到最后一行替换

```bash
sed '3,$s/old/new/g' file.txt
```

---

## 6. 使用不同分隔符

默认替换格式是：

```bash
sed 's/old/new/g'
```

如果内容中有 `/`，可以换分隔符。

### 替换路径

```bash
sed 's#/usr/local#/opt/app#g' file.txt
```

也可以使用：

```bash
sed 's|/usr/local|/opt/app|g' file.txt
```

这样比转义 `/` 更清晰。

---

## 7. 删除行

### 删除第 3 行

```bash
sed '3d' file.txt
```

### 删除第 3 到第 5 行

```bash
sed '3,5d' file.txt
```

### 删除最后一行

```bash
sed '$d' file.txt
```

### 删除空行

```bash
sed '/^$/d' file.txt
```

### 删除包含 error 的行

```bash
sed '/error/d' file.txt
```

### 删除不包含 error 的行

```bash
sed '/error/!d' file.txt
```

---

## 8. 打印指定行

默认 `sed` 会打印所有行，如果只想打印指定行，需要加 `-n`。

### 打印第 3 行

```bash
sed -n '3p' file.txt
```

### 打印第 3 到第 5 行

```bash
sed -n '3,5p' file.txt
```

### 打印最后一行

```bash
sed -n '$p' file.txt
```

### 打印包含 error 的行

```bash
sed -n '/error/p' file.txt
```

---

## 9. 插入和追加

### 在第 3 行前插入内容

```bash
sed '3i\insert text' file.txt
```

### 在第 3 行后追加内容

```bash
sed '3a\append text' file.txt
```

### 在匹配行前插入

```bash
sed '/error/i\before error' file.txt
```

### 在匹配行后追加

```bash
sed '/error/a\after error' file.txt
```

---

## 10. 替换整行

### 将第 3 行替换为新内容

```bash
sed '3c\new line content' file.txt
```

### 将包含 error 的行替换为新内容

```bash
sed '/error/c\this line has been replaced' file.txt
```

---

## 11. 多个 sed 命令

### 使用多个 `-e`

```bash
sed -e 's/error/ERROR/g' -e 's/warn/WARN/g' app.log
```

### 使用分号

```bash
sed 's/error/ERROR/g; s/warn/WARN/g' app.log
```

---

## 12. 正则匹配

### 匹配以 error 开头的行

```bash
sed -n '/^error/p' app.log
```

### 匹配以 .log 结尾的行

```bash
sed -n '/\.log$/p' file.txt
```

### 匹配包含数字的行

```bash
sed -n '/[0-9]/p' file.txt
```

### 使用扩展正则

不同系统参数可能不同，Linux 常用：

```bash
sed -E 's/[0-9]+/NUM/g' file.txt
```

或者：

```bash
sed -r 's/[0-9]+/NUM/g' file.txt
```

---

## 13. 分组替换

### 使用分组引用

```bash
echo "name=Tom" | sed -E 's/name=(.*)/user=\1/'
```

输出：

```text
user=Tom
```

### 调换两个字段

```bash
echo "Tom 18" | sed -E 's/([A-Za-z]+) ([0-9]+)/\2 \1/'
```

输出：

```text
18 Tom
```

---

## 14. 替换特殊字符

### 替换 `/`

推荐换分隔符：

```bash
sed 's#/home/user#/data/app#g' file.txt
```

### 替换 `&`

在 replacement 中，`&` 表示匹配到的完整内容。

如果要替换成普通 `&`，需要转义：

```bash
sed 's/Tom/Tom \& Jerry/g' file.txt
```

### 替换反斜杠

```bash
sed 's/\\/\\\\/g' file.txt
```

---

## 15. 大小写转换

GNU sed 支持 `\L`、`\U`。

### 转大写

```bash
echo "hello" | sed 's/.*/\U&/'
```

输出：

```text
HELLO
```

### 转小写

```bash
echo "HELLO" | sed 's/.*/\L&/'
```

输出：

```text
hello
```

---

## 16. 行范围操作

### 从匹配 start 的行到匹配 end 的行打印

```bash
sed -n '/start/,/end/p' file.txt
```

### 从匹配 start 的行到匹配 end 的行删除

```bash
sed '/start/,/end/d' file.txt
```

### 从第 10 行到匹配 end 的行打印

```bash
sed -n '10,/end/p' file.txt
```

---

## 17. 每隔几行处理

### 打印奇数行

```bash
sed -n '1~2p' file.txt
```

### 打印偶数行

```bash
sed -n '2~2p' file.txt
```

说明：`1~2` 表示从第 1 行开始，每隔 2 行处理一次。

---

## 18. 配合管道使用

### 替换命令输出中的内容

```bash
ps -ef | sed 's/java/JAVA/g'
```

### 查看日志并替换敏感信息

```bash
cat app.log | sed -E 's/[0-9]{11}/PHONE/g'
```

### tail 实时查看并高亮替换

```bash
tail -f app.log | sed 's/ERROR/[ERROR]/g'
```

---

## 19. 常见日志处理示例

### 打印第 100 到 200 行日志

```bash
sed -n '100,200p' app.log
```

### 删除空行

```bash
sed '/^$/d' app.log
```

### 删除注释行

```bash
sed '/^#/d' config.conf
```

### 删除空行和注释行

```bash
sed '/^$/d; /^#/d' config.conf
```

### 替换日志中的手机号

```bash
sed -E 's/1[3-9][0-9]{9}/PHONE/g' app.log
```

### 替换日志中的 IP

```bash
sed -E 's/([0-9]{1,3}\.){3}[0-9]{1,3}/IP/g' app.log
```

---

## 20. 常用实战命令

### 批量替换配置项

```bash
sed -i 's/server.port=8080/server.port=9090/' application.properties
```

### 修改 YAML 配置

```bash
sed -i 's/port: 8080/port: 9090/' application.yml
```

### 替换文件中的域名

```bash
sed -i 's#http://old.com#https://new.com#g' config.txt
```

### 删除所有空行

```bash
sed -i '/^$/d' file.txt
```

### 删除所有注释行

```bash
sed -i '/^#/d' file.txt
```

### 打印文件指定范围

```bash
sed -n '20,50p' file.txt
```

---

## 21. 和 grep、awk 的区别

| 命令 | 主要用途 |
|---|---|
| `grep` | 查找、过滤文本 |
| `sed` | 替换、删除、插入、修改文本 |
| `awk` | 按列处理、统计、格式化输出 |

简单理解：

```text
grep：找内容
sed：改内容
awk：按列分析内容
```

---

## 22. sed 常用参数总结

| 参数 | 说明 |
|---|---|
| `-n` | 取消默认输出，常和 `p` 配合 |
| `-i` | 直接修改文件 |
| `-i.bak` | 修改前备份文件 |
| `-e` | 执行多个 sed 命令 |
| `-E` | 使用扩展正则 |
| `-r` | 使用扩展正则，GNU sed 支持 |

---

## 23. sed 常用命令总结

| 命令 | 说明 |
|---|---|
| `s/old/new/` | 替换每行第一个匹配 |
| `s/old/new/g` | 替换每行所有匹配 |
| `p` | 打印 |
| `d` | 删除 |
| `a\text` | 行后追加 |
| `i\text` | 行前插入 |
| `c\text` | 替换整行 |
| `y/abc/ABC/` | 字符转换 |

---

## 24. 工作常用命令

| 场景 | 命令 |
|---|---|
| 替换文本 | `sed 's/old/new/g' file` |
| 直接修改文件 | `sed -i 's/old/new/g' file` |
| 修改并备份 | `sed -i.bak 's/old/new/g' file` |
| 打印指定行 | `sed -n '10p' file` |
| 打印范围行 | `sed -n '10,20p' file` |
| 删除指定行 | `sed '10d' file` |
| 删除空行 | `sed '/^$/d' file` |
| 删除注释行 | `sed '/^#/d' file` |
| 行前插入 | `sed '3i\text' file` |
| 行后追加 | `sed '3a\text' file` |
| 替换整行 | `sed '3c\new text' file` |

---

## 25. 一句话总结

> `sed` 是 Linux 中常用的流编辑工具，适合对文本进行批量替换、删除、插入、追加和指定行处理，常用于配置修改、日志处理和脚本自动化。
