package io.agora.gpt.inf;

public interface TtsCallback {
    void onTtsStart(String text, boolean isOnSpeaking);

    void onFirstTts();

    void onTtsFinish(boolean isOnSpeaking);

    void updateTtsHistoryInfo(String message);
}
