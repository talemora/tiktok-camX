package com.talemora.tiktokbridge.tiktokstubs

import android.content.Context
import android.os.Handler

abstract class C1MjC(
    protected val context: Context,
    protected val cameraCallback: C1MjQ?,
    protected val handler: Handler,
    protected val cameraConfig: C03611Mgx?
) {
    abstract fun D3(callback: C1MjR?, enabled: Boolean)

    abstract fun J4(zoom: Float, callback: C1MjR?)

    abstract fun LIZ(value: Int)

    abstract fun LJ(): Int

    abstract fun LJFF(): Int

    abstract fun LJIIIIZZ(): Int

    abstract fun LJIIIZ(value: Int): Int

    abstract fun s3(value: C1MiY?)

    abstract fun stopCapture()

    abstract fun t4(width: Int, height: Int)

    open fun u4() {
    }

    abstract fun y4()
}
