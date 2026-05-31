package com.pv239.beelocal.ui.screens.bingo

import com.pv239.beelocal.model.BingoCard
import com.pv239.beelocal.model.BingoTask

sealed interface BingoUiState {
    data object Loading : BingoUiState
    data object NoCardAvailable : BingoUiState
    data class Error(val message: String) : BingoUiState
    data class Ready(
        val card: BingoCard,
        val completedTaskIds: Set<String>,
        val completedTaskPhotoUrls: Map<String, String> = emptyMap(), // taskId -> photoUrl
        val submittingTaskId: String? = null,
        val bingoLines: Set<Int> = emptySet(), // indices of winning lines (0-3 rows, 4-7 cols, 8-9 diagonals)
        val showBingoCelebration: Boolean = false,
    ) : BingoUiState {
        /** 4x4 grid of tasks derived from the card's flat task list. */
        val grid: List<List<BingoTask>> get() = card.tasks.chunked(4)

        fun isCompleted(taskId: String) = taskId in completedTaskIds
    }
}
