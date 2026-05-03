package com.example.pricecart.auth

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.pricecart.R
import com.example.pricecart.data.AuthRepository
import com.example.pricecart.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val authRepository = AuthRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.registerButton.setOnClickListener {
            attemptRegistration()
        }
        binding.confirmPasswordEditText.setOnEditorActionListener { _, _, _ ->
            attemptRegistration()
            true
        }
        binding.loginLinkTextView.setOnClickListener {
            (activity as? AuthNavigationHost)?.showLoginScreen()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun attemptRegistration() {
        val emailAddress = binding.emailEditText.text?.toString()?.trim().orEmpty()
        val password = binding.passwordEditText.text?.toString().orEmpty()
        val confirmPassword = binding.confirmPasswordEditText.text?.toString().orEmpty()
        val validationResult = AuthFormValidator.validateRegistrationInput(
            context = requireContext(),
            emailAddress = emailAddress,
            password = password,
            confirmPassword = confirmPassword,
        )
        binding.emailInputLayout.error = validationResult.emailError
        binding.passwordInputLayout.error = validationResult.passwordError
        binding.confirmPasswordInputLayout.error = validationResult.confirmPasswordError
        if (validationResult.hasErrors) {
            return
        }

        setLoadingState(true, getString(R.string.creating_account))
        authRepository.registerWithEmail(
            emailAddress = emailAddress,
            password = password,
            onSuccess = {
                if (!isAdded) {
                    return@registerWithEmail
                }
                setLoadingState(false)
                (activity as? AuthNavigationHost)?.completeAuthentication()
            },
            onError = { exception ->
                if (!isAdded) {
                    return@registerWithEmail
                }
                setLoadingState(false)
                Log.e(TAG, "Registration failed while setting up the Firebase profile", exception)
                val message = if (isDebugBuild()) {
                    buildString {
                        append(
                            exception.localizedMessage ?: getString(R.string.generic_auth_error),
                        )
                        exception.cause?.localizedMessage
                            ?.takeIf { it.isNotBlank() }
                            ?.let { causeMessage ->
                                append("\n\nDetails: ")
                                append(causeMessage)
                            }
                    }
                } else {
                    exception.localizedMessage ?: getString(R.string.generic_auth_error)
                }
                Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_LONG,
                ).show()
            },
        )
    }

    private fun setLoadingState(isLoading: Boolean, message: String? = null) {
        binding.registerButton.isEnabled = !isLoading
        binding.registerProgressBar.isVisible = isLoading
        binding.registerMessageTextView.isVisible = isLoading && !message.isNullOrBlank()
        binding.registerMessageTextView.text = message
    }

    private fun isDebugBuild(): Boolean {
        return (requireContext().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    companion object {
        private const val TAG = "RegisterFragment"
    }
}
