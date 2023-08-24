package io.agora.gpt.ui.view

import android.content.Context
import android.view.View
import android.view.WindowManager
import io.agora.ai.sdk.AIRole
import io.agora.gpt.R
import io.agora.gpt.databinding.ChooseAiRoleDialogBinding
import io.agora.gpt.ui.adapter.RoleSelectAdapter
import io.agora.gpt.ui.base.BaseDialog
import io.agora.gpt.utils.AppUtils
import io.agora.gpt.utils.Constant

/**
 * @author create by zhangwei03
 */
class ChooseRoleDialog constructor(context: Context) : BaseDialog(context) {

    private lateinit var binding: ChooseAiRoleDialogBinding

    private lateinit var mRoleAdapter: RoleSelectAdapter

    private var selectRoleCallback: ((index: Int) -> Unit)? = null

    fun setSelectRoleCallback(callback: ((index: Int) -> Unit)) {
        this.selectRoleCallback = callback
    }

    override fun initContentView() {
        binding = ChooseAiRoleDialogBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun setContentView(view: View) {
        super.setContentView(view)
        window?.let { window ->
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
            selectRoleCallback?.invoke(mRoleAdapter.selectIndex)
            dismiss()
        }
    }

    private fun setupCurrentRoleView(index: Int, selectRole: AIRole) {
        var drawableId = AppUtils.getDrawableRes(context, "ai_avatar_" + (index + 1))
        if (drawableId == 0) drawableId = R.drawable.ai_avatar_1
        binding.ivRoleAvatar.setImageResource(drawableId)
        binding.tvRoleIntroduce.text = when (selectRole.aiRoleName) {
            Constant.ROLE_FOODIE -> context.getString(R.string.role_foodie_tips)
            Constant.ROLE_LATTE_LOVE -> context.getString(R.string.role_latte_love_tips)
            else -> context.getString(R.string.role_foodie_tips)
        }
        binding.tvRoleName.text = when (selectRole.aiRoleName) {
            Constant.ROLE_FOODIE -> context.getString(R.string.role_foodie)
            Constant.ROLE_LATTE_LOVE -> context.getString(R.string.role_latte_love)
            else -> context.getString(R.string.role_foodie)
        }
    }

    fun setupAiRoles(selectIndex: Int, aiRoles: Array<AIRole>) {
        mRoleAdapter = RoleSelectAdapter(context, selectIndex, aiRoles)
        mRoleAdapter.setOnSelectItemClickListener {
            val selectedIndex = mRoleAdapter.selectIndex
            val aiChatRole = aiRoles[selectedIndex]
            setupCurrentRoleView(selectedIndex, aiChatRole)
        }
        binding.recyclerRole.adapter = mRoleAdapter
        val aiChatRole = aiRoles[selectIndex]
        setupCurrentRoleView(selectIndex, aiChatRole)
    }
}
