package io.agora.gpt.models;

import androidx.annotation.NonNull;

public class DataStreamModel {
    private String cmd;
    private String message;

    private boolean isEnd;

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


    public boolean getIsEnd() {
        return isEnd;
    }

    public void setIsEnd(boolean isEnd) {
        this.isEnd = isEnd;
    }

    @NonNull
    @Override
    public String toString() {
        return "DataStreamModel{" +
                "cmd='" + cmd + '\'' +
                ", message='" + message + '\'' +
                ", isEnd=" + isEnd +
                '}';
    }
}
