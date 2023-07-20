package io.agora.metagpt.stt;

import io.agora.metagpt.inf.ISttRobot;
import io.agora.metagpt.inf.SttCallback;
import io.agora.metagpt.stt.xf.XFSttIstRobot;
import io.agora.metagpt.stt.xf.XFSttRtasrRobot;
import io.agora.metagpt.utils.Constants;


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
