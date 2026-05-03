package com.example.pricecart

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pricecart.auth.AuthActivity
import com.example.pricecart.data.AuthRepository
import com.example.pricecart.data.FavouritesRepository
import com.example.pricecart.data.ServiceLocator
import com.example.pricecart.main.MainActivity
import org.junit.After
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PriceCartInstrumentedTest {
    private val fakeAuthClient = AndroidTestFakeAuthClient()
    private val fakeUserProfileStore = AndroidTestFakeUserProfileStore()
    private val fakeFavouritesStore = AndroidTestFakeFavouritesStore()
    private val fakeRecentSearchStore = AndroidTestFakeRecentSearchStore()
    private val fakeRecentlyViewedStore = AndroidTestFakeRecentlyViewedStore()

    private val authRepository by lazy { AuthRepository(fakeAuthClient, fakeUserProfileStore) }
    private val favouritesRepository by lazy { FavouritesRepository(fakeFavouritesStore, authRepository) }

    @Before
    fun setUp() {
        ServiceLocator.authClientFactory = { fakeAuthClient }
        ServiceLocator.userProfileStoreFactory = { fakeUserProfileStore }
        ServiceLocator.favouritesStoreFactory = { fakeFavouritesStore }
        ServiceLocator.recentSearchStoreFactory = { fakeRecentSearchStore }
        ServiceLocator.recentlyViewedStoreFactory = { fakeRecentlyViewedStore }
        authRepository.signOut()
    }

    @After
    fun tearDown() {
        ServiceLocator.reset()
    }

    @Test
    fun loginScreen_acceptsDemoCredentialsAndOpensMainScreen() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        ActivityScenario.launch<AuthActivity>(AuthActivity.createIntent(context)).use {
            onView(withId(R.id.emailEditText)).perform(
                replaceText("demo@example.com"),
                closeSoftKeyboard(),
            )
            onView(withId(R.id.passwordEditText)).perform(
                replaceText("123456"),
                closeSoftKeyboard(),
            )
            onView(withId(R.id.loginButton)).perform(click())
            onView(withId(R.id.bottomNavigationView)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun loginScreen_showsValidationMessagesWhenFieldsAreEmpty() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        ActivityScenario.launch<AuthActivity>(AuthActivity.createIntent(context)).use {
            onView(withId(R.id.loginButton)).perform(click())
            onView(withTextResource(R.string.email_required_error)).check(matches(isDisplayed()))
            onView(withTextResource(R.string.password_required_error)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun mainActivity_showsHomeAndSavedScreensFromBottomNavigation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        signInSynchronously("tabs@example.com")

        ActivityScenario.launch<MainActivity>(MainActivity.createIntent(context)).use {
            onView(withId(R.id.searchButton)).check(matches(isDisplayed()))
            onView(withId(R.id.navigation_saved)).perform(click())
            onView(withId(R.id.savedAccountEmailTextView)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun logout_returnsUserToAuthScreen() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        signInSynchronously("logout@example.com")

        ActivityScenario.launch<MainActivity>(MainActivity.createIntent(context)).use {
            openActionBarOverflowOrOptionsMenu(context)
            onView(withTextResource(R.string.logout)).perform(click())
            onView(withId(R.id.loginButton)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun savedItemsPersistAfterRelaunch() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        signInSynchronously("persist@example.com")
        saveFavouriteSynchronously("Rice")

        ActivityScenario.launch<MainActivity>(MainActivity.createIntent(context)).use {
            onView(withId(R.id.navigation_saved)).perform(click())
            onView(withText("Rice")).check(matches(isDisplayed()))
        }

        ActivityScenario.launch<MainActivity>(MainActivity.createIntent(context)).use {
            onView(withId(R.id.navigation_saved)).perform(click())
            onView(withText("Rice")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun tappingFavouriteItem_opensProductDetailScreen() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        signInSynchronously("saved-detail@example.com")
        saveFavouriteSynchronously("Rice")

        ActivityScenario.launch<MainActivity>(MainActivity.createIntent(context)).use {
            onView(withId(R.id.navigation_saved)).perform(click())
            onView(withId(R.id.savedFavouritesRecyclerView)).perform(
                actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()),
            )

            onView(withId(R.id.detailProductNameTextView)).check(matches(withText("Rice")))
            onView(withId(R.id.detailStoreNameTextView)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun homeSearch_hidesLoadingAndShowsResultsAfterSearchCompletes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        signInSynchronously("search@example.com")

        ActivityScenario.launch<MainActivity>(MainActivity.createIntent(context)).use {
            onView(withId(R.id.searchEditText)).perform(
                replaceText("Rice"),
                closeSoftKeyboard(),
            )
            onView(withId(R.id.searchButton)).perform(click())

            onView(withId(R.id.resultHeaderTextView)).check(
                matches(withTextResource(R.string.search_result_header, "Rice")),
            )
            onView(withId(R.id.searchResultsRecyclerView)).check(matches(isDisplayed()))
            onView(withId(R.id.searchButton)).check(matches(isEnabled()))
            onView(withId(R.id.searchEditText)).check(matches(isEnabled()))
            onView(withId(R.id.searchProgressBar)).check(matches(not(isDisplayed())))
        }
    }

    @Test
    fun tappingSearchResult_opensProductDetailScreen() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        signInSynchronously("detail@example.com")

        ActivityScenario.launch<MainActivity>(MainActivity.createIntent(context)).use {
            onView(withId(R.id.searchEditText)).perform(
                replaceText("Rice"),
                closeSoftKeyboard(),
            )
            onView(withId(R.id.searchButton)).perform(click())
            onView(withId(R.id.searchResultsRecyclerView)).perform(
                actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()),
            )

            onView(withId(R.id.detailProductNameTextView)).check(matches(withText("Rice")))
            onView(withId(R.id.detailStoreNameTextView)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun savedTab_showsRecentSearchesAndReopensSearch() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        signInSynchronously("recents@example.com")

        ActivityScenario.launch<MainActivity>(MainActivity.createIntent(context)).use {
            onView(withId(R.id.searchEditText)).perform(
                replaceText("Rice"),
                closeSoftKeyboard(),
            )
            onView(withId(R.id.searchButton)).perform(click())
            onView(withId(R.id.navigation_saved)).perform(click())
            onView(withText("Rice")).check(matches(isDisplayed()))
            onView(withId(R.id.savedRecentSearchesRecyclerView)).perform(
                actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()),
            )

            onView(withId(R.id.searchEditText)).check(matches(withText("Rice")))
        }
    }

    private fun withTextResource(stringRes: Int, vararg formatArgs: Any) =
        androidx.test.espresso.matcher.ViewMatchers.withText(
            ApplicationProvider.getApplicationContext<Context>().getString(stringRes, *formatArgs),
        )

    private fun signInSynchronously(emailAddress: String) {
        authRepository.signInWithEmail(
            emailAddress = emailAddress,
            password = "123456",
            onSuccess = { },
            onError = { throw it },
        )
    }

    private fun saveFavouriteSynchronously(productName: String) {
        favouritesRepository.saveFavouriteItem(
            productName = productName,
            onSuccess = { },
            onError = { throw it },
        )
    }
}
