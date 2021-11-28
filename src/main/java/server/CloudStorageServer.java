package server;

import common.MyMessageDecoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.io.File;

/**
 * Класс сервера
 */
public class CloudStorageServer {

    public static final File serverDirectory = new File("serverDirectory");

    public static void main(String[] args) throws InterruptedException {
        new CloudStorageServer().start();
    }

    public void start() throws InterruptedException {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap server = new ServerBootstrap();
            server
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) {
                            ch.pipeline().addLast(
                                    new ObjectEncoder(),
                                    new ObjectDecoder(1024 * 1024 + 2048, ClassResolvers.cacheDisabled(null)), // размер указан исходя из того, что размер данных в request = 1024 * 1024
                                    new MyMessageDecoder(serverDirectory));
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture channelFuture = server.bind(9000).sync();
            System.out.println("Server started");
            channelFuture.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
