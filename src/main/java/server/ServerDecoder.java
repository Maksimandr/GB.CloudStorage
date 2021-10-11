package server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Декодер сервера для приема файла в виде массива байт
 */
public class ServerDecoder extends SimpleChannelInboundHandler<byte[]> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, byte[] bytes) {
        try {
            // создаем папку для приёма
            File f = new File("Recieved");
            if (!f.exists()) {
                f.mkdir();
            }
            // создаем файл
            f = new File("./Recieved/history.txt");
            if (!f.exists()) {
                f.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(f);
            // записываем в файл порцию данных
            fos.write(bytes, 0, bytes.length);

            fos.flush();
            fos.close();

        } catch (IOException e) {
            System.err.println("Receive IO Error!");
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("New channel is active");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("Client disconnected");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
    }
}
