# Linux 常用命令

## 1. 目录操作

| 命令 | 说明 |
|---|---|
| `pwd` | 查看当前所在目录 |
| `ls` | 查看当前目录文件 |
| `ls -l` | 查看详细信息 |
| `ls -a` | 查看隐藏文件 |
| `ls -lh` | 以可读方式显示文件大小 |
| `cd /path` | 切换目录 |
| `cd ..` | 返回上一级目录 |
| `cd ~` | 回到用户家目录 |
| `mkdir dir` | 创建目录 |
| `mkdir -p a/b/c` | 递归创建目录 |
| `rmdir dir` | 删除空目录 |

---

## 2. 文件操作

| 命令 | 说明 |
|---|---|
| `touch file.txt` | 创建空文件 |
| `cp a.txt b.txt` | 复制文件 |
| `cp -r dir1 dir2` | 复制目录 |
| `mv a.txt b.txt` | 重命名文件 |
| `mv file /tmp/` | 移动文件 |
| `rm file.txt` | 删除文件 |
| `rm -r dir` | 删除目录 |
| `rm -rf dir` | 强制删除目录，谨慎使用 |

---

## 3. 查看文件内容

| 命令 | 说明 |
|---|---|
| `cat file.txt` | 查看整个文件 |
| `more file.txt` | 分页查看文件 |
| `less file.txt` | 分页查看，可上下翻 |
| `head file.txt` | 查看前 10 行 |
| `head -n 20 file.txt` | 查看前 20 行 |
| `tail file.txt` | 查看后 10 行 |
| `tail -n 100 file.txt` | 查看后 100 行 |
| `tail -f app.log` | 实时查看日志 |
| `tail -fn 200 app.log` | 实时查看最后 200 行日志 |

---

## 4. 查找文件

| 命令 | 说明 |
|---|---|
| `find . -name "*.log"` | 查找当前目录下所有 `.log` 文件 |
| `find / -name "app.log"` | 从根目录查找文件 |
| `find . -type f` | 查找普通文件 |
| `find . -type d` | 查找目录 |
| `find . -size +100M` | 查找大于 100MB 的文件 |
| `find . -mtime -1` | 查找 1 天内修改的文件 |
| `find . -mmin -10` | 查找 10 分钟内修改的文件 |

示例：

```bash
find /var/log -type f -name "*.log" -mtime +30
```

查找 `/var/log` 下 30 天前的日志文件。

---

## 5. 搜索文本 grep

| 命令 | 说明 |
|---|---|
| `grep "error" app.log` | 查找包含 error 的行 |
| `grep -n "error" app.log` | 显示行号 |
| `grep -i "error" app.log` | 忽略大小写 |
| `grep -v "debug" app.log` | 排除 debug 行 |
| `grep -r "keyword" .` | 递归查找 |
| `grep -E "error|warn" app.log` | 匹配多个关键字 |
| `grep -C 5 "Exception" app.log` | 查看匹配行前后 5 行 |

示例：

```bash
tail -f app.log | grep -E "ERROR|Exception|timeout"
```

实时查看错误日志。

---

## 6. 文本处理 awk

| 命令 | 说明 |
|---|---|
| `awk '{print $1}' file` | 打印第 1 列 |
| `awk '{print $NF}' file` | 打印最后一列 |
| `awk -F ':' '{print $1}' /etc/passwd` | 按冒号分隔打印第 1 列 |
| `awk '$3 > 100 {print}' file` | 打印第 3 列大于 100 的行 |
| `awk '{sum += $1} END{print sum}' file` | 求和 |
| `awk '{count[$1]++} END{for(i in count) print i,count[i]}' file` | 分组统计 |

示例：

```bash
awk '{print $1}' access.log | sort | uniq -c | sort -nr | head
```

统计访问量最高的 IP。

---

## 7. 文本替换 sed

| 命令 | 说明 |
|---|---|
| `sed 's/old/new/' file` | 替换每行第一个 old |
| `sed 's/old/new/g' file` | 替换所有 old |
| `sed -i 's/old/new/g' file` | 直接修改文件 |
| `sed -n '10p' file` | 打印第 10 行 |
| `sed -n '10,20p' file` | 打印第 10 到 20 行 |
| `sed '10d' file` | 删除第 10 行 |
| `sed '/^$/d' file` | 删除空行 |

