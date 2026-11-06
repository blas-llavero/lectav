/*
 * LECTAV — voluntary local request cap. Does NOT read ChatGPT plan balances.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

internal data class LocalUsageSnapshot(
    val weeklyLimit: Int,
    val attempts: Int,
    val weekStart: String
) {
    val remaining get() = (weeklyLimit - attempts).coerceAtLeast(0)
    val blocked get() = remaining == 0
}

internal class ChatGPTUsageGuard(context: Context) {
    // Per-installation safety limit, independent of OpenAI's real quotas.
    // All accounts on this installation share the same conservative cap.
    private val prefs = context.applicationContext.getSharedPreferences(
        "lectav_chatgpt_request_cap", Context.MODE_PRIVATE
    )
    private val mutable = MutableStateFlow(snapshot())
    val state = mutable.asStateFlow()

    private fun weekStart(): String = LocalDate.now()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()

    @Synchronized
    private fun snapshot(): LocalUsageSnapshot {
        val week = weekStart()
        val previousWeek = prefs.getString("week", null)
        return LocalUsageSnapshot(
            weeklyLimit = prefs.getInt("limit", 3).coerceIn(0, 30),
            attempts = if (previousWeek == week) prefs.getInt("attempts", 0)
                .coerceAtLeast(0) else 0,
            weekStart = week
        )
    }

    @Synchronized
    fun refresh() {
        mutable.value = snapshot()
    }

    @Synchronized
    fun setWeeklyLimit(value: Int): Boolean {
        val valid = value.coerceIn(0, 30)
        val ok = prefs.edit().putInt("limit", valid).commit()
        refresh()
        return ok
    }

    @Synchronized
    fun reserveAttempt(): Boolean {
        val now = snapshot()
        if (now.blocked) {
            mutable.value = now
            return false
        }
        // Reserve before any network request, even if it later fails. Count
        // attempts, NOT tokens, money or billable use; never invent a balance.
        val persisted = prefs.edit()
            .putString("week", now.weekStart)
            .putInt("attempts", now.attempts + 1)
            .commit()
        mutable.value = snapshot()
        return persisted
    }
}
