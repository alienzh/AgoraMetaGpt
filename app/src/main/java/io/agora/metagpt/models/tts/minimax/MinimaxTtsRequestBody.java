package io.agora.metagpt.models.tts.minimax;

import androidx.annotation.NonNull;

public class MinimaxTtsRequestBody {

    private String voice_id;
    private String text;
    private String model;

    public String getVoice_id() {
        return voice_id;
    }

    public void setVoice_id(String voice_id) {
        this.voice_id = voice_id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @NonNull
    @Override
    public String toString() {
        return "MinimaxTtsRequestBody{" +
                "voice_id='" + voice_id + '\'' +
                ", text='" + text + '\'' +
                ", model='" + model + '\'' +
                '}';
    }
}
