# Netty

## 1. Netty 是什么

Netty 是一个基于 Java NIO 的高性能网络通信框架。

它主要用于开发：

```text
高性能 TCP 服务
RPC 框架
网关
IM 即时通信
游戏服务器
长连接服务
消息推送系统
自定义协议服务
```

一句话理解：

> Netty 是对 Java NIO 的高级封装，用来更方便地开发高性能、异步、事件驱动的网络程序。

---

## 2. 为什么需要 Netty

Java 原生提供了：

```text
BIO
NIO
AIO
```

但是直接使用 Java NIO 开发比较复杂。

需要自己处理：

```text
Selector
Channel
Buffer
连接注册
事件轮询
读写事件
半包粘包
线程模型
异常处理
连接关闭
心跳检测
编解码
```

Netty 帮我们封装好了这些底层细节。

开发者只需要关注：

```text
连接建立后做什么
收到消息后怎么处理
消息怎么编码/解码
异常怎么处理
```

---

## 3. Netty 适合解决什么问题

### 3.1 高并发连接

例如：

```text
即时通信
消息推送
物联网设备连接
游戏长连接
网关转发
```

这些场景需要维持大量 TCP 连接，Netty 非常适合。

---

### 3.2 自定义协议通信

如果 HTTP 不满足需求，需要自定义二进制协议，可以使用 Netty。

例如：

```text
RPC 协议
IM 协议
物联网协议
游戏协议
私有网关协议
```

---

### 3.3 高性能网络 IO

Netty 基于 NIO 和事件驱动模型，适合高吞吐、低延迟网络通信。

---

## 4. BIO、NIO、Netty 区别

| 对比项 | BIO | NIO | Netty |
|---|---|---|---|
| IO 模型 | 阻塞 IO | 非阻塞 IO | 基于 NIO 封装 |
| 线程模型 | 一个连接一个线程 | 一个线程处理多个连接 | Reactor 线程模型 |
| 开发难度 | 简单 | 复杂 | 较简单 |
| 性能 | 并发高时差 | 高 | 高 |
| 适合场景 | 小并发 | 高并发 | 高并发网络服务 |

---

## 5. BIO 模型

BIO 是阻塞 IO。

```text
客户端连接
   ↓
服务端创建线程
   ↓
线程阻塞等待数据
   ↓
读取数据
   ↓
处理业务
```

特点：

```text
一个连接通常对应一个线程
连接多时线程数量暴涨
线程切换成本高
资源消耗大
```

示意：

```text
Client 1 -> Thread 1
Client 2 -> Thread 2
Client 3 -> Thread 3
Client N -> Thread N
```

---

## 6. NIO 模型

NIO 是非阻塞 IO。

核心组件：

```text
Channel
Buffer
Selector
```

一个线程可以通过 Selector 监听多个连接事件：

```text
Selector
   ↓
监听多个 Channel
   ↓
哪个 Channel 有事件就处理哪个
```

示意：

```text
Thread
  ↓
Selector
  ↓
Channel 1
Channel 2
Channel 3
Channel N
```

优点：

```text
一个线程可以管理多个连接
适合高并发连接
线程资源消耗低
```

---

## 7. Netty 核心架构

Netty 核心组件：

```text
Bootstrap
ServerBootstrap
EventLoopGroup
EventLoop
Channel
ChannelPipeline
ChannelHandler
ChannelHandlerContext
ByteBuf
```

整体结构：

```text
ServerBootstrap
   ↓
EventLoopGroup
   ↓
Channel
   ↓
ChannelPipeline
   ↓
ChannelHandler
   ↓
业务处理
```

---

# 一、Netty 核心组件

## 8. Bootstrap 和 ServerBootstrap

### 8.1 Bootstrap

`Bootstrap` 用于客户端启动。

```java
Bootstrap bootstrap = new Bootstrap();
```

适合：

```text
Netty 客户端
连接远程服务
```

---

### 8.2 ServerBootstrap

