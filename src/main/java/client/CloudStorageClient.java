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
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Класс клиента
 */
public class CloudStorageClient {

    private static final File localDirectory = new File("localDirectory");

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

            // отсылаем всё дерево директорий и файлов на сервер
            sendAllFiles(channelFuture);

            // через 5 сек отправляем запрос на их удаление
            Thread.sleep(5000);
            deleteAllFiles(channelFuture);

        } finally {
            group.shutdownGracefully();
        }
    }

    /**
     * Отсылает запрос на удаление всех файлов на сервере
     * @param channelFuture
     * @throws InterruptedException
     */
    private void deleteAllFiles(ChannelFuture channelFuture) throws InterruptedException {
        Request request = new Request();
        request.setFilename("");
        request.setCommand(RequestCommands.DELETE_ALL);
        channelFuture.channel().writeAndFlush(request).sync();
    }

    /**
     * Отсылает серию запросов содержащих все файлы в локальной папке клиента
     * @param channelFuture
     * @throws InterruptedException
     * @throws IOException
     */
    private void sendAllFiles(ChannelFuture channelFuture) throws InterruptedException, IOException {
        List<File> fileList = new ArrayList<>(RequestDecoder.getDirTree(localDirectory));
        fileList.remove(0);
        for (File f : fileList) {
            if (f.isFile()) {
                createFile(channelFuture, f.getCanonicalPath().substring(localDirectory.getCanonicalPath().length()));
            } else {
                createDir(channelFuture, f.getCanonicalPath().substring(localDirectory.getCanonicalPath().length()));
            }
        }
    }

    /**
     * Отсылает запрос на удаление указанного файла на сервере
     * @param channelFuture
     * @param fileName
     * @throws InterruptedException
     */
    private void deleteFile(ChannelFuture channelFuture, String fileName) throws InterruptedException {
        Request request = new Request();
        request.setFilename(fileName);
        request.setCommand(RequestCommands.DELETE_FILE);
        channelFuture.channel().writeAndFlush(request).sync();
    }

    /**
     * Отсылает запрос на создание директории на сервере
     * @param channelFuture
     * @param fileName
     * @throws InterruptedException
     */
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
