package common;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

import static common.MessageCommands.*;
import static common.MyMessageMethods.clearDir;
import static common.MyMessageMethods.rqSendFile;

/**
 * Декодер сервера для приема файла в виде массива байт
 */
public class MyMessageDecoder extends SimpleChannelInboundHandler<Object> {

    private File localDir;

    public MyMessageDecoder(File localDir) {
        this.localDir = localDir;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object o) throws IOException {
        List<File> fileList = null;

        if (o instanceof Request) {
            Request request = (Request) o;
            MessageCommands command = request.getCommand();
            File file = new File(localDir.getCanonicalPath() + File.separator + request.getFilename());

            Response response = new Response();
            response.setFilename(request.getFilename());

            switch (command) {
                case CREATE_DIR: // команда на создание директории
                    if (file.mkdirs()) {
                        response.setCommand(CREATE_DIR);
                        response.setStatus(DIR_OK);
                        System.out.println(request.getFilename() + " " + DIR_OK);
                        ctx.writeAndFlush(response);
                    }
                    break;
                case SEND_FILE: // команда на отправку файла
                    try (RandomAccessFile accessFile = new RandomAccessFile(file, "rw")) {
                        byte[] bytes = request.getFile();
                        if (bytes != null) {
                            accessFile.seek(request.getPosition());
                            accessFile.write(bytes);
                            // отправляем ответ о приеме блока с данными и позицией в файле на которой остановились
                            response.setCommand(command);
                            response.setPosition(request.getPosition() + bytes.length);
                        } else {
                            response.setCommand(SEND_FILE);
                            response.setStatus(SEND_OK);
                            System.out.println(request.getFilename() + " " + LOAD_OK);
                        }
                        ctx.writeAndFlush(response);
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println("IO error occurred");
                    }
                    break;
                case LOAD_FILE: // команда на загрузку файла
                    rqSendFile(ctx, request.getFilename(), localDir, request.getPosition(), SEND_FILE);
                    break;
                case DELETE_FILE: // команда на удаление указанного файла на удаленной стороне
                    if (file.delete()) {
                        response.setCommand(DELETE_FILE);
                        response.setStatus(DEL_OK);
                        ctx.writeAndFlush(response);
                        System.out.println(request.getFilename() + " " + DEL_OK);
                    }
                    break;
                case DELETE_ALL: // команда на удаление всех файлов на удаленной стороне
                    if (clearDir(file)) {
                        response.setCommand(DELETE_ALL);
                        response.setStatus(DEL_ALL_OK);
                        ctx.writeAndFlush(response);
                        System.out.println("Очистка директории " + file.getName() + " " + DEL_ALL_OK);
                    }
                    break;
            }

        } else if (o instanceof Response) {
            Response response = (Response) o;
            MessageCommands command = response.getCommand();
            MessageCommands status = response.getStatus();
            File file = new File(localDir.getCanonicalPath() + File.separator + response.getFilename());

            switch (command) {
                case SEND_FILE:
                    if (status != null) {
                        if (status.equals(SEND_OK)) {
                            System.out.println(file.getName() + " " + status);
                        }
                    } else {
                        rqSendFile(ctx, response.getFilename(), localDir, response.getPosition(), SEND_FILE);
                    }
                    break;
                default:
                    System.out.println(file.getName() + " " + status);
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