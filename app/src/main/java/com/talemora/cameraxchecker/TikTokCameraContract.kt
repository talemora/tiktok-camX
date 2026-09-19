package com.talemora.cameraxchecker

import android.view.Surface

/**
 * Clean representation of the TikTok camera operations
 * discovered in TECamera.
 *
 * This interface contains no proprietary TikTok classes.
 */
interface TikTokCameraContract {

    enum class Facing {
        FRONT,
        BACK
    }

    /**
     * TikTok mapping:
     * C1MjC.R4(...)
     */
    fun open(
        facing: Facing,
        onReady: () -> Unit = {}
    )

    /**
     * TikTok provider mapping:
     * C1MjP.LIZ() -> Surface
     */
    fun setOutputSurface(surface: Surface)

    /**
     * TikTok mapping:
     * C1MjC.u4()
     */
    fun startCapture()

    /**
     * TikTok mapping:
     * C1MjC.J4(float, ...)
     */
    fun setZoom(ratio: Float)

    /**
     * TikTok mapping:
     * C1MjC.stopCapture()
     */
    fun stopCapture()

    /**
     * Backend cleanup.
     */
    fun close()
}