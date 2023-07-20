package io.agora.metagpt.models;

import androidx.annotation.NonNull;

public class GameInfo {
    private int gameId;
    private String gameName;

    private String gameExtraInfo;

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public String getGameExtraInfo() {
        return gameExtraInfo;
    }

    public void setGameExtraInfo(String gameExtraInfo) {
        this.gameExtraInfo = gameExtraInfo;
    }

    @NonNull
    @Override
    public String toString() {
        return "GameInfo{" +
                "gameId=" + gameId +
                ", gameName='" + gameName + '\'' +
                ", gameExtraInfo='" + gameExtraInfo + '\'' +
                '}';
    }
}
