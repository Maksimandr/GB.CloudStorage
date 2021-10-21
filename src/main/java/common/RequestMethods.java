package common;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

/**
 * Класс содержит методы для работы с запросами
 */
public class RequestMethods {

    /**
     * Извлекает канал соединения из объекта
     *
     * @param o объект содержащий соединение
     * @return канал соединения
     */
    private static Channel channelCheck(Object o) {
        if (o instanceof ChannelHandlerContext) {
            return ((ChannelHandlerContext) o).channel();
        } else if (o instanceof ChannelFuture) {
            return ((ChannelFuture) o).channel();
        } else if (o instanceof Channel) {
            return (Channel) o;
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Возвращает список всех файлов и директорий начиная с указанной директории
     *
     * @param file начальная папка
     * @return список всех объектов во всей структуре от начальной папки
     */
    public static List<File> getDirTree(File file) {
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
     * Очищает указанную папку
     *
     * @param localDir локальная директория для хранения файлов
     */
    public static void clearDir(File localDir) throws IOException {
        List<File> fileList = new ArrayList<>(getDirTree(localDir));
        fileList.remove(0);
        deleteFiles(fileList);
    }

    /**
     * Удаляет файлы указанные в списке
     *
     * @param fileList список файлов
     */
    public static void deleteFiles(List<File> fileList) throws IOException {
        fileList.sort(Comparator.reverseOrder());
        for (File f : fileList) {
            new File(f.getCanonicalPath()).delete();//
        }
    }

    /**
     * Отсылает серию запросов содержащих все файлы в локальной папке
     *
     * @param o        объект содержащий соединение
     * @param localDir локальная директория для хранения файлов
     */
    public static void rqSendAllFiles(Object o, File localDir) throws IOException {
        Channel ctx = channelCheck(o);
        List<File> fileList = new ArrayList<>(getDirTree(localDir));
        fileList.remove(0);
        for (File f : fileList) {
            if (f.isFile()) {
                rqSendFile(ctx, f.getCanonicalPath().substring(localDir.getCanonicalPath().length()), localDir);
            } else {
                rqCreateDir(ctx, f.getCanonicalPath().substring(localDir.getCanonicalPath().length()));
            }
        }
    }

    /**
     * Отправляет запрос на получение всех файлов с удаленной стороны
     *
     * @param o объект содержащий соединение
     */
    public static void rqReceiveAllFiles(Object o) {
        Channel ctx = channelCheck(o);
        Request request = new Request();
        request.setFilename("");
        request.setCommand(RequestCommands.RECEIVE_ALL);
        ctx.writeAndFlush(request);
    }

    /**
     * Отсылает запрос на удаление всех файлов на удаленной стороне
     *
     * @param o объект содержащий соединение
     */
    public static void rqDeleteAllFiles(Object o) {
        Channel ctx = channelCheck(o);
        Request request = new Request();
        request.setFilename("");
        request.setCommand(RequestCommands.DELETE_ALL);
        ctx.writeAndFlush(request);
    }


    /**
     * Отсылает запрос на удаление указанного файла на удаленной стороне
     *
     * @param o        объект содержащий соединение
     * @param fileName имя файла
     */
    public static void rqDeleteFile(Object o, String fileName) {
        Channel ctx = channelCheck(o);
        Request request = new Request();
        request.setFilename(fileName);
        request.setCommand(RequestCommands.DELETE_FILE);
        ctx.writeAndFlush(request);
    }

    /**
     * Отсылает запрос на создание директории на удаленной стороне
     *
     * @param o        объект содержащий соединение
     * @param fileName имя файла
     */
    public static void rqCreateDir(Object o, String fileName) {
        Channel ctx = channelCheck(o);
        Request request = new Request();
        request.setFilename(fileName);
        request.setCommand(RequestCommands.CREATE_DIR);
        ctx.writeAndFlush(request);
    }

    /**
     * Метод для отправки заданного файла в виде массива байт
     *
     * @param o        объект содержащий соединение
     * @param fileName имя файла
     * @param localDir локальная директория для хранения файлов
     */
    public static void rqSendFile(Object o, String fileName, File localDir) {
        Channel ctx = channelCheck(o);
        try (RandomAccessFile accessFile = new RandomAccessFile(localDir.getCanonicalPath() + File.separator + fileName, "rw")) {
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