示例：

```bash
sed -i 's/8080/9090/g' application.yml
```

把配置文件中的 `8080` 替换为 `9090`。

---

## 8. 文件权限

| 命令 | 说明 |
|---|---|
| `chmod 755 file` | 修改文件权限 |
| `chmod +x app.sh` | 添加执行权限 |
| `chmod -x app.sh` | 去掉执行权限 |
| `chown user file` | 修改文件所属用户 |
| `chown user:group file` | 修改用户和用户组 |
| `chown -R user:group dir` | 递归修改目录权限 |

权限说明：

```text
r = 读 = 4
w = 写 = 2
x = 执行 = 1
```

常见权限：

| 权限 | 含义 |
|---|---|
| `644` | 文件常用权限 |
| `755` | 脚本/目录常用权限 |
| `777` | 所有人可读写执行，不推荐 |

---

## 9. 压缩和解压

### tar

| 命令 | 说明 |
|---|---|
| `tar -cvf a.tar dir` | 打包目录 |
| `tar -xvf a.tar` | 解包 |
| `tar -zcvf a.tar.gz dir` | 打包并 gzip 压缩 |
| `tar -zxvf a.tar.gz` | 解压 tar.gz |
| `tar -tf a.tar.gz` | 查看压缩包内容 |

### zip

| 命令 | 说明 |
|---|---|
| `zip a.zip file` | 压缩文件 |
| `zip -r a.zip dir` | 压缩目录 |
| `unzip a.zip` | 解压 zip |
| `unzip a.zip -d /tmp` | 解压到指定目录 |

---

## 10. 进程管理

| 命令 | 说明 |
|---|---|
| `ps -ef` | 查看所有进程 |
| `ps -ef | grep java` | 查找 Java 进程 |
| `top` | 实时查看系统资源 |
| `top -Hp pid` | 查看指定进程下线程 CPU |
| `kill pid` | 结束进程 |
| `kill -9 pid` | 强制结束进程 |
| `jps` | 查看 Java 进程 |
| `jstack pid` | 查看 Java 线程栈 |
| `jmap -heap pid` | 查看 JVM 堆信息 |

示例：

```bash
ps -ef | grep java
kill -9 12345
```

---

## 11. 网络命令

| 命令 | 说明 |
|---|---|
| `ping www.baidu.com` | 测试网络连通性 |
| `curl http://localhost:8080` | 发送 HTTP 请求 |
| `curl -I url` | 查看响应头 |
| `wget url` | 下载文件 |
| `netstat -tunlp` | 查看端口监听 |
| `ss -tunlp` | 查看端口监听，推荐 |
| `lsof -i:8080` | 查看 8080 端口占用 |
| `telnet host port` | 测试端口连通 |
| `nc -vz host port` | 测试端口连通 |

示例：

```bash
lsof -i:8080
```

查看哪个进程占用了 `8080` 端口。

---

## 12. 磁盘和内存

| 命令 | 说明 |
|---|---|
| `df -h` | 查看磁盘空间 |
| `du -sh dir` | 查看目录大小 |
| `du -sh *` | 查看当前目录下各文件大小 |
| `free -h` | 查看内存 |
| `top` | 查看 CPU、内存 |
| `vmstat 1` | 查看系统运行状态 |
| `iostat` | 查看磁盘 IO |
| `uptime` | 查看系统负载 |

示例：

```bash
du -sh * | sort -h
```

按大小查看当前目录文件。

---

## 13. 系统信息

| 命令 | 说明 |
|---|---|
| `uname -a` | 查看系统内核信息 |
| `hostname` | 查看主机名 |
| `whoami` | 查看当前用户 |
| `id` | 查看用户 ID 和组 |
| `date` | 查看当前时间 |
| `cal` | 查看日历 |
| `env` | 查看环境变量 |
| `history` | 查看历史命令 |

---

## 14. 用户和用户组

| 命令 | 说明 |
|---|---|
| `useradd user` | 添加用户 |
| `passwd user` | 修改用户密码 |
| `userdel user` | 删除用户 |
| `groupadd group` | 添加用户组 |
| `groups user` | 查看用户所属组 |
| `su - user` | 切换用户 |
| `sudo command` | 以管理员权限执行命令 |

