package com.example.beelocal.model

import com.example.beelocal.model.types.BingoTaskType

data class BingoTask(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: BingoTaskType = BingoTaskType.PHOTO
)
