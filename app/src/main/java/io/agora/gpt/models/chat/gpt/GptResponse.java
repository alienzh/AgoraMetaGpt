package io.agora.gpt.models.chat.gpt;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class GptResponse implements Serializable {
    private String id;
    private String answer;
    private long created;
    private String model;
    private String object;
    private String usage;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }

    @NonNull
    @Override
    public String toString() {
        return "Response{" +
                "id='" + id + '\'' +
                ", answer='" + answer + '\'' +
                ", created=" + created +
                ", model='" + model + '\'' +
                ", object='" + object + '\'' +
                ", usage='" + usage + '\'' +
                '}';
    }
}
