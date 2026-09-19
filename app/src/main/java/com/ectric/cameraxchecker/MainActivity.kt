package com.ectric.cameraxchecker

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ectric.cameraxchecker.ui.theme.CameraXCheckerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            CameraXCheckerTheme {
                CameraScreen(cameraExecutor)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}


private class TextureViewSurfaceProvider(
    private val textureView: TextureView,
    private val executor: Executor
) : Preview.SurfaceProvider, TextureView.SurfaceTextureListener {

    private var pendingRequest: SurfaceRequest? = null

    private var activeRequest: SurfaceRequest? = null

    private var activeSurface: Surface? = null

    init {
        textureView.surfaceTextureListener = this
    }

    override fun onSurfaceRequested(request: SurfaceRequest) {

        Log.d(
            TAG,
            "CameraX requested surface: " +
                    "${request.resolution.width}x${request.resolution.height}"
        )

        pendingRequest?.let { oldRequest ->
            if (oldRequest !== request) {
                Log.d(TAG, "Replacing pending SurfaceRequest")
                oldRequest.willNotProvideSurface()
            }
        }

        pendingRequest = request

        tryProvideSurface()
    }


    private fun tryProvideSurface() {

        if (activeSurface != null) {
            Log.d(TAG, "Waiting for previous surface to be released")
            return
        }

        val request = pendingRequest ?: return

        if (!textureView.isAvailable) {
            Log.d(TAG, "TextureView isn't available yet")
            return
        }

        val surfaceTexture = textureView.surfaceTexture ?: run {
            Log.d(TAG, "SurfaceTexture is null")
            return
        }

        surfaceTexture.setDefaultBufferSize(
            request.resolution.width,
            request.resolution.height
        )

        val surface = Surface(surfaceTexture)

        pendingRequest = null
        activeRequest = request
        activeSurface = surface

        Log.d(
            TAG,
            "Providing OUR Surface to CameraX: " +
                    "${request.resolution.width}x${request.resolution.height}"
        )

        request.provideSurface(
            surface,
            executor
        ) { result ->

            Log.d(
                TAG,
                "CameraX released our Surface. resultCode=${result.resultCode}"
            )

            surface.release()

            if (activeRequest === request) {
                activeRequest = null
                activeSurface = null
            }

            tryProvideSurface()
        }
    }


    override fun onSurfaceTextureAvailable(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int
    ) {
        Log.d(
            TAG,
            "TextureView SurfaceTexture available: ${width}x${height}"
        )

        tryProvideSurface()
    }


    override fun onSurfaceTextureSizeChanged(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int
    ) {
        Log.d(
            TAG,
            "TextureView size changed: ${width}x${height}"
        )
    }


    override fun onSurfaceTextureDestroyed(
        surfaceTexture: SurfaceTexture
    ): Boolean {

        Log.d(TAG, "TextureView SurfaceTexture destroyed")

        pendingRequest?.willNotProvideSurface()
        pendingRequest = null

        return true
    }


    override fun onSurfaceTextureUpdated(
        surfaceTexture: SurfaceTexture
    ) {
    }


    companion object {
        private const val TAG = "CustomCameraXSurface"
    }
}


