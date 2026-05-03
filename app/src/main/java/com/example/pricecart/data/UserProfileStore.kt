package com.example.pricecart.data

interface UserProfileStore {
    fun createUserProfile(
        userId: String,
        emailAddress: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    )

    fun recordUserSignIn(
        userId: String,
        emailAddress: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        createUserProfile(
            userId = userId,
            emailAddress = emailAddress,
            onSuccess = onSuccess,
            onError = onError,
        )
    }
}
