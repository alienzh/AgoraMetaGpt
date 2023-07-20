package io.agora.metagpt.ai.gpt;


import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONArray;

import java.io.Serializable;

public class Gpt4RequestBody implements Serializable {
    private String model;
    private float temperature;
    private JSONArray messages;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
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
        return "Gpt4RequestBody{" +
                "model='" + model + '\'' +
                ", temperature=" + temperature +
                ", messages=" + messages +
                '}';
    }
}