`ServerBootstrap` 用于服务端启动。

```java
ServerBootstrap bootstrap = new ServerBootstrap();
```

适合：

```text
Netty 服务端
监听端口
接收客户端连接
```

---

## 9. EventLoopGroup

`EventLoopGroup` 是事件循环线程组。

服务端一般有两个线程组：

```text
bossGroup
workerGroup
```

示例：

```java
EventLoopGroup bossGroup = new NioEventLoopGroup(1);
EventLoopGroup workerGroup = new NioEventLoopGroup();
```

### bossGroup

负责：

```text
接收客户端连接
处理 accept 事件
把连接注册给 workerGroup
```

### workerGroup

负责：

```text
处理客户端读写事件
执行 ChannelPipeline 中的 Handler
```

---

## 10. EventLoop

`EventLoop` 是事件循环线程。

一个 `EventLoop` 负责处理多个 Channel 的 IO 事件。

特点：

```text
一个 EventLoop 对应一个线程
一个 Channel 只绑定一个 EventLoop
同一个 Channel 的 IO 操作都在同一个线程中执行
避免频繁线程切换
```

结构：

```text
EventLoopGroup
   ├── EventLoop-1 -> Channel A / Channel B
   ├── EventLoop-2 -> Channel C / Channel D
   └── EventLoop-3 -> Channel E / Channel F
```

---

## 11. Channel

`Channel` 表示一个网络连接。

常见 Channel：

| Channel | 说明 |
|---|---|
| `NioServerSocketChannel` | 服务端 Channel |
| `NioSocketChannel` | 客户端 TCP Channel |
| `DatagramChannel` | UDP Channel |

可以理解为：

```text
Channel = 一条网络连接
```

---

## 12. ChannelPipeline

`ChannelPipeline` 是处理器链。

每个 Channel 都有自己的 Pipeline。

```text
Channel
   ↓
ChannelPipeline
   ↓
Handler1 -> Handler2 -> Handler3
```

它类似责任链模式。

请求进入后，会依次经过多个 Handler。

---

## 13. ChannelHandler

`ChannelHandler` 是真正处理 IO 事件的组件。

常见事件：

```text
连接建立
连接断开
读取数据
写出数据
异常发生
```

常见 Handler 类型：

| Handler | 说明 |
|---|---|
| `ChannelInboundHandler` | 处理入站事件 |
| `ChannelOutboundHandler` | 处理出站事件 |
| `ChannelDuplexHandler` | 同时处理入站和出站事件 |
| `SimpleChannelInboundHandler` | 常用入站消息处理器 |

---

## 14. Inbound 和 Outbound

Netty 中事件分为两类：

```text
Inbound 入站事件
Outbound 出站事件
```

### Inbound

从网络进入应用程序。

```text
客户端请求
   ↓
Netty
   ↓
业务 Handler
```

常见事件：

```text
channelActive
channelRead
channelReadComplete
exceptionCaught
```

---

### Outbound

从应用程序写回网络。

```text
业务响应
   ↓
Netty
   ↓
客户端
```

常见事件：

```text
write
flush
connect
bind
close
```

---

## 15. ChannelHandlerContext

`ChannelHandlerContext` 是 Handler 的上下文对象。

作用：

```text
获取 Channel
获取 Pipeline
向下传播事件
写出响应
关闭连接
```

示例：

```java
ctx.writeAndFlush("hello");
ctx.channel().close();
ctx.fireChannelRead(msg);
```

---

## 16. ByteBuf

`ByteBuf` 是 Netty 自己实现的字节缓冲区。

它比 Java NIO 原生 `ByteBuffer` 更好用。

特点：

```text
读写指针分离
支持池化
支持零拷贝
自动扩容
API 更友好
```

ByteBuf 有两个重要指针：

```text
readerIndex
writerIndex
```

结构：

```text
+-------------------+-------------------+-------------------+
| 已读区域           | 可读区域           | 可写区域           |
+-------------------+-------------------+-------------------+
0              readerIndex        writerIndex          capacity
```

