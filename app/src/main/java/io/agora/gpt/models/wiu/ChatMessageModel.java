package io.agora.gpt.models.wiu;

import androidx.annotation.NonNull;

public class ChatMessageModel {
    private int uid;
    private String username;
    private String message;

    private boolean isAiMessage;

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isAiMessage() {
        return isAiMessage;
    }

    public void setAiMessage(boolean aiMessage) {
        isAiMessage = aiMessage;
    }

    @NonNull
    @Override
    public String toString() {
        return "ChatMessageModel{" +
                "uid=" + uid +
                ", username='" + username + '\'' +
                ", message='" + message + '\'' +
                ", isAiMessage=" + isAiMessage +
                '}';
    }
}
