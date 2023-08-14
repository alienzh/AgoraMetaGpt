package io.agora.gpt.stt.ms;

import android.text.TextUtils;
import android.util.Log;

import com.microsoft.cognitiveservices.speech.CancellationReason;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioStreamFormat;
import com.microsoft.cognitiveservices.speech.audio.PullAudioInputStream;
import com.microsoft.cognitiveservices.speech.audio.PullAudioInputStreamCallback;
import com.microsoft.cognitiveservices.speech.transcription.ConversationTranscriber;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

import io.agora.gpt.BuildConfig;
import io.agora.gpt.context.GameContext;
import io.agora.gpt.stt.SttRobotBase;
import io.agora.gpt.utils.Constants;
import io.agora.gpt.utils.RingBuffer;

public class MsSttRobot extends SttRobotBase {

    private SpeechConfig mSpeechConfig;
    private SpeechRecognizer mRecognizer;

    private final RingBuffer mRingBuffer;

    private String mSpeakId;

    private final Map<String, Integer> mSpeakerMap;

    public MsSttRobot() {
        super();
        mRingBuffer = new RingBuffer(1024 * 1024 * 5);
        mSpeakerMap = new HashMap<String, Integer>();
    }

    @Override
    public void init() {
        super.init();
    }

    private void startRecognition() {
        if (null != mRecognizer) {
            return;
        }
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    AudioStreamFormat audioFormat = AudioStreamFormat.getWaveFormatPCM((long) Constants.STT_SAMPLE_RATE, (short) Constants.STT_BITS_PER_SAMPLE, (short) Constants.STT_SAMPLE_NUM_OF_CHANNEL);
                    PullAudioInputStream inputStream = PullAudioInputStream.createPullStream(new AudioStreamCallback(), audioFormat);
                    AudioConfig audioInput = AudioConfig.fromStreamInput(inputStream);

                    mSpeechConfig = SpeechConfig.fromSubscription(BuildConfig.MS_SPEECH_KEY, BuildConfig.MS_SPEECH_REGION);
                    mSpeechConfig.setSpeechRecognitionLanguage(Constants.LANG_ZH_CN);
                    mRecognizer = new SpeechRecognizer(mSpeechConfig, audioInput);
                    recognition(audioInput);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(Constants.TAG, "e:" + e);
                }
            }
        });
    }

    private void recognition(AudioConfig audioInput) throws InterruptedException, ExecutionException {
        Semaphore stopRecognitionSemaphore = new Semaphore(0);

        ConversationTranscriber conversationTranscriber = new ConversationTranscriber(mSpeechConfig, audioInput);
        {
            // Subscribes to events.
            conversationTranscriber.transcribing.addEventListener((s, e) -> {
                Log.i(Constants.TAG, "TRANSCRIBING: Text=" + e.getResult().getText());
            });

            conversationTranscriber.transcribed.addEventListener((s, e) -> {
                if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                    Log.i(Constants.TAG, "TRANSCRIBED: Text=" + e.getResult().getText() + " Speaker ID=" + e.getResult().getSpeakerId());
                    String speakerId = e.getResult().getSpeakerId();
                    String text = e.getResult().getText();
                    if (TextUtils.isEmpty(text)) {
                        return;
                    }
                    if (GameContext.getInstance().isEnableSpeakerDiarization()) {
                        if (!Constants.UNKNOWN.equals(speakerId)) {
                            int count = 0;
                            if (!mSpeakerMap.containsKey(speakerId)) {
                                count = 1;
                            } else {
                                Integer c = mSpeakerMap.get(speakerId);
                                if (c == null) {
                                    count = 1;
                                } else {
                                    count = c + 1;
                                }
                            }
                            mSpeakerMap.put(speakerId, count);
                            if (count >= Constants.MAX_COUNT_SPEAKER_DIARIZATION) {
                                mSpeakId = speakerId;
                                mSpeakerMap.clear();
                            }
                        }
                        if (null != mCallback) {
                            if (!TextUtils.isEmpty(mSpeakId)) {
                                if (mSpeakId.equals(speakerId)) {
                                    mCallback.onSttResult(text, false);
                                } else {
                                    mCallback.onSttResultIgnore(text);
                                    Log.i(Constants.TAG, "TRANSCRIBED discard mSpeakId:" + mSpeakId + ",speakerId:" + speakerId);
                                }
                            } else {
                                mCallback.onSttResult(text, false);
                            }
                        }
                    } else {
                        if (null != mCallback) {
                            mCallback.onSttResult(text, false);
                        }
                    }
                } else if (e.getResult().getReason() == ResultReason.NoMatch) {
                    Log.i(Constants.TAG, "NOMATCH: Speech could not be transcribed.");
                }
            });

            conversationTranscriber.canceled.addEventListener((s, e) -> {
                Log.i(Constants.TAG, "CANCELED: Reason=" + e.getReason());

                if (e.getReason() == CancellationReason.Error) {
                    Log.i(Constants.TAG, "CANCELED: ErrorCode=" + e.getErrorCode());
                    Log.i(Constants.TAG, "CANCELED: ErrorDetails=" + e.getErrorDetails());
                    if (null != mCallback) {
                        mCallback.onSttFail(e.getErrorCode().getValue(), e.getErrorDetails());
                    }
                }

                stopRecognitionSemaphore.release();
            });

            conversationTranscriber.sessionStarted.addEventListener((s, e) -> {
                Log.i(Constants.TAG, "Session started event.");
            });

            conversationTranscriber.sessionStopped.addEventListener((s, e) -> {
                Log.i(Constants.TAG, " Session stopped event.");
                if (null != mCallback) {
                    mCallback.onSttResult("", true);
                }
            });

            conversationTranscriber.startTranscribingAsync().get();

            // Waits for completion.
            stopRecognitionSemaphore.acquire();

            conversationTranscriber.stopTranscribingAsync().get();
        }
    }


    @Override
    public synchronized void stt(byte[] bytes) {
        super.stt(bytes);
        if (null != bytes) {
            for (byte aByte : bytes) {
                mRingBuffer.put(aByte);
            }
            if (mRingBuffer.size() > Constants.STT_SAMPLE_RATE / 1000 * Constants.STT_BITS_PER_SAMPLE / 8 * Constants.INTERVAL_XF_REQUEST * Constants.MAX_COUNT_AUDIO_FRAME) {
                startRecognition();
            }
        }
    }

    @Override
    public void close() {
        if (null != mRecognizer) {
            mRecognizer.close();
            mRecognizer = null;
        }

        if (null != mSpeechConfig) {
            mSpeechConfig.close();
            mSpeechConfig = null;
        }
        mSpeakId = "";
        mSpeakerMap.clear();

        mRingBuffer.clear();
    }

    public class AudioStreamCallback extends PullAudioInputStreamCallback {
        public AudioStreamCallback() {

        }

        @Override
        public int read(byte[] dataBuffer) {
            try {
                long curTime = System.nanoTime();

                long diffMs = (curTime - mLastSendTime) / 1000 / 1000;
                if (diffMs < Constants.INTERVAL_XF_REQUEST) {
                    Thread.sleep(Constants.INTERVAL_XF_REQUEST - diffMs);
                }

                byte[] datas = getTtsBuffer(1280);
                mLastSendTime = System.nanoTime();
                if (datas == null) {
                    return 0;
                } else {
                    System.arraycopy(datas, 0, dataBuffer, 0, datas.length);
                    return datas.length;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }

        @Override
        public void close() {

        }
    }

    public byte[] getTtsBuffer(int length) {
        if (mRingBuffer.size() < length) {
            return null;
        }
        Object o;
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            o = mRingBuffer.take();
            if (null == o) {
                return null;
            }
            bytes[i] = (byte) o;
        }
        return bytes;

    }
}