---

# 二、Netty 服务端示例

## 17. 服务端代码

```java
public class NettyServer {

    public static void main(String[] args) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new ServerHandler());
                        }
                    });

            ChannelFuture future = bootstrap.bind(8080).sync();

            System.out.println("Netty server started on port 8080");

            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
```

---

## 18. 服务端 Handler

```java
public class ServerHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("客户端连接：" + ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(
            ChannelHandlerContext ctx,
            ByteBuf msg
    ) {
        String request = msg.toString(CharsetUtil.UTF_8);

        System.out.println("收到消息：" + request);

        ByteBuf response = Unpooled.copiedBuffer(
                "Hello Client",
                CharsetUtil.UTF_8
        );

        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(
            ChannelHandlerContext ctx,
            Throwable cause
    ) {
        cause.printStackTrace();
        ctx.close();
    }
}
```

---

# 三、Netty 客户端示例

## 19. 客户端代码

```java
public class NettyClient {

    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();

            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new ClientHandler());
                        }
                    });

            ChannelFuture future = bootstrap
                    .connect("127.0.0.1", 8080)
                    .sync();

            future.channel().writeAndFlush(
                    Unpooled.copiedBuffer("Hello Server", CharsetUtil.UTF_8)
            );

            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}
```

---

## 20. 客户端 Handler

```java
public class ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    protected void channelRead0(
            ChannelHandlerContext ctx,
            ByteBuf msg
    ) {
        String response = msg.toString(CharsetUtil.UTF_8);
        System.out.println("收到服务端响应：" + response);
    }

    @Override
    public void exceptionCaught(
            ChannelHandlerContext ctx,
            Throwable cause
    ) {
        cause.printStackTrace();
        ctx.close();
    }
}
```

---

# 四、Netty 线程模型

## 21. Reactor 模型

Netty 基于 Reactor 模型。

Reactor 模型核心思想：

> 一个或多个线程监听 IO 事件，事件发生后分发给对应 Handler 处理。

常见 Reactor 模型：

```text
单 Reactor 单线程
单 Reactor 多线程
主从 Reactor 多线程
```

---

## 22. 单 Reactor 单线程

```text
一个线程负责：
接收连接
读取数据
业务处理
写回响应
```

优点：

```text
模型简单
没有线程切换
```

缺点：

```text
无法利用多核 CPU
业务处理慢会阻塞所有连接
```

---

## 23. 单 Reactor 多线程

```text
一个 Reactor 线程负责接收和 IO
业务处理交给线程池
```

优点：

```text
业务处理不会阻塞 Reactor
可以利用多线程
```

缺点：

```text
连接接收和 IO 仍由一个 Reactor 负责
高并发下可能成为瓶颈
```

---

## 24. 主从 Reactor 多线程

Netty 服务端常用这种模型。

```text
BossGroup：负责接收连接
WorkerGroup：负责读写事件
业务线程池：可选，负责耗时业务
```

结构：

```text
Client
  ↓
Boss EventLoopGroup
  ↓ accept
Worker EventLoopGroup
  ↓ read/write
ChannelPipeline
  ↓
Business Handler
```

优点：

```text
接收连接和读写分离
支持高并发
适合多核机器
```

---

# 五、Netty 粘包和拆包

## 25. 什么是粘包和拆包

TCP 是面向字节流的协议，它不关心应用层消息边界。

所以可能出现：

```text
发送方发送两条消息
服务端一次读到一整块数据
```

### 粘包

```text
发送：
msg1
msg2

接收：
msg1msg2
```

多个消息粘在一起。

---

### 拆包

```text
发送：
hello world

接收：
hello
world
```

一条消息被拆成多次读取。

---

## 26. 为什么会粘包拆包

原因：

```text
TCP 是字节流协议
操作系统缓冲区合并
Nagle 算法
网络传输分片
接收方读取速度不固定
```

