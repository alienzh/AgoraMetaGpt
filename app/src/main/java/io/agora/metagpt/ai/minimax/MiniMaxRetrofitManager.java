package io.agora.metagpt.ai.minimax;

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
import retrofit2.converter.gson.GsonConverterFactory;

public class MiniMaxRetrofitManager {
    private MinimaxRequest mMiniMaxRequest;

    private static final class InstanceHolder {
        static final MiniMaxRetrofitManager M_INSTANCE = new MiniMaxRetrofitManager();
    }

    public static MiniMaxRetrofitManager getInstance() {
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
                                .addHeader("Content-Type", "application/json")
                                .addHeader("Authorization", "Bearer " + BuildConfig.MINIMAX_AUTHORIZATION)
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
                .baseUrl(BuildConfig.MINIMAX_SERVER_HOST)
                // 适配rxjava，目的在于使用观察者模式，分解上层请求的过程，便于我们横加干预（比如请求嵌套）
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                // 使用Gson框架解析请求返回的结果，因为返回的是xml，只有解析过后，才能将数据变为对象，放置到我们刚刚创建你的实体类当中，便于数据的传递使用
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // 创建一个request
        mMiniMaxRequest = retrofit.create(MinimaxRequest.class);
    }

    public MinimaxRequest getMinimaxRequest() {
        return mMiniMaxRequest;
    }
}
