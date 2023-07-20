package io.agora.metagpt.ai.minimax;


import com.alibaba.fastjson.JSONObject;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;


public interface MinimaxRequest {

    /**
     * @param groupId
     * @param body
     * @return
     */
    @POST("v1/text/completion")
    Observable<JSONObject> getMinimaxCompletionResponse(@Query("GroupId") String groupId, @Body MinimaxCompletionRequestBody body);

    /**
     *
     * @param groupId
     * @param body
     * @return
     */
    @POST("v1/text/chatcompletion")
    Observable<JSONObject> getMinimaxChatCompletionResponse(@Query("GroupId") String groupId, @Body MinimaxChatCompletionRequestBody body);

}
