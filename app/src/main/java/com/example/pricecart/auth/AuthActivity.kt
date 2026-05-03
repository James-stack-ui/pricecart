package com.example.pricecart.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.example.pricecart.R
import com.example.pricecart.data.AuthRepository
import com.example.pricecart.databinding.ActivityAuthBinding
import com.example.pricecart.main.MainActivity

class AuthActivity : AppCompatActivity(), AuthNavigationHost {
    private lateinit var binding: ActivityAuthBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.authFragmentContainer, LoginFragment())
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (hasAuthenticatedSession()) {
            openMainScreen()
        }
    }

    override fun showRegisterScreen() {
        supportFragmentManager.commit {
            replace(R.id.authFragmentContainer, RegisterFragment())
            addToBackStack(RegisterFragment::class.java.simpleName)
        }
    }

    override fun showLoginScreen() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            supportFragmentManager.commit {
                replace(R.id.authFragmentContainer, LoginFragment())
            }
        }
    }

    override fun completeAuthentication() {
        openMainScreen()
    }

    private fun hasAuthenticatedSession(): Boolean {
        return authRepository.isUserLoggedIn() ||
            intent.getBooleanExtra(EXTRA_FORCE_AUTHENTICATED_FOR_TESTS, false)
    }

    private fun openMainScreen() {
        startActivity(MainActivity.createIntent(this))
        finish()
    }

    companion object {
        private const val EXTRA_FORCE_AUTHENTICATED_FOR_TESTS = "extra_force_authenticated_for_tests"

        fun createIntent(
            context: Context,
            forceAuthenticatedForTests: Boolean = false,
        ): Intent {
            return Intent(context, AuthActivity::class.java).apply {
                putExtra(EXTRA_FORCE_AUTHENTICATED_FOR_TESTS, forceAuthenticatedForTests)
            }
        }
    }
}
