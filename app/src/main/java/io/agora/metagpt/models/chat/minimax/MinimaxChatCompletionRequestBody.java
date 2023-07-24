package io.agora.metagpt.models.chat.minimax;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONArray;

import java.io.Serializable;

public class MinimaxChatCompletionRequestBody implements Serializable {
    private String model;
    private boolean with_emotion;
    private boolean stream;
    private boolean use_standard_sse;
    private String prompt;
    private MinimaxRoleMeta role_meta;
    private JSONArray messages;

    private int tokens_to_generate;
    private float top_p;
    private float temperature;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isWith_emotion() {
        return with_emotion;
    }

    public void setWith_emotion(boolean with_emotion) {
        this.with_emotion = with_emotion;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public boolean isUse_standard_sse() {
        return use_standard_sse;
    }

    public void setUse_standard_sse(boolean use_standard_sse) {
        this.use_standard_sse = use_standard_sse;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public MinimaxRoleMeta getRole_meta() {
        return role_meta;
    }

    public void setRole_meta(MinimaxRoleMeta role_meta) {
        this.role_meta = role_meta;
    }

    public JSONArray getMessages() {
        return messages;
    }

    public void setMessages(JSONArray messages) {
        this.messages = messages;
    }

    public int getTokens_to_generate() {
        return tokens_to_generate;
    }

    public void setTokens_to_generate(int tokens_to_generate) {
        this.tokens_to_generate = tokens_to_generate;
    }

    public float getTop_p() {
        return top_p;
    }

    public void setTop_p(float top_p) {
        this.top_p = top_p;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    @NonNull
    @Override
    public String toString() {
        return "MinimaxChatCompletionRequestBody{" +
                "model='" + model + '\'' +
                ", with_emotion=" + with_emotion +
                ", stream=" + stream +
                ", use_standard_sse=" + use_standard_sse +
                ", prompt='" + prompt + '\'' +
                ", role_meta=" + role_meta +
                ", messages=" + messages +
                ", tokens_to_generate=" + tokens_to_generate +
                ", top_p=" + top_p +
                ", temperature=" + temperature +
                '}';
    }
}
