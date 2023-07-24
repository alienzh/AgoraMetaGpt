package io.agora.metagpt.tts.minimax;

import io.agora.metagpt.models.tts.minimax.MinimaxTtsRequestBody;
import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;


public interface MinimaxTtsRequest {

    /**
     * @param groupId
     * @param body
     * @return
     */
    @POST("v1/text_to_speech")
    Observable<ResponseBody> getTtsResult(@Query("GroupId") String groupId, @Body MinimaxTtsRequestBody body);

}
