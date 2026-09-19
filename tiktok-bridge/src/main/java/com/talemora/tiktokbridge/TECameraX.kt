package com.talemora.tiktokbridge

import android.content.Context
import android.os.Handler
import android.view.Surface
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import X.C03611Mgx
import X.C1MiY
import X.C1MjC
import X.C1MjQ
import X.C1MjR
import androidx.camera.core.CameraSelector

class TECameraX(
    context: Context,
    cameraCallback: C1MjQ?,
    handler: Handler,
    cameraConfig: C03611Mgx?
) : C1MjC(
    context,
    cameraCallback,
    handler,
    cameraConfig
) {

    private class CameraLifecycleOwner : LifecycleOwner {

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

    private val lifecycleOwner = CameraLifecycleOwner()

    private var cameraProvider: ProcessCameraProvider? = null

    private var outputSurface: Surface? = null

    fun setOutputSurface(surface: Surface) {
        outputSurface = surface
    }

    private fun createPreview(): Preview {
        val preview = Preview.Builder()
            .build()

        preview.setSurfaceProvider { request ->
            val surface = outputSurface

            if (surface == null || !surface.isValid) {
                request.willNotProvideSurface()
                return@setSurfaceProvider
            }

            request.provideSurface(
                surface,
                ContextCompat.getMainExecutor(context)
            ) {
            }
        }

        return preview
    }

    override fun D3(callback: C1MjR?, enabled: Boolean) {
    }

    override fun J4(zoom: Float, callback: C1MjR?) {
    }

    override fun LIZ(value: Int) {
    }

    override fun LJ(): Int = 3

    override fun LJFF(): Int = 0

    override fun LJIIIIZZ(): Int = 0

    override fun LJIIIZ(value: Int): Int = 0

    override fun s3(value: C1MiY?) {
    }

    override fun stopCapture() {
        lifecycleOwner.stop()
        cameraProvider?.unbindAll()
    }

    override fun t4(width: Int, height: Int) {
    }

    override fun u4() {
        super.u4()

        lifecycleOwner.start()

        val providerFuture =
            ProcessCameraProvider.getInstance(context)

        providerFuture.addListener(
            {
                val provider = providerFuture.get()
                cameraProvider = provider

                val preview = createPreview()

                provider.unbindAll()

                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview
                )
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    override fun y4() {
    }
}