---

## 15. 服务管理 systemctl

| 命令 | 说明 |
|---|---|
| `systemctl status nginx` | 查看服务状态 |
| `systemctl start nginx` | 启动服务 |
| `systemctl stop nginx` | 停止服务 |
| `systemctl restart nginx` | 重启服务 |
| `systemctl reload nginx` | 重新加载配置 |
| `systemctl enable nginx` | 设置开机自启 |
| `systemctl disable nginx` | 取消开机自启 |

---

## 16. 查看日志 journalctl

| 命令 | 说明 |
|---|---|
| `journalctl` | 查看系统日志 |
| `journalctl -u nginx` | 查看 nginx 服务日志 |
| `journalctl -u nginx -f` | 实时查看 nginx 日志 |
| `journalctl -n 100` | 查看最近 100 行日志 |
| `journalctl --since "1 hour ago"` | 查看最近 1 小时日志 |

---

## 17. 软件安装

### CentOS / Rocky / AlmaLinux

```bash
yum install nginx
yum remove nginx
yum update
```

或者新版：

```bash
dnf install nginx
dnf remove nginx
dnf update
```

### Ubuntu / Debian

```bash
apt update
apt install nginx
apt remove nginx
apt upgrade
```

---

## 18. 常用组合命令

### 查看 Java 进程

```bash
ps -ef | grep java
```

### 查看端口占用

```bash
lsof -i:8080
```

或者：

```bash
ss -tunlp | grep 8080
```

### 实时查看日志

```bash
tail -f app.log
```

### 查看错误日志

```bash
grep -n "ERROR" app.log
```

### 查看异常上下文

```bash
grep -C 10 "Exception" app.log
```

### 查找大文件

```bash
find / -type f -size +1G
```

### 查看当前目录文件大小

```bash
du -sh * | sort -h
```

### 统计访问最多的 IP

```bash
awk '{print $1}' access.log | sort | uniq -c | sort -nr | head
```

---

## 19. 线上排查常用命令

### CPU 高排查

```bash
top
top -Hp <pid>
printf "%x\n" <线程id>
jstack <pid> > jstack.log
grep "<十六进制线程id>" jstack.log
```

---

### 内存高排查

```bash
free -h
top
jmap -heap <pid>
jmap -dump:format=b,file=heap.hprof <pid>
```

---

### 磁盘满排查

```bash
df -h
du -sh /*
du -sh * | sort -h
find / -type f -size +1G
```

---

### 端口占用排查

```bash
lsof -i:8080
ss -tunlp | grep 8080
```

---

### 日志排查

```bash
tail -fn 200 app.log
grep -n "ERROR" app.log
grep -C 10 "Exception" app.log
grep -E "ERROR|Exception|timeout" app.log
```

---

## 20. 常用命令速查表

| 场景 | 命令 |
|---|---|
| 查看目录 | `ls -lh` |
| 切换目录 | `cd /path` |
| 查看当前位置 | `pwd` |
| 创建目录 | `mkdir -p dir` |
| 删除文件 | `rm file` |
| 删除目录 | `rm -rf dir` |
| 复制文件 | `cp a b` |
| 移动文件 | `mv a b` |
| 查看文件 | `cat file` |
| 实时日志 | `tail -f app.log` |
| 查找文件 | `find . -name "*.log"` |
| 搜索内容 | `grep "error" app.log` |
| 替换内容 | `sed -i 's/old/new/g' file` |
| 按列处理 | `awk '{print $1}' file` |
| 查看进程 | `ps -ef` |
| 查看端口 | `lsof -i:8080` |
| 查看磁盘 | `df -h` |
| 查看内存 | `free -h` |
| 查看 CPU | `top` |
| 修改权限 | `chmod 755 file` |
| 解压文件 | `tar -zxvf a.tar.gz` |

---

## 21. 一句话总结

> Linux 常用命令主要分为文件目录、文本处理、权限管理、进程管理、网络排查、磁盘内存、系统服务和日志排查几类，工作中最常用的是 `ls`、`cd`、`cat`、`tail`、`grep`、`awk`、`sed`、`find`、`ps`、`top`、`kill`、`df`、`du`、`curl`、`ss`、`lsof`。
