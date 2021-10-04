package client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.codec.string.StringDecoder;

import javax.swing.*;
import java.io.*;
import java.util.Arrays;

/**
 * Класс клиента
 */
public class CloudStorageClient {

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
                                    new ByteArrayEncoder(),

                                    new StringDecoder(),
                                    new ClientDecoder());
                        }
                    });

            ChannelFuture future = client.connect("localhost", 9000).sync();

            System.out.println("Client started");

            sendFile(future);

        } finally {
            group.shutdownGracefully();
        }
    }

    /**
     * Метод для отправки заданного файла в виде массива байт
     *
     * @param future установленное соединение с сервером
     */
    private void sendFile(ChannelFuture future) throws InterruptedException {

        // файл для отправки
        File historyFile = new File("history_l1.txt");

        if (historyFile.exists()) {
            try {
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(historyFile));
                // файл отправляется частями не более 512 байт
                byte[] byteArray = new byte[512];
                int in, i = 0;
                // в цикле читаем из файла пока есть данные
                while ((in = bis.read(byteArray)) != -1) {
                    // если блок данных меньше 512 байт пересоздаем массив, чтобы не отправлять лишние данные
                    if (in != byteArray.length) {
                        byteArray = Arrays.copyOf(byteArray, in);
                    }
                    System.out.println("Запись в файл №" + (i++) + " " + in);
                    // отправляем блок данных на сервер
                    future.channel().writeAndFlush(byteArray).sync();
                }
                bis.close();
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Send error occurred");
            }
        } else {
            JOptionPane.showMessageDialog(null, "History file not exist!");
        }
    }
}
