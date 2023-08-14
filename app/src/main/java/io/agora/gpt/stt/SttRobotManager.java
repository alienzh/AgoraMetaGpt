package io.agora.gpt.stt;

import io.agora.gpt.inf.ISttRobot;
import io.agora.gpt.inf.SttCallback;
import io.agora.gpt.stt.ms.MsSttRobot;
import io.agora.gpt.stt.xf.XFSttIstRobot;
import io.agora.gpt.stt.xf.XFSttRtasrRobot;
import io.agora.gpt.utils.Constants;


public class SttRobotManager {
    private ISttRobot mSttRobot;
    private SttCallback mSttCallback;

    public SttRobotManager() {

    }

    public void setSttCallback(SttCallback sttCallback) {
        this.mSttCallback = sttCallback;
    }


    public void setAiSttPlatformIndex(int aiSttPlatformIndex) {
        switch (aiSttPlatformIndex) {
            case Constants.STT_PLATFORM_XF_RTASR:
                mSttRobot = new XFSttRtasrRobot();
                break;
            case Constants.STT_PLATFORM_XF_IST:
                mSttRobot = new XFSttIstRobot();
                break;
            case Constants.STT_PLATFORM_MS:
                mSttRobot = new MsSttRobot();
                break;
            default:
                break;
        }
        if (null != mSttRobot) {
            mSttRobot.init();
            mSttRobot.setSttCallback(mSttCallback);
        }
    }


    public void requestStt(byte[] bytes) {
        if (null != mSttRobot) {
            mSttRobot.stt(bytes);
        }
    }


    public void close() {
        if (null != mSttRobot) {
            mSttRobot.close();
        }
    }
}
