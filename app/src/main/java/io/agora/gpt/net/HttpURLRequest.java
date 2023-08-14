package io.agora.gpt.net;


import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import io.agora.gpt.utils.Config;
import io.agora.gpt.utils.Constants;
import io.agora.gpt.utils.ErrorCode;

public class HttpURLRequest {
    private RequestCallback mCallback;
    private volatile boolean mCancelled;

    public HttpURLRequest() {
        mCancelled = false;
    }

    public void setCallback(RequestCallback callback) {
        this.mCallback = callback;
    }

    public void setCancelled(boolean cancelled) {
        this.mCancelled = cancelled;
    }

    public void requestPostUrl(String urlStr, Map<String, String> requestProperty, String writeData, boolean isStream) {
        InputStream is = null;
        StringBuilder responseContent = new StringBuilder();
        try {
            URL url = new URL(urlStr);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(Constants.HTTP_TIMEOUT * 1000);
            conn.setReadTimeout(Constants.HTTP_TIMEOUT * 1000);

            conn.setRequestMethod("POST");
            for (Map.Entry<String, String> entry : requestProperty.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }

            conn.setDoOutput(true);

            conn.setDoInput(true);

            OutputStream os = conn.getOutputStream();

            os.write(writeData.getBytes("UTF-8"));
            os.flush();
            os.close();

            is = conn.getInputStream();

            int code = conn.getResponseCode();

            if (Config.ENABLE_HTTP_LOG) {
                // 获取服务器返回的头信息
                Map<String, List<String>> headers = conn.getHeaderFields();
                for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    String name = entry.getKey();
                    for (String value : entry.getValue()) {
                        Log.i(Constants.TAG, "http response header:" + name + " : " + value);
                    }
                }
            }

            if (HttpURLConnection.HTTP_OK == code) {
                byte[] bytes = new byte[1024 * 4];
                int len = 0;
                boolean isFirstResponse = false;
                while ((len = is.read(bytes)) != -1) {
                    if (mCancelled) {
                        break;
                    }
                    if (len == 0) {
                        continue;
                    }
                    if (Config.ENABLE_HTTP_LOG) {
                        Log.i(Constants.TAG, "http read len: " + len);
                    }
                    if (isStream) {
                        if (null != mCallback) {
                            if (!isFirstResponse) {
                                isFirstResponse = true;
                                mCallback.updateResponseData(bytes, len, true);
                            } else {
                                mCallback.updateResponseData(bytes, len, false);
                            }
                        }
                    } else {
                        responseContent.append(new String(bytes, 0, len, StandardCharsets.UTF_8));
                    }
                }
                if (!isStream) {
                    if (null != mCallback) {
                        mCallback.onHttpResponse(responseContent.toString());
                    }
                }

                if (null != mCallback) {
                    mCallback.requestFinish();
                }
            } else {
                if (null != mCallback) {
                    mCallback.requestFail(code, "");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (null != mCallback) {
                mCallback.requestFail(ErrorCode.ERROR_GENERAL, "请求错误：" + e);
            }
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void requestGetUrl(String urlStr, Map<String, String> headers) {
        try {
            URL url = new URL(urlStr);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(Constants.HTTP_TIMEOUT * 1000);
            conn.setReadTimeout(Constants.HTTP_TIMEOUT * 1000);

            conn.setRequestMethod("GET");
            if (null != headers) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }


            int code = conn.getResponseCode();

            if (Config.ENABLE_HTTP_LOG) {
                // 获取服务器返回的头信息
                Map<String, List<String>> responseHeaders = conn.getHeaderFields();
                for (Map.Entry<String, List<String>> entry : responseHeaders.entrySet()) {
                    String name = entry.getKey();
                    for (String value : entry.getValue()) {
                        Log.i(Constants.TAG, "http response header:" + name + " : " + value);
                    }
                }
            }

            if (HttpURLConnection.HTTP_OK == code) {
                BufferedReader reader;
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                if (null != mCallback) {
                    mCallback.onHttpResponse(response.toString());
                    mCallback.requestFinish();
                }
            } else {
                if (null != mCallback) {
                    mCallback.requestFail(code, "");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(Constants.TAG, "http response " + e);
            if (null != mCallback) {
                mCallback.requestFail(ErrorCode.ERROR_GENERAL, "请求错误：" + e);
            }
        }
    }

    public interface RequestCallback {
        default void updateResponseData(byte[] bytes, int len, boolean isFirstResponse) {
        }

        default void requestFail(int errorCode, String msg) {

        }

        default void requestFinish() {

        }

        default void onHttpResponse(String responseTxt) {

        }
    }
}
