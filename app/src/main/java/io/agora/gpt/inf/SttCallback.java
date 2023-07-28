package io.agora.gpt.inf;

public interface SttCallback {
    void onSttResult(String text, boolean isFinish);

    void onSttFail(int errorCode, String message);
}
