package com.lodz.android.conjurer.camera

import android.content.Context
import android.hardware.Camera
import android.widget.Toast
import java.util.*


/**
 * 自动对焦管理
 * @author zhouL
 * @date 2022/8/5
 */
class AutoFocusManager(private val camera: Camera) {

    /** 自动对焦间隔（毫秒） */
    private val AUTO_FOCUS_INTERVAL_MS = 1000L
    /** 聚焦类型 */
    private val FOCUS_MODES = arrayListOf(Camera.Parameters.FOCUS_MODE_AUTO, Camera.Parameters.FOCUS_MODE_MACRO)

    /** 是否对焦重 */
    private var isActive = false
    /** 是否使用手动对焦 */
    private var isUseManual = false
    /** 是否使用自动对焦 */
    private var isUseAutoFocus = false
    /** 定时器 */
    private val mTimer = Timer(true)
    /** 定时器任务 */
    private var mFocusTask: TimerTask? = null

    init {
        val focusMode = camera.parameters.focusMode
        isUseAutoFocus = FOCUS_MODES.contains(focusMode)
        isUseManual = false
        checkAndStart()
    }

    /** 检测并启动对焦 */
    private fun checkAndStart() {
        if (isUseAutoFocus) {
            isActive = true
            start()
        }
    }

    /** 自动对焦 */
    fun start() {
        try {
            camera.autoFocus { success, camera ->
                if (isActive && !isUseManual) {
                    mFocusTask = object : TimerTask() {
                        override fun run() {
                            checkAndStart()
                        }
                    }
                    mTimer.schedule(mFocusTask, AUTO_FOCUS_INTERVAL_MS)
                }
                isUseManual = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isActive = false
        }
    }

    /** 手动对焦，延迟时间[delay] */
    fun start(delay: Long) {
        mFocusTask = object : TimerTask() {
            override fun run() {
                isUseManual = true
                start()
            }
        }
        mTimer.schedule(mFocusTask, delay)
    }

    fun stop() {
        if (isUseAutoFocus) {
            camera.cancelAutoFocus()
        }
        if (mFocusTask != null) {
            mFocusTask?.cancel()
            mFocusTask = null
        }
        isActive = false
        isUseManual = false
    }
}
