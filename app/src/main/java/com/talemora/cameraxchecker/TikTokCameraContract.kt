package com.talemora.cameraxchecker

import android.view.Surface

interface TikTokCameraContract {

    enum class Facing {
        FRONT,
        BACK
    }

    fun open(
        facing: Facing,
        onReady: () -> Unit = {}
    )

    fun setOutputSurface(surface: Surface)

    fun startCapture()

    fun setZoom(ratio: Float)

    fun stopCapture()

    fun close()
}
