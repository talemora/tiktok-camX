package com.talemora.cameraxchecker

import android.content.Context
import android.util.Log
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

private class CameraBackendLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = registry

    fun start() {
        registry.currentState = Lifecycle.State.STARTED
    }

    fun stop() {
        registry.currentState = Lifecycle.State.CREATED
    }

    fun destroy() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}

class TikTokCameraXBackend(
    private val context: Context
) {
    private val lifecycleOwner = CameraBackendLifecycleOwner()

    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var outputSurface: Surface? = null
    private var camera: Camera? = null

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
                    Facing.FRONT ->
                        CameraSelector.DEFAULT_FRONT_CAMERA

                    Facing.BACK ->
                        CameraSelector.DEFAULT_BACK_CAMERA
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

    fun startCapture(
        surface: Surface,
        facing: Facing = Facing.BACK
    ) {
        val provider = cameraProvider
            ?: throw IllegalStateException(
                "Call open() before startCapture()"
            )

        outputSurface = surface

        val selector = when (facing) {
            Facing.FRONT ->
                CameraSelector.DEFAULT_FRONT_CAMERA

            Facing.BACK ->
                CameraSelector.DEFAULT_BACK_CAMERA
        }

        val newPreview = Preview.Builder()
            .build()
            .also { preview ->
                preview.setSurfaceProvider(mainExecutor) { request ->
                    request.provideSurface(
                        surface,
                        mainExecutor
                    ) { result ->
                        Log.d(
                            "TikTokCameraXBackend",
                            "Surface finished: ${result.resultCode}"
                        )
                    }
                }
            }

        preview = newPreview

        provider.unbindAll()

        lifecycleOwner.start()

        camera = provider.bindToLifecycle(
            lifecycleOwner,
            selector,
            newPreview
        )
    }

    fun setZoom(ratio: Float) {
        val currentCamera = camera ?: return

        val zoomState =
            currentCamera.cameraInfo.zoomState.value ?: return

        val clampedRatio = ratio.coerceIn(
            zoomState.minZoomRatio,
            zoomState.maxZoomRatio
        )

        currentCamera.cameraControl.setZoomRatio(
            clampedRatio
        )
    }

    fun getZoomRange(): Pair<Float, Float>? {
        val zoomState =
            camera?.cameraInfo?.zoomState?.value ?: return null

        return Pair(
            zoomState.minZoomRatio,
            zoomState.maxZoomRatio
        )
    }

    fun stopCapture() {
        cameraProvider?.unbindAll()
        lifecycleOwner.stop()

        camera = null
        preview = null
        outputSurface = null
    }

    fun close() {
        cameraProvider?.unbindAll()
        lifecycleOwner.destroy()

        camera = null
        preview = null
        outputSurface = null
        cameraProvider = null
    }
}