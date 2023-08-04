package io.agora.gpt.ui.view

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import io.agora.gpt.R
import io.agora.gpt.context.GameContext
import io.agora.gpt.databinding.ChooseAiRoleDialogBinding
import io.agora.gpt.models.chat.ChatBotRole
import io.agora.gpt.ui.adapter.RoleSelectAdapter
import io.agora.gpt.ui.base.BaseDialog
import io.agora.gpt.utils.AppUtils

/**
 * @author create by zhangwei03
 */
class ChooseRoleDialog constructor(context: Context) : BaseDialog<ChooseAiRoleDialogBinding>(context) {


    private var mRoleAdapter: RoleSelectAdapter? = null

    private var selectRoleCallback: ((index: Int) -> Unit)? = null

    fun setSelectRoleCallback(callback: ((index: Int) -> Unit)) {
        this.selectRoleCallback = callback
    }

    override fun getViewBinding(inflater: LayoutInflater): ChooseAiRoleDialogBinding {
        return ChooseAiRoleDialogBinding.inflate(inflater)
    }

    override fun setContentView(view: View) {
        super.setContentView(view)
        window?.let { window ->
//            StatusBarUtil.hideStatusBar(window, Color.BLACK, true)
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.setDimAmount(0f)
            window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_PANEL)
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            )
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            window.attributes.apply {
                val lp = WindowManager.LayoutParams()
                lp.copyFrom(window.attributes)
                lp.width = WindowManager.LayoutParams.MATCH_PARENT
                lp.height = WindowManager.LayoutParams.MATCH_PARENT
                window.attributes = lp
            }
        }
        setCanceledOnTouchOutside(true)
    }

    override fun initView() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }
        binding.btnSelectRole.setOnClickListener {
            val selectIndex = mRoleAdapter?.selectIndex ?: 0
            GameContext.getInstance().currentChatBotRoleIndex = selectIndex
            selectRoleCallback?.invoke(selectIndex)
            dismiss()
        }
        mRoleAdapter = RoleSelectAdapter(
            context, GameContext.getInstance().currentChatBotRoleIndex,
            GameContext.getInstance().availableChatBotRoles
        ).apply {
        }
        mRoleAdapter?.setOnSelectItemClickListener {
            val selectIndex = mRoleAdapter?.selectIndex ?: 0
            setupCurrentRoleView(GameContext.getInstance().getChatBotRoleByIndex(selectIndex))
        }
        binding.recyclerRole.adapter = mRoleAdapter
        GameContext.getInstance().getChatBotRoleByIndex(GameContext.getInstance().currentChatBotRoleIndex)?.let {
            setupCurrentRoleView(it)
        }
    }

    private fun setupCurrentRoleView(selectRole: ChatBotRole) {
        var drawableId = AppUtils.getDrawableRes(context, "ai_avatar_" + selectRole.chatBotId)
        if (drawableId == 0) drawableId = R.drawable.ai_avatar_1
        binding.ivRoleAvatar.setImageResource(drawableId)
        binding.tvRoleIntroduce.text = selectRole.introduce
        binding.tvRoleName.text = selectRole.chatBotName
    }

    override fun setGravity() {
        window?.attributes?.gravity = Gravity.TOP
    }
}