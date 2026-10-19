package com.quangthanh.xiangqi.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quangthanh.xiangqi.application.dto.GameHistory
import com.quangthanh.xiangqi.application.usecase.GetGameHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(private val getGameHistory: GetGameHistoryUseCase) : ViewModel() {

    /** null khi đang tải. */
    private val _history = MutableStateFlow<GameHistory?>(null)
    val history: StateFlow<GameHistory?> = _history.asStateFlow()

    fun refresh() {
        viewModelScope.launch { _history.value = getGameHistory() }
    }
}
