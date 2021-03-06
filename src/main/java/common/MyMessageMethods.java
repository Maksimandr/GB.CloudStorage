package common;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

import static common.MessageCommands.*;

/**
 * Класс содержит методы для работы с запросами
 */
public class MyMessageMethods {

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
            for (File f : Objects.requireNonNull(file.listFiles())) {
                fileList.addAll(getDirTree(f));
            }
        }
        return fileList;
    }

    /**
     * Возвращает часть пути до файла без начальной части
     *
     * @param file     обрабатываемый файл
     * @param localDir убираемая часть
     * @return строка пути до файла без начальной части
     */
    public static String subPath(File file, File localDir) {
        try {
            return file.getCanonicalPath().substring(localDir.getCanonicalPath().length());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Очищает указанную папку
     *
     * @param localDir локальная директория для хранения файлов
     */
    public static boolean clearDir(File localDir) throws IOException {
        List<File> fileList = new ArrayList<>(getDirTree(localDir));
        fileList.remove(0);
        return deleteFiles(fileList);
    }

    /**
     * Удаляет файлы указанные в списке
     *
     * @param fileList список файлов
     */
    public static boolean deleteFiles(List<File> fileList) throws IOException {
        boolean flag = true;
        fileList.sort(Comparator.reverseOrder());
        for (File f : fileList) {
            flag = flag && new File(f.getCanonicalPath()).delete();//
        }
        return flag;
    }

    /**
     * Отсылает серию запросов содержащих все файлы в локальной папке
     *
     * @param o объект содержащий соединение
     */
    public static void rqSendAllFiles(Object o) throws IOException {
        Channel ctx = channelCheck(o);
        Request request = new Request();
        request.setFilename("");
        request.setCommand(SEND_ALL);
        ctx.writeAndFlush(request);
    }

    /**
     * Отправляет запрос на получение всех файлов с удаленной стороны
     *
     * @param o объект содержащий соединение
     */
    public static void rqLoadAllFiles(Object o) {
        Channel ctx = channelCheck(o);
        Request request = new Request();
        request.setFilename("");
        request.setCommand(LOAD_ALL);
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
        request.setCommand(DELETE_ALL);
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
        request.setCommand(DELETE_FILE);
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
        request.setCommand(CREATE_DIR);
        ctx.writeAndFlush(request);
    }

    /**
     * Отсылает запрос на загрузку файла с удаленной стороны
     *
     * @param o        объект содержащий соединение
     * @param fileName имя файла
     */
    public static void rqLoadFile(Object o, String fileName) {
        Channel ctx = channelCheck(o);
        Request request = new Request();
        request.setFilename(fileName);
        request.setCommand(LOAD_FILE);
        ctx.writeAndFlush(request);
    }

    /**
     * Метод для отправки заданного файла в виде массива байт
     *
     * @param o        объект содержащий соединение
     * @param fileName имя файла
     * @param localDir локальная директория для хранения файлов
     */
    public static void rqSendFile(Object o, String fileName, File localDir, long position, MessageCommands command) {
        Channel ctx = channelCheck(o);
        try (RandomAccessFile accessFile = new RandomAccessFile(localDir.getCanonicalPath() + File.separator + fileName, "rw")) {
            // файл отправляется частями
            byte[] buffer = new byte[1024 * 1024];
            Request request = new Request();
            request.setFilename(fileName);
            request.setCommand(command);
            request.setPosition(position);
            accessFile.seek(position);
            int read = accessFile.read(buffer);
            if (read < buffer.length - 1) {
                // если блок данных меньше заданного пересоздаем массив, чтобы не отправлять лишние данные
                if (read > 0) {
                    buffer = Arrays.copyOf(buffer, read);
                    request.setFile(buffer);
                }
                ctx.writeAndFlush(request);
            } else {
                request.setFile(buffer);
                ctx.writeAndFlush(request);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("IO error occurred");
        }
    }
}
