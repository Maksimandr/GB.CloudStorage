package client;

import common.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * Класс клиента
 */
public class CloudStorageClient {

    private static final File clientDirectory = new File("clientDirectory");

    public static void main(String[] args) throws InterruptedException, IOException {
        new CloudStorageClient().start();
    }

    public void start() throws InterruptedException, IOException {
        NioEventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap client = new Bootstrap();
            client.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) {
                            ch.pipeline().addLast(
                                    new LengthFieldBasedFrameDecoder(1024 * 1024 * 1024,
                                            0,
                                            8,
                                            0,
                                            8),
                                    new LengthFieldPrepender(8),
                                    new ByteArrayEncoder(),
                                    new ByteArrayDecoder(),
                                    new JsonEncoder(),
                                    new JsonDecoder(),
                                    new RequestDecoder(clientDirectory));
                        }
                    });

            ChannelFuture channelFuture = client.connect("localhost", 9000).sync();
            System.out.println("Client started");

            System.out.println("Команды для работы:");
            System.out.println("all del - удаляет все файлы на удаленной стороне");
            System.out.println("all send - отправляет все файлы на удаленную сторону");
            System.out.println("all rec - получаем все файлы с удаленной стороны");
            System.out.println("del имя_файла - удаляет указанный файл/директорию с удаленной стороны");
            System.out.println("dir имя_директории - создает указанную директорию на удаленной стороне");
            System.out.println("file имя_файла - отправляет указанный файл на удаленной стороне");

            Scanner scanner = new Scanner(System.in);
            String input;

            while (true) {

                input = scanner.nextLine();
                if (input.equalsIgnoreCase("q")) {
                    //завершаем работу
                    break;

                } else if (input.equalsIgnoreCase("all del")) {
                    // очищаем папку на сервере
                    RequestMethods.rqDeleteAllFiles(channelFuture);
                } else if (input.equalsIgnoreCase("all send")) {
                    // отсылаем всё дерево директорий и файлов на сервер
                    RequestMethods.rqSendAllFiles(channelFuture, clientDirectory);
                } else if (input.equalsIgnoreCase("all rec")) {
                    // получаем всё дерево директорий и файлов от сервера
                    RequestMethods.rqReceiveAllFiles(channelFuture);
                } else {

                    String[] strings = input.split(" ");
                    if (strings.length > 1) {
                        if (strings[0].equals("del")) {
                            // удаляем файл
                            RequestMethods.rqDeleteFile(channelFuture, strings[1]);
                        } else if (strings[0].equals("dir")) {
                            // создаем каталог
                            RequestMethods.rqCreateDir(channelFuture, strings[1]);
                        } else if (strings[0].equals("file")) {
                            // отправляем файл
                            RequestMethods.rqSendFile(channelFuture, strings[1], clientDirectory);
                        }
                    }
                }
            }
        } finally {
            group.shutdownGracefully();
        }
    }
}
