package io.agora.gpt.models.wiu;

import androidx.annotation.NonNull;

public class UserSpeakInfoModel {
    private int uid;
    private String username;
    private int gamerNumber;
    private String message;

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

    public int getGamerNumber() {
        return gamerNumber;
    }

    public void setGamerNumber(int gamerNumber) {
        this.gamerNumber = gamerNumber;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @NonNull
    @Override
    public String toString() {
        return "UserSpeakInfoModel{" +
                "uid=" + uid +
                ", username='" + username + '\'' +
                ", gamerNumber='" + gamerNumber + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
