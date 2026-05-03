package com.example.pricecart.data

import com.example.pricecart.data.model.AuthUser
import com.google.firebase.auth.FirebaseAuth

class FirebaseAuthClient(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
) : AuthClient {
    override fun currentUser(): AuthUser? {
        return firebaseAuth.currentUser?.let { firebaseUser ->
            AuthUser(
                uid = firebaseUser.uid,
                email = firebaseUser.email,
            )
        }
    }

    override fun signIn(
        emailAddress: String,
        password: String,
        onSuccess: (AuthUser) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        firebaseAuth.signInWithEmailAndPassword(emailAddress, password)
            .addOnSuccessListener { authResult ->
                val firebaseUser = authResult.user
                if (firebaseUser == null) {
                    onError(IllegalStateException("Firebase did not return a signed-in user."))
                    return@addOnSuccessListener
                }

                onSuccess(AuthUser(uid = firebaseUser.uid, email = firebaseUser.email))
            }
            .addOnFailureListener(onError)
    }

    override fun register(
        emailAddress: String,
        password: String,
        onSuccess: (AuthUser) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        firebaseAuth.createUserWithEmailAndPassword(emailAddress, password)
            .addOnSuccessListener { authResult ->
                val firebaseUser = authResult.user
                if (firebaseUser == null) {
                    onError(IllegalStateException("Firebase did not return a newly created user."))
                    return@addOnSuccessListener
                }

                onSuccess(AuthUser(uid = firebaseUser.uid, email = firebaseUser.email))
            }
            .addOnFailureListener(onError)
    }

    override fun deleteCurrentUser(
        onComplete: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null) {
            onComplete()
            return
        }

        firebaseUser.delete()
            .addOnSuccessListener { onComplete() }
            .addOnFailureListener(onError)
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }
}
