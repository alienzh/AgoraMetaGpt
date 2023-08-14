package io.agora.gpt.models.chat;

import java.util.Arrays;

public class ChatBotRole {
    private int chatBotId;
    private boolean isEnable;
    private String chatBotRoleName;
    private String chatBotRole;
    private String chatBotName;
    private String chatBotUserName;
    private String chatBotPrompt;
    private String selfIntroduce;
    private String introduce;
    private String welcomeMessage;
    private VoiceName[] voiceNames;

    public int getChatBotId() {
        return chatBotId;
    }

    public void setChatBotId(int chatBotId) {
        this.chatBotId = chatBotId;
    }

    public boolean isEnable() {
        return isEnable;
    }

    public void setEnable(boolean enable) {
        isEnable = enable;
    }

    public String getChatBotRoleName() {
        return chatBotRoleName;
    }

    public void setChatBotRoleName(String chatBotRoleName) {
        this.chatBotRoleName = chatBotRoleName;
    }

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

    public String getSelfIntroduce() {
        return selfIntroduce;
    }

    public void setSelfIntroduce(String selfIntroduce) {
        this.selfIntroduce = selfIntroduce;
    }

    public String getIntroduce() {
        return introduce;
    }

    public void setIntroduce(String introduce) {
        this.introduce = introduce;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    public VoiceName[] getVoiceNames() {
        return voiceNames;
    }

    public void setVoiceNames(VoiceName[] voiceNames) {
        this.voiceNames = voiceNames;
    }

    @Override
    public String toString() {
        return "ChatBotRole{" +
                "chatBotId=" + chatBotId +
                ", isEnable=" + isEnable +
                ", chatBotRole='" + chatBotRole + '\'' +
                ", chatBotName='" + chatBotName + '\'' +
                ", chatBotUserName='" + chatBotUserName + '\'' +
                ", chatBotPrompt='" + chatBotPrompt + '\'' +
                ", selfIntroduce='" + selfIntroduce + '\'' +
                ", introduce='" + introduce + '\'' +
                ", welcomeMessage='" + welcomeMessage + '\'' +
                ", voiceNames=" + Arrays.toString(voiceNames) +
                '}';
    }
}
