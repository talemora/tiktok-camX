package X;

import android.content.Context;
import android.os.Handler;

/**
 * Minimal compile-time ABI stub for TikTok's camera backend base class.
 *
 * This contains signatures only.
 * It must NOT be packaged into the runtime bridge.
 */
public abstract class C1MjC {

    protected final Context context;

    public C1MjC(
            Context context,
            C1MjQ cameraCallback,
            Handler handler,
            C03611Mgx cameraConfig
    ) {
        this.context = context;
    }

    public abstract void D3(C1MjR callback, boolean enabled);

    public abstract void J4(float zoom, C1MjR callback);

    public abstract void LIZ(int value);

    public abstract int LJ();

    public abstract int LJFF();

    public abstract int LJIIIIZZ();

    public abstract int LJIIIZ(int value);

    public abstract void s3(C1MiY value);

    public abstract void stopCapture();

    public abstract void t4(int width, int height);

    public void u4() {
        // Compile-time stub only.
        // At runtime this resolves to TikTok's real X.C1MjC.u4().
    }

    public abstract void y4();
}