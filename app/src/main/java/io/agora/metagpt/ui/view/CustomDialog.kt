package io.agora.metagpt.ui.view

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import coil.load
import coil.transform.CircleCropTransformation
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.GridItem
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import io.agora.metagpt.R
import io.agora.metagpt.databinding.ProgressBarBinding
import io.agora.metagpt.databinding.ProgressLoadingBinding

class CustomDialog {

    companion object {

        data class AvatarGridItem(
            val url: String,
        ) : GridItem {
            override val title: String
                get() = ""

            override fun configureTitle(textView: TextView) {
                textView.visibility = View.GONE
            }

            override fun populateIcon(imageView: ImageView) {
                imageView.load(url) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }
            }
        }

        @JvmStatic
        fun showDownloadingChooser(
            context: Context,
            positive: ((MaterialDialog) -> Unit)?,
            negative: ((MaterialDialog) -> Unit)?,
        ): MaterialDialog {
            return MaterialDialog(context).show {
                title(text = "下载提示")
                message(text = "首次进入MetaChat场景需下载350M数据包")
                positiveButton(text = "立即下载", click = positive)
                negativeButton(text = "下次再说", click = negative)
            }
        }

        @JvmStatic
        fun showDownloadingProgress(
            context: Context, negative: ((MaterialDialog) -> Unit)?,
        ): MaterialDialog {
            return MaterialDialog(context).show {
                title(text = "下载中")
                message(text = "首次进入MetaChat场景需下载350M数据包")
                customView(
                    view = ProgressBarBinding.inflate(LayoutInflater.from(context)).root,
                    horizontalPadding = true,
                )
                cancelOnTouchOutside(false)
                negativeButton(text = "取消", click = negative)
            }
        }

        @JvmStatic
        fun <T> getCustomView(dialog: MaterialDialog): T {
            return dialog.getCustomView() as T
        }

        @JvmStatic
        fun showLoadingProgress(
            context: Context,
        ): AlertDialog {
            return AlertDialog.Builder(context).setView(R.layout.progress_loading)
                .create().apply {
                    window?.decorView?.setBackgroundColor(Color.TRANSPARENT)
                    setCancelable(false)
                    setCanceledOnTouchOutside(false)
                }
        }
    }

}
