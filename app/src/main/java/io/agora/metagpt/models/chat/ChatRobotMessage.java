package io.agora.metagpt.models.chat;

import androidx.annotation.NonNull;

public class ChatRobotMessage {
    private String role;
    private String content;

    public ChatRobotMessage() {

    }

    public ChatRobotMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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
        return "ChatRobotMessage{" +
                "role='" + role + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
