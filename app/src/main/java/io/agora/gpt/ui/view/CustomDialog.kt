package io.agora.gpt.ui.view

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
import io.agora.gpt.R
import io.agora.gpt.databinding.ProgressBarBinding
import io.agora.gpt.utils.AppUtils

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
            size: Long,
            positive: ((MaterialDialog) -> Unit)?,
            negative: ((MaterialDialog) -> Unit)?,
        ): MaterialDialog {
            return MaterialDialog(context).show {
                title(text = getContext().getString(R.string.download_title))
                message(text = getContext().getString(R.string.download_content,AppUtils.getNetFileSizeDescription(size)))
                cancelOnTouchOutside(false)
                positiveButton(text = getContext().getString(R.string.download_now), click = positive)
                negativeButton(text = getContext().getString(R.string.download_next_time), click = negative)
            }
        }

        @JvmStatic
        fun showDownloadingProgress(
            context: Context, size: Long, negative: ((MaterialDialog) -> Unit)?,
        ): MaterialDialog {
            return MaterialDialog(context).show {
                title(text = getContext().getString(R.string.download_title))
                message(text = getContext().getString(R.string.download_content,AppUtils.getNetFileSizeDescription(size)))
                customView(
                    view = ProgressBarBinding.inflate(LayoutInflater.from(context)).root,
                    horizontalPadding = true,
                )
                cancelOnTouchOutside(false)
                negativeButton(text = getContext().getString(R.string.cancel), click = negative)
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
