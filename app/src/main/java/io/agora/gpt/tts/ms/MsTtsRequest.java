package io.agora.gpt.tts.ms;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Streaming;


public interface MsTtsRequest {

    /**
     * get gpt response
     *
     * @param body
     * @return
     */
    @POST("cognitiveservices/v1")
    @Streaming
    Observable<ResponseBody> getTtsResponse(@Body String body);
}
