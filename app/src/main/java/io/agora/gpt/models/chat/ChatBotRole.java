package io.agora.gpt.models.chat;

import java.util.Arrays;

public class ChatBotRole {
    private int chatBotId;
    private boolean isEnable;
    private String chatBotRole;
    private String chatBotName;
    private String chatBotUserName;
    private String chatBotPrompt;
    private String introduceReplace;
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
                ", introduceReplace='" + introduceReplace + '\'' +
                ", introduce='" + introduce + '\'' +
                ", welcomeMessage='" + welcomeMessage + '\'' +
                ", voiceNames=" + Arrays.toString(voiceNames) +
                '}';
    }
}
