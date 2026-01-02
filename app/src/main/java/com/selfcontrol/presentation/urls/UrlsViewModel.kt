package com.selfcontrol.presentation.urls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfcontrol.domain.repository.UrlRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class UrlsViewModel @Inject constructor(
    private val urlRepository: UrlRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UrlsState())
    val uiState: StateFlow<UrlsState> = _uiState.asStateFlow()

    init {
        loadUrls()
    }

    private fun loadUrls() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            urlRepository.observeBlockedUrls()
                .catch { e ->
                    Timber.e(e, "[UrlsViewModel] Error loading URLs")
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { urls ->
                    _uiState.update { it.copy(urls = urls, isLoading = false) }
                }
        }
    }
}
