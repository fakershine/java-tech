# Linux 常用命令与线上问题排查面试题总结

## 目录

- [一、常用 Linux 命令有哪些](#一常用-linux-命令有哪些)
- [二、如何查看进程](#二如何查看进程)
- [三、如何查看端口占用](#三如何查看端口占用)
- [四、如何查看日志](#四如何查看日志)
- [五、如何查看 CPU、内存、磁盘](#五如何查看-cpu内存磁盘)
- [六、如何定位线上服务异常](#六如何定位线上服务异常)
- [七、top、ps、netstat、lsof、grep、tail 怎么用](#七toppsnetstatlsofgreptail-怎么用)
- [八、面试速记版](#八面试速记版)
- [九、总览表](#九总览表)
- [十、完整面试回答模板](#十完整面试回答模板)

---

# 一、常用 Linux 命令有哪些

## 1. 文件和目录相关命令

| 命令 | 作用 |
|---|---|
| `pwd` | 查看当前目录 |
| `ls` | 查看目录内容 |
| `cd` | 切换目录 |
| `mkdir` | 创建目录 |
| `rm` | 删除文件或目录 |
| `cp` | 复制文件或目录 |
| `mv` | 移动或重命名 |
| `touch` | 创建空文件 |
| `cat` | 查看文件内容 |
| `less` | 分页查看文件 |
| `head` | 查看文件前几行 |
| `tail` | 查看文件后几行 |
| `find` | 查找文件 |
| `du` | 查看目录或文件大小 |
| `df` | 查看磁盘空间 |

---

## 2. 示例

查看当前目录：

```bash
pwd
```

查看当前目录文件：

```bash
ls -lh
```

创建目录：

```bash
mkdir logs
```

删除文件：

```bash
rm app.log
```

删除目录：

```bash
rm -rf logs
```

复制文件：

```bash
cp app.log app.log.bak
```

移动或重命名文件：

```bash
mv app.log app-old.log
```

查看文件前 100 行：

```bash
head -n 100 app.log
```

查看文件后 100 行：

```bash
tail -n 100 app.log
```

实时查看日志：

```bash
tail -f app.log
```

---

## 3. 文本处理相关命令

| 命令 | 作用 |
|---|---|
| `grep` | 按关键字搜索 |
| `awk` | 文本分析、按列处理 |
| `sed` | 文本替换、编辑 |
| `sort` | 排序 |
| `uniq` | 去重 |
| `wc` | 统计行数、字数、字符数 |
| `cut` | 按列截取 |
| `xargs` | 参数传递 |
| `vim` | 编辑文件 |

---

## 4. 示例

搜索日志中的异常：

```bash
grep "Exception" app.log
```

统计异常出现次数：

```bash
grep "Exception" app.log | wc -l
```

查看包含关键字的前后 5 行：

```bash
grep -C 5 "NullPointerException" app.log
```

按空格打印第一列：

```bash
awk '{print $1}' access.log
```

统计访问 IP 次数：

```bash
awk '{print $1}' access.log | sort | uniq -c | sort -nr | head
```

---

## 5. 进程和端口相关命令

| 命令 | 作用 |
|---|---|
| `ps` | 查看进程 |
| `top` | 实时查看系统资源和进程 |
| `kill` | 终止进程 |
| `netstat` | 查看网络连接和端口 |
| `ss` | 查看网络连接和端口，netstat 的替代命令 |
| `lsof` | 查看文件、端口、进程占用 |
| `jps` | 查看 Java 进程 |
| `jstack` | 查看 Java 线程栈 |
| `jmap` | 查看 Java 内存信息 |
| `jstat` | 查看 JVM GC 情况 |

---

## 6. 系统资源相关命令

| 命令 | 作用 |
|---|---|
| `top` | 查看 CPU、内存、进程 |
| `free` | 查看内存 |
| `df` | 查看磁盘空间 |
| `du` | 查看目录大小 |
| `iostat` | 查看磁盘 IO |
| `vmstat` | 查看系统运行状态 |
| `uptime` | 查看系统运行时间和负载 |
| `sar` | 查看历史系统性能数据 |
| `dmesg` | 查看内核日志 |

---

# 二、如何查看进程

## 1. 使用 ps 查看进程

查看所有进程：

```bash
ps -ef
```

查看包含 Java 的进程：

```bash
ps -ef | grep java
```

查看指定服务进程：

```bash
ps -ef | grep order-service
```

---

## 2. ps 常用参数

| 参数 | 说明 |
|---|---|
| `-e` | 显示所有进程 |
| `-f` | 全格式显示 |
| `aux` | BSD 风格显示所有进程 |
| `-p` | 指定 PID |

---

## 3. 查看 Java 进程

如果是 Java 应用，可以使用：

```bash
jps -l
```

输出示例：

```text
12345 com.example.OrderApplication
23456 sun.tools.jps.Jps
```

---

## 4. 使用 top 查看进程资源

```bash
top
```

常用操作：

| 操作 | 说明 |
|---|---|
| `P` | 按 CPU 使用率排序 |
| `M` | 按内存使用率排序 |
| `1` | 查看每个 CPU 核心使用情况 |
| `H` | 查看线程 |
| `q` | 退出 |

---

## 5. 查看指定进程

```bash
top -p 12345
```

查看某个 Java 进程下的线程：

```bash
top -Hp 12345
```

---

## 6. 终止进程

正常终止进程：

```bash
kill 12345
```

强制终止进程：

```bash
kill -9 12345
```

建议：

```text
优先使用 kill，不要直接 kill -9。
```

因为普通 `kill` 发送的是 `SIGTERM`，应用有机会做优雅停机。

`kill -9` 是强制杀死进程，应用没有机会释放资源。

---

## 7. 面试回答

查看进程常用 `ps -ef | grep 服务名`，可以查看进程 PID、启动命令和运行用户。也可以使用 `top` 实时查看进程 CPU、内存占用。如果是 Java 服务，可以用 `jps -l` 查看 Java 进程。

如果需要查看某个进程下线程资源，可以使用 `top -Hp pid`。终止进程时优先使用 `kill pid`，不要随意使用 `kill -9`，避免服务无法优雅停机。

---

# 三、如何查看端口占用

## 1. 使用 netstat 查看端口

查看所有监听端口：

```bash
netstat -tunlp
```

查看指定端口：

```bash
netstat -tunlp | grep 8080
```

---

## 2. netstat 参数说明

| 参数 | 说明 |
|---|---|
| `-t` | TCP |
| `-u` | UDP |
| `-n` | 使用数字显示，不解析域名 |
| `-l` | 只显示监听状态 |
| `-p` | 显示进程 PID 和程序名 |

---

## 3. 使用 ss 查看端口

`ss` 是 `netstat` 的替代命令，性能更好。

查看监听端口：

```bash
ss -tunlp
```

查看指定端口：

```bash
ss -tunlp | grep 8080
```

---

## 4. 使用 lsof 查看端口占用

查看 8080 端口被哪个进程占用：

```bash
lsof -i:8080
```

输出示例：

```text
COMMAND   PID USER   FD   TYPE DEVICE SIZE/OFF NODE NAME
java    12345 root  123u  IPv6 123456      0t0  TCP *:8080 (LISTEN)
```

其中：

```text
PID = 12345
```

表示端口被该进程占用。

---

## 5. 查到端口后终止进程

```bash
kill 12345
```

必要时强制：

```bash
kill -9 12345
```

---

## 6. 查看某个进程占用哪些端口

```bash
lsof -Pan -p 12345 -i
```

或者：

```bash
netstat -tunlp | grep 12345
```

---

## 7. 面试回答

查看端口占用常用 `netstat -tunlp | grep 端口号`，也可以使用 `ss -tunlp | grep 端口号`。如果想直接查看某个端口被哪个进程占用，可以使用 `lsof -i:端口号`。

查到 PID 后，可以用 `ps -ef | grep PID` 查看进程详情，再根据情况使用 `kill PID` 停止进程。

---

# 四、如何查看日志

## 1. 查看完整日志

```bash
cat app.log
```

适合小文件。

大文件不建议直接 `cat`，容易刷屏。

---

## 2. 分页查看日志

```bash
less app.log
```

常用操作：

| 操作 | 说明 |
|---|---|
| `/关键字` | 搜索关键字 |
| `n` | 下一个匹配 |
| `N` | 上一个匹配 |
| `G` | 跳到文件末尾 |
| `g` | 跳到文件开头 |
| `q` | 退出 |

---

## 3. 查看日志最后几行

查看最后 100 行：

```bash
tail -n 100 app.log
```

实时查看：

```bash
tail -f app.log
```

查看最后 200 行并持续输出：

```bash
tail -n 200 -f app.log
```

---

## 4. 按关键字搜索日志

搜索异常：

```bash
grep "Exception" app.log
```

忽略大小写：

```bash
grep -i "error" app.log
```

显示行号：

```bash
grep -n "Exception" app.log
```

显示匹配前后 5 行：

```bash
grep -C 5 "Exception" app.log
```

显示匹配前 5 行：

```bash
grep -B 5 "Exception" app.log
```

显示匹配后 5 行：

```bash
grep -A 5 "Exception" app.log
```

---

## 5. 实时过滤日志

```bash
tail -f app.log | grep "ERROR"
```

同时过滤多个关键字：

```bash
tail -f app.log | grep -E "ERROR|Exception|Timeout"
```

---

## 6. 按时间范围查看日志

如果日志中包含时间，例如：

```text
2026-04-29 10:15:30 ERROR xxx
```

可以使用：

```bash
sed -n '/2026-04-29 10:00:00/,/2026-04-29 10:30:00/p' app.log
```

也可以先 grep 某个时间段：

```bash
grep "2026-04-29 10:" app.log
```

---

## 7. 统计错误次数

```bash
grep "ERROR" app.log | wc -l
```

统计某类异常次数：

```bash
grep "NullPointerException" app.log | wc -l
```

---

## 8. 查看压缩日志

查看 `.gz` 日志：

```bash
zcat app.log.gz
```

搜索压缩日志：

```bash
zgrep "Exception" app.log.gz
```

分页查看压缩日志：

```bash
zless app.log.gz
```

---

## 9. 查看 systemd 服务日志

如果服务由 systemd 管理，可以使用：

```bash
journalctl -u 服务名
```

实时查看：

```bash
journalctl -u 服务名 -f
```

查看最近 100 行：

```bash
journalctl -u 服务名 -n 100
```

---

## 10. 面试回答

查看日志常用 `tail`、`less` 和 `grep`。如果要实时查看日志，可以用 `tail -f app.log`；如果要搜索异常，可以用 `grep "Exception" app.log`；如果要查看异常上下文，可以用 `grep -C 5 "Exception" app.log`；如果日志文件很大，可以用 `less` 分页查看，或者结合时间、关键字过滤。

线上排查时一般先看最近日志，再按 traceId、订单号、用户 ID 或异常关键字进行过滤。

---

# 五、如何查看 CPU、内存、磁盘

## 1. 查看 CPU

### 使用 top

```bash
top
```

重点关注：

```text
%Cpu(s)
load average
进程 CPU 占用
```

---

### 查看 CPU 核数

```bash
nproc
```

或者：

```bash
cat /proc/cpuinfo | grep processor | wc -l
```

---

### 查看系统负载

```bash
uptime
```

输出示例：

```text
load average: 1.20, 0.80, 0.60
```

含义：

```text
最近 1 分钟、5 分钟、15 分钟系统平均负载。
```

一般来说：

```text
如果 load average 长期大于 CPU 核数，说明系统压力较大。
```

---

## 2. 查看内存

```bash
free -h
```

输出示例：

```text
              total        used        free      shared  buff/cache   available
Mem:           7.6G        3.2G        1.1G        100M        3.3G        4.0G
Swap:          2.0G        0.0G        2.0G
```

重点看：

```text
available
```

它表示当前系统大概还能给新进程使用的内存。

---

## 3. 查看进程内存占用

```bash
ps aux --sort=-%mem | head
```

查看指定进程：

```bash
ps -p 12345 -o pid,ppid,cmd,%mem,%cpu
```

---

## 4. 查看磁盘空间

```bash
df -h
```

重点看：

```text
Use%
```

如果磁盘使用率超过 80% 或 90%，需要关注。

---

## 5. 查看目录大小

查看当前目录下各文件夹大小：

```bash
du -sh *
```

查看某个目录总大小：

```bash
du -sh /data/logs
```

找出大文件或大目录：

```bash
du -ah /data | sort -rh | head -n 20
```

---

## 6. 查看磁盘 IO

```bash
iostat -x 1
```

重点关注：

| 指标 | 说明 |
|---|---|
| `%util` | 磁盘繁忙程度 |
| `await` | IO 平均等待时间 |
| `r/s` | 每秒读次数 |
| `w/s` | 每秒写次数 |
| `rkB/s` | 每秒读取 KB |
| `wkB/s` | 每秒写入 KB |

如果 `%util` 长期接近 100%，说明磁盘 IO 压力较大。

---

## 7. 查看系统整体状态

```bash
vmstat 1
```

重点关注：

| 指标 | 说明 |
|---|---|
| `r` | 等待 CPU 的进程数 |
| `b` | 阻塞进程数 |
| `si` | swap in |
| `so` | swap out |
| `us` | 用户态 CPU |
| `sy` | 系统态 CPU |
| `wa` | IO 等待 |
| `id` | 空闲 CPU |

---

## 8. 面试回答

查看 CPU 一般用 `top`、`uptime`、`vmstat`，重点看 CPU 使用率和 load average。如果 load 长期大于 CPU 核数，说明系统压力较大。

查看内存用 `free -h`，重点看 `available`。查看磁盘空间用 `df -h`，查看目录大小用 `du -sh *`。如果怀疑磁盘 IO 高，可以用 `iostat -x 1`，重点看 `%util` 和 `await`。

---

# 六、如何定位线上服务异常

## 1. 线上服务异常常见类型

线上服务异常常见表现：

```text
接口响应慢
接口大量 500
服务无响应
CPU 飙高
内存持续上涨
频繁 Full GC
磁盘满
日志大量报错
数据库连接池打满
线程池耗尽
端口无法访问
服务频繁重启
```

---

## 2. 定位线上问题的整体思路

线上异常排查一般按照以下顺序：

```text
1. 确认影响范围
2. 查看服务是否存活
3. 查看日志错误
4. 查看 CPU、内存、磁盘、网络
5. 查看进程和端口
6. 查看依赖服务状态
7. 查看数据库、Redis、MQ 等中间件
8. 查看线程栈和 GC 情况
9. 定位代码或配置问题
10. 临时止血和后续修复
```

---

## 3. 第一步：确认服务是否存活

查看进程：

```bash
ps -ef | grep java
```

查看 Java 进程：

```bash
jps -l
```

查看端口：

```bash
netstat -tunlp | grep 8080
```

或者：

```bash
lsof -i:8080
```

---

## 4. 第二步：查看最近日志

实时查看：

```bash
tail -n 200 -f app.log
```

搜索错误：

```bash
grep -E "ERROR|Exception|Timeout|拒绝|失败" app.log
```

查看异常上下文：

```bash
grep -C 10 "Exception" app.log
```

如果有 traceId：

```bash
grep "traceId=xxx" app.log
```

---

## 5. 第三步：查看系统资源

查看 CPU 和进程：

```bash
top
```

查看内存：

```bash
free -h
```

查看磁盘：

```bash
df -h
```

查看目录大小：

```bash
du -sh /data/logs/*
```

查看磁盘 IO：

```bash
iostat -x 1
```

查看系统负载：

```bash
uptime
```

---

## 6. 第四步：CPU 飙高排查

### 6.1 找到 CPU 高的 Java 进程

```bash
top
```

假设 PID 是：

```text
12345
```

---

### 6.2 找到 CPU 高的线程

```bash
top -Hp 12345
```

假设线程 ID 是：

```text
12366
```

---

### 6.3 转成十六进制

```bash
printf "%x\n" 12366
```

输出：

```text
304e
```

---

### 6.4 导出线程栈

```bash
jstack 12345 > thread.txt
```

---

### 6.5 搜索 nid

```bash
grep -n "0x304e" thread.txt
```

查看该线程正在执行的代码。

---

## 7. 第五步：内存问题排查

查看内存：

```bash
free -h
```

查看 Java 堆情况：

```bash
jstat -gcutil 12345 1000 10
```

查看对象统计：

```bash
jmap -histo:live 12345 | head -n 30
```

导出堆 dump：

```bash
jmap -dump:format=b,file=heap.hprof 12345
```

注意：

```text
线上导出 dump 可能影响服务，需要谨慎操作。
```

---

## 8. 第六步：GC 问题排查

查看 GC 情况：

```bash
jstat -gcutil 12345 1000 10
```

重点看：

```text
YGC
YGCT
FGC
FGCT
GCT
Old 区使用率
```

如果 Full GC 频繁，可能是：

```text
内存泄漏
大对象过多
堆太小
缓存无限增长
对象创建速度过快
```

---

## 9. 第七步：线程阻塞排查

导出线程栈：

```bash
jstack 12345 > thread.txt
```

搜索：

```bash
grep -n "BLOCKED" thread.txt
grep -n "WAITING" thread.txt
grep -n "deadlock" thread.txt
```

如果大量线程处于 `BLOCKED`，可能是锁竞争。

如果大量线程等待数据库连接，可能是连接池耗尽。

---

## 10. 第八步：磁盘满排查

查看磁盘：

```bash
df -h
```

找大目录：

```bash
du -sh /*
```

找 `/data` 下大文件：

```bash
du -ah /data | sort -rh | head -n 20
```

常见原因：

```text
日志文件过大
dump 文件过大
临时文件未清理
磁盘归档失败
容器日志无限增长
```

---

## 11. 第九步：端口不通排查

查看服务端口是否监听：

```bash
netstat -tunlp | grep 8080
```

查看防火墙：

```bash
iptables -L
```

测试本机访问：

```bash
curl http://127.0.0.1:8080/health
```

测试远程连通：

```bash
telnet ip 8080
```

或者：

```bash
nc -vz ip 8080
```

---

## 12. 第十步：快速止血

线上问题需要优先恢复服务。

常见止血方式：

```text
重启异常实例
摘除异常节点
扩容服务实例
限流
降级
关闭非核心功能
回滚版本
清理磁盘
扩容磁盘
重启依赖连接
切换流量
```

---

## 13. 线上排查注意事项

```text
1. 先止血，再复盘
2. 不要直接在线上乱改数据
3. 不要随意 kill -9
4. dump、jstack、jmap 操作要谨慎
5. 保留现场日志和监控数据
6. 操作前确认机器和环境
7. 关键操作要留记录
```

---

## 14. 面试回答

定位线上服务异常时，我会先确认影响范围和服务是否存活，使用 `ps`、`jps`、`netstat` 或 `lsof` 查看进程和端口。然后查看应用日志，使用 `tail` 和 `grep` 搜索异常、超时、错误码或 traceId。

接着查看系统资源，例如用 `top` 看 CPU 和负载，用 `free -h` 看内存，用 `df -h` 看磁盘，用 `iostat` 看磁盘 IO。如果是 CPU 飙高，会用 `top -Hp pid` 找到高 CPU 线程，把线程 ID 转成十六进制，再用 `jstack` 定位线程栈。如果是内存或 GC 问题，会用 `jstat`、`jmap`、GC 日志和 dump 文件分析。

线上处理原则是先止血恢复服务，再深入定位根因，最后复盘和修复。

---

# 七、top、ps、netstat、lsof、grep、tail 怎么用

# 1. top

## 1.1 top 是什么？

`top` 用于实时查看系统资源和进程状态。

执行：

```bash
top
```

可以看到：

```text
CPU 使用率
内存使用情况
系统负载
进程列表
进程 CPU 占用
进程内存占用
```

---

## 1.2 top 常用操作

| 操作 | 说明 |
|---|---|
| `P` | 按 CPU 排序 |
| `M` | 按内存排序 |
| `1` | 显示每个 CPU 核心 |
| `H` | 显示线程 |
| `c` | 显示完整命令 |
| `q` | 退出 |

---

## 1.3 查看指定进程

```bash
top -p 12345
```

---

## 1.4 查看进程下线程

```bash
top -Hp 12345
```

常用于排查 Java CPU 飙高。

---

# 2. ps

## 2.1 ps 是什么？

`ps` 用于查看当前系统进程快照。

---

## 2.2 常用命令

查看所有进程：

```bash
ps -ef
```

查看 Java 进程：

```bash
ps -ef | grep java
```

查看指定服务：

```bash
ps -ef | grep order-service
```

查看进程资源：

```bash
ps aux | head
```

按 CPU 排序：

```bash
ps aux --sort=-%cpu | head
```

按内存排序：

```bash
ps aux --sort=-%mem | head
```

---

## 2.3 ps -ef 输出说明

```text
UID        PID  PPID  C STIME TTY          TIME CMD
root     12345     1  0 10:00 ?        00:01:20 java -jar app.jar
```

| 字段 | 说明 |
|---|---|
| `UID` | 用户 |
| `PID` | 进程 ID |
| `PPID` | 父进程 ID |
| `C` | CPU 占用 |
| `STIME` | 启动时间 |
| `TIME` | 累计 CPU 时间 |
| `CMD` | 启动命令 |

---

# 3. netstat

## 3.1 netstat 是什么？

`netstat` 用于查看网络连接、路由表和端口监听情况。

---

## 3.2 查看监听端口

```bash
netstat -tunlp
```

---

## 3.3 查看指定端口

```bash
netstat -tunlp | grep 8080
```

---

## 3.4 查看 TCP 连接

```bash
netstat -ant
```

---

## 3.5 统计连接状态

```bash
netstat -ant | awk '{print $6}' | sort | uniq -c | sort -nr
```

可以查看：

```text
ESTABLISHED
TIME_WAIT
CLOSE_WAIT
LISTEN
```

---

## 3.6 常见连接状态

| 状态 | 含义 |
|---|---|
| `LISTEN` | 正在监听 |
| `ESTABLISHED` | 已建立连接 |
| `TIME_WAIT` | 主动关闭后等待 |
| `CLOSE_WAIT` | 被动关闭后等待应用关闭 |
| `SYN_SENT` | 已发送连接请求 |
| `SYN_RECV` | 收到连接请求 |

---

# 4. lsof

## 4.1 lsof 是什么？

`lsof` 用于查看打开的文件。

在 Linux 中，一切皆文件，所以它也可以查看：

```text
端口占用
文件占用
目录占用
进程打开的文件
网络连接
```

---

## 4.2 查看端口占用

```bash
lsof -i:8080
```

---

## 4.3 查看某个进程打开的文件

```bash
lsof -p 12345
```

---

## 4.4 查看某个文件被谁占用

```bash
lsof /data/app.log
```

---

## 4.5 查看某个进程的网络连接

```bash
lsof -Pan -p 12345 -i
```

---

# 5. grep

## 5.1 grep 是什么？

`grep` 用于按关键字搜索文本。

---

## 5.2 基本用法

```bash
grep "ERROR" app.log
```

---

## 5.3 忽略大小写

```bash
grep -i "error" app.log
```

---

## 5.4 显示行号

```bash
grep -n "ERROR" app.log
```

---

## 5.5 反向匹配

```bash
grep -v "DEBUG" app.log
```

表示过滤掉包含 `DEBUG` 的行。

---

## 5.6 匹配多个关键字

```bash
grep -E "ERROR|Exception|Timeout" app.log
```

---

## 5.7 查看上下文

查看前后 5 行：

```bash
grep -C 5 "Exception" app.log
```

查看后 5 行：

```bash
grep -A 5 "Exception" app.log
```

查看前 5 行：

```bash
grep -B 5 "Exception" app.log
```

---

# 6. tail

## 6.1 tail 是什么？

`tail` 用于查看文件末尾内容。

常用于查看实时日志。

---

## 6.2 查看最后 100 行

```bash
tail -n 100 app.log
```

---

## 6.3 实时查看日志

```bash
tail -f app.log
```

---

## 6.4 查看最后 200 行并实时跟踪

```bash
tail -n 200 -f app.log
```

---

## 6.5 实时查看并过滤

```bash
tail -f app.log | grep "ERROR"
```

多个关键字：

```bash
tail -f app.log | grep -E "ERROR|Exception|Timeout"
```

---

# 八、面试速记版

## 1. 常用 Linux 命令

```text
文件目录：ls、cd、pwd、mkdir、rm、cp、mv、find
查看文件：cat、less、head、tail
文本处理：grep、awk、sed、sort、uniq、wc
进程查看：ps、top、jps
端口查看：netstat、ss、lsof
资源查看：top、free、df、du、iostat、vmstat、uptime
```

---

## 2. 如何查看进程？

```bash
ps -ef | grep java
jps -l
top
top -p PID
```

---

## 3. 如何查看端口占用？

```bash
netstat -tunlp | grep 8080
ss -tunlp | grep 8080
lsof -i:8080
```

---

## 4. 如何查看日志？

```bash
tail -f app.log
tail -n 100 app.log
grep "ERROR" app.log
grep -C 5 "Exception" app.log
less app.log
```

---

## 5. 如何查看 CPU、内存、磁盘？

```bash
top
uptime
free -h
df -h
du -sh *
iostat -x 1
vmstat 1
```

---

## 6. 如何定位线上服务异常？

```text
1. 看进程是否存在
2. 看端口是否监听
3. 看应用日志
4. 看 CPU、内存、磁盘
5. 看线程栈
6. 看 GC 情况
7. 看数据库、Redis、MQ 等依赖
8. 先止血，再定位根因
```

---

## 7. CPU 飙高怎么排查？

```bash
top
top -Hp PID
printf "%x\n" TID
jstack PID > thread.txt
grep "nid=0x十六进制线程ID" thread.txt
```

---

## 8. 磁盘满怎么排查？

```bash
df -h
du -sh /*
du -ah /data | sort -rh | head -n 20
```

---

# 九、总览表

| 问题 | 核心命令 |
|---|---|
| 常用 Linux 命令 | `ls`、`cd`、`grep`、`tail`、`ps`、`top`、`netstat`、`lsof` |
| 查看进程 | `ps -ef`、`ps aux`、`top`、`jps -l` |
| 查看端口占用 | `netstat -tunlp`、`ss -tunlp`、`lsof -i:端口` |
| 查看日志 | `tail -f`、`less`、`grep`、`cat` |
| 查看 CPU | `top`、`uptime`、`vmstat` |
| 查看内存 | `free -h`、`top` |
| 查看磁盘 | `df -h`、`du -sh` |
| 查看磁盘 IO | `iostat -x 1` |
| CPU 飙高定位 | `top`、`top -Hp`、`jstack` |
| Java 内存排查 | `jstat`、`jmap`、GC 日志 |
| 日志关键字搜索 | `grep -n`、`grep -C`、`grep -E` |

---

# 十、完整面试回答模板

常用 Linux 命令主要分为几类：文件目录类有 `ls`、`cd`、`pwd`、`mkdir`、`rm`、`cp`、`mv`、`find`；日志查看类有 `cat`、`less`、`head`、`tail`；文本处理类有 `grep`、`awk`、`sed`、`sort`、`uniq`、`wc`；进程和端口类有 `ps`、`top`、`netstat`、`ss`、`lsof`；资源查看类有 `free`、`df`、`du`、`iostat`、`vmstat` 和 `uptime`。

查看进程常用 `ps -ef | grep 服务名`，可以看到进程 PID、启动命令和运行用户。也可以用 `top` 实时查看进程 CPU 和内存占用。如果是 Java 服务，可以用 `jps -l` 查看 Java 进程。如果要查看某个进程下线程资源，可以使用 `top -Hp pid`。

查看端口占用常用 `netstat -tunlp | grep 端口号`，也可以用 `ss -tunlp | grep 端口号`。如果想直接查看某个端口被哪个进程占用，可以使用 `lsof -i:端口号`。查到 PID 后，可以用 `ps -ef | grep PID` 查看进程详情。

查看日志常用 `tail`、`less` 和 `grep`。实时查看日志可以用 `tail -f app.log`，查看最后 100 行可以用 `tail -n 100 app.log`，搜索异常可以用 `grep "Exception" app.log`，查看异常上下文可以用 `grep -C 5 "Exception" app.log`。如果日志文件很大，推荐使用 `less` 分页查看。

查看 CPU 一般使用 `top`、`uptime` 和 `vmstat`。`top` 可以查看 CPU 使用率和进程资源占用，`uptime` 可以查看系统负载。查看内存使用 `free -h`，重点看 `available`。查看磁盘空间使用 `df -h`，查看目录大小使用 `du -sh *`。如果怀疑磁盘 IO 高，可以使用 `iostat -x 1`。

定位线上服务异常时，我会先确认服务是否存活，使用 `ps`、`jps`、`netstat` 或 `lsof` 查看进程和端口。然后查看应用日志，使用 `tail` 和 `grep` 搜索异常、超时、错误码或 traceId。接着查看系统资源，例如 CPU、内存、磁盘和 IO。如果是 CPU 飙高，会用 `top -Hp pid` 找到高 CPU 线程，把线程 ID 转成十六进制，再用 `jstack` 定位线程栈。如果是内存或 GC 问题，会用 `jstat`、`jmap`、GC 日志和 dump 文件分析。线上处理原则是先止血恢复服务，再定位根因和复盘修复。
