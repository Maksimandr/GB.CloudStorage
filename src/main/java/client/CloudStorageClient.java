package client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;

import java.io.*;
import java.util.Arrays;

/**
 * Класс клиента
 */
public class CloudStorageClient {

    public static final String localDirectory = "localDirectory/";

    public static void main(String[] args) throws InterruptedException {
        new CloudStorageClient().start();
    }

    public void start() throws InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap client = new Bootstrap();
            client.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) {
                            ch.pipeline().addLast(
                                    new LengthFieldBasedFrameDecoder(1024 * 1024,
                                            0,
                                            8,
                                            0,
                                            8),
                                    new LengthFieldPrepender(8),
                                    new ByteArrayEncoder(),
                                    new ByteArrayDecoder(),
                                    new JsonEncoder(),
                                    new JsonDecoder());
                        }
                    });

            ChannelFuture channelFuture = client.connect("localhost", 9000).sync();

            System.out.println("Client started");

            sendRequest(channelFuture, "test.t/", RequestCommands.CREATE_DIR);
            sendRequest(channelFuture, "test.txt/", RequestCommands.CREATE_FILE);
        } finally {
            group.shutdownGracefully();
        }
    }

    private void sendRequest(ChannelFuture channelFuture, String fileName, RequestCommands command) throws InterruptedException {
        if (command.equals(RequestCommands.CREATE_DIR)) {
            createDir(channelFuture, fileName);
        } else if (command.equals(RequestCommands.CREATE_FILE)) {
            createFile(channelFuture, fileName);
        }
    }

    private void createDir(ChannelFuture channelFuture, String fileName) throws InterruptedException {
        Request request = new Request();
        request.setFilename(fileName);
        request.setCommand(RequestCommands.CREATE_DIR);
        channelFuture.channel().writeAndFlush(request).sync();
    }

    /**
     * Метод для отправки заданного файла в виде массива байт
     *
     * @param fileName запрос отсылаемый серверу
     */
    private void createFile(ChannelFuture channelFuture, String fileName) throws InterruptedException {
        try (RandomAccessFile accessFile = new RandomAccessFile(localDirectory + fileName, "rw")) {
            // файл отправляется частями
            Request request;
            byte[] buffer;
            int read;
            // в цикле читаем из файла пока есть данные
            while (true) {
                buffer = new byte[1024];
                request = new Request();
                request.setFilename(fileName);
                request.setCommand(RequestCommands.CREATE_FILE);
                request.setPosition(accessFile.getFilePointer());
                read = accessFile.read(buffer);
                if (read < buffer.length - 1) {
                    // если блок данных меньше заданного пересоздаем массив, чтобы не отправлять лишние данные
                    buffer = Arrays.copyOf(buffer, read);
                    request.setFile(buffer);
                    channelFuture.channel().writeAndFlush(request).sync();
                    break;
                } else {
                    request.setFile(buffer);
                    channelFuture.channel().writeAndFlush(request).sync();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("IO error occurred");
        }
    }
}
