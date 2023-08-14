package io.agora.gpt.models.wiu;

import androidx.annotation.NonNull;

public class GamerInfo {
    private int uid;
    private String userName;
    private int gamerNumber;
    private String gameName;
    private String word;

    private boolean isOut;

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }


    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getGamerNumber() {
        return gamerNumber;
    }

    public void setGamerNumber(int gamerNumber) {
        this.gamerNumber = gamerNumber;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public boolean isOut() {
        return isOut;
    }

    public void setOut(boolean out) {
        isOut = out;
    }

    @NonNull
    @Override
    public String toString() {
        return "GamerInfo{" +
                "uid=" + uid +
                ", userName='" + userName + '\'' +
                ", gamerNumber=" + gamerNumber +
                ", gameName='" + gameName + '\'' +
                ", word='" + word + '\'' +
                ", isOut=" + isOut +
                '}';
    }
}
