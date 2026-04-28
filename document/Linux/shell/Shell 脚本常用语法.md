# Shell 脚本常用语法

## 1. Shell 脚本是什么

Shell 脚本就是把一组 Linux 命令写到一个文件中，按顺序执行。

常用于：

- 自动部署
- 日志清理
- 定时任务
- 服务启动/停止
- 文件备份
- 批量处理
- 线上问题排查

---

## 2. 第一个 Shell 脚本

创建文件：

```bash
vim hello.sh
```

内容：

```bash
#!/bin/bash

echo "Hello Shell"
```

添加执行权限：

```bash
chmod +x hello.sh
```

执行脚本：

```bash
./hello.sh
```

或者：

```bash
bash hello.sh
```

---

## 3. `#!/bin/bash` 是什么

```bash
#!/bin/bash
```

表示这个脚本使用 `/bin/bash` 来执行。

也可以写成：

```bash
#!/usr/bin/env bash
```

更通用一些。

---

## 4. 变量

### 定义变量

```bash
name="Tom"
age=18
```

注意：`=` 两边不能有空格。

错误写法：

```bash
name = "Tom"
```

---

### 使用变量

```bash
echo $name
echo ${name}
```

推荐使用：

```bash
echo "${name}"
```

---

### 示例

```bash
#!/bin/bash

app_name="user-service"
port=8080

echo "应用名称：${app_name}"
echo "端口号：${port}"
```

---

## 5. 命令结果赋值

```bash
now=$(date "+%Y-%m-%d %H:%M:%S")
echo "${now}"
```

查看当前目录：

```bash
current_dir=$(pwd)
echo "${current_dir}"
```

---

## 6. 脚本参数

执行脚本：

```bash
./deploy.sh user-service 8080
```

脚本内容：

```bash
#!/bin/bash

echo "脚本名称：$0"
echo "第一个参数：$1"
echo "第二个参数：$2"
echo "参数个数：$#"
echo "所有参数：$@"
```

---

## 7. 常用特殊变量

| 变量 | 说明 |
|---|---|
| `$0` | 脚本名称 |
| `$1` | 第 1 个参数 |
| `$2` | 第 2 个参数 |
| `$#` | 参数个数 |
| `$@` | 所有参数 |
| `$?` | 上一条命令执行结果 |
| `$$` | 当前脚本进程 ID |

---

## 8. 判断命令是否执行成功

Linux 中约定：

```text
0     表示成功
非 0  表示失败
```

示例：

```bash
mkdir /tmp/test

if [ $? -eq 0 ]; then
    echo "创建成功"
else
    echo "创建失败"
fi
```

---

## 9. if 判断

### 基本语法

```bash
if [ 条件 ]; then
    命令
else
    命令
fi
```

示例：

```bash
#!/bin/bash

age=18

if [ ${age} -ge 18 ]; then
    echo "成年人"
else
    echo "未成年人"
fi
```

---

## 10. 数字比较

| 表达式 | 说明 |
|---|---|
| `-eq` | 等于 |
| `-ne` | 不等于 |
| `-gt` | 大于 |
| `-ge` | 大于等于 |
| `-lt` | 小于 |
| `-le` | 小于等于 |

示例：

```bash
num=10

if [ ${num} -gt 5 ]; then
    echo "num 大于 5"
fi
```

---

## 11. 字符串判断

| 表达式 | 说明 |
|---|---|
| `=` | 字符串相等 |
| `!=` | 字符串不相等 |
| `-z` | 字符串为空 |
| `-n` | 字符串不为空 |

示例：

```bash
name="Tom"

if [ "${name}" = "Tom" ]; then
    echo "名字是 Tom"
fi
```

判断字符串为空：

```bash
if [ -z "${name}" ]; then
    echo "name 为空"
fi
```

判断字符串不为空：

```bash
if [ -n "${name}" ]; then
    echo "name 不为空"
fi
```

---

## 12. 文件判断

| 表达式 | 说明 |
|---|---|
| `-e file` | 文件或目录存在 |
| `-f file` | 普通文件存在 |
| `-d dir` | 目录存在 |
| `-r file` | 文件可读 |
| `-w file` | 文件可写 |
| `-x file` | 文件可执行 |

