package com.example.pricecart.auth

import android.content.Context
import android.util.Patterns
import com.example.pricecart.R

data class AuthValidationResult(
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
) {
    val hasErrors: Boolean
        get() = emailError != null || passwordError != null || confirmPasswordError != null
}

object AuthFormValidator {
    fun validateLoginInput(
        context: Context,
        emailAddress: String,
        password: String,
    ): AuthValidationResult {
        return AuthValidationResult(
            emailError = validateEmailAddress(context, emailAddress),
            passwordError = validatePassword(context, password, allowBlankOnly = false),
        )
    }

    fun validateRegistrationInput(
        context: Context,
        emailAddress: String,
        password: String,
        confirmPassword: String,
    ): AuthValidationResult {
        val passwordError = validatePassword(context, password, allowBlankOnly = false)
        val confirmPasswordError = when {
            confirmPassword.isBlank() -> context.getString(R.string.confirm_password_required_error)
            password != confirmPassword -> context.getString(R.string.password_mismatch_error)
            else -> null
        }

        return AuthValidationResult(
            emailError = validateEmailAddress(context, emailAddress),
            passwordError = passwordError,
            confirmPasswordError = confirmPasswordError,
        )
    }

    private fun validateEmailAddress(context: Context, emailAddress: String): String? {
        return when {
            emailAddress.isBlank() -> context.getString(R.string.email_required_error)
            !Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches() -> {
                context.getString(R.string.email_invalid_error)
            }
            else -> null
        }
    }

    private fun validatePassword(
        context: Context,
        password: String,
        allowBlankOnly: Boolean,
    ): String? {
        return when {
            password.isBlank() -> context.getString(R.string.password_required_error)
            !allowBlankOnly && password.length < 6 -> context.getString(R.string.password_too_short_error)
            else -> null
        }
    }
}
