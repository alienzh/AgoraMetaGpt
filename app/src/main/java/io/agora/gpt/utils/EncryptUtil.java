package io.agora.gpt.utils;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class EncryptUtil {

    /**
     * 加密数字签名（基于HMACSHA1算法）
     * 
     * @param encryptText
     * @param encryptKey
     * @return
     * @throws SignatureException
     */
    public static String HmacSHA1Encrypt(String encryptText, String encryptKey) throws SignatureException {
        byte[] rawHmac = null;
        try {
            byte[] data = encryptKey.getBytes("UTF-8");
            SecretKeySpec secretKey = new SecretKeySpec(data, "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(secretKey);
            byte[] text = encryptText.getBytes("UTF-8");
            rawHmac = mac.doFinal(text);
        } catch (InvalidKeyException e) {
            throw new SignatureException("InvalidKeyException:" + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new SignatureException("NoSuchAlgorithmException:" + e.getMessage());
        } catch (UnsupportedEncodingException e) {
            throw new SignatureException("UnsupportedEncodingException:" + e.getMessage());
        }
        String oauth = new String(Base64.encodeBase64(rawHmac));
        
        return oauth;
    }

    public final static String MD5(String pstr) {
        char md5String[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        try {
            byte[] btInput = pstr.getBytes();
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            mdInst.update(btInput);
            byte[] md = mdInst.digest();
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) { // i = 0
                byte byte0 = md[i]; // 95
                str[k++] = md5String[byte0 >>> 4 & 0xf]; // 5
                str[k++] = md5String[byte0 & 0xf]; // F
            }

            return new String(str);
        } catch (Exception e) {
            return null;
        }
    }

    public static String assembleAgoraRequestUrl(String requestUrl, String apiKey, String token,
                                                 int uid) {
        URL url = null;
        String httpRequestUrl = requestUrl.replace("ws://", "http://").replace("wss://", "https://");
        try {
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            String date = format.format(new Date());

            Charset charset = Charset.forName("UTF-8");
            Mac mac = Mac.getInstance("hmacsha256");
            SecretKeySpec spec = new SecretKeySpec(apiKey.getBytes(charset), "hmacsha256");
            mac.init(spec);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                String authorization = String.format("api_key=\"%s\", token=\"%s\", uid=\"%s\",algorithm=\"%s\"", apiKey, token, String.valueOf(uid), "hmacsha256");
                byte[] hexDigits = mac.doFinal(authorization.toString().getBytes(charset));
                authorization = java.util.Base64.getEncoder().encodeToString(hexDigits);
                String result = String.format("%s?authorization=%s&date=%s", requestUrl, URLEncoder.encode(authorization), URLEncoder.encode(date));
                Log.i(Constants.TAG, "assembleAgoraRequestUrl result=" + result);
                return result;
            } else {
                return null;
            }
        } catch (Exception e) {
            // throw new RuntimeException("assemble requestUrl error:"+e.getMessage());
            throw new RuntimeException(e);
        }

    }
}
