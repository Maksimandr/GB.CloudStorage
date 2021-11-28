package client;

import common.MyMessageDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import static common.MessageCommands.SEND_FILE;
import static common.MyMessageMethods.*;

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
                                    new ObjectEncoder(),
                                    new ObjectDecoder(1024 * 1024 + 1024, ClassResolvers.cacheDisabled(null)), // размер указан исходя из того, что размер данных в request = 1024 * 1024
                                    new MyMessageDecoder(clientDirectory));
                        }
                    });

            ChannelFuture channelFuture = client.connect("localhost", 9000).sync();
            System.out.println("Client started");

            FileAlterationMonitor monitor = new FileAlterationMonitor(1000);
            FileAlterationObserver observer = new FileAlterationObserver(clientDirectory);
            FileAlterationListener listener = createFAL(channelFuture);

            observer.addListener(listener);
            monitor.addObserver(observer);

            System.out.println("Команды для работы:");
            System.out.println("all del - удаляет все файлы на удаленной стороне");
//            System.out.println("all send - отправляет все файлы на удаленную сторону");
//            System.out.println("all load - получаем все файлы с удаленной стороны");
            System.out.println("del имя_файла - удаляет указанный файл/директорию на удаленной стороне");
            System.out.println("dir имя_директории - создает указанную директорию на удаленной стороне");
            System.out.println("send имя_файла - отправляет указанный файл на удаленную сторону");
            System.out.println("load имя_файла - загружает указанный файл с удаленной стороны");
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
                    rqSendAllFiles(channelFuture); // пока не сделано
                } else if (input.equalsIgnoreCase("all load")) {
                    // получаем всё дерево директорий и файлов от сервера
                    rqLoadAllFiles(channelFuture); // пока не сделано
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
                    String fileName = input.substring(strings[0].length() + 1);
                    if (strings.length > 1) {
                        if (strings[0].equals("del")) {
                            // удаляем файл
                            rqDeleteFile(channelFuture, fileName);
                        } else if (strings[0].equals("dir")) {
                            // создаем каталог
                            rqCreateDir(channelFuture, fileName);
                        } else if (strings[0].equals("send")) {
                            // отправляем файл
                            rqSendFile(channelFuture, fileName, clientDirectory, 0, SEND_FILE);
                        } else if (strings[0].equals("load")) {
                            // отправляем файл
                            rqLoadFile(channelFuture, fileName);
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

    private FileAlterationListener createFAL(ChannelFuture channelFuture) {
        return new FileAlterationListener() {
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
                    rqSendFile(channelFuture, subPath(file, clientDirectory), clientDirectory, 0, SEND_FILE);
                    System.out.println("Created File: " + file.getName() + " path:" + file.getCanonicalPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFileChange(File file) {
                try {
                    rqSendFile(channelFuture, subPath(file, clientDirectory), clientDirectory, 0, SEND_FILE);
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
    }
}
