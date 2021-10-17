package common;

public class Request {
    private String filename;
    private RequestCommands command;
    private long position;
    private byte[] file;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public RequestCommands getCommand() {
        return command;
    }

    public void setCommand(RequestCommands command) {
        this.command = command;
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
