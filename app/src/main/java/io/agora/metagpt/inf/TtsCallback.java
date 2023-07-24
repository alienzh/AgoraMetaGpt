package io.agora.metagpt.inf;

public interface TtsCallback {
    void onTtsStart(String text, boolean isOnSpeaking);

    void onTtsFinish(boolean isOnSpeaking);

    void updateTtsHistoryInfo(String message);
}
