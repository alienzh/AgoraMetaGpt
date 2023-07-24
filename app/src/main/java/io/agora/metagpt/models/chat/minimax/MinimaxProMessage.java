package io.agora.metagpt.models.chat.minimax;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class MinimaxProMessage implements Serializable {
    private String sender_type;
    private String sender_name;
    private String text;

    public String getSender_type() {
        return sender_type;
    }

    public void setSender_type(String sender_type) {
        this.sender_type = sender_type;
    }

    public String getSender_name() {
        return sender_name;
    }

    public void setSender_name(String sender_name) {
        this.sender_name = sender_name;
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
        return "MinimaxProMessage{" +
                "sender_type='" + sender_type + '\'' +
                ", sender_name='" + sender_name + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
