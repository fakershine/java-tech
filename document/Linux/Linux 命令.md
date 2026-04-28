# Linux 命令

## 1. 查看文件内容

| 命令 | 作用 |
|---|---|
| `cat` | 查看文件全部内容 |
| `less` | 分页查看文件，适合大文件 |
| `more` | 分页查看文件 |
| `head` | 查看文件前几行 |
| `tail` | 查看文件后几行 |
| `tail -f` | 实时查看日志 |

### 常用示例

```bash
cat app.log
```

```bash
less app.log
```

```bash
head -n 100 app.log
```

```bash
tail -n 200 app.log
```

```bash
tail -f app.log
```

---

## 2. 排序、去重、统计

| 命令 | 作用 |
|---|---|
| `sort` | 排序 |
| `uniq` | 去重 |
| `wc` | 统计行数、字数、字节数 |
| `cut` | 按列截取内容 |
| `tr` | 字符替换、删除、压缩 |
| `sort + uniq` | 常用于统计重复次数 |

### 常用示例

```bash
wc -l app.log
```

统计文件行数。

```bash
sort file.txt
```

排序。

```bash
sort file.txt | uniq
```

排序后去重。

```bash
sort file.txt | uniq -c | sort -nr
```

统计重复次数并倒序排序。

```bash
cut -d ':' -f 1 /etc/passwd
```

按 `:` 分隔，取第 1 列。

```bash
cat file.txt | tr 'a-z' 'A-Z'
```

小写转大写。

---

## 3. 文件和目录操作

| 命令 | 作用 |
|---|---|
| `ls` | 查看文件 |
| `cd` | 切换目录 |
| `pwd` | 查看当前目录 |
| `mkdir` | 创建目录 |
| `touch` | 创建空文件 |
| `cp` | 复制文件 |
| `mv` | 移动或重命名 |
| `rm` | 删除文件 |
| `tree` | 树形查看目录结构 |

### 常用示例

```bash
ls -lh
```

```bash
mkdir -p /data/logs/app
```

```bash
cp app.log app.log.bak
```

```bash
mv old.txt new.txt
```

```bash
rm -rf target/
```

```bash
tree -L 2
```

查看 2 层目录结构。

---

## 4. 查看磁盘和目录大小

| 命令 | 作用 |
|---|---|
| `df` | 查看磁盘空间 |
| `du` | 查看文件或目录大小 |
| `lsblk` | 查看磁盘设备 |
| `mount` | 查看挂载信息 |

### 常用示例

```bash
df -h
```

查看磁盘空间。

```bash
du -sh *
```

查看当前目录下每个文件/目录大小。

```bash
du -sh * | sort -h
```

按大小排序。

```bash
lsblk
```

查看磁盘分区。

---

## 5. 进程排查

| 命令 | 作用 |
|---|---|
| `ps` | 查看进程 |
| `top` | 实时查看 CPU、内存 |
| `htop` | 更友好的 top |
| `kill` | 结束进程 |
| `killall` | 按进程名杀进程 |
| `pidof` | 查进程 PID |
| `pgrep` | 按名称查进程 |
| `pstree` | 树形查看进程关系 |

### 常用示例

```bash
ps -ef | grep java
```

```bash
top
```

```bash
top -Hp <pid>
```

查看某个 Java 进程下线程 CPU。

```bash
kill <pid>
```

```bash
kill -9 <pid>
```

强制结束进程。

```bash
pgrep java
```

```bash
pstree -p <pid>
```

---

## 6. 网络排查

| 命令 | 作用 |
|---|---|
| `curl` | 发送 HTTP 请求 |
| `wget` | 下载文件 |
| `ping` | 测试网络连通性 |
| `telnet` | 测试端口连通性 |
| `nc` | 测试端口、发送 TCP/UDP 数据 |
| `ss` | 查看连接和端口 |
| `netstat` | 查看连接和端口，老命令 |
| `lsof` | 查看端口被哪个进程占用 |
| `traceroute` | 查看网络路由 |
| `dig` | DNS 查询 |
| `nslookup` | DNS 查询 |

