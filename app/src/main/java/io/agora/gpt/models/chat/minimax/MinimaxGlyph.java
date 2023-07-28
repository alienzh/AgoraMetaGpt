package io.agora.gpt.models.chat.minimax;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class MinimaxGlyph implements Serializable {
    private String type;
    private String raw_glyph;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRaw_glyph() {
        return raw_glyph;
    }

    public void setRaw_glyph(String raw_glyph) {
        this.raw_glyph = raw_glyph;
    }

    @NonNull
    @Override
    public String toString() {
        return "MinimaxGlyph{" +
                "type='" + type + '\'' +
                ", raw_glyph='" + raw_glyph + '\'' +
                '}';
    }
}
