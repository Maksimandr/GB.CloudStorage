package client;

import common.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import static common.RequestMethods.*;

/**
 * Класс клиента
 */
public class CloudStorageClient {

    private static final File clientDirectory = new File("clientDirectory");

    public static void main(String[] args) {
        new CloudStorageClient().start();
    }

    public void start() {
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
                                    new LengthFieldBasedFrameDecoder(
                                            1024 * 1024 * 1024,
                                            0,
                                            8,
                                            0,
                                            8),
                                    new LengthFieldPrepender(8),
                                    new ByteArrayDecoder(),
                                    new ByteArrayEncoder(),
                                    new JsonDecoder(),
                                    new JsonEncoder(),
                                    new RequestDecoder(clientDirectory));
                        }
                    });

            ChannelFuture channelFuture = client.connect("localhost", 9000).sync();
            System.out.println("Client started");

            FileAlterationMonitor monitor = new FileAlterationMonitor(1000);
            FileAlterationObserver observer = new FileAlterationObserver(clientDirectory);
            FileAlterationListener listener = new FileAlterationListener() {
                @Override
                public void onStart(FileAlterationObserver observer) {

                }

                @Override
                public void onDirectoryCreate(File directory) {
                    try {
                        rqCreateDir(channelFuture, subPath(directory, clientDirectory));
                        System.out.println("Created Directory: " + directory.getName() + " path:" + directory.getCanonicalPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDirectoryChange(File directory) {
                    try {
                        System.out.println("Changes in Directory: " + directory.getName() + " path:" + directory.getCanonicalPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDirectoryDelete(File directory) {
                    try {
                        rqDeleteFile(channelFuture, subPath(directory, clientDirectory));
                        System.out.println("Delete Directory: " + directory.getName() + " path:" + directory.getCanonicalPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFileCreate(File file) {
                    try {
                        rqSendFile(channelFuture, subPath(file, clientDirectory), clientDirectory);
                        System.out.println("Created File: " + file.getName() + " path:" + file.getCanonicalPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFileChange(File file) {
                    try {
                        rqSendFile(channelFuture, subPath(file, clientDirectory), clientDirectory);
                        System.out.println("Change File: " + file.getName() + " path:" + file.getCanonicalPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFileDelete(File file) {
                    try {
                        rqDeleteFile(channelFuture, subPath(file, clientDirectory));
                        System.out.println("Delete File: " + file.getName() + " path:" + file.getCanonicalPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onStop(FileAlterationObserver observer) {

                }
            };

            observer.addListener(listener);
            monitor.addObserver(observer);


            Auth auth = new Auth("l1", "p1");
            channelFuture.channel().writeAndFlush(auth);

            System.out.println("Команды для работы:");
            System.out.println("all del - удаляет все файлы на удаленной стороне");
            System.out.println("all send - отправляет все файлы на удаленную сторону");
            System.out.println("all rec - получаем все файлы с удаленной стороны");
            System.out.println("del имя_файла - удаляет указанный файл/директорию с удаленной стороны");
            System.out.println("dir имя_директории - создает указанную директорию на удаленной стороне");
            System.out.println("file имя_файла - отправляет указанный файл на удаленной стороне");
            System.out.println("auto on - запуск автоматического режима отслеживания изменений в директории");
            System.out.println("auto off - остановка автоматического режима отслеживания изменений в директории");

            String input;

            while (true) {

                input = scanner.nextLine();
                if (input.equalsIgnoreCase("q")) {
                    //завершаем работу
                    break;

                } else if (input.equalsIgnoreCase("all del")) {
                    // очищаем папку на сервере
                    rqDeleteAllFiles(channelFuture);
                } else if (input.equalsIgnoreCase("all send")) {
                    // отсылаем всё дерево директорий и файлов на сервер
                    rqSendAllFiles(channelFuture, clientDirectory);
                } else if (input.equalsIgnoreCase("all rec")) {
                    // получаем всё дерево директорий и файлов от сервера
                    rqReceiveAllFiles(channelFuture);
                } else if (input.equalsIgnoreCase("auto on")) {
                    // запуск автоматического режима отслеживания изменений
                    try {
                        monitor.start();
                        System.out.println("Запущен автоматический режим отслеживания изменений в директории");
                    } catch (Exception e) {
                        System.out.println("Мониторинг изменений в директории уже запущен!");
                    }
                } else if (input.equalsIgnoreCase("auto off")) {
                    // остановка автоматического режима отслеживания изменений
                    try {
                        monitor.stop();
                        System.out.println("Остановлен автоматический режим отслеживания изменений в директории");
                    } catch (Exception e) {
                        System.out.println("Мониторинг изменений в директории уже остановлен!");
                    }
                } else {

                    String[] strings = input.split(" ");
                    if (strings.length > 1) {
                        if (strings[0].equals("del")) {
                            // удаляем файл
                            rqDeleteFile(channelFuture, strings[1]);
                        } else if (strings[0].equals("dir")) {
                            // создаем каталог
                            rqCreateDir(channelFuture, strings[1]);
                        } else if (strings[0].equals("file")) {
                            // отправляем файл
                            rqSendFile(channelFuture, strings[1], clientDirectory);
                        }
                    }
                }
            }
            try {
                monitor.stop();
            } catch (Exception ignored) {
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
            scanner.close();

        }
    }
}