### 常用示例

```bash
curl http://localhost:8080/health
```

```bash
curl -I https://example.com
```

查看响应头。

```bash
curl -o /dev/null -s -w "%{time_connect} %{time_starttransfer} %{time_total}\n" http://localhost:8080
```

查看接口耗时。

```bash
ping www.baidu.com
```

```bash
nc -vz 192.168.1.10 3306
```

测试端口是否通。

```bash
ss -tunlp | grep 8080
```

查看端口监听。

```bash
lsof -i:8080
```

查看 8080 端口被谁占用。

```bash
dig www.baidu.com
```

DNS 解析。

---

## 7. 压缩和解压

| 命令 | 作用 |
|---|---|
| `tar` | 打包/解包 |
| `gzip` | gzip 压缩 |
| `gunzip` | gzip 解压 |
| `zip` | zip 压缩 |
| `unzip` | zip 解压 |

### 常用示例

```bash
tar -zcvf logs.tar.gz logs/
```

打包并压缩。

```bash
tar -zxvf logs.tar.gz
```

解压。

```bash
tar -tf logs.tar.gz
```

查看压缩包内容。

```bash
zip -r logs.zip logs/
```

```bash
unzip logs.zip
```

---

## 8. 权限和用户

| 命令 | 作用 |
|---|---|
| `chmod` | 修改权限 |
| `chown` | 修改所属用户 |
| `chgrp` | 修改所属用户组 |
| `whoami` | 查看当前用户 |
| `id` | 查看用户 UID/GID |
| `su` | 切换用户 |
| `sudo` | 以管理员权限执行 |
| `passwd` | 修改密码 |

### 常用示例

```bash
chmod +x start.sh
```

```bash
chmod 755 start.sh
```

```bash
chown app:app app.log
```

```bash
chown -R app:app /data/app
```

```bash
whoami
```

```bash
id
```

---

## 9. 系统资源查看

| 命令 | 作用 |
|---|---|
| `free` | 查看内存 |
| `vmstat` | 查看 CPU、内存、IO 综合信息 |
| `iostat` | 查看磁盘 IO |
| `iotop` | 查看进程 IO |
| `uptime` | 查看系统负载 |
| `uname` | 查看系统信息 |
| `date` | 查看时间 |
| `dmesg` | 查看内核日志 |

### 常用示例

```bash
free -h
```

```bash
uptime
```

```bash
vmstat 1
```

```bash
iostat -x 1
```

```bash
iotop
```

```bash
uname -a
```

```bash
dmesg | tail -100
```

---

## 10. 服务管理

| 命令 | 作用 |
|---|---|
| `systemctl` | 管理系统服务 |
| `journalctl` | 查看 systemd 服务日志 |
| `service` | 老版本服务管理命令 |
| `crontab` | 定时任务管理 |

### 常用示例

```bash
systemctl status nginx
```

```bash
systemctl start nginx
```

```bash
systemctl stop nginx
```

```bash
systemctl restart nginx
```

```bash
systemctl enable nginx
```

设置开机自启。

```bash
journalctl -u nginx -f
```

实时查看 nginx 服务日志。

```bash
crontab -l
```

查看定时任务。

```bash
crontab -e
```

编辑定时任务。

---

## 11. Java 排查常用命令

| 命令 | 作用 |
|---|---|
| `jps` | 查看 Java 进程 |
| `jstack` | 查看线程栈 |
| `jmap` | 查看堆内存、导出堆 |
| `jstat` | 查看 GC 情况 |
| `jcmd` | JVM 综合诊断命令 |
| `jinfo` | 查看 JVM 参数 |

### 常用示例

```bash
jps -l
```

```bash
jstack <pid> > jstack.log
```

```bash
jstat -gcutil <pid> 1000
```

```bash
jmap -heap <pid>
```

```bash
jmap -histo <pid> | head -50
```

```bash
jmap -dump:format=b,file=heap.hprof <pid>
```

```bash
jcmd <pid> VM.flags
```

```bash
jcmd <pid> Thread.print
```

