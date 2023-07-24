package io.agora.metagpt.models.chat.minimax;

import androidx.annotation.NonNull;

public class MinimaxRoleMeta {
    private String user_name;
    private String bot_name;

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getBot_name() {
        return bot_name;
    }

    public void setBot_name(String bot_name) {
        this.bot_name = bot_name;
    }

    @NonNull
    @Override
    public String toString() {
        return "RoleMeta{" +
                "user_name='" + user_name + '\'' +
                ", bot_name='" + bot_name + '\'' +
                '}';
    }
}
