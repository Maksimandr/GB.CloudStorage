package common;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import server.CloudStorageServer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Декодер сервера для приема файла в виде массива байт
 */
public class RequestDecoder extends SimpleChannelInboundHandler<Request> {

    private static File localDir;

    public RequestDecoder(File localDir) {
        this.localDir = localDir;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Request request) throws IOException, InterruptedException {
        RequestCommands command = request.getCommand();

        // команда на создание директории
        if (command.equals(RequestCommands.CREATE_DIR)) {
            new File(localDir + request.getFilename()).mkdirs();

            // команда на сохранение файла
        } else if (command.equals(RequestCommands.CREATE_FILE)) {
            try (RandomAccessFile accessFile = new RandomAccessFile(localDir + request.getFilename(), "rw")) {
                accessFile.seek(request.getPosition());
                accessFile.write(request.getFile());
            } catch (IOException e) {
                System.out.println("Receive IO Error!");
            }

            // команда на удаление указанного файла
        } else if (command.equals(RequestCommands.DELETE_FILE)) {
            new File(localDir + request.getFilename()).delete();

            // команда на удаление всех файлов на сервере
        } else if (command.equals(RequestCommands.DELETE_ALL)) {
            List<File> fileList = new ArrayList<>(getDirTree(localDir));
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
        } else if (command.equals(RequestCommands.RECEIVE_ALL)) {
            deleteAllFiles(ctx);
            Thread.sleep(5000);
            sendAllFiles(ctx);
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
     *
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

    /**
     * Отсылает серию запросов содержащих все файлы в локальной папке клиента
     *
     * @param ctx
     * @throws InterruptedException
     * @throws IOException
     */
    public void sendAllFiles(ChannelHandlerContext ctx) throws InterruptedException, IOException {
        List<File> fileList = new ArrayList<>(RequestDecoder.getDirTree(localDir));
        fileList.remove(0);
        for (File f : fileList) {
            if (f.isFile()) {
                createFile(ctx, f.getCanonicalPath().substring(localDir.getCanonicalPath().length()));
            } else {
                createDir(ctx, f.getCanonicalPath().substring(localDir.getCanonicalPath().length()));
            }
        }
    }

    /**
     * Отсылает запрос на удаление всех файлов на сервере
     *
     * @param ctx
     * @throws InterruptedException
     */
    private void deleteAllFiles(ChannelHandlerContext ctx) throws InterruptedException {
        Request request = new Request();
        request.setFilename("");
        request.setCommand(RequestCommands.DELETE_ALL);
        ctx.channel().writeAndFlush(request).sync();
    }

    /**
     * Отсылает запрос на создание директории на сервере
     *
     * @param ctx
     * @param fileName
     * @throws InterruptedException
     */
    public void createDir(ChannelHandlerContext ctx, String fileName) throws InterruptedException {
        Request request = new Request();
        request.setFilename(fileName);
        request.setCommand(RequestCommands.CREATE_DIR);
        ctx.writeAndFlush(request).sync();
    }

    /**
     * Метод для отправки заданного файла в виде массива байт
     *
     * @param fileName запрос отсылаемый серверу
     */
    public void createFile(ChannelHandlerContext ctx, String fileName) throws InterruptedException {
        try (RandomAccessFile accessFile = new RandomAccessFile(localDir + fileName, "rw")) {
            // файл отправляется частями
            Request request;
            byte[] buffer;
            int read;
            // в цикле читаем из файла пока есть данные
            while (true) {
                buffer = new byte[1024 * 1024];
                request = new Request();
                request.setFilename(fileName);
                request.setCommand(RequestCommands.CREATE_FILE);
                request.setPosition(accessFile.getFilePointer());
                read = accessFile.read(buffer);
                if (read < buffer.length - 1) {
                    // если блок данных меньше заданного пересоздаем массив, чтобы не отправлять лишние данные
                    buffer = Arrays.copyOf(buffer, read);
                    request.setFile(buffer);
                    ctx.writeAndFlush(request);
                    break;
                } else {
                    request.setFile(buffer);
                    ctx.writeAndFlush(request);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("IO error occurred");
        }
    }
}