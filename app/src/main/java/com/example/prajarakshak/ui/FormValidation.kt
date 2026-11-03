package com.example.prajarakshak.ui

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

internal fun emailValidationError(value: String): String? =
    when {
        value.isBlank() -> "Email is required."
        !android.util.Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches() ->
            "Enter a valid email address."
        else -> null
    }

internal fun phoneValidationError(value: String): String? {
    if (value.isBlank()) return "Mobile number is required."
    val normalized = value.filterNot { it.isWhitespace() || it == '-' || it == '(' || it == ')' }
    return if (normalized.matches(Regex("^(?:\\+?91)?[6-9]\\d{9}$"))) {
        null
    } else {
        "Enter a valid 10-digit mobile number."
    }
}

internal fun reportDateValidationError(value: String): String? {
    if (value.isBlank()) return null
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    val position = ParsePosition(0)
    val valid = value.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) &&
        formatter.parse(value, position) != null &&
        position.index == value.length
    return if (valid) null else "Use YYYY-MM-DD, for example 2026-11-03."
}

internal fun passwordValidationError(value: String, requireStrong: Boolean): String? =
    when {
        value.isBlank() -> "Password is required."
        requireStrong && value.length < 8 -> "Use at least 8 characters."
        else -> null
    }

internal fun requiredFieldError(value: String, label: String): String? =
    if (value.isBlank()) "$label is required." else null
