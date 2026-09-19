package com.sbro.emucorea.core

data class GameMetadata(
    val title: String,
    val serial: String? = null,
    val serialWithCrc: String? = null
)
