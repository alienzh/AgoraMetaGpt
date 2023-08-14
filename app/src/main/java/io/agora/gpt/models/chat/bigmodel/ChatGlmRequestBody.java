package io.agora.gpt.models.chat.bigmodel;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONArray;

import java.io.Serializable;

public class ChatGlmRequestBody implements Serializable {
    private JSONArray prompt;
    private float temperature;
    private float top_p;
    private String request_id;
    private boolean incremental;

    public JSONArray getPrompt() {
        return prompt;
    }

    public void setPrompt(JSONArray prompt) {
        this.prompt = prompt;
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

    public String getRequest_id() {
        return request_id;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    public boolean isIncremental() {
        return incremental;
    }

    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    @NonNull
    @Override
    public String toString() {
        return "ChatGlmRequestBody{" +
                "prompt=" + prompt +
                ", temperature=" + temperature +
                ", top_p=" + top_p +
                ", request_id='" + request_id + '\'' +
                ", incremental=" + incremental +
                '}';
    }
}