@Composable
fun CameraScreen(
    cameraExecutor: ExecutorService,
    viewModel: CameraViewModel = viewModel()
) {

    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }


    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->

            hasCameraPermission =
                permissions[Manifest.permission.CAMERA] == true &&
                        permissions[Manifest.permission.RECORD_AUDIO] == true
        }
    )


    LaunchedEffect(Unit) {

        if (!hasCameraPermission) {

            launcher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }


    var cameraProvider by remember {
        mutableStateOf<ProcessCameraProvider?>(null)
    }

    var extensionsManager by remember {
        mutableStateOf<ExtensionsManager?>(null)
    }


    LaunchedEffect(Unit) {

        try {

            val provider =
                ProcessCameraProvider
                    .getInstance(context)
                    .await()

            val extensions = try {

                ExtensionsManager
                    .getInstanceAsync(
                        context,
                        provider
                    )
                    .await()

            } catch (e: Exception) {

                Log.e(
                    "CameraX",
                    "Extensions initialization failed",
                    e
                )

                null
            }

            cameraProvider = provider
            extensionsManager = extensions

        } catch (e: Exception) {

            Log.e(
                "CameraX",
                "Camera initialization failed",
                e
            )
        }
    }


    if (hasCameraPermission) {

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            if (cameraProvider != null) {

                CameraPreview(
                    cameraProvider = cameraProvider!!,
                    extensionsManager = extensionsManager,
                    lensFacing = uiState.lensFacing,
                    extensionMode = uiState.extensionMode,
                    is10BitHdrEnabled = uiState.is10BitHdrEnabled,
                    isRecording = uiState.isRecording,
                    zoomRatio = uiState.zoomRatio,
                    cameraExecutor = cameraExecutor,

                    onRecordingStarted = {
                        viewModel.setRecording(true)
                    },

                    onRecordingStopped = {
                        viewModel.setRecording(false)
                    },

                    onBindingComplete = {
                        viewModel.setCameraInitializing(false)
                    },

                    onZoomLimitsDetected = { min, max ->
                        viewModel.updateZoomLimits(min, max)
                    }
                )
            }


            if (
                uiState.isCameraInitializing ||
                cameraProvider == null
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(alpha = 0.5f)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    CircularProgressIndicator(
                        color = Color.White
                    )
                }
            }


            val zoomLevels = remember(
                uiState.minZoom,
                uiState.maxZoom
            ) {

                val levels = mutableListOf<Float>()

                if (uiState.minZoom < 1.0f) {
                    levels.add(uiState.minZoom)
                }

                levels.add(1.0f)

                if (uiState.maxZoom >= 2.0f) {
                    levels.add(2.0f)
                }

                if (uiState.maxZoom >= 5.0f) {
                    levels.add(5.0f)
                }

                levels
                    .distinct()
                    .sorted()
            }


            if (zoomLevels.size > 1) {

                LazyRow(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp)
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(8.dp),

                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    items(zoomLevels) { ratio ->

                        val isSelected =
                            uiState.zoomRatio == ratio

                        Button(
                            onClick = {
                                viewModel.setZoom(ratio)
                            },

                            enabled =
                                !uiState.isCameraInitializing &&
                                        !uiState.isRecording,

                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor =
                                        if (isSelected)
                                            Color.White
                                        else
                                            Color.Gray.copy(alpha = 0.5f),

                                    contentColor =
                                        if (isSelected)
                                            Color.Black
                                        else
                                            Color.White
                                ),

                            shape = CircleShape,

                            contentPadding =
                                PaddingValues(
                                    horizontal = 12.dp
                                ),

                            modifier =
                                Modifier.height(36.dp)
                        ) {

                            Text(
                                text =
                                    "${"%.1f".format(Locale.US, ratio)}x",

                                style =
                                    MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }


            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),

                horizontalArrangement =
                    Arrangement.SpaceEvenly,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {


                IconButton(
                    onClick = {
                        viewModel.toggleLensFacing()
                    },

                    enabled =
                        !uiState.isCameraInitializing &&
                                !uiState.isRecording,

                    modifier =
                        Modifier.background(
                            Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {

                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Switch",
                        tint = Color.White
                    )
                }


                FloatingActionButton(
                    onClick = {

                        if (!uiState.isCameraInitializing) {

                            viewModel.setRecording(
                                !uiState.isRecording
                            )
                        }
                    },

                    shape = CircleShape,

                    containerColor =
                        if (uiState.isRecording)
                            Color.Red
                        else if (uiState.isCameraInitializing)
                            Color.Gray
                        else
                            Color.White
                ) {

                    Icon(
                        if (uiState.isRecording)
                            Icons.Default.Stop
                        else
                            Icons.Default.Videocam,

                        contentDescription = "Record"
                    )
                }


                var showSettings by remember {
                    mutableStateOf(false)
                }


                IconButton(
                    onClick = {
                        showSettings = true
                    },

                    enabled =
                        !uiState.isCameraInitializing &&
                                !uiState.isRecording,

                    modifier =
                        Modifier.background(
                            Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {

                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }


                if (showSettings) {

                    SettingsDialog(
                        uiState = uiState,

                        onDismiss = {
                            showSettings = false
                        },

                        on10BitHdrToggle = {
                            viewModel.set10BitHdr(it)
                        },

                        onExtensionModeChange = {
                            viewModel.setExtensionMode(it)
                        }
                    )
                }
            }
        }

    } else {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Text("Permissions required")
        }
    }
}


@Composable
fun CameraPreview(
    cameraProvider: ProcessCameraProvider,
    extensionsManager: ExtensionsManager?,
    lensFacing: Int,
    extensionMode: Int,
    is10BitHdrEnabled: Boolean,
    isRecording: Boolean,
    zoomRatio: Float,
    cameraExecutor: ExecutorService,
    onRecordingStarted: () -> Unit,
    onRecordingStopped: () -> Unit,
    onBindingComplete: () -> Unit,
    onZoomLimitsDetected: (Float, Float) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val textureView = remember {
        TextureView(context)
    }

    val tikTokBackend = remember(context, lifecycleOwner) {
        TikTokCameraXBackend(
            context = context,
            lifecycleOwner = lifecycleOwner
        )
    }

    var backendSurface by remember {
        mutableStateOf<Surface?>(null)
    }

    DisposableEffect(textureView, tikTokBackend) {
        val listener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                backendSurface?.release()
                backendSurface = Surface(surfaceTexture)
                Log.d(
                    "TikTokCameraXBackend",
                    "TextureView surface available: ${width}x${height}"
                )
            }

            override fun onSurfaceTextureSizeChanged(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                Log.d(
                    "TikTokCameraXBackend",
                    "TextureView surface changed: ${width}x${height}"
                )
            }

            override fun onSurfaceTextureDestroyed(
                surfaceTexture: SurfaceTexture
            ): Boolean {
                tikTokBackend.stop()
                backendSurface?.release()
                backendSurface = null
                return true
            }

            override fun onSurfaceTextureUpdated(
                surfaceTexture: SurfaceTexture
            ) = Unit
        }

        textureView.surfaceTextureListener = listener

        onDispose {
            textureView.surfaceTextureListener = null
            tikTokBackend.close()
            backendSurface?.release()
            backendSurface = null
        }
    }

    LaunchedEffect(backendSurface, lensFacing) {
        val surface = backendSurface ?: return@LaunchedEffect

        val facing =
            if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                TikTokCameraXBackend.Facing.FRONT
            } else {
                TikTokCameraXBackend.Facing.BACK
            }

        tikTokBackend.open(facing) {
            tikTokBackend.start(
                surface = surface,
                facing = facing
            )

            onZoomLimitsDetected(1.0f, 1.0f)
            onBindingComplete()

            Log.d(
                "TikTokCameraXBackend",
                "Backend started with external Surface, facing=$facing"
            )
        }
    }

    AndroidView(
        factory = { textureView },
        modifier = Modifier.fillMaxSize()
    )
}


@Composable
fun SettingsDialog(
    uiState: CameraUiState,
    onDismiss: () -> Unit,
    on10BitHdrToggle: (Boolean) -> Unit,
    onExtensionModeChange: (Int) -> Unit
) {

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text("Settings")
        },

        text = {

            Column {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        "10-bit HDR Video"
                    )


                    Spacer(
                        Modifier.weight(1f)
                    )


                    Switch(
                        checked =
                            uiState.is10BitHdrEnabled,

                        onCheckedChange =
                            on10BitHdrToggle
                    )
                }


                Spacer(
                    Modifier.height(8.dp)
                )


                Text(
                    "Extensions",

                    style =
                        MaterialTheme.typography.titleSmall
                )


                listOf(
                    "None" to ExtensionMode.NONE,
                    "Night" to ExtensionMode.NIGHT,
                    "HDR" to ExtensionMode.HDR,
                    "Bokeh" to ExtensionMode.BOKEH
                )
                    .forEach { (name, mode) ->

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            RadioButton(
                                selected =
                                    uiState.extensionMode == mode,

                                onClick = {
                                    onExtensionModeChange(mode)
                                }
                            )


                            Text(name)
                        }
                    }
            }
        },

        confirmButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text("Close")
            }
        }
    )
}