package io.agora.metagpt.ai.minimax;

import androidx.annotation.NonNull;

public class Message {
    private String sender_type;
    private String text;

    public String getSender_type() {
        return sender_type;
    }

    public void setSender_type(String sender_type) {
        this.sender_type = sender_type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @NonNull
    @Override
    public String toString() {
        return "Messages{" +
                "sender_type='" + sender_type + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
