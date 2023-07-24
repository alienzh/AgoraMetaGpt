package io.agora.metagpt.models.chat.gpt;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Arrays;

public class GptRequestBody implements Serializable {
    private String[] questions;

    public String[] getQuestions() {
        return questions;
    }

    public void setQuestions(String[] questions) {
        this.questions = questions;
    }

    @NonNull
    @Override
    public String toString() {
        return "GptRequestBody{" +
                "questions=" + Arrays.toString(questions) +
                '}';
    }
}
