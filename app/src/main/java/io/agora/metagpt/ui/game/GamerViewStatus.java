package io.agora.metagpt.ui.game;

public class GamerViewStatus {
    private GamerViewStatus() {
    }

    // 初始状态
    public static class Initial extends GamerViewStatus {
    }

    // 开始游戏
    public static class StartGame extends GamerViewStatus{

    }

    // 开始发言
    public static class StartSpeak extends GamerViewStatus {
    }

    // 开始投票
    public static class StartVote extends GamerViewStatus {
    }

    // 自己发言/发言结束
    public static class SelfSpeak extends GamerViewStatus {
        public boolean isSpeaking;

        public SelfSpeak(boolean isSpeaking) {
            this.isSpeaking = isSpeaking;
        }
    }

    // 其他玩家发言/发言结束
    public static class OtherSpeak extends GamerViewStatus{
        public boolean isSpeaking;

        public OtherSpeak(boolean isSpeaking) {
            this.isSpeaking = isSpeaking;
        }
    }

    // 结束游戏
   public static class EndGame extends GamerViewStatus{

   }
}