示例：

```bash
file="/data/app/app.jar"

if [ -f "${file}" ]; then
    echo "文件存在"
else
    echo "文件不存在"
fi
```

判断目录：

```bash
dir="/data/logs"

if [ ! -d "${dir}" ]; then
    mkdir -p "${dir}"
fi
```

---

## 13. 多条件判断

### 与条件

```bash
if [ ${age} -ge 18 ] && [ "${name}" = "Tom" ]; then
    echo "满足条件"
fi
```

### 或条件

```bash
if [ "${env}" = "dev" ] || [ "${env}" = "test" ]; then
    echo "测试环境"
fi
```

---

## 14. for 循环

### 遍历列表

```bash
for name in Tom Jerry Jack
do
    echo "${name}"
done
```

也可以写成：

```bash
for name in Tom Jerry Jack; do
    echo "${name}"
done
```

---

### 遍历数字

```bash
for i in {1..5}
do
    echo "${i}"
done
```

---

### 遍历文件

```bash
for file in *.log
do
    echo "${file}"
done
```

---

## 15. while 循环

```bash
count=1

while [ ${count} -le 5 ]
do
    echo "${count}"
    count=$((count + 1))
done
```

---

## 16. case 判断

适合多分支判断。

```bash
#!/bin/bash

env=$1

case "${env}" in
    dev)
        echo "开发环境"
        ;;
    test)
        echo "测试环境"
        ;;
    prod)
        echo "生产环境"
        ;;
    *)
        echo "未知环境"
        ;;
esac
```

执行：

```bash
./test.sh prod
```

---

## 17. 函数

### 定义函数

```bash
start() {
    echo "start service"
}
```

### 调用函数

```bash
start
```

### 示例

```bash
#!/bin/bash

start() {
    echo "启动服务"
}

stop() {
    echo "停止服务"
}

restart() {
    stop
    start
}

restart
```

---

## 18. 函数传参

```bash
print_msg() {
    echo "第一个参数：$1"
    echo "第二个参数：$2"
}

print_msg "hello" "world"
```

---

## 19. 退出脚本

```bash
exit 0
```

表示正常退出。

```bash
exit 1
```

表示异常退出。

示例：

```bash
if [ ! -f "app.jar" ]; then
    echo "app.jar 不存在"
    exit 1
fi
```

---

## 20. 重定向

### 覆盖写入

```bash
echo "hello" > a.txt
```

### 追加写入

```bash
echo "hello" >> a.txt
```

### 错误输出重定向

```bash
command 2> error.log
```

### 标准输出和错误输出都写入文件

```bash
command > app.log 2>&1
```

或者：

```bash
command &> app.log
```

---

## 21. 后台执行

```bash
nohup java -jar app.jar > app.log 2>&1 &
```

说明：

| 部分 | 说明 |
|---|---|
| `nohup` | 退出终端后继续运行 |
| `>` | 标准输出重定向 |
| `2>&1` | 错误输出合并到标准输出 |
| `&` | 后台运行 |

---

## 22. 读取用户输入

```bash
#!/bin/bash

read -p "请输入姓名：" name

echo "你好，${name}"
```

---

## 23. 数组

```bash
arr=("java" "mysql" "redis")

echo "${arr[0]}"
echo "${arr[1]}"
echo "${arr[2]}"
```

遍历数组：

```bash
for item in "${arr[@]}"
do
    echo "${item}"
done
```

---

## 24. 算术运算

```bash
a=10
b=20

sum=$((a + b))

echo "${sum}"
```

常见写法：

```bash
count=$((count + 1))
```

---

## 25. 常用脚本模板

### 25.1 Java 服务启动脚本

