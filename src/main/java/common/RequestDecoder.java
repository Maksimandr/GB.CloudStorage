package common;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Декодер сервера для приема файла в виде массива байт
 */
public class RequestDecoder extends SimpleChannelInboundHandler<Object> {

    private File localDir;

    public RequestDecoder(File localDir) {
        this.localDir = localDir;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object o) throws IOException {
        Request request = (Request) o;
        RequestCommands command = request.getCommand();
        File file = new File(localDir.getCanonicalPath() + File.separator + request.getFilename());

        if (command.equals(RequestCommands.CREATE_DIR)) {// команда на создание директории
            file.mkdirs();

        } else if (command.equals(RequestCommands.CREATE_FILE)) {// команда на сохранение файла
            try (RandomAccessFile accessFile = new RandomAccessFile(file, "rw")) {
                accessFile.seek(request.getPosition());
                accessFile.write(request.getFile());
            }

        } else if (command.equals(RequestCommands.DELETE_FILE)) {// команда на удаление указанного файла
            new File(localDir.getCanonicalPath() + File.separator + request.getFilename()).delete();

        } else if (command.equals(RequestCommands.DELETE_ALL)) {// команда на удаление всех файлов на сервере
            RequestMethods.clearDir(new File(localDir.getCanonicalPath() + File.separator + request.getFilename()));

        } else if (command.equals(RequestCommands.RECEIVE_ALL)) { // команда на получение всех файлов с сервера
            RequestMethods.rqSendAllFiles(ctx, localDir);
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