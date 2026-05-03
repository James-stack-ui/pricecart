package com.example.pricecart.data

import com.example.pricecart.data.model.AuthUser

interface AuthClient {
    fun currentUser(): AuthUser?

    fun signIn(
        emailAddress: String,
        password: String,
        onSuccess: (AuthUser) -> Unit,
        onError: (Exception) -> Unit,
    )

    fun register(
        emailAddress: String,
        password: String,
        onSuccess: (AuthUser) -> Unit,
        onError: (Exception) -> Unit,
    )

    fun deleteCurrentUser(
        onComplete: () -> Unit,
        onError: (Exception) -> Unit,
    )

    fun signOut()
}