```bash
#!/bin/bash

APP_NAME="user-service"
JAR_NAME="user-service.jar"
APP_DIR="/data/app/${APP_NAME}"
LOG_DIR="/data/logs/${APP_NAME}"
LOG_FILE="${LOG_DIR}/app.log"

mkdir -p "${LOG_DIR}"

cd "${APP_DIR}" || exit 1

pid=$(ps -ef | grep "${JAR_NAME}" | grep -v grep | awk '{print $2}')

if [ -n "${pid}" ]; then
    echo "${APP_NAME} 已经启动，pid=${pid}"
    exit 0
fi

echo "开始启动 ${APP_NAME}"

nohup java -jar "${JAR_NAME}" > "${LOG_FILE}" 2>&1 &

sleep 3

pid=$(ps -ef | grep "${JAR_NAME}" | grep -v grep | awk '{print $2}')

if [ -n "${pid}" ]; then
    echo "${APP_NAME} 启动成功，pid=${pid}"
else
    echo "${APP_NAME} 启动失败"
    exit 1
fi
```

---

### 25.2 Java 服务停止脚本

```bash
#!/bin/bash

JAR_NAME="user-service.jar"

pid=$(ps -ef | grep "${JAR_NAME}" | grep -v grep | awk '{print $2}')

if [ -z "${pid}" ]; then
    echo "服务未启动"
    exit 0
fi

echo "停止服务，pid=${pid}"

kill "${pid}"

sleep 5

pid=$(ps -ef | grep "${JAR_NAME}" | grep -v grep | awk '{print $2}')

if [ -n "${pid}" ]; then
    echo "服务未正常停止，强制 kill"
    kill -9 "${pid}"
else
    echo "服务已停止"
fi
```

---

### 25.3 Java 服务重启脚本

```bash
#!/bin/bash

APP_NAME="user-service"
JAR_NAME="user-service.jar"
APP_DIR="/data/app/${APP_NAME}"
LOG_DIR="/data/logs/${APP_NAME}"
LOG_FILE="${LOG_DIR}/app.log"

mkdir -p "${LOG_DIR}"

cd "${APP_DIR}" || exit 1

pid=$(ps -ef | grep "${JAR_NAME}" | grep -v grep | awk '{print $2}')

if [ -n "${pid}" ]; then
    echo "停止 ${APP_NAME}，pid=${pid}"
    kill "${pid}"
    sleep 5
fi

pid=$(ps -ef | grep "${JAR_NAME}" | grep -v grep | awk '{print $2}')

if [ -n "${pid}" ]; then
    echo "强制停止 ${APP_NAME}，pid=${pid}"
    kill -9 "${pid}"
fi

echo "启动 ${APP_NAME}"

nohup java -jar "${JAR_NAME}" > "${LOG_FILE}" 2>&1 &

sleep 3

pid=$(ps -ef | grep "${JAR_NAME}" | grep -v grep | awk '{print $2}')

if [ -n "${pid}" ]; then
    echo "${APP_NAME} 启动成功，pid=${pid}"
else
    echo "${APP_NAME} 启动失败"
    exit 1
fi
```

---

## 26. 通用服务管理脚本

```bash
#!/bin/bash

APP_NAME="user-service"
JAR_NAME="user-service.jar"
APP_DIR="/data/app/${APP_NAME}"
LOG_DIR="/data/logs/${APP_NAME}"
LOG_FILE="${LOG_DIR}/app.log"

JAVA_OPTS="-Xms512m -Xmx512m"

mkdir -p "${LOG_DIR}"

start() {
    cd "${APP_DIR}" || exit 1

    pid=$(get_pid)

    if [ -n "${pid}" ]; then
        echo "${APP_NAME} 已经启动，pid=${pid}"
        return
    fi

    echo "启动 ${APP_NAME}"

    nohup java ${JAVA_OPTS} -jar "${JAR_NAME}" > "${LOG_FILE}" 2>&1 &

    sleep 3

    pid=$(get_pid)

    if [ -n "${pid}" ]; then
        echo "${APP_NAME} 启动成功，pid=${pid}"
    else
        echo "${APP_NAME} 启动失败"
        exit 1
    fi
}

stop() {
    pid=$(get_pid)

    if [ -z "${pid}" ]; then
        echo "${APP_NAME} 未启动"
        return
    fi

    echo "停止 ${APP_NAME}，pid=${pid}"
    kill "${pid}"

    sleep 5

    pid=$(get_pid)

    if [ -n "${pid}" ]; then
        echo "强制停止 ${APP_NAME}，pid=${pid}"
        kill -9 "${pid}"
    fi

    echo "${APP_NAME} 已停止"
}

restart() {
    stop
    start
}

status() {
    pid=$(get_pid)

    if [ -n "${pid}" ]; then
        echo "${APP_NAME} 正在运行，pid=${pid}"
    else
        echo "${APP_NAME} 未运行"
    fi
}

get_pid() {
    ps -ef | grep "${JAR_NAME}" | grep -v grep | awk '{print $2}'
}

case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    *)
        echo "用法：$0 {start|stop|restart|status}"
        exit 1
        ;;
esac
```

