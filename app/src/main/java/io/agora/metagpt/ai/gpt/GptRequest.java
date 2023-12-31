package io.agora.metagpt.ai.gpt;


import com.alibaba.fastjson.JSONObject;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;


public interface GptRequest {

    /**
     * get gpt response
     *
     * @param body
     * @return
     */
    @POST("chatgpt-internal/api/external/v1/chat-gpt/")
    Observable<JSONObject> getGptResponse(@Body GptRequestBody body);

    /**
     * get gpt response
     *
     * @param body
     * @return
     */
    @POST("chatgpt-internal/api/external/v1/chat-gpt/chat/completions")
    Observable<JSONObject> getGpt4Response(@Body Gpt4RequestBody body);
}
