package com.example.pricecart

import com.example.pricecart.data.AuthRepository
import com.example.pricecart.data.model.AuthUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {
    @Test
    fun registerWithEmail_createsProfileAfterAuthSucceeds() {
        val authClient = FakeAuthClient()
        val userProfileStore = FakeUserProfileStore()
        val repository = AuthRepository(authClient, userProfileStore)

        var succeeded = false
        repository.registerWithEmail(
            emailAddress = "student@example.com",
            password = "123456",
            onSuccess = { succeeded = true },
            onError = { throw it },
        )

        assertTrue(succeeded)
        assertEquals(listOf("uid-student@example.com" to "student@example.com"), userProfileStore.createdProfiles)
        assertEquals("uid-student@example.com", repository.currentUserId())
        assertEquals("student@example.com", repository.currentUserEmail())
    }

    @Test
    fun registerWithEmail_reportsProfileFailureAndDeletesCurrentUser() {
        val authClient = FakeAuthClient()
        val userProfileStore = FakeUserProfileStore().apply {
            nextCreateError = IllegalStateException("firestore down")
        }
        val repository = AuthRepository(authClient, userProfileStore)

        var receivedError: Exception? = null
        repository.registerWithEmail(
            emailAddress = "student@example.com",
            password = "123456",
            onSuccess = { throw AssertionError("Expected registration to fail") },
            onError = { receivedError = it },
        )

        assertEquals(1, authClient.deleteCurrentUserCalls)
        assertFalse(repository.isUserLoggedIn())
        assertTrue(receivedError is IllegalStateException)
        assertEquals(
            "Your account was created, but we could not finish setting up your profile. Please check Firestore permissions and try again.",
            receivedError?.message,
        )
    }

    @Test
    fun signInWithEmail_usesFirebaseUserState() {
        val authClient = FakeAuthClient(initialUser = AuthUser(uid = "uid-start", email = "start@example.com"))
        val repository = AuthRepository(authClient, FakeUserProfileStore())

        assertTrue(repository.isUserLoggedIn())
        assertEquals("uid-start", repository.currentUserId())
        assertEquals("start@example.com", repository.currentUserEmail())

        repository.signOut()

        assertFalse(repository.isUserLoggedIn())
        assertNull(repository.currentUserEmail())
    }

    @Test
    fun signInWithEmail_recordsFirebaseProfileAfterAuthSucceeds() {
        val authClient = FakeAuthClient()
        val userProfileStore = FakeUserProfileStore()
        val repository = AuthRepository(authClient, userProfileStore)

        var succeeded = false
        repository.signInWithEmail(
            emailAddress = "student@example.com",
            password = "123456",
            onSuccess = { succeeded = true },
            onError = { throw it },
        )

        assertTrue(succeeded)
        assertEquals(listOf("uid-student@example.com" to "student@example.com"), userProfileStore.createdProfiles)
        assertEquals("uid-student@example.com", repository.currentUserId())
    }

    @Test
    fun signInWithEmail_signsOutWhenProfileRecordFails() {
        val authClient = FakeAuthClient()
        val userProfileStore = FakeUserProfileStore().apply {
            nextCreateError = IllegalStateException("firestore down")
        }
        val repository = AuthRepository(authClient, userProfileStore)

        var receivedError: Exception? = null
        repository.signInWithEmail(
            emailAddress = "student@example.com",
            password = "123456",
            onSuccess = { throw AssertionError("Expected sign in to fail") },
            onError = { receivedError = it },
        )

        assertEquals(1, authClient.signOutCalls)
        assertFalse(repository.isUserLoggedIn())
        assertTrue(receivedError is IllegalStateException)
        assertEquals(
            "You signed in, but we could not load your Firebase profile. Please check Firestore permissions and try again.",
            receivedError?.message,
        )
    }
}
