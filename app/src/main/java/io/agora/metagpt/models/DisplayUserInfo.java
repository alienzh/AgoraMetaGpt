package io.agora.metagpt.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DisplayUserInfo {
    private int uid;
    private String username;

    private boolean isAi;

    private int number;

    private boolean isSpeaking;

    private boolean isOut;

    public DisplayUserInfo() {

    }

    public DisplayUserInfo(int uid, String username) {
        this.uid = uid;
        this.username = username;
    }

    public DisplayUserInfo(int uid, String username, boolean isAi) {
        this.uid = uid;
        this.username = username;
        this.isAi = isAi;
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

    public boolean isAi() {
        return isAi;
    }

    public void setAi(boolean ai) {
        isAi = ai;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public boolean isSpeaking() {
        return isSpeaking;
    }

    public void setSpeaking(boolean speaking) {
        isSpeaking = speaking;
    }

    public boolean isOut() {
        return isOut;
    }

    public void setOut(boolean out) {
        isOut = out;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof  DisplayUserInfo){
            return ((DisplayUserInfo) obj).number==this.number && ((DisplayUserInfo) obj).uid==this.uid;
        }
        return super.equals(obj);
    }

    public void restore(){
        isSpeaking = false;
        isOut = false;
    }

    @NonNull
    @Override
    public String toString() {
        return "UserInfo{" +
                "uid=" + uid +
                ", username='" + username + '\'' +
                ", isAi='" + isAi + '\'' +
                '}';
    }
}
