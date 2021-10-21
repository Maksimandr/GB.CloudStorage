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
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.util.Scanner;

/**
 * Класс клиента
 */
public class CloudStorageClient {

    private static final File clientDirectory = new File("clientDirectory");

    public static void main(String[] args) throws Exception {
        new CloudStorageClient().start();
    }

    public void start() throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup();
        Scanner scanner = new Scanner(System.in);

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

            FileAlterationObserver observer = new FileAlterationObserver("/Users/a17081740/watch");
            FileAlterationMonitor monitor = new FileAlterationMonitor(1000);
            FileAlterationListener listener = new FileAlterationListener() {
                @Override
                public void onStart(FileAlterationObserver observer) {

                }

                @Override
                public void onDirectoryCreate(File directory) {

                }

                @Override
                public void onDirectoryChange(File directory) {

                }

                @Override
                public void onDirectoryDelete(File directory) {

                }

                @Override
                public void onFileCreate(File file) {
                    System.out.println("Created Filename: " + file.getName() + " path:" + file.getAbsolutePath());
                }

                @Override
                public void onFileChange(File file) {
                    System.out.println("Change Filename: " + file.getName() + " path:" + file.getAbsolutePath());
                }

                @Override
                public void onFileDelete(File file) {
                    System.out.println("Delete Filename: " + file.getName() + " path:" + file.getAbsolutePath());
                }

                @Override
                public void onStop(FileAlterationObserver observer) {

                }
            };

            observer.addListener(listener);
            monitor.addObserver(observer);
            monitor.start();

            System.out.println("Команды для работы:");
            System.out.println("all del - удаляет все файлы на удаленной стороне");
            System.out.println("all send - отправляет все файлы на удаленную сторону");
            System.out.println("all rec - получаем все файлы с удаленной стороны");
            System.out.println("del имя_файла - удаляет указанный файл/директорию с удаленной стороны");
            System.out.println("dir имя_директории - создает указанную директорию на удаленной стороне");
            System.out.println("file имя_файла - отправляет указанный файл на удаленной стороне");


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
            scanner.close();
        }
    }
}
