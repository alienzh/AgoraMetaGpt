package io.agora.gpt.ui.view

import android.R
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.text.style.ReplacementSpan
import android.util.TypedValue

data class IconTextSpanConfig constructor(
    var textColor: String = "#FFFFFF",
    var textSizePx: Int = 14,
    var backgroundColor: String = "#CC7084CB",
    var backgroundRadiusPx: Int = 16,
    var marginLeftPx: Int = 5,
    var paddingHorizontalPx: Int = 5,
    var paddingVerticalPx: Int = 2,
    var imageHeightPx: Int = 16,
    var imageWidthPx: Int = -1,
    var text: String = "",
    var imageResId: Int = -1,
    var imageArrowLeft: Boolean = true,
    var imageMargin: Int = 0
)

class IconTextSpan constructor(
    context: Context,
    iconTextSpanConfig: IconTextSpanConfig
) : ReplacementSpan() {

    private lateinit var context: Context
    private var textColorResId: Int = -1 // 文字颜色的 id名字
    private var imageResId: Int = -1 // 图片的资源 id
    private var textSizeDp: Float = -1f /// 文字大小 单位 dp
    private var backgroundColorResId: Int = -1 //
    private var backgroundRadiusDp: Float = -1f
    private var paddingHorizontalDp: Float = -1f
    private var paddingVerticalDp: Float = -1f
    private var marginLeftDp: Float = -1f    // 背景距离左边界距离
    private var text: String = ""
    private var imageWidthDp = -1f
    private var imageHeightDp = -1f
    private var backgroundWidthDp: Float = -1f // 背景的宽度
    private var textWidthDp: Float = -1f //  文字的宽度
    private var imageMargin = -1f //  图片margin
    private var imageArrowLeft = true //  图片位置左右
    private var bgPaint = Paint() // 背景的画笔
    private var textPaint = Paint() // 文字的画笔

    init {
        // 初始化默认数
        initValue(context, iconTextSpanConfig) // 初始化数据，主要是为了转换单位
        initPaint()
    }

    private fun initValue(context: Context, iconTextSpanConfig: IconTextSpanConfig) {
        textColorResId = Color.parseColor(iconTextSpanConfig.textColor)
        textSizeDp = px2dp(context, iconTextSpanConfig.textSizePx)
        backgroundColorResId = Color.parseColor(iconTextSpanConfig.backgroundColor)
        backgroundRadiusDp = px2dp(context, iconTextSpanConfig.backgroundRadiusPx)
        marginLeftDp = px2dp(context, iconTextSpanConfig.marginLeftPx)
        paddingHorizontalDp = px2dp(context, iconTextSpanConfig.paddingHorizontalPx)
        paddingVerticalDp = px2dp(context, iconTextSpanConfig.paddingVerticalPx)
        imageHeightDp = px2dp(context, iconTextSpanConfig.imageHeightPx)
        imageWidthDp = px2dp(context, iconTextSpanConfig.imageWidthPx)
        imageResId = iconTextSpanConfig.imageResId
        imageMargin = px2dp(context, iconTextSpanConfig.imageMargin)
        backgroundWidthDp = calculateBgWidth(iconTextSpanConfig.text)
        this.context = context
        text = iconTextSpanConfig.text

        imageArrowLeft = iconTextSpanConfig.imageArrowLeft
    }

    private fun px2dp(context: Context, px: Int): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px.toFloat(), context.resources.displayMetrics);
    }

    private fun initPaint() {
        bgPaint.color = backgroundColorResId
        bgPaint.style = Paint.Style.FILL
        bgPaint.isAntiAlias = true
        //初始化文字画笔
        textPaint.color = textColorResId
        textPaint.textSize = textSizeDp
        textPaint.isAntiAlias = true
    }

    private fun calculateBgWidth(text: String): Float {
        textPaint.textSize = textSizeDp
        textWidthDp = textPaint.measureText(text)
        return paddingHorizontalDp * 2 + textWidthDp
    }

    override fun getSize(paint: Paint, text: CharSequence?, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        // 此处返回整个画布所占的宽度
        if (fm != null) {
            fm.top -= paddingVerticalDp.toInt() // 防止上届与下界不够用，padding留出来位置
            fm.bottom += paddingVerticalDp.toInt()
        }
        return (backgroundWidthDp + marginLeftDp).toInt()
    }

    override fun draw(
        canvas: Canvas, text: CharSequence?, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint
    ) {
        // 画背景
        drawBackground(x, y, canvas)
        drawText(x, y, this.text, canvas)
        if (imageResId != -1) {
            drawArrow(x, y, canvas, paint)
        }
    }

    private fun drawArrow(x: Float, y: Int, canvas: Canvas, paint: Paint) {
        val fontMetrics = textPaint.fontMetrics
        val bitmap = BitmapFactory.decodeResource(context.resources, imageResId)
        val arrowStartX = if (imageArrowLeft) marginLeftDp else x + marginLeftDp + textWidthDp + imageMargin
        val arrowEndX = arrowStartX + imageWidthDp
        val middleY = y - (fontMetrics.descent - fontMetrics.ascent) / 2 + fontMetrics.descent
        val arrowStartY = middleY - imageHeightDp / 2
        val arrowEndY = middleY + imageHeightDp / 2
        val arrowRect = Rect(arrowStartX.toInt(), arrowStartY.toInt(), arrowEndX.toInt(), arrowEndY.toInt())
        canvas.drawBitmap(bitmap, null, arrowRect, paint)
    }

    private fun drawBackground(x: Float, y: Int, canvas: Canvas) {
        // 画背景需要两个坐标  左上角坐标(bgStartX,bgStartY) 右下角坐标(bgEndX, bgEndY)
        val fontMetrics = textPaint.fontMetrics
        val bgStartX = x - paddingHorizontalDp + marginLeftDp
        val bgStartY = y + fontMetrics.ascent - paddingVerticalDp
        var bgEndX = x + backgroundWidthDp - paddingHorizontalDp + marginLeftDp
        if (imageResId != -1) {
            bgEndX += imageWidthDp + imageMargin // 如果有 icon，也要考虑到 icon
        }
        val bgEndY = fontMetrics.descent + y + paddingVerticalDp
        val backgroundRect = RectF(bgStartX, bgStartY, bgEndX, bgEndY)
        canvas.drawRoundRect(backgroundRect, backgroundRadiusDp, backgroundRadiusDp, bgPaint)
    }

    private fun drawText(x: Float, y: Int, text: String, canvas: Canvas) {
        val textStartX = if (imageArrowLeft) x + marginLeftDp + imageWidthDp + imageMargin else x + marginLeftDp
        canvas.drawText(text, textStartX, y.toFloat(), textPaint)
    }
}