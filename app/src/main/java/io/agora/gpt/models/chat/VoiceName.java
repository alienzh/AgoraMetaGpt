package io.agora.gpt.models.chat;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class VoiceName implements Serializable {
    private String platformName;
    private String voiceName;
    private String voiceNameValue;
    private String voiceNameStyle;

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getVoiceName() {
        return voiceName;
    }

    public void setVoiceName(String voiceName) {
        this.voiceName = voiceName;
    }

    public String getVoiceNameValue() {
        return voiceNameValue;
    }

    public void setVoiceNameValue(String voiceNameValue) {
        this.voiceNameValue = voiceNameValue;
    }

    public String getVoiceNameStyle() {
        return voiceNameStyle;
    }

    public void setVoiceNameStyle(String voiceNameStyle) {
        this.voiceNameStyle = voiceNameStyle;
    }

    @NonNull
    @Override
    public String toString() {
        return "VoiceName{" +
                "platformName='" + platformName + '\'' +
                ", voiceName='" + voiceName + '\'' +
                ", voiceNameValue='" + voiceNameValue + '\'' +
                ", voiceNameStyle='" + voiceNameStyle + '\'' +
                '}';
    }
}
