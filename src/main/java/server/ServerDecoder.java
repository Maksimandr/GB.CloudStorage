package server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Декодер сервера для приема файла в виде массива байт
 */
public class ServerDecoder extends SimpleChannelInboundHandler<Request> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Request request) {
        RequestCommands command = request.getCommand();
        if (command.equals(RequestCommands.CREATE_DIR)) {
            File file = new File(CloudStorageServer.cloudDirectory + request.getFilename());
            System.out.println(file.mkdirs());
        } else if (command.equals(RequestCommands.CREATE_FILE)) {
            try (RandomAccessFile accessFile = new RandomAccessFile(CloudStorageServer.cloudDirectory + request.getFilename(), "rw")) {
                accessFile.seek(request.getPosition());
                accessFile.write(request.getFile());
            } catch (IOException e) {
                System.out.println("Receive IO Error!");
            }
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