使用方式：

```bash
chmod +x app.sh

./app.sh start
./app.sh stop
./app.sh restart
./app.sh status
```

---

## 27. 日志清理脚本

删除 30 天前的日志：

```bash
#!/bin/bash

LOG_DIR="/data/logs"
DAYS=30

if [ ! -d "${LOG_DIR}" ]; then
    echo "日志目录不存在：${LOG_DIR}"
    exit 1
fi

echo "开始清理 ${LOG_DIR} 下 ${DAYS} 天前的日志"

find "${LOG_DIR}" -type f -name "*.log" -mtime +${DAYS} -delete

echo "日志清理完成"
```

---

## 28. 文件备份脚本

```bash
#!/bin/bash

SOURCE_DIR="/data/app"
BACKUP_DIR="/data/backup"
DATE=$(date "+%Y%m%d_%H%M%S")

mkdir -p "${BACKUP_DIR}"

tar -zcvf "${BACKUP_DIR}/app_${DATE}.tar.gz" "${SOURCE_DIR}"

if [ $? -eq 0 ]; then
    echo "备份成功：${BACKUP_DIR}/app_${DATE}.tar.gz"
else
    echo "备份失败"
    exit 1
fi
```

---

## 29. 定时任务 crontab

查看定时任务：

```bash
crontab -l
```

编辑定时任务：

```bash
crontab -e
```

每天凌晨 2 点清理日志：

```bash
0 2 * * * /bin/bash /data/scripts/clean_log.sh
```

每 5 分钟执行一次：

```bash
*/5 * * * * /bin/bash /data/scripts/check.sh
```

---

## 30. Shell 脚本注意事项

### 30.1 变量引用建议加双引号

推荐：

```bash
rm -rf "${dir}"
```

不推荐：

```bash
rm -rf $dir
```

如果变量为空，可能造成严重问题。

---

### 30.2 删除操作要谨慎

危险写法：

```bash
rm -rf ${dir}/*
```

如果 `dir` 为空，可能变成：

```bash
rm -rf /*
```

更安全：

```bash
if [ -n "${dir}" ] && [ -d "${dir}" ]; then
    rm -rf "${dir:?}"/*
fi
```

---

### 30.3 脚本开头可以加严格模式

```bash
set -e
set -u
set -o pipefail
```

说明：

| 配置 | 说明 |
|---|---|
| `set -e` | 命令失败立即退出 |
| `set -u` | 使用未定义变量时报错 |
| `set -o pipefail` | 管道中任意命令失败都算失败 |

常用写法：

```bash
set -euo pipefail
```

---

## 31. 面试/工作常用 Shell 命令组合

### 查找 Java 进程

```bash
ps -ef | grep java | grep -v grep
```

### 获取 Java 进程 PID

```bash
pid=$(ps -ef | grep app.jar | grep -v grep | awk '{print $2}')
```

### 判断进程是否存在

```bash
if [ -n "${pid}" ]; then
    echo "进程存在"
else
    echo "进程不存在"
fi
```

### 批量处理日志

```bash
for file in *.log
do
    echo "处理文件：${file}"
    grep "ERROR" "${file}"
done
```

### 统计日志错误数量

```bash
error_count=$(grep -c "ERROR" app.log)
echo "错误数量：${error_count}"
```

---

## 32. 一句话总结

> Shell 脚本就是把 Linux 命令组织起来，实现自动化执行。核心语法包括变量、参数、条件判断、循环、函数、重定向和退出码，实际工作中最常用于服务启动停止、日志清理、文件备份、定时任务和线上问题排查。