---

## 27. 如何解决粘包拆包

常见方案：

| 方案 | 说明 |
|---|---|
| 固定长度 | 每个消息固定长度 |
| 分隔符 | 使用特殊字符作为消息结束标记 |
| 消息头 + 消息体 | 消息头中记录消息长度 |
| LineBasedFrameDecoder | 按换行符拆分 |
| DelimiterBasedFrameDecoder | 按自定义分隔符拆分 |
| LengthFieldBasedFrameDecoder | 按长度字段拆分，最常用 |

---

## 28. LineBasedFrameDecoder

按换行符拆包。

```java
ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
ch.pipeline().addLast(new StringDecoder());
```

适合：

```text
每条消息以 \n 或 \r\n 结尾
```

---

## 29. DelimiterBasedFrameDecoder

按指定分隔符拆包。

```java
ByteBuf delimiter = Unpooled.copiedBuffer("$_".getBytes());

ch.pipeline().addLast(
        new DelimiterBasedFrameDecoder(1024, delimiter)
);
```

适合：

```text
自定义分隔符协议
```

---

## 30. LengthFieldBasedFrameDecoder

按消息长度字段拆包。

这是最常用的方式。

协议示例：

```text
消息总长度 + 消息内容
```

例如：

```text
4 字节长度字段 + N 字节消息体
```

配置示例：

```java
ch.pipeline().addLast(
        new LengthFieldBasedFrameDecoder(
                1024 * 1024,
                0,
                4,
                0,
                4
        )
);
```

参数说明：

| 参数 | 说明 |
|---|---|
| `maxFrameLength` | 最大帧长度 |
| `lengthFieldOffset` | 长度字段偏移量 |
| `lengthFieldLength` | 长度字段长度 |
| `lengthAdjustment` | 长度修正值 |
| `initialBytesToStrip` | 跳过前几个字节 |

---

# 六、Netty 编解码

## 31. 编码和解码是什么

### 编码 Encoder

把 Java 对象转换成字节。

```text
Java 对象
   ↓
ByteBuf
   ↓
网络传输
```

---

### 解码 Decoder

把字节转换成 Java 对象。

```text
网络字节
   ↓
ByteBuf
   ↓
Java 对象
```

---

## 32. 常见编解码器

| 编解码器 | 说明 |
|---|---|
| `StringDecoder` | ByteBuf 转 String |
| `StringEncoder` | String 转 ByteBuf |
| `ObjectEncoder` | Java 对象编码 |
| `ObjectDecoder` | Java 对象解码 |
| `LengthFieldBasedFrameDecoder` | 按长度拆包 |
| `MessageToByteEncoder` | 自定义编码 |
| `ByteToMessageDecoder` | 自定义解码 |

---

## 33. 自定义编码器

```java
public class MyEncoder extends MessageToByteEncoder<MyMessage> {

    @Override
    protected void encode(
            ChannelHandlerContext ctx,
            MyMessage msg,
            ByteBuf out
    ) {
        byte[] data = msg.getBody().getBytes(StandardCharsets.UTF_8);

        out.writeInt(data.length);
        out.writeBytes(data);
    }
}
```

---

## 34. 自定义解码器

```java
public class MyDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(
            ChannelHandlerContext ctx,
            ByteBuf in,
            List<Object> out
    ) {
        if (in.readableBytes() < 4) {
            return;
        }

        in.markReaderIndex();

        int length = in.readInt();

        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }

        byte[] data = new byte[length];
        in.readBytes(data);

        MyMessage message = new MyMessage(
                new String(data, StandardCharsets.UTF_8)
        );

        out.add(message);
    }
}
```

---

# 七、Netty 心跳机制

## 35. 为什么需要心跳

长连接场景中，需要判断连接是否还活着。

可能出现：

```text
客户端断网
客户端异常退出
连接假死
防火墙断开空闲连接
服务端资源泄露
```

心跳机制用于检测连接状态。

---

