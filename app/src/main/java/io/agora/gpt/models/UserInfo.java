package io.agora.gpt.models;

import androidx.annotation.NonNull;

public class UserInfo {
    private int uid;
    private String username;

    public UserInfo() {

    }

    public UserInfo(int uid, String username) {
        this.uid = uid;
        this.username = username;
    }

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

    @NonNull
    @Override
    public String toString() {
        return "UserInfo{" +
                "uid=" + uid +
                ", username='" + username + '\'' +
                '}';
    }
}
