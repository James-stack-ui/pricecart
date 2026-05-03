package com.example.pricecart.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.pricecart.R
import com.example.pricecart.data.AuthRepository
import com.example.pricecart.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val authRepository = AuthRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginButton.setOnClickListener {
            attemptLogin()
        }
        binding.passwordEditText.setOnEditorActionListener { _, _, _ ->
            attemptLogin()
            true
        }
        binding.registerLinkTextView.setOnClickListener {
            (activity as? AuthNavigationHost)?.showRegisterScreen()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun attemptLogin() {
        val emailAddress = binding.emailEditText.text?.toString()?.trim().orEmpty()
        val password = binding.passwordEditText.text?.toString().orEmpty()
        val validationResult = AuthFormValidator.validateLoginInput(
            context = requireContext(),
            emailAddress = emailAddress,
            password = password,
        )
        binding.emailInputLayout.error = validationResult.emailError
        binding.passwordInputLayout.error = validationResult.passwordError
        if (validationResult.hasErrors) {
            return
        }

        setLoadingState(true, getString(R.string.signing_in))
        authRepository.signInWithEmail(
            emailAddress = emailAddress,
            password = password,
            onSuccess = {
                if (!isAdded) {
                    return@signInWithEmail
                }
                setLoadingState(false)
                (activity as? AuthNavigationHost)?.completeAuthentication()
            },
            onError = { exception ->
                if (!isAdded) {
                    return@signInWithEmail
                }
                setLoadingState(false)
                Toast.makeText(
                    requireContext(),
                    exception.localizedMessage ?: getString(R.string.generic_auth_error),
                    Toast.LENGTH_LONG,
                ).show()
            },
        )
    }

    private fun setLoadingState(isLoading: Boolean, message: String? = null) {
        binding.loginButton.isEnabled = !isLoading
        binding.loginProgressBar.isVisible = isLoading
        binding.loginMessageTextView.isVisible = isLoading && !message.isNullOrBlank()
        binding.loginMessageTextView.text = message
    }
}
