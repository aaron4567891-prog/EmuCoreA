// SPDX-FileCopyrightText: 2026 EmuCoreA contributors
// SPDX-License-Identifier: GPL-3.0+

package com.sbro.emucorea.network

enum class MultiplayerSessionError {
    InvalidRoomCode,
    RoomNotFound,
    RoomFull,
    ConnectionFailed
}

internal fun Throwable.toMultiplayerSessionError(): MultiplayerSessionError = when (this) {
    is SignalingException -> when {
        status == 404 -> MultiplayerSessionError.RoomNotFound
        status == 409 && code == "room_full" -> MultiplayerSessionError.RoomFull
        else -> MultiplayerSessionError.ConnectionFailed
    }
    else -> MultiplayerSessionError.ConnectionFailed
}
