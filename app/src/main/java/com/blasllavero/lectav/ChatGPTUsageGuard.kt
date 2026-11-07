/*
 * LECTAV — backward-compatible bridge for legacy local usage preferences.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * LECTAV 0.9.1 no tracks or enforces an arbitrary number of local requests.
 * Only OpenAI controls the account's actual plan usage and access.
 * Kept for compatibility with previously exported encrypted .lectavbak files.
 */
internal data class LocalUsageSnapshot(
    val weeklyLimit: Int = 0,
    val attempts: Int = 0,
    val weekStart: String = ""
) {
    val remaining: Int get() = Int.MAX_VALUE // Legacy API; never shown as real quota.
    val blocked: Boolean get() = false
}

internal class ChatGPTUsageGuard(@Suppress("UNUSED_PARAMETER") context: Context) {
    private val mutable = MutableStateFlow(LocalUsageSnapshot())
    val state = mutable.asStateFlow()

    fun refresh() = Unit

    @Suppress("UNUSED_PARAMETER")
    fun setWeeklyLimit(value: Int): Boolean = false

    // A restored backup from LECTAV <= 0.9.0 can contain 0..30 local attempts.
    // They are deliberately ignored so restoring never re-enables a local lock.
    @Suppress("UNUSED_PARAMETER")
    fun restoreBackupLimit(limit: Int, attempts: Int, backupWeek: String) = Unit

    // Older generators still call this function at the request boundary.
    // It never blocks and never records a request.
    fun reserveAttempt(): Boolean = true
}