## 36. IdleStateHandler

Netty 提供了 `IdleStateHandler`。

```java
ch.pipeline().addLast(new IdleStateHandler(60, 0, 0));
ch.pipeline().addLast(new HeartbeatHandler());
```

参数：

```java
new IdleStateHandler(readerIdleTime, writerIdleTime, allIdleTime)
```

| 参数 | 说明 |
|---|---|
| `readerIdleTime` | 多久没有读事件 |
| `writerIdleTime` | 多久没有写事件 |
| `allIdleTime` | 多久没有读写事件 |

---

## 37. 心跳 Handler

```java
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void userEventTriggered(
            ChannelHandlerContext ctx,
            Object evt
    ) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;

            if (event.state() == IdleState.READER_IDLE) {
                System.out.println("读空闲，关闭连接");
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
```

---

# 八、Netty 零拷贝

## 38. Netty 零拷贝是什么

Netty 零拷贝不是完全没有拷贝，而是尽量减少内存拷贝。

Netty 中常见零拷贝体现：

```text
CompositeByteBuf
ByteBuf slice
ByteBuf duplicate
FileRegion 文件传输
DirectBuffer
```

---

## 39. CompositeByteBuf

把多个 ByteBuf 组合成一个逻辑 ByteBuf，不需要真正复制数据。

```java
CompositeByteBuf composite = Unpooled.compositeBuffer();

composite.addComponents(true, headerBuf, bodyBuf);
```

---

## 40. slice

`slice()` 可以共享原始 ByteBuf 数据。

```java
ByteBuf slice = byteBuf.slice(0, 10);
```

特点：

```text
不复制底层数据
只是创建一个视图
```

---

## 41. FileRegion

用于文件传输，底层可以利用操作系统零拷贝能力。

```java
FileRegion region = new DefaultFileRegion(
        fileChannel,
        0,
        fileChannel.size()
);

ctx.writeAndFlush(region);
```

---

# 九、Netty 高性能原因

## 42. Netty 为什么性能高

主要原因：

```text
基于 NIO 非阻塞 IO
Reactor 线程模型
事件驱动
零拷贝
ByteBuf 池化
内存池设计
无锁串行化设计
减少线程切换
高效的 Pipeline 机制
支持 epoll
```

---

## 43. 无锁串行化设计

一个 Channel 绑定一个 EventLoop。

同一个 Channel 的事件都在同一个线程中执行。

好处：

```text
避免多线程竞争
减少锁开销
线程模型简单
性能更高
```

---

## 44. ByteBuf 池化

Netty 可以复用 ByteBuf，减少频繁内存分配和 GC。

常见池化实现：

```text
PooledByteBufAllocator
```

优点：

```text
减少对象创建
减少 GC 压力
提升内存利用率
```

---

## 45. epoll 支持

在 Linux 下，Netty 可以使用 epoll 提升性能。

```java
EventLoopGroup bossGroup = new EpollEventLoopGroup();
EventLoopGroup workerGroup = new EpollEventLoopGroup();
```

对应 Channel：

```java
EpollServerSocketChannel.class
```

---

# 十、Netty 常见应用

## 46. RPC 框架

很多 RPC 框架底层使用 Netty 做网络通信。

例如：

```text
Dubbo
gRPC Java 底层部分场景
Motan
自研 RPC
```

RPC 流程：

```text
客户端代理
   ↓
序列化请求
   ↓
Netty 发送
   ↓
服务端 Netty 接收
   ↓
反序列化
   ↓
调用目标方法
   ↓
返回响应
```

---

## 47. API 网关

网关需要处理大量网络连接，Netty 很适合。

例如：

```text
Spring Cloud Gateway
```

底层常基于：

```text
WebFlux + Reactor Netty
```

---

## 48. IM 即时通信

IM 系统需要长连接。

```text
客户端登录
   ↓
建立 TCP 长连接
   ↓
心跳保活
   ↓
消息推送
   ↓
断线重连
```

