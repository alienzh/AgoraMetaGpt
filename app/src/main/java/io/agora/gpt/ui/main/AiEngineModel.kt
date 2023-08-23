package io.agora.gpt.ui.main

import io.agora.ai.sdk.AIEngineAction
import io.agora.ai.sdk.AIEngineCode

data class DownloadProgressModel constructor(
    val progress: Int,
    val index: Int,
    val count: Int
)

data class ActionResultModel constructor(
    val vcAction: AIEngineAction,
    val vcEngineCode: AIEngineCode,
    val extraInfo: String?
)
