package io.agora.gpt.models.chat.minimax;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class MinimaxReplyConstraints implements Serializable {
    private String sender_type;
    private String sender_name;
    private MinimaxGlyph glyph;

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

    public MinimaxGlyph getGlyph() {
        return glyph;
    }

    public void setGlyph(MinimaxGlyph glyph) {
        this.glyph = glyph;
    }

    @NonNull
    @Override
    public String toString() {
        return "MinimaxReplyConstraints{" +
                "sender_type='" + sender_type + '\'' +
                ", sender_name='" + sender_name + '\'' +
                ", glyph=" + glyph +
                '}';
    }
}