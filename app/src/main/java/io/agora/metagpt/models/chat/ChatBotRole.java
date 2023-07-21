package io.agora.metagpt.models.chat;

import androidx.annotation.NonNull;

public class ChatBotRole {
    private String chatBotRole;
    private String chatBotName;
    private String chatBotUserName;
    private String chatBotPrompt;
    private String introduceReplace;
    private String welcomeMessage;

    public String getChatBotRole() {
        return chatBotRole;
    }

    public void setChatBotRole(String chatBotRole) {
        this.chatBotRole = chatBotRole;
    }

    public String getChatBotName() {
        return chatBotName;
    }

    public void setChatBotName(String chatBotName) {
        this.chatBotName = chatBotName;
    }

    public String getChatBotUserName() {
        return chatBotUserName;
    }

    public void setChatBotUserName(String chatBotUserName) {
        this.chatBotUserName = chatBotUserName;
    }

    public String getChatBotPrompt() {
        return chatBotPrompt;
    }

    public void setChatBotPrompt(String chatBotPrompt) {
        this.chatBotPrompt = chatBotPrompt;
    }

    public String getIntroduceReplace() {
        return introduceReplace;
    }

    public void setIntroduceReplace(String introduceReplace) {
        this.introduceReplace = introduceReplace;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    @NonNull
    @Override
    public String toString() {
        return "ChatBotRole{" +
                "chatBotRole='" + chatBotRole + '\'' +
                ", chatBotName='" + chatBotName + '\'' +
                ", chatBotUserName='" + chatBotUserName + '\'' +
                ", chatBotPrompt='" + chatBotPrompt + '\'' +
                ", introduceReplace='" + introduceReplace + '\'' +
                ", welcomeMessage='" + welcomeMessage + '\'' +
                '}';
    }
}
