package io.agora.metagpt.models;

import androidx.annotation.NonNull;

public class HistoryModel {
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


    @Override
    @NonNull
    public String toString() {
        return "HistoryModel{" +
                ", message='" + message + '\'' +
                '}';
    }
}
