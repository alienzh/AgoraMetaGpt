package io.agora.gpt.models.wiu;

import androidx.annotation.NonNull;

public class GamePrompt {
    private int seq;
    private String content;

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @NonNull
    @Override
    public String toString() {
        return "GamePrompt{" +
                "seq=" + seq +
                ", content='" + content + '\'' +
                '}';
    }
}
