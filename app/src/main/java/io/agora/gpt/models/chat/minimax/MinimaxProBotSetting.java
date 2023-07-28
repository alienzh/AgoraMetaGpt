package io.agora.gpt.models.chat.minimax;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class MinimaxProBotSetting implements Serializable {
    private String bot_name;
    private String content;

    public String getBot_name() {
        return bot_name;
    }

    public void setBot_name(String bot_name) {
        this.bot_name = bot_name;
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
        return "MinimaxProBotSetting{" +
                "bot_name='" + bot_name + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