Netty 很适合这类场景。

---

## 49. 游戏服务器

游戏服务器对延迟和连接数要求较高。

Netty 常用于：

```text
TCP 长连接
自定义二进制协议
高并发消息处理
```

---

## 50. 物联网 IoT

IoT 场景设备多，连接多，协议多。

Netty 常用于：

```text
设备接入网关
MQTT 协议
私有 TCP 协议
心跳检测
设备上下线管理
```

---

# 十一、Netty 和 Servlet 的区别

| 对比项 | Servlet | Netty |
|---|---|---|
| 定位 | Java Web 规范 | 网络通信框架 |
| 主要协议 | HTTP | TCP / UDP / HTTP / 自定义协议 |
| 常见容器 | Tomcat、Jetty、Undertow | Netty 自身处理网络 IO |
| 编程模型 | Servlet API | Channel / Pipeline / Handler |
| IO 模型 | 传统 Servlet 多为阻塞，Servlet 3 支持异步 | 异步非阻塞 |
| 适合场景 | Web 应用、REST 接口 | RPC、网关、长连接、自定义协议 |

简单理解：

```text
Servlet 更偏 Web 请求处理规范
Netty 更偏底层网络通信框架
```

---

# 十二、Netty 和 Tomcat 的关系

Tomcat 是 Servlet 容器，主要用于运行 Java Web 应用。

Netty 是网络通信框架，可以用来开发自己的服务器。

对比：

```text
Tomcat：
  主要处理 HTTP 请求
  运行 Servlet / Spring MVC

Netty：
  可以处理 TCP、UDP、HTTP、自定义协议
  更灵活，更底层
```

---

# 十三、Netty 常见问题

## 51. Netty 是同步还是异步？

Netty 是异步事件驱动的网络框架。

它通过 `ChannelFuture` 表示异步操作结果。

```java
ChannelFuture future = channel.writeAndFlush(msg);

future.addListener(f -> {
    if (f.isSuccess()) {
        System.out.println("发送成功");
    } else {
        System.out.println("发送失败");
    }
});
```

---

## 52. Netty 是阻塞还是非阻塞？

Netty 通常基于 NIO，是非阻塞 IO。

一个 EventLoop 线程可以处理多个 Channel 的 IO 事件。

---

## 53. ChannelFuture 是什么？

`ChannelFuture` 表示异步操作的结果。

例如：

```java
ChannelFuture future = bootstrap.bind(8080);
```

绑定端口不是立即完成的，可以通过监听器获取结果：

```java
future.addListener(listener -> {
    if (listener.isSuccess()) {
        System.out.println("绑定成功");
    }
});
```

---

## 54. Netty 的 Handler 是否线程安全？

通常情况下，同一个 Channel 的 Handler 方法由同一个 EventLoop 执行。

但是：

```text
如果 Handler 被多个 Channel 共享
或者使用 @Sharable
或者在业务线程池中处理
```

就需要注意线程安全。

---

## 55. Netty 中可以执行耗时业务吗？

不建议在 EventLoop 线程中执行耗时业务。

错误做法：

```java
@Override
protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
    // 慢 SQL
    // 远程 HTTP 调用
    // 大量计算
}
```

问题：

```text
EventLoop 被阻塞
该 EventLoop 管理的其他连接也会被影响
```

推荐：

```text
耗时业务交给业务线程池
EventLoop 只做 IO 和轻量逻辑
```

示例：

```java
DefaultEventExecutorGroup businessGroup =
        new DefaultEventExecutorGroup(16);

ch.pipeline().addLast(businessGroup, new BusinessHandler());
```

---

# 十四、Netty 面试常问问题

## 56. Netty 是什么？

```text
Netty 是一个基于 Java NIO 的异步事件驱动网络通信框架。
它封装了 Selector、Channel、Buffer 等底层细节，提供了高性能的网络编程能力，常用于 RPC、网关、IM、游戏服务器、物联网等场景。
```

