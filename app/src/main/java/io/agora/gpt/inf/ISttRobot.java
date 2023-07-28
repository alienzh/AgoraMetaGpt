package io.agora.gpt.inf;

public interface ISttRobot {
    void init();

    void setSttCallback(SttCallback callback);

    void stt(byte[] bytes);

    void close();
}
