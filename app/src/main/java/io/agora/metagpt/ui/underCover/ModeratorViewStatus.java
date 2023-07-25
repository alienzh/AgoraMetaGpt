package io.agora.metagpt.ui.underCover;

public class ModeratorViewStatus {
    private ModeratorViewStatus() {
    }

    // 初始状态
    public static class Initial extends ModeratorViewStatus {
    }

    // 开始游戏
    public static class StartGame extends ModeratorViewStatus {
    }

    // 新一轮准备
    public static class RoundsReady extends ModeratorViewStatus {
    }

    // AI 发言结束
    public static class AISpeakOver extends ModeratorViewStatus {
    }

    // 结束游戏
    public static class EndGame extends ModeratorViewStatus {
    }
}
