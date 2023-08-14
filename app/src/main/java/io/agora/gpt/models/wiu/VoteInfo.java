package io.agora.gpt.models.wiu;

import androidx.annotation.NonNull;

public class VoteInfo {
    private int gamerNumber;
    private int undercoverNumber;

    private boolean isAiVote;

    public VoteInfo() {

    }

    public VoteInfo(int gamerNumber, int undercoverNumber, boolean isAiVote) {
        this.gamerNumber = gamerNumber;
        this.undercoverNumber = undercoverNumber;
        this.isAiVote = isAiVote;
    }

    public int getGamerNumber() {
        return gamerNumber;
    }

    public void setGamerNumber(int gamerNumber) {
        this.gamerNumber = gamerNumber;
    }


    public int getUndercoverNumber() {
        return undercoverNumber;
    }

    public void setUndercoverNumber(int undercoverNumber) {
        this.undercoverNumber = undercoverNumber;
    }

    public boolean isAiVote() {
        return isAiVote;
    }

    public void setAiVote(boolean aiVote) {
        isAiVote = aiVote;
    }

    @NonNull
    @Override
    public String toString() {
        return "VoteInfo{" +
                "gamerNumber=" + gamerNumber +
                ", undercoverNumber=" + undercoverNumber +
                ", isAiVote=" + isAiVote +
                '}';
    }
}
