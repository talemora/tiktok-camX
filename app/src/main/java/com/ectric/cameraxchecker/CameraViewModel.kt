package com.ectric.cameraxchecker

import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.extensions.ExtensionMode
import androidx.camera.video.Quality
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CameraUiState(
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val extensionMode: Int = ExtensionMode.NONE,
    val is10BitHdrEnabled: Boolean = false,
    val isRecording: Boolean = false,
    val isCameraInitializing: Boolean = false,
    val zoomRatio: Float = 1.0f,
    val minZoom: Float = 1.0f,
    val maxZoom: Float = 1.0f
)

class CameraViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun toggleLensFacing() {
        if (_uiState.value.isCameraInitializing || _uiState.value.isRecording) return
        
        val nextFacing = if (_uiState.value.lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        _uiState.update {
            it.copy(
                lensFacing = nextFacing,
                isCameraInitializing = true,
                zoomRatio = 1.0f // Reset zoom on flip
            )
        }
    }

    fun setZoom(ratio: Float) {
        _uiState.update { it.copy(zoomRatio = ratio) }
    }

    fun updateZoomLimits(min: Float, max: Float) {
        _uiState.update { it.copy(minZoom = min, maxZoom = max) }
    }

    fun setExtensionMode(mode: Int) {
        if (_uiState.value.isCameraInitializing || _uiState.value.isRecording) return
        _uiState.update { it.copy(extensionMode = mode, isCameraInitializing = true) }
    }

    fun setRecording(isRecording: Boolean) {
        _uiState.update { it.copy(isRecording = isRecording) }
    }

    fun set10BitHdr(enabled: Boolean) {
        if (_uiState.value.isCameraInitializing || _uiState.value.isRecording) return
        _uiState.update { it.copy(is10BitHdrEnabled = enabled, isCameraInitializing = true) }
    }
    
    fun setCameraInitializing(initializing: Boolean) {
        _uiState.update { it.copy(isCameraInitializing = initializing) }
    }
}
