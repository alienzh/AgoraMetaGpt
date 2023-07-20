package io.agora.metagpt.tts.ms;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.POST;


public interface MsTtsRequest {

    /**
     * get gpt response
     *
     * @param body
     * @return
     */
    @POST("cognitiveservices/v1")
    Observable<ResponseBody> getTtsResponse(@Body String body);
}
