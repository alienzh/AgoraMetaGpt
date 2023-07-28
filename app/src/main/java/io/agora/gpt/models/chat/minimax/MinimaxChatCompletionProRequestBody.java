package io.agora.gpt.models.chat.minimax;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONArray;

import java.io.Serializable;

public class MinimaxChatCompletionProRequestBody implements Serializable {
    private String model;
    private boolean stream;
    private int tokens_to_generate;
    private float temperature;

    private float top_p;

    private boolean mask_sensitive_info;

    private JSONArray messages;
    private JSONArray bot_setting;

    private MinimaxReplyConstraints reply_constraints;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public int getTokens_to_generate() {
        return tokens_to_generate;
    }

    public void setTokens_to_generate(int tokens_to_generate) {
        this.tokens_to_generate = tokens_to_generate;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public float getTop_p() {
        return top_p;
    }

    public void setTop_p(float top_p) {
        this.top_p = top_p;
    }

    public boolean isMask_sensitive_info() {
        return mask_sensitive_info;
    }

    public void setMask_sensitive_info(boolean mask_sensitive_info) {
        this.mask_sensitive_info = mask_sensitive_info;
    }

    public JSONArray getMessages() {
        return messages;
    }

    public void setMessages(JSONArray messages) {
        this.messages = messages;
    }

    public JSONArray getBot_setting() {
        return bot_setting;
    }

    public void setBot_setting(JSONArray bot_setting) {
        this.bot_setting = bot_setting;
    }

    public MinimaxReplyConstraints getReply_constraints() {
        return reply_constraints;
    }

    public void setReply_constraints(MinimaxReplyConstraints reply_constraints) {
        this.reply_constraints = reply_constraints;
    }

    @NonNull
    @Override
    public String toString() {
        return "MinimaxChatCompletionProRequestBody{" +
                "model='" + model + '\'' +
                ", stream=" + stream +
                ", tokens_to_generate=" + tokens_to_generate +
                ", temperature=" + temperature +
                ", top_p=" + top_p +
                ", mask_sensitive_info=" + mask_sensitive_info +
                ", messages=" + messages +
                ", bot_setting=" + bot_setting +
                ", reply_constraints=" + reply_constraints +
                '}';
    }
}
