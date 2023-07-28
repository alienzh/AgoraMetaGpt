package io.agora.gpt.models.chat.minimax;


import com.alibaba.fastjson.JSONObject;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Streaming;


public interface MinimaxRequest {

    /**
     * @param groupId
     * @param body
     * @return
     */
    @POST("v1/text/completion")
    Observable<JSONObject> getMinimaxCompletionResponse(@Query("GroupId") String groupId, @Body MinimaxCompletionRequestBody body);

    /**
     * @param groupId
     * @param body
     * @return
     */
    @POST("v1/text/chatcompletion")
    Observable<JSONObject> getMinimaxChatCompletionResponse(@Query("GroupId") String groupId, @Body MinimaxChatCompletionRequestBody body);

    @POST("v1/text/chatcompletion")
    @Streaming
    Observable<ResponseBody> getMinimaxChatCompletionResponseWithStream(@Query("GroupId") String groupId, @Body MinimaxChatCompletionRequestBody body);


    /**
     * @param groupId
     * @param body
     * @return
     */
    @POST("v1/text/chatcompletion_pro")
    Observable<JSONObject> getMinimaxChatCompletionProResponse(@Query("GroupId") String groupId, @Body MinimaxChatCompletionProRequestBody body);
}
