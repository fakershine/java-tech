# 百万级数据量导出 Excel 设计方案

## 目录

- [一、问题背景](#一问题背景)
- [二、百万级 Excel 导出的核心难点](#二百万级-excel-导出的核心难点)
- [三、总体设计思路](#三总体设计思路)
- [四、为什么不能同步导出](#四为什么不能同步导出)
- [五、推荐整体架构](#五推荐整体架构)
- [六、导出流程设计](#六导出流程设计)
- [七、数据库查询优化](#七数据库查询优化)
- [八、分页查询和游标查询](#八分页查询和游标查询)
- [九、Excel 写入优化](#九excel-写入优化)
- [十、如何避免 OOM](#十如何避免-oom)
- [十一、如何处理 Excel 行数限制](#十一如何处理-excel-行数限制)
- [十二、导出任务表设计](#十二导出任务表设计)
- [十三、任务状态设计](#十三任务状态设计)
- [十四、如何保证任务可靠性](#十四如何保证任务可靠性)
- [十五、如何防止重复导出](#十五如何防止重复导出)
- [十六、如何做权限和数据安全](#十六如何做权限和数据安全)
- [十七、如何做限流和资源控制](#十七如何做限流和资源控制)
- [十八、如何做失败重试和补偿](#十八如何做失败重试和补偿)
- [十九、百万级导出代码示例](#十九百万级导出代码示例)
- [二十、面试回答模板](#二十面试回答模板)

---

# 一、问题背景

百万级数据量导出 Excel，一般出现在管理后台、报表系统、订单系统、财务系统、对账系统中。

例如：

```text
导出 100 万订单
导出 100 万用户
导出 100 万交易流水
导出 100 万操作日志
导出 100 万库存明细
```

这种导出不能简单用普通接口同步返回文件，否则很容易出现：

```text
接口超时
网关超时
数据库压力过大
JVM 内存溢出
Excel 文件过大
用户体验差
任务失败不可追踪
```

---

# 二、百万级 Excel 导出的核心难点

百万级导出主要难点有：

```text
1. 数据量大，查询慢
2. 一次性加载数据容易 OOM
3. Excel 文件生成耗时长
4. HTTP 同步请求容易超时
5. 多人同时导出会拖垮数据库
6. 大文件上传下载耗时长
7. Excel 单 Sheet 有行数限制
8. 任务失败后需要重试和追踪
9. 导出数据涉及权限和脱敏
10. 需要控制导出资源和并发
```

---

# 三、总体设计思路

百万级数据导出，核心思路是：

```text
异步导出
分页查询
流式写入
文件上传
下载中心
任务状态管理
限流控制
失败补偿
```

不要让用户请求一直等待文件生成完成。

推荐流程是：

```text
用户提交导出任务
        |
        v
后端创建导出任务
        |
        v
MQ 异步投递
        |
        v
Worker 后台分页查询数据
        |
        v
流式写入 Excel / CSV
        |
        v
上传 OSS / MinIO / 文件服务器
        |
        v
更新任务状态
        |
        v
用户到下载中心下载
```

---

# 四、为什么不能同步导出

## 1. 同步导出问题

同步导出流程：

```text
用户点击导出
    |
    v
后端查询所有数据
    |
    v
生成 Excel
    |
    v
接口直接返回文件
```

这种方式在小数据量时可以使用。

但是百万级数据会有问题：

```text
1. 接口可能执行几分钟甚至十几分钟
2. Nginx、Gateway、浏览器可能超时
3. 查询结果全部放内存容易 OOM
4. 文件生成期间线程一直被占用
5. 多用户同时导出会拖垮服务
```

---

## 2. 推荐异步导出

异步导出流程：

```text
1. 用户提交导出条件
2. 后端立即返回 taskId
3. 后台异步生成文件
4. 用户轮询任务状态
5. 生成完成后下载
```

优点：

```text
接口响应快
任务可追踪
失败可重试
资源可控制
用户体验更好
```

---

# 五、推荐整体架构

```text
前端页面
   |
   | 1. 提交导出条件
   v
导出接口
   |
   | 2. 创建导出任务
   v
export_task 表
   |
   | 3. 发送 MQ 消息
   v
MQ 消息队列
   |
   | 4. Worker 消费任务
   v
导出 Worker
   |
   | 5. 分页查询数据库
   | 6. 流式写 Excel
   v
临时文件
   |
   | 7. 上传 OSS / MinIO
   v
文件存储
   |
   | 8. 更新任务状态为成功
   v
下载中心
```

---

# 六、导出流程设计

## 1. 创建导出任务

用户点击导出时，后端不直接生成文件，而是创建导出任务。

流程：

```text
1. 校验用户登录状态
2. 校验导出权限
3. 校验查询条件
4. 校验数据量是否超过限制
5. 防止重复提交
6. 创建导出任务，状态为 WAITING
7. 发送 MQ 消息
8. 返回 taskId
```

---

## 2. 后台执行任务

Worker 消费 MQ 消息后执行导出：

```text
1. 查询导出任务
2. 判断任务状态是否 WAITING
3. 抢占任务，更新为 PROCESSING
4. 分页查询数据
5. 流式写入 Excel
6. 上传文件到 OSS / MinIO
7. 更新任务状态为 SUCCESS
8. 通知用户下载
```

---

## 3. 用户下载文件

```text
1. 用户进入下载中心
2. 查看导出任务列表
3. 任务成功后点击下载
4. 后端校验权限
5. 返回临时下载 URL
6. 用户下载文件
```

---

# 七、数据库查询优化

百万级导出中，数据库查询是最容易成为瓶颈的地方。

## 1. 避免 SELECT *

错误写法：

```sql
SELECT *
FROM orders
WHERE create_time BETWEEN ? AND ?;
```

正确写法：

```sql
SELECT id, order_no, user_id, amount, status, create_time
FROM orders
WHERE create_time BETWEEN ? AND ?;
```

只查询导出需要的字段。

---

## 2. 必须建立合适索引

例如按时间范围导出订单：

```sql
CREATE INDEX idx_create_time ON orders(create_time);
```

如果是多租户系统：

```sql
CREATE INDEX idx_tenant_time ON orders(tenant_id, create_time);
```

如果按用户和时间查询：

```sql
CREATE INDEX idx_user_time ON orders(user_id, create_time);
```

---

## 3. 避免深分页

不推荐：

```sql
SELECT id, order_no, amount
FROM orders
ORDER BY id
LIMIT 900000, 1000;
```

因为偏移量越大，扫描成本越高。

推荐使用游标分页：

```sql
SELECT id, order_no, amount
FROM orders
WHERE id > #{lastId}
ORDER BY id
LIMIT 1000;
```

---

## 4. 使用只读库或报表库

百万级导出可能对主库造成压力。

推荐：

```text
普通业务查询走主库
大数据导出走从库
复杂报表走报表库
```

---

## 5. 控制单次导出范围

不要允许用户无条件导出全部数据。

可以限制：

```text
最多导出 100 万行
最多导出 3 个月数据
普通用户最多导出 10 万行
管理员最多导出 100 万行
```

---

# 八、分页查询和游标查询

## 1. 普通分页

普通分页：

```sql
SELECT id, order_no, amount
FROM orders
WHERE create_time BETWEEN ? AND ?
ORDER BY id
LIMIT #{offset}, #{pageSize};
```

适合：

```text
小数据量
前几页查询
```

不适合百万级深分页。

---

## 2. 游标分页

推荐使用：

```sql
SELECT id, order_no, amount
FROM orders
WHERE id > #{lastId}
  AND create_time BETWEEN ? AND ?
ORDER BY id
LIMIT #{pageSize};
```

每次查询后记录最后一条数据的 ID：

```text
lastId = 当前批次最后一条记录的 id
```

下一次从这个 ID 后面继续查。

---

## 3. 游标分页优点

```text
性能稳定
不会随着页码变大而明显变慢
适合大数据量导出
实现简单
```

---

## 4. 注意事项

使用游标分页时，排序字段要稳定。

推荐：

```text
主键 id
创建时间 + id
```

如果只按 create_time 排序，可能存在相同时间导致数据遗漏或重复的问题。

可以使用：

```sql
WHERE create_time > #{lastCreateTime}
   OR (create_time = #{lastCreateTime} AND id > #{lastId})
ORDER BY create_time, id
LIMIT #{pageSize};
```

---

# 九、Excel 写入优化

## 1. 推荐使用 EasyExcel

百万级数据导出推荐使用：

```text
EasyExcel
```

原因：

```text
1. 支持流式写入
2. 内存占用较低
3. 适合大数据量 Excel 导出
4. 使用简单
```

不建议直接使用 Apache POI 普通模式一次性构建整个 Workbook。

---

## 2. 流式写入思想

核心思想：

```text
查一批
写一批
清一批
```

不要这样：

```java
List<Data> allData = queryAll();
writeExcel(allData);
```

应该这样：

```text
while 有数据:
    查询一页数据
    写入 Excel
    释放当前页数据
```

---

## 3. 分批写入

例如每批 1000 或 5000 条：

```text
每次查询 5000 条
写入 Excel
清理 List
继续下一批
```

具体 pageSize 要根据：

```text
单行字段数量
字段大小
数据库性能
JVM 内存
文件写入性能
```

综合评估。

---

# 十、如何避免 OOM

## 1. OOM 常见原因

```text
1. 一次性查询所有数据
2. 使用 List 保存全部导出结果
3. 使用 POI 普通模式生成大 Excel
4. 多个导出任务同时执行
5. 单行字段过大
6. 导出图片、富文本等复杂内容
7. JVM 堆配置过小
```

---

## 2. 避免 OOM 的核心方案

```text
1. 分页查询
2. 游标分页
3. 流式写入
4. 限制导出字段
5. 限制单次导出最大行数
6. 控制导出任务并发
7. 使用 EasyExcel 或 CSV
8. Worker 独立部署
9. 及时释放临时对象
10. 监控 JVM 内存和 GC
```

---

## 3. 不要把所有数据放内存

错误方式：

```java
List<OrderExportDTO> allData = orderMapper.selectAll(param);
easyExcel.write(allData);
```

正确方式：

```java
while (true) {
    List<OrderExportDTO> pageData = orderMapper.selectByLastId(lastId, pageSize);
    if (pageData.isEmpty()) {
        break;
    }

    excelWriter.write(pageData, sheet);
    lastId = pageData.get(pageData.size() - 1).getId();
    pageData.clear();
}
```

---

# 十一、如何处理 Excel 行数限制

## 1. Excel 单 Sheet 行数限制

`.xlsx` 单个 Sheet 最大行数大约是：

```text
1,048,576 行
```

所以百万级数据如果接近或超过这个数量，需要注意：

```text
不能无限写入同一个 Sheet
```

---

## 2. 解决方案一：拆分多个 Sheet

例如每个 Sheet 写 50 万行：

```text
订单数据_1：50 万行
订单数据_2：50 万行
订单数据_3：剩余数据
```

---

## 3. 解决方案二：拆分多个文件并压缩

例如超过 100 万行时：

```text
order_export_1.xlsx
order_export_2.xlsx
order_export_3.xlsx
```

然后打包成：

```text
order_export.zip
```

---

## 4. 解决方案三：使用 CSV

如果用户只是做数据分析，不需要 Excel 样式，推荐 CSV。

CSV 优点：

```text
生成快
文件小
内存占用低
适合千万级数据
```

CSV 缺点：

```text
没有复杂样式
公式和多 Sheet 支持较弱
```

---

# 十二、导出任务表设计

```sql
CREATE TABLE export_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_no VARCHAR(64) NOT NULL,
    tenant_id BIGINT DEFAULT NULL,
    user_id BIGINT NOT NULL,
    export_type VARCHAR(64) NOT NULL,
    export_name VARCHAR(255) NOT NULL,
    request_param TEXT,
    param_md5 VARCHAR(64) DEFAULT NULL,
    file_name VARCHAR(255) DEFAULT NULL,
    file_path VARCHAR(512) DEFAULT NULL,
    file_size BIGINT DEFAULT NULL,
    status TINYINT NOT NULL,
    progress INT DEFAULT 0,
    total_count BIGINT DEFAULT 0,
    exported_count BIGINT DEFAULT 0,
    retry_count INT DEFAULT 0,
    fail_reason TEXT,
    expire_time DATETIME DEFAULT NULL,
    start_time DATETIME DEFAULT NULL,
    finish_time DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_task_no(task_no),
    INDEX idx_user_status(user_id, status),
    INDEX idx_tenant_user_time(tenant_id, user_id, create_time),
    INDEX idx_status_update_time(status, update_time),
    INDEX idx_param_md5(user_id, export_type, param_md5)
);
```

---

# 十三、任务状态设计

## 1. 状态枚举

```text
WAITING：等待中
PROCESSING：处理中
SUCCESS：成功
FAILED：失败
CANCELLED：已取消
EXPIRED：已过期
```

---

## 2. 状态流转

```text
WAITING -> PROCESSING
PROCESSING -> SUCCESS
PROCESSING -> FAILED
PROCESSING -> CANCELLED
SUCCESS -> EXPIRED
FAILED -> WAITING
```

---

## 3. 状态更新要带条件

Worker 抢占任务时：

```sql
UPDATE export_task
SET status = 1,
    start_time = NOW(),
    update_time = NOW()
WHERE id = ?
  AND status = 0;
```

只有更新成功的 Worker 才能继续执行。

这样可以防止多个 Worker 重复导出同一个任务。

---

# 十四、如何保证任务可靠性

## 1. 可能出现的问题

```text
1. 任务创建成功，MQ 发送失败
2. MQ 发送成功，Worker 消费失败
3. Worker 导出过程中宕机
4. 文件生成成功，上传 OSS 失败
5. OSS 上传成功，任务状态更新失败
6. 任务一直卡在 PROCESSING
```

---

## 2. 可靠性方案

```text
1. 创建任务和本地消息表放在同一个事务
2. MQ 发送失败可以重试
3. Worker 消费失败自动重试
4. 任务状态机防止重复执行
5. PROCESSING 超时任务定时补偿
6. 文件上传成功后记录 file_path
7. 失败任务支持人工重试
8. 任务执行日志可追踪
```

---

## 3. 本地消息表方案

流程：

```text
1. 插入 export_task
2. 插入 local_message
3. 提交本地事务
4. 后台任务扫描 local_message
5. 发送 MQ
6. 发送成功后更新 local_message 状态
7. 发送失败继续重试
```

这样可以保证：

```text
导出任务创建成功后，消息最终能被发送出去。
```

---

## 4. 超时补偿

定时扫描卡住的任务：

```sql
SELECT *
FROM export_task
WHERE status = 1
  AND update_time < NOW() - INTERVAL 30 MINUTE;
```

处理方式：

```text
1. 如果 retry_count 未超过限制，重置为 WAITING
2. 重新投递 MQ
3. 如果超过限制，标记为 FAILED
```

---

# 十五、如何防止重复导出

## 1. 重复导出场景

```text
用户连续点击导出按钮
前端超时后重试
同一个条件多次提交
网关重试
```

---

## 2. 防重复方案

```text
1. 前端按钮置灰
2. Redis SET NX
3. requestId 幂等
4. 用户 + 导出类型 + 参数 MD5 去重
5. 数据库唯一索引兜底
```

---

## 3. Redis 防重复 Key

```text
export:submit:{tenantId}:{userId}:{exportType}:{paramMd5}
```

设置：

```bash
SET export:submit:1001:2001:ORDER_EXPORT:abc123 1 NX EX 60
```

设置成功：

```text
允许创建任务
```

设置失败：

```text
重复提交，直接返回已有任务或提示稍后再试
```

---

## 4. 参数 MD5

将导出参数标准化后计算 MD5：

```text
paramMd5 = md5(userId + exportType + sortedRequestParam)
```

这样可以识别相同用户、相同导出类型、相同条件的重复导出。

---

# 十六、如何做权限和数据安全

## 1. 导出前权限校验

导出前必须校验：

```text
1. 用户是否登录
2. 用户是否有导出按钮权限
3. 用户是否有当前业务数据权限
4. 是否允许导出敏感字段
5. 是否超过导出数量限制
```

---

## 2. 数据权限

例如订单导出：

```sql
SELECT id, order_no, amount, status
FROM orders
WHERE tenant_id = 当前租户
  AND dept_id IN 当前用户可访问部门
  AND create_time BETWEEN ? AND ?;
```

---

## 3. 敏感字段脱敏

如果用户没有敏感字段权限：

```text
手机号：138****8000
身份证：110101********1234
银行卡：6222 **** **** 1234
```

---

## 4. 文件下载安全

文件生成后不要暴露永久地址。

推荐：

```text
OSS 私有 Bucket
后端校验权限
生成短期临时 URL
记录下载日志
文件过期自动清理
```

---

# 十七、如何做限流和资源控制

## 1. 为什么要限流

百万级导出非常消耗资源：

```text
数据库连接
CPU
内存
磁盘 IO
网络带宽
OSS 流量
```

如果不限制，可能影响正常业务。

---

## 2. 限制维度

```text
1. 单用户同时导出任务数
2. 单租户同时导出任务数
3. 全局 Worker 并发数
4. 单次最大导出行数
5. 单次最大文件大小
6. 单用户每天导出次数
```

---

## 3. 示例规则

```text
单用户最多同时 2 个导出任务
单租户最多同时 10 个导出任务
全局最多同时 50 个导出任务
普通用户最多导出 10 万条
管理员最多导出 100 万条
超过 100 万建议改为 CSV
```

---

## 4. Worker 线程池

```text
核心线程数：5
最大线程数：10
队列长度：100
拒绝策略：任务重新入队或标记等待
```

---

# 十八、如何做失败重试和补偿

## 1. 失败类型

```text
数据库查询失败
文件写入失败
OSS 上传失败
MQ 消费失败
任务被取消
数据量超过限制
权限不足
```

---

## 2. 可重试失败

```text
数据库连接超时
OSS 临时异常
网络抖动
MQ 临时异常
```

---

## 3. 不可重试失败

```text
参数错误
权限不足
数据量超过限制
业务条件非法
```

---

## 4. 重试策略

```text
最大重试 3 次
每次重试间隔递增
记录失败原因
超过次数标记 FAILED
用户可以重新发起导出
```

---

# 十九、百万级导出代码示例

## 1. 导出核心代码示例

```java
public void exportOrders(Long taskId) {
    ExportTask task = exportTaskMapper.selectById(taskId);

    if (task == null) {
        return;
    }

    int updated = exportTaskMapper.updateStatus(
            taskId,
            ExportStatus.WAITING,
            ExportStatus.PROCESSING
    );

    if (updated == 0) {
        return;
    }

    File tempFile = null;

    try {
        tempFile = File.createTempFile("order_export_" + taskId, ".xlsx");

        ExcelWriter excelWriter = EasyExcel.write(tempFile, OrderExportDTO.class).build();

        try {
            int sheetNo = 0;
            int sheetRowCount = 0;
            int maxSheetRows = 500000;
            Long lastId = 0L;
            int pageSize = 5000;
            long exportedCount = 0;

            WriteSheet writeSheet = EasyExcel.writerSheet(sheetNo, "订单数据_" + (sheetNo + 1)).build();

            while (true) {
                List<OrderExportDTO> dataList = orderMapper.selectExportPage(
                        lastId,
                        pageSize,
                        task.getRequestParam()
                );

                if (dataList == null || dataList.isEmpty()) {
                    break;
                }

                if (sheetRowCount + dataList.size() > maxSheetRows) {
                    sheetNo++;
                    sheetRowCount = 0;
                    writeSheet = EasyExcel.writerSheet(sheetNo, "订单数据_" + (sheetNo + 1)).build();
                }

                excelWriter.write(dataList, writeSheet);

                lastId = dataList.get(dataList.size() - 1).getId();
                exportedCount += dataList.size();
                sheetRowCount += dataList.size();

                exportTaskMapper.updateProgress(taskId, exportedCount);

                dataList.clear();
            }
        } finally {
            excelWriter.finish();
        }

        String filePath = fileService.upload(tempFile);

        exportTaskMapper.updateSuccess(
                taskId,
                filePath,
                tempFile.length()
        );
    } catch (Exception e) {
        exportTaskMapper.updateFailed(taskId, e.getMessage());
    } finally {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }
}
```

---

## 2. Mapper 查询示例

```sql
SELECT id,
       order_no,
       user_id,
       amount,
       status,
       create_time
FROM orders
WHERE id > #{lastId}
  AND create_time BETWEEN #{startTime} AND #{endTime}
ORDER BY id
LIMIT #{pageSize}
```

多租户场景：

```sql
SELECT id,
       order_no,
       user_id,
       amount,
       status,
       create_time
FROM orders
WHERE tenant_id = #{tenantId}
  AND id > #{lastId}
  AND create_time BETWEEN #{startTime} AND #{endTime}
ORDER BY id
LIMIT #{pageSize}
```

---

# 二十、面试回答模板

如果让我设计百万级数据导出 Excel，我不会直接用同步接口返回文件，而是会设计成异步导出任务。

用户点击导出后，后端先校验权限、参数和数据范围，然后创建一条导出任务记录，状态为等待中，再通过 MQ 投递给后台 Worker，接口直接返回 taskId。前端可以通过 taskId 查询任务进度，等任务完成后到下载中心下载文件。

后台 Worker 消费导出任务时，会先通过状态机抢占任务，例如 `UPDATE export_task SET status = PROCESSING WHERE id = ? AND status = WAITING`，防止多个 Worker 重复执行同一个任务。然后按分页或游标方式查询数据，不能一次性把百万数据全部查出来放到内存中。推荐使用 `id > lastId ORDER BY id LIMIT pageSize` 的游标分页方式，避免深分页带来的性能问题。

文件生成时，我会使用 EasyExcel 这类支持流式写入的工具，采用“查一批、写一批、清一批”的方式处理数据。每次查询 1000 到 5000 条数据，写入 Excel 后立即释放当前批次对象，避免 JVM 内存溢出。如果数据接近或超过 Excel 单 Sheet 行数限制，需要自动拆分多个 Sheet，或者拆成多个 Excel 文件后打成 ZIP。如果用户不需要复杂 Excel 样式，大数据量场景下也可以优先推荐 CSV。

数据库方面，要避免 `SELECT *`，只查询导出需要的字段，并且根据导出条件建立合适索引，比如 `(tenant_id, create_time)`、`(user_id, create_time)` 或主键游标索引。大批量导出最好走只读库或报表库，避免影响线上主库。同时要限制单次最大导出条数，防止用户无条件导出全表。

为了保证任务可靠性，创建导出任务和发送 MQ 消息可以结合本地消息表，避免任务创建成功但消息发送失败。Worker 执行失败后可以重试，长时间卡在处理中状态的任务由定时任务扫描出来，如果未超过最大重试次数，就重新投递；超过次数则标记失败。

权限和安全方面，导出前要校验用户是否有导出权限和数据权限。导出的数据也要遵守数据权限范围，不能因为导出绕过权限控制。手机号、身份证、银行卡等敏感字段要根据权限决定是否脱敏。文件生成后上传到 OSS 或 MinIO，下载时后端再次校验权限，并生成短期有效的临时 URL，同时记录下载日志。

资源控制方面，需要限制单用户、单租户和全局同时导出任务数，也要限制 Worker 线程池大小，避免多个百万级导出任务同时执行拖垮数据库和应用服务。

总结来说，百万级 Excel 导出的核心方案是：异步任务、MQ 削峰、分页查询、游标分页、流式写入、文件上传、下载中心、权限控制、任务重试和资源限流。这样既能避免接口超时和 OOM，也能保证导出任务可追踪、可重试、可控制。
