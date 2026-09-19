package com.talemora.cameraxchecker

import android.content.Context
import android.view.Surface

/**
 * Bridge between TikTok's camera contract and our CameraX backend.
 *
 * For now this is tested inside the checker.
 * Later the TikTok-specific C1MjC adapter will call this class.
 */
class TikTokCameraAdapter(
    context: Context
) {
    private val backend = TikTokCameraXBackend(context)

    private var outputSurface: Surface? = null

    private var facing =
        TikTokCameraXBackend.Facing.BACK

    /**
     * Future TikTok mapping:
     *
     * C1MjC.R4(...) -> open(...)
     */
    fun open(
        facing: TikTokCameraXBackend.Facing =
            TikTokCameraXBackend.Facing.BACK,
        onReady: () -> Unit = {}
    ) {
        this.facing = facing

        backend.open(
            facing = facing,
            onReady = onReady
        )
    }

    /**
     * Later this Surface will come from:
     *
     * C1MjP.LIZ()
     */
    fun setOutputSurface(surface: Surface) {
        outputSurface = surface
    }

    /**
     * Future TikTok mapping:
     *
     * C1MjC.u4() -> startCapture()
     */
    fun startCapture() {
        val surface = outputSurface
            ?: throw IllegalStateException(
                "Output Surface must be set before startCapture()"
            )

        backend.startCapture(
            surface = surface,
            facing = facing
        )
    }

    /**
     * Future TikTok mapping:
     *
     * C1MjC.J4(float, ...) -> setZoom(...)
     */
    fun setZoom(ratio: Float) {
        backend.setZoom(ratio)
    }

    fun getZoomRange(): Pair<Float, Float>? {
        return backend.getZoomRange()
    }

    /**
     * Future TikTok mapping:
     *
     * C1MjC.stopCapture()
     */
    fun stopCapture() {
        backend.stopCapture()
    }

    /**
     * Final backend cleanup.
     */
    fun close() {
        backend.close()
        outputSurface = null
    }
}