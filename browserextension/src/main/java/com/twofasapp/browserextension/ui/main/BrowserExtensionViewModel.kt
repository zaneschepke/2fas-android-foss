package com.twofasapp.browserextension.ui.main

import androidx.lifecycle.viewModelScope
import com.twofasapp.base.BaseViewModel
import com.twofasapp.base.dispatcher.Dispatchers
import com.twofasapp.browserextension.domain.FetchPairedBrowsersCase
import com.twofasapp.browserextension.domain.ObserveMobileDeviceCase
import com.twofasapp.browserextension.domain.ObservePairedBrowsersCase
import com.twofasapp.browserextension.domain.UpdateMobileDeviceCase
import com.twofasapp.navigation.SettingsDirections
import com.twofasapp.navigation.SettingsRouter
import com.twofasapp.permissions.CameraPermissionRequestFlow
import com.twofasapp.permissions.PermissionStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal class BrowserExtensionViewModel(
    private val dispatchers: Dispatchers,
    private val settingsRouter: SettingsRouter,
    private val cameraPermissionRequest: CameraPermissionRequestFlow,
    private val observeMobileDeviceCase: ObserveMobileDeviceCase,
    private val observePairedBrowsersCase: ObservePairedBrowsersCase,
    private val updateMobileDeviceCase: UpdateMobileDeviceCase,
    private val fetchPairedBrowsersCase: FetchPairedBrowsersCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(BrowserExtensionUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {

            launch {
                combine(
                    observeMobileDeviceCase().flowOn(dispatchers.io()),
                    observePairedBrowsersCase().flowOn(dispatchers.io()),
                ) { a, b -> Pair(a, b) }.collect { (mobileDevice, pairedBrowsers) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pairedBrowsers = pairedBrowsers,
                            mobileDevice = mobileDevice,
                        )
                    }
                }
            }

            launch(dispatchers.io()) {
                runCatching { fetchPairedBrowsersCase() }
            }
        }
    }

    fun onPairBrowserClick() {
        cameraPermissionRequest.execute()
            .take(1)
            .onEach {
                when (it) {
                    PermissionStatus.GRANTED -> settingsRouter.navigate(SettingsDirections.PairingScan)
                    PermissionStatus.DENIED -> Unit
                    PermissionStatus.DENIED_NEVER_ASK -> _uiState.update { state ->
                        state.copy(showRationaleDialog = true)
                    }
                }
            }.launchIn(viewModelScope)
    }

    fun onRationaleDialogDismiss() {
        _uiState.update { it.copy(showRationaleDialog = false) }
    }

    fun onEditDeviceClick() {
        _uiState.update { it.copy(showEditDeviceDialog = true) }
    }

    fun onEditDeviceDialogDismiss(newName: String? = null) {
        if (newName != null) {
            viewModelScope.launch(dispatchers.io()) {
                try {
                    updateMobileDeviceCase(newName)
                } catch (e: Exception) {
                    _uiState.update {
                        it.postEvent(BrowserExtensionUiState.Event.ShowSnackbarError("Network error. Please try again."))
                    }
                }
            }
        }

        _uiState.update { it.copy(showEditDeviceDialog = false) }
    }

    fun eventHandled(id: String) {
        _uiState.update { it.reduceEvent(id) }
    }
}
