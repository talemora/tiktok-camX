package com.talemora.cameraxchecker

import android.content.Context
import android.view.Surface

class TikTokCameraAdapter(
    context: Context
) : TikTokCameraContract {

    private val backend = TikTokCameraXBackend(context)

    private var outputSurface: Surface? = null

    private var facing =
        TikTokCameraXBackend.Facing.BACK

    override fun open(
        facing: TikTokCameraContract.Facing,
        onReady: () -> Unit
    ) {
        this.facing = when (facing) {
            TikTokCameraContract.Facing.FRONT ->
                TikTokCameraXBackend.Facing.FRONT

            TikTokCameraContract.Facing.BACK ->
                TikTokCameraXBackend.Facing.BACK
        }

        backend.open(
            facing = this.facing,
            onReady = onReady
        )
    }

    override fun setOutputSurface(surface: Surface) {
        outputSurface = surface
    }

    override fun startCapture() {
        val surface = outputSurface
            ?: throw IllegalStateException(
                "Output Surface must be set before startCapture()"
            )

        backend.startCapture(
            surface = surface,
            facing = facing
        )
    }

    override fun setZoom(ratio: Float) {
        backend.setZoom(ratio)
    }

    fun getZoomRange(): Pair<Float, Float>? {
        return backend.getZoomRange()
    }

    override fun stopCapture() {
        backend.stopCapture()
    }

    override fun close() {
        backend.close()
        outputSurface = null
    }
}