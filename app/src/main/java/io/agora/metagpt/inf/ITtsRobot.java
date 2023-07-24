package io.agora.metagpt.inf;

import java.util.Map;

public interface ITtsRobot {
    String getTtsPlatformName();

    void setVoiceName(String voiceName);

    void setTtsCallback(TtsCallback callback);

    void init(String pcmFilePath);

    void setIsSpeaking(boolean isSpeaking);

    void tts(String message);

    void tts(String message, boolean isOnSpeaking);

    void tipTts(Map<String, String> tipTtsMessages);

    void cancelTtsRequest(boolean cancel);

    void clearRingBuffer();

    byte[] getTtsBuffer(int length);

    int getTtsBufferLength();

    void addTtsBuffer(byte[] buffer);

    void close();
}
