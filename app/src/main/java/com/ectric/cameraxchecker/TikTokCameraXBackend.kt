package com.ectric.cameraxchecker

import android.content.Context
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

class TikTokCameraXBackend(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var outputSurface: Surface? = null

    private val mainExecutor
        get() = ContextCompat.getMainExecutor(context)

    enum class Facing {
        FRONT,
        BACK
    }

fun open(
    facing: Facing = Facing.BACK,
    onReady: () -> Unit = {}
) {
    val providerFuture = ProcessCameraProvider.getInstance(context)

    providerFuture.addListener({
        try {
            cameraProvider = providerFuture.get()

            val selector = when (facing) {
                Facing.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
                Facing.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
            }

            if (!cameraProvider!!.hasCamera(selector)) {
                throw IllegalStateException(
                    "Requested CameraX camera is not available: $facing"
                )
            }

            onReady()
        } catch (e: Exception) {
            throw RuntimeException(
                "TikTokCameraXBackend.open() failed",
                e
            )
        }
    }, mainExecutor)
}
    fun start(surface: Surface, facing: Facing = Facing.BACK) {
        val provider = cameraProvider
            ?: throw IllegalStateException("Call open() before start()")

        outputSurface = surface

        val selector = when (facing) {
            Facing.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            Facing.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
        }

        val newPreview = Preview.Builder()
            .build()
            .also { preview ->
                preview.setSurfaceProvider(mainExecutor) { request ->
                    request.provideSurface(
                        surface,
                        mainExecutor
                    ) { result ->
                        android.util.Log.d(
                            "TikTokCameraXBackend",
                            "Surface finished: ${result.resultCode}"
                        )
                    }
                }
            }

        preview = newPreview

        provider.unbindAll()
        provider.bindToLifecycle(
            lifecycleOwner,
            selector,
            newPreview
        )
    }

    fun stop() {
        preview = null
        outputSurface = null

        cameraProvider?.unbindAll()
    }

    fun close() {
        cameraProvider?.unbindAll()

        preview = null
        outputSurface = null
        cameraProvider = null
    }
}


