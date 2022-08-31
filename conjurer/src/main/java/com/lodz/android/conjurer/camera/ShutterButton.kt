package com.lodz.android.conjurer.camera

import android.content.Context
import android.util.AttributeSet
import android.view.SoundEffectConstants
import androidx.appcompat.widget.AppCompatImageView

/**
 * @author zhouL
 * @date 2022/8/8
 */
class ShutterButton : AppCompatImageView {

    private var mListener: OnShutterButtonListener? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        if (isPressed){
            onActionDownFocus(isPressed)
        }
    }

    private fun onActionDownFocus(isPressed: Boolean) {
        mListener?.onActionDownFocus(this, isPressed)
    }

    override fun performClick(): Boolean {
        playSoundEffect(SoundEffectConstants.CLICK)
        mListener?.onActionUpClick(this)
        return super.performClick()
    }

    /** 设置监听器 */
    fun setOnShutterButtonListener(listener: OnShutterButtonListener) {
        mListener = listener
    }

    interface OnShutterButtonListener {
        /** 长按快门进行聚焦 */
        fun onActionDownFocus(btn: ShutterButton, pressed: Boolean)
        /** 松开快门拍照 */
        fun onActionUpClick(btn: ShutterButton)
    }
}