package io.agora.metagpt.inf;

public interface SttCallback {
    void onSttResult(String text, boolean isFinish);

    void onSttFail(int errorCode, String message);
}
