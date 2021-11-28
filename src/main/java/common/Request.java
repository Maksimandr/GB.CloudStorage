package common;

import java.io.Serializable;

public class Request implements Serializable {
    private String filename;
    private MessageCommands command;
    private MessageCommands status;
    private long position;
    private byte[] file;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public MessageCommands getCommand() {
        return command;
    }

    public void setCommand(MessageCommands command) {
        this.command = command;
    }

    public MessageCommands getStatus() {
        return status;
    }

    public void setStatus(MessageCommands status) {
        this.status = status;
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public byte[] getFile() {
        return file;
    }

    public void setFile(byte[] file) {
        this.file = file;
    }
}