---

## 12. 文件传输

| 命令 | 作用 |
|---|---|
| `scp` | 远程复制文件 |
| `rsync` | 增量同步文件 |
| `sftp` | 交互式文件传输 |
| `sz/rz` | 服务器和本地传文件，常用于跳板机 |

### 常用示例

```bash
scp app.log user@192.168.1.10:/tmp/
```

```bash
scp user@192.168.1.10:/tmp/app.log .
```

```bash
rsync -avz logs/ user@192.168.1.10:/data/logs/
```

---

## 13. 查看命令帮助

| 命令 | 作用 |
|---|---|
| `man` | 查看命令手册 |
| `--help` | 查看命令帮助 |
| `which` | 查看命令路径 |
| `whereis` | 查看命令相关路径 |
| `type` | 查看命令类型 |

### 常用示例

```bash
man grep
```

```bash
ls --help
```

```bash
which java
```

```bash
whereis nginx
```

```bash
type cd
```

---

## 14. SSH 远程连接

| 命令 | 作用 |
|---|---|
| `ssh` | 远程登录服务器 |
| `ssh-keygen` | 生成 SSH 密钥 |
| `ssh-copy-id` | 拷贝公钥到远程机器 |

### 常用示例

```bash
ssh user@192.168.1.10
```

```bash
ssh -p 2222 user@192.168.1.10
```

```bash
ssh-keygen -t rsa
```

```bash
ssh-copy-id user@192.168.1.10
```

---

## 15. 常用组合命令

### 查看占用 CPU 最高的进程

```bash
ps aux --sort=-%cpu | head
```

### 查看占用内存最高的进程

```bash
ps aux --sort=-%mem | head
```

### 查看当前目录大文件

```bash
du -sh * | sort -h
```

### 统计访问最多的 IP

```bash
cat access.log | awk '{print $1}' | sort | uniq -c | sort -nr | head
```

### 查看 8080 端口占用

```bash
lsof -i:8080
```

### 查看 TCP 连接状态数量

```bash
ss -ant | awk '{count[$1]++} END{for(i in count) print i,count[i]}'
```

### 查看日志中异常上下文

```bash
grep -C 10 "Exception" app.log
```

### 查看接口耗时

```bash
curl -o /dev/null -s -w "connect:%{time_connect} start:%{time_starttransfer} total:%{time_total}\n" http://localhost:8080/api
```

---

## 16. 工作中最有用的一批命令

| 类别 | 命令 |
|---|---|
| 文件查看 | `cat`、`less`、`head`、`tail` |
| 文本统计 | `sort`、`uniq`、`wc`、`cut`、`tr` |
| 文件操作 | `ls`、`cd`、`cp`、`mv`、`rm`、`mkdir` |
| 磁盘排查 | `df`、`du`、`lsblk` |
| 进程排查 | `ps`、`top`、`kill`、`pgrep`、`pstree` |
| 网络排查 | `curl`、`ping`、`nc`、`ss`、`lsof`、`dig` |
| 压缩解压 | `tar`、`gzip`、`zip`、`unzip` |
| 权限管理 | `chmod`、`chown`、`sudo` |
| 系统资源 | `free`、`vmstat`、`iostat`、`uptime` |
| 服务管理 | `systemctl`、`journalctl`、`crontab` |
| Java 排查 | `jps`、`jstack`、`jmap`、`jstat`、`jcmd` |
| 文件传输 | `scp`、`rsync`、`sftp` |

---

## 17. 一句话总结

除了 `find`、`grep`、`awk`、`sed`，工作中最常用、最值得掌握的是：

```text
cat less head tail
sort uniq wc cut tr
ps top kill pgrep
df du free vmstat iostat
curl ping nc ss lsof
tar zip unzip
chmod chown
systemctl journalctl crontab
jps jstack jmap jstat jcmd
scp rsync ssh
```

这些命令基本覆盖了：

```text
文件查看
日志排查
进程排查
CPU/内存/磁盘排查
网络排查
服务管理
Java 问题定位
文件传输
```
