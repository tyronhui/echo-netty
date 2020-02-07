package cn.mldn.http.server;


import cn.mldn.http.server.handler.HttpServerHandler;
import cn.mldn.info.HostInfo;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.stream.ChunkedWriteHandler;

public class HttpServer {
    static {
        DiskFileUpload.baseDirectory = System.getProperty("user.dir") + "/upload/" ;
    }
    public void run() throws Exception {
        // 线程池是提升服务器性能的重要技术手段，利用定长的线程池可以保证核心线程的有效数量
        // 在Netty之中线程池的实现分为两类：主线程池（接收客户端连接）、工作线程池（处理客户端连接）
        EventLoopGroup bossGroup = new NioEventLoopGroup(10); // 创建接收线程池
        EventLoopGroup workerGroup = new NioEventLoopGroup(20); // 创建工作线程池
        System.out.println("服务器启动成功，监听端口为：" + HostInfo.PORT);
        try {
            // 创建一个服务器端的程序类进行NIO启动，同时可以设置Channel
            ServerBootstrap serverBootstrap = new ServerBootstrap();   // 服务器端
            // 设置要使用的线程池以及当前的Channel类型
            serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);
            // 接收到信息之后需要进行处理，于是定义子处理器
            serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    socketChannel.pipeline().addLast(new HttpResponseEncoder()) ;   // 响应编码
                    socketChannel.pipeline().addLast(new HttpRequestDecoder()) ;    // 请求解码
                    socketChannel.pipeline().addLast(new ChunkedWriteHandler()) ; // 图片传输处理器
                    socketChannel.pipeline().addLast(new HttpServerHandler()) ;
                }
            });
            // 可以直接利用常亮进行TCP协议的相关配置
            serverBootstrap.option(ChannelOption.SO_BACKLOG, 128);
            serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
            // ChannelFuture描述的时异步回调的处理操作
            ChannelFuture future = serverBootstrap.bind(HostInfo.PORT).sync();
            future.channel().closeFuture().sync();// 等待Socket被关闭
        } finally {
            workerGroup.shutdownGracefully() ;
            bossGroup.shutdownGracefully() ;
        }
    }
}
