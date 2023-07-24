package io.agora.metagpt.utils;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;

public class AndroidPcmPlayer {
    private final static String TAG = Constants.TAG + "-" + AndroidPcmPlayer.class.getSimpleName();
    private AudioTrack mAudioTrack = null;
    private int mBufferSize;

    public AndroidPcmPlayer(int sampleRate, int channelConfig, int audioFormat) {
        init(sampleRate, channelConfig, audioFormat);
    }

    private void init(int sampleRate, int channelConfig, int audioFormat) {
        mBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        Log.i(TAG, "init buffer size: " + mBufferSize);
        mAudioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build())
                .setBufferSizeInBytes(mBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();
    }

    public void play(String path) {
        if (mAudioTrack == null) {
            Log.e(TAG, "audio track state uninitialized");
            return;
        }
        if (mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
            Log.e(TAG, "audio track state uninitialized");
            return;
        }
        try {
            // 开始播放音频数据
            mAudioTrack.play();

            mAudioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                @Override
                public void onMarkerReached(AudioTrack track) {
                    Log.e(TAG, "onMarkerReached");
                    //MetaContext.getInstance().updateRoleSpeak(true);
                }

                @Override
                public void onPeriodicNotification(AudioTrack track) {

                }
            });

            // 读取PCM数据到缓冲区
            byte[] buffer = new byte[mBufferSize];
            File file = new File(path);

            FileInputStream fis = new FileInputStream(file);

            mAudioTrack.setNotificationMarkerPosition(fis.available());

            int read;
            while ((read = fis.read(buffer)) != -1) {
                mAudioTrack.write(buffer, 0, read);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (mAudioTrack != null) {
            mAudioTrack.stop();
        }
    }

    public void release() {
        if (mAudioTrack != null) {
            mAudioTrack.release();
            mAudioTrack = null;
        }
    }
}


