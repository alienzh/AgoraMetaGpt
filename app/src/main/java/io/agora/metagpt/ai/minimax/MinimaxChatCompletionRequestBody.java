package io.agora.metagpt.ai.minimax;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONArray;

import java.io.Serializable;

public class MinimaxChatCompletionRequestBody implements Serializable {
    private String model;
    private String prompt;
    private int tokens_to_generate;
    private float top_p;
    private float temperature;

    private boolean stream;

    private RoleMeta role_meta;
    private JSONArray messages;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
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

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public RoleMeta getRole_meta() {
        return role_meta;
    }

    public void setRole_meta(RoleMeta role_meta) {
        this.role_meta = role_meta;
    }

    public JSONArray getMessages() {
        return messages;
    }

    public void setMessages(JSONArray messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public String toString() {
        return "MinimaxChatcompletionRequestBody{" +
                "model='" + model + '\'' +
                ", prompt='" + prompt + '\'' +
                ", tokens_to_generate=" + tokens_to_generate +
                ", top_p=" + top_p +
                ", temperature=" + temperature +
                ", stream=" + stream +
                ", role_meta=" + role_meta +
                ", messages=" + messages +
                '}';
    }
}
