package io.agora.gpt.ui.main

import io.agora.aigc.sdk.constants.ServiceCode
import io.agora.aigc.sdk.constants.ServiceEvent

data class DownloadProgressModel constructor(
    val progress: Int,
    val index: Int,
    val count: Int
)

data class DownloadResModel constructor(
    val totalSize: Long,
    val fileCount: Int
)

data class EventResultModel constructor(
    val event: ServiceEvent,
    val code: ServiceCode
)

data class ChatMessageModel constructor(
    val isAiMessage: Boolean,
    val sid: String,
    var name: String,
    var message: String,
    var costTime1: Long = 0
)

data class SportsTextModel constructor(
    var time: Int,
    var content: String
){

    override fun toString(): String {
        return "${time}分钟 - $content"
    }
}