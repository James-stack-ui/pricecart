package com.example.pricecart.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import com.example.pricecart.R
import com.example.pricecart.auth.AuthActivity
import com.example.pricecart.data.AuthRepository
import com.example.pricecart.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val authRepository = AuthRepository()
    private lateinit var homeFragment: Fragment
    private lateinit var savedFragment: Fragment
    private var currentNavigationItemId: Int = R.id.navigation_home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.topAppBar)

        setupFragments(savedInstanceState)
        setupBottomNavigation(savedInstanceState)
        setupSavedSearchListener()
    }

    override fun onStart() {
        super.onStart()
        if (!shouldSkipAuthRedirect() && !authRepository.isUserLoggedIn()) {
            startActivity(AuthActivity.createIntent(this))
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                authRepository.signOut()
                startActivity(AuthActivity.createIntent(this))
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_SELECTED_NAVIGATION_ITEM, binding.bottomNavigationView.selectedItemId)
        super.onSaveInstanceState(outState)
    }

    private fun setupFragments(savedInstanceState: Bundle?) {
        val selectedItemId = savedInstanceState?.getInt(KEY_SELECTED_NAVIGATION_ITEM) ?: R.id.navigation_home
        homeFragment = supportFragmentManager.findFragmentByTag(HOME_FRAGMENT_TAG) ?: HomeFragment()
        savedFragment = supportFragmentManager.findFragmentByTag(SAVED_FRAGMENT_TAG) ?: SavedFragment()
        currentNavigationItemId = selectedItemId
        supportFragmentManager.commitNow {
            setReorderingAllowed(true)
            if (!homeFragment.isAdded) {
                add(R.id.mainFragmentContainer, homeFragment, HOME_FRAGMENT_TAG)
            }
            if (!savedFragment.isAdded) {
                add(R.id.mainFragmentContainer, savedFragment, SAVED_FRAGMENT_TAG)
            }
            if (selectedItemId == R.id.navigation_saved) {
                hide(homeFragment)
                show(savedFragment)
            } else {
                show(homeFragment)
                hide(savedFragment)
            }
        }
    }

    private fun setupBottomNavigation(savedInstanceState: Bundle?) {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    showFragment(homeFragment)
                    true
                }

                R.id.navigation_saved -> {
                    showFragment(savedFragment)
                    true
                }

                else -> false
            }
        }

        binding.bottomNavigationView.selectedItemId = savedInstanceState
            ?.getInt(KEY_SELECTED_NAVIGATION_ITEM)
            ?: R.id.navigation_home
    }

    private fun setupSavedSearchListener() {
        supportFragmentManager.setFragmentResultListener(
            SavedFragment.REQUEST_KEY_OPEN_SEARCH,
            this,
        ) { _, result ->
            val productName = result.getString(SavedFragment.BUNDLE_PRODUCT_NAME).orEmpty()
            val searchBundle = Bundle().apply {
                putString(HomeFragment.BUNDLE_QUERY, productName)
            }
            supportFragmentManager.setFragmentResult(
                HomeFragment.REQUEST_KEY_SEARCH_FROM_SAVED,
                searchBundle,
            )
            binding.bottomNavigationView.selectedItemId = R.id.navigation_home
        }
    }

    private fun showFragment(fragmentToShow: Fragment) {
        val navigationItemId = if (fragmentToShow === homeFragment) {
            R.id.navigation_home
        } else {
            R.id.navigation_saved
        }
        if (navigationItemId == currentNavigationItemId) {
            return
        }

        currentNavigationItemId = navigationItemId
        supportFragmentManager.commitNow {
            setReorderingAllowed(true)
            if (navigationItemId == R.id.navigation_saved) {
                hide(homeFragment)
                show(savedFragment)
            } else {
                show(homeFragment)
                hide(savedFragment)
            }
        }
    }

    private fun shouldSkipAuthRedirect(): Boolean {
        return intent.getBooleanExtra(EXTRA_SKIP_AUTH_REDIRECT_FOR_TESTS, false)
    }

    companion object {
        private const val EXTRA_SKIP_AUTH_REDIRECT_FOR_TESTS = "extra_skip_auth_redirect_for_tests"
        private const val KEY_SELECTED_NAVIGATION_ITEM = "key_selected_navigation_item"
        private const val HOME_FRAGMENT_TAG = "home_fragment"
        private const val SAVED_FRAGMENT_TAG = "saved_fragment"

        fun createIntent(
            context: Context,
            skipAuthRedirectForTests: Boolean = false,
        ): Intent {
            return Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_SKIP_AUTH_REDIRECT_FOR_TESTS, skipAuthRedirectForTests)
            }
        }
    }
}
