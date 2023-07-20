package io.agora.metagpt.stt.xf;

public interface SttCallback {
    void onSttResult(String text, boolean isFinish);

    void onSttFail(int errorCode, String message);
}
