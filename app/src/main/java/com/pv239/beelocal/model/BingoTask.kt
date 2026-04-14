package com.pv239.beelocal.model

import com.pv239.beelocal.model.types.BingoTaskType

data class BingoTask(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: BingoTaskType = BingoTaskType.PHOTO
)
