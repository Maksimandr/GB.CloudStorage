package common;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import server.CloudStorageServer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Декодер сервера для приема файла в виде массива байт
 */
public class RequestDecoder extends SimpleChannelInboundHandler<Request> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Request request) throws IOException {
        RequestCommands command = request.getCommand();

        // команда на создание директории
        if (command.equals(RequestCommands.CREATE_DIR)) {
            new File(CloudStorageServer.cloudDirectory + request.getFilename()).mkdirs();

            // команда на сохранение файла
        } else if (command.equals(RequestCommands.CREATE_FILE)) {
            try (RandomAccessFile accessFile = new RandomAccessFile(CloudStorageServer.cloudDirectory + request.getFilename(), "rw")) {
                accessFile.seek(request.getPosition());
                accessFile.write(request.getFile());
            } catch (IOException e) {
                System.out.println("Receive IO Error!");
            }

            // команда на удаление указанного файла
        } else if (command.equals(RequestCommands.DELETE_FILE)) {
            new File(CloudStorageServer.cloudDirectory + request.getFilename()).delete();

            // команда на удаление всех файлов на сервере
        } else if (command.equals(RequestCommands.DELETE_ALL)) {
            List<File> fileList = new ArrayList<>(getDirTree(CloudStorageServer.cloudDirectory));
            List<String> dirList = new ArrayList<>();
            fileList.remove(0);
            for (File f : fileList) {
                if (f.isFile()) {
                    new File(f.getCanonicalPath()).delete();
                } else {
                    dirList.add(f.getCanonicalPath());
                }
            }
            Collections.sort(dirList, Collections.reverseOrder());

            for (String f : dirList) {
                new File(f).delete();
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

    /**
     * Возвращает список всех файлов и директорий начиная с указанной директории
     * @param file
     * @return
     * @throws IOException
     */
    public static List<File> getDirTree(File file) throws IOException {
        List<File> fileList = new ArrayList<>();
        fileList.add(file);
        if (!file.isFile()) {
            for (File f : file.listFiles()) {
                fileList.addAll(getDirTree(f));
            }
        }
        return fileList;
    }
}