---

## 57. Netty 为什么性能高？

```text
基于 NIO 非阻塞 IO
Reactor 线程模型
事件驱动
一个 Channel 绑定一个 EventLoop，减少锁竞争
ByteBuf 池化减少 GC
零拷贝减少内存复制
Pipeline 责任链模型高效扩展
Linux 下支持 epoll
```

---

## 58. Netty 线程模型是什么？

```text
Netty 服务端通常采用主从 Reactor 多线程模型。
bossGroup 负责接收客户端连接，
workerGroup 负责处理已建立连接的读写事件。
每个 Channel 会绑定到一个 EventLoop，
同一个 Channel 的 IO 事件都由同一个 EventLoop 线程处理。
```

---

## 59. Netty 如何解决 TCP 粘包拆包？

```text
TCP 是字节流协议，没有消息边界，所以会出现粘包和拆包。
Netty 通常通过编解码器解决，比如：
固定长度解码器
分隔符解码器
LineBasedFrameDecoder
DelimiterBasedFrameDecoder
LengthFieldBasedFrameDecoder

实际项目中最常用的是消息头 + 消息体，即使用长度字段标识消息体长度。
```

---

## 60. ChannelPipeline 是什么？

```text
ChannelPipeline 是 Netty 的处理器链，每个 Channel 都有一个 Pipeline。
请求进入后会经过多个 ChannelHandler，
这些 Handler 可以负责编解码、日志、鉴权、心跳、业务处理等。
它本质上是责任链模式。
```

---

## 61. ByteBuf 和 ByteBuffer 区别？

```text
ByteBuffer 是 Java NIO 原生缓冲区，读写共用一个 position，使用起来比较麻烦。
ByteBuf 是 Netty 提供的缓冲区，读写指针分离，支持池化、自动扩容、零拷贝，API 更友好，性能也更好。
```

---

## 62. Netty 中为什么不能阻塞 EventLoop？

```text
因为一个 EventLoop 线程会负责多个 Channel 的 IO 事件。
如果在 EventLoop 中执行慢 SQL、远程调用、大量计算等阻塞操作，
会导致该 EventLoop 管理的所有连接都被影响，
出现请求延迟升高甚至连接超时。
所以耗时业务应该放到业务线程池中执行。
```

---

# 十五、总结

Netty 是一个基于 Java NIO 的高性能网络通信框架，采用异步事件驱动模型，封装了原生 NIO 中复杂的 Selector、Channel、Buffer、线程模型等细节，使我们可以更方便地开发高并发网络服务。

Netty 的核心组件包括 `EventLoopGroup`、`EventLoop`、`Channel`、`ChannelPipeline`、`ChannelHandler` 和 `ByteBuf`。服务端通常使用两个线程组，`bossGroup` 负责接收客户端连接，`workerGroup` 负责处理连接上的读写事件。每个 Channel 会绑定到一个 EventLoop，同一个 Channel 的 IO 事件都在同一个线程中执行，这样可以减少锁竞争和线程切换。

Netty 的高性能主要来自 NIO 非阻塞 IO、Reactor 线程模型、事件驱动、ByteBuf 池化、零拷贝、Pipeline 责任链以及 Linux 下的 epoll 支持。Netty 常用于 RPC 框架、API 网关、IM 长连接、游戏服务器、物联网设备接入等场景。

使用 Netty 时要注意 TCP 粘包拆包问题，通常通过 `LengthFieldBasedFrameDecoder` 等编解码器解决。同时不能在 EventLoop 线程中执行耗时业务，否则会阻塞该 EventLoop 管理的所有连接，耗时任务应该交给业务线程池处理。

---

## 63. 一句话总结

> Netty 是一个基于 Java NIO 的异步非阻塞网络通信框架，核心是 Reactor 线程模型、Channel、Pipeline、Handler 和 ByteBuf，适合开发高并发 TCP 服务、RPC 框架、网关、IM 长连接和自定义协议系统。
