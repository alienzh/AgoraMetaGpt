package io.agora.metagpt.tts.ms;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.agora.metagpt.BuildConfig;
import io.agora.metagpt.utils.Constants;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class MsTtsRetrofitManager {
    private MsTtsRequest mTtsRequest;

    private static final class InstanceHolder {
        static final MsTtsRetrofitManager M_INSTANCE = new MsTtsRetrofitManager();
    }

    public static MsTtsRetrofitManager getInstance() {
        return InstanceHolder.M_INSTANCE;
    }

    public void init() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(@NonNull String s) {
                if (BuildConfig.DEBUG) {
                    Log.i(Constants.TAG, s);
                }
            }
        });
        loggingInterceptor.level(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(Constants.HTTP_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(Constants.HTTP_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constants.HTTP_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(new Interceptor() {
                    @NonNull
                    @Override
                    public Response intercept(@NonNull Chain chain) throws IOException {

                        Request original = chain.request();
                        Request request = original.newBuilder()
                                .addHeader("Ocp-Apim-Subscription-Key", BuildConfig.MS_SPEECH_KEY)
                                .addHeader("Content-Type", "application/ssml+xml")
                                .addHeader("X-Microsoft-OutputFormat", "raw-16khz-16bit-mono-pcm")
                                .addHeader("User-Agent", Constants.TAG)
                                .method(original.method(), original.body())
                                .build();
                        return chain.proceed(request);
                    }
                })
                .addInterceptor(loggingInterceptor)
                .build();

        // 创建一个retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .client(client)
                // 设置基址
                .baseUrl(BuildConfig.MS_SERVER_HOST)
//                .addConverterFactory(SimpleXmlConverterFactory.create())
                // 适配rxjava，目的在于使用观察者模式，分解上层请求的过程，便于我们横加干预（比如请求嵌套）
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                // 使用Gson框架解析请求返回的结果，因为返回的是xml，只有解析过后，才能将数据变为对象，放置到我们刚刚创建你的实体类当中，便于数据的传递使用
                .build();

        // 创建一个request
        mTtsRequest = retrofit.create(MsTtsRequest.class);
    }

    public MsTtsRequest getTtsRequest() {
        return mTtsRequest;
    }
}
