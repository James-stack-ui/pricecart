package com.example.pricecart.data

class AuthRepository(
    private val authClient: AuthClient = ServiceLocator.authClientFactory(),
    private val userProfileStore: UserProfileStore = ServiceLocator.userProfileStoreFactory(),
) {
    fun isUserLoggedIn(): Boolean {
        return authClient.currentUser() != null
    }

    fun currentUserId(): String? {
        return authClient.currentUser()?.uid
    }

    fun currentUserEmail(): String? {
        return authClient.currentUser()?.email
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun signInWithEmail(
        emailAddress: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val trimmedEmailAddress = emailAddress.trim()
        if (trimmedEmailAddress.isBlank() || password.length < 6) {
            onError(IllegalArgumentException("Enter a valid email and a password with at least 6 characters."))
            return
        }

        authClient.signIn(
            emailAddress = trimmedEmailAddress,
            password = password,
            onSuccess = { authUser ->
                val profileEmail = authUser.email?.trim().takeUnless { it.isNullOrBlank() } ?: trimmedEmailAddress
                userProfileStore.recordUserSignIn(
                    userId = authUser.uid,
                    emailAddress = profileEmail,
                    onSuccess = onSuccess,
                    onError = { profileException ->
                        logProfileSetupFailure(
                            message = "Failed to update Firestore profile for signed-in user ${authUser.uid}",
                            exception = profileException,
                        )
                        authClient.signOut()
                        onError(
                            IllegalStateException(
                                "You signed in, but we could not load your Firebase profile. Please check Firestore permissions and try again.",
                                profileException,
                            ),
                        )
                    },
                )
            },
            onError = onError,
        )
    }

    fun registerWithEmail(
        emailAddress: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val trimmedEmailAddress = emailAddress.trim()
        if (trimmedEmailAddress.isBlank() || password.length < 6) {
            onError(IllegalArgumentException("Enter a valid email and a password with at least 6 characters."))
            return
        }

        authClient.register(
            emailAddress = trimmedEmailAddress,
            password = password,
            onSuccess = { authUser ->
                val profileEmail = authUser.email?.trim().takeUnless { it.isNullOrBlank() } ?: trimmedEmailAddress
                userProfileStore.createUserProfile(
                    userId = authUser.uid,
                    emailAddress = profileEmail,
                    onSuccess = onSuccess,
                    onError = { profileException ->
                        logProfileSetupFailure(
                            message = "Failed to create Firestore profile for user ${authUser.uid}",
                            exception = profileException,
                        )
                        authClient.deleteCurrentUser(
                            onComplete = {
                                onError(
                                    IllegalStateException(
                                        "Your account was created, but we could not finish setting up your profile. Please check Firestore permissions and try again.",
                                        profileException,
                                    ),
                                )
                            },
                            onError = { deleteException ->
                                logProfileSetupFailure(
                                    message = "Failed to roll back Auth user ${authUser.uid} after profile setup error",
                                    exception = deleteException,
                                )
                                onError(
                                    IllegalStateException(
                                        "Your account was created, but we could not finish setting up your profile. Please check Firestore permissions and try again.",
                                        profileException,
                                    ),
                                )
                            },
                        )
                    },
                )
            },
            onError = onError,
        )
    }

    fun signOut() {
        authClient.signOut()
    }

    private fun logProfileSetupFailure(
        message: String,
        exception: Exception,
    ) {
        System.err.println("AuthRepository: $message")
        exception.printStackTrace(System.err)
    }
}
