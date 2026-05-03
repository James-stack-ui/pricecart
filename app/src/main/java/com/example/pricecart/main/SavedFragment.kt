package com.example.pricecart.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pricecart.R
import com.example.pricecart.auth.AuthActivity
import com.example.pricecart.data.AuthRepository
import com.example.pricecart.data.FavouritesRepository
import com.example.pricecart.data.PriceRepository
import com.example.pricecart.data.RecentSearchRepository
import com.example.pricecart.data.model.RecentSearchItem
import com.example.pricecart.databinding.FragmentSavedBinding
import com.example.pricecart.detail.ProductDetailActivity
import com.example.pricecart.search.SearchTextFormatter
import com.google.firebase.firestore.FirebaseFirestore

class SavedFragment : Fragment() {
    private var _binding: FragmentSavedBinding? = null
    private val binding get() = _binding!!

    private val authRepository = AuthRepository()
    private val favouritesRepository = FavouritesRepository()
    private val priceRepository = PriceRepository(firestore = FirebaseFirestore.getInstance())
    private val recentSearchRepository = RecentSearchRepository()

    private lateinit var favouritesAdapter: FavouritesAdapter
    private lateinit var recentSearchAdapter: RecentSearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        favouritesAdapter = FavouritesAdapter(
            onItemClicked = { favouriteItem ->
                openFavouriteDetail(favouriteItem.productName)
            },
            onRemoveClicked = { favouriteItem ->
                removeFavouriteItem(favouriteItem.productName)
            },
        )
        recentSearchAdapter = RecentSearchAdapter { recentSearchItem ->
            reopenSearch(recentSearchItem.queryDisplayName)
        }

        binding.savedFavouritesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = favouritesAdapter
            isNestedScrollingEnabled = false
        }
        binding.savedRecentSearchesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentSearchAdapter
            isNestedScrollingEnabled = false
        }

        // Keep the screen populated even before async data finishes loading.
        binding.savedFavouritesRecyclerView.isVisible = false
        binding.savedFavouritesEmptyTextView.isVisible = true
        binding.savedRecentSearchesRecyclerView.isVisible = false
        binding.savedRecentSearchesEmptyTextView.isVisible = true

        binding.savedLogoutButton.setOnClickListener {
            authRepository.signOut()
            startActivity(AuthActivity.createIntent(requireContext()))
            requireActivity().finish()
        }

    }

    override fun onResume() {
        super.onResume()
        if (!isHidden) {
            loadSavedContent()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && view != null) {
            loadSavedContent()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadSavedContent() {
        if (!isAdded || _binding == null) {
            return
        }

        setLoadingState(true)
        try {
            binding.savedAccountEmailTextView.text = authRepository.currentUserEmail()
                ?: getString(R.string.saved_account_unknown)
            loadFavourites()
            loadRecentSearches()
        } catch (exception: Exception) {
            binding.savedAccountEmailTextView.text = getString(R.string.saved_account_unknown)
            favouritesAdapter.updateItems(emptyList())
            recentSearchAdapter.updateItems(emptyList())
            binding.savedFavouritesRecyclerView.isVisible = false
            binding.savedFavouritesEmptyTextView.isVisible = true
            binding.savedRecentSearchesRecyclerView.isVisible = false
            binding.savedRecentSearchesEmptyTextView.isVisible = true
            Toast.makeText(
                requireContext(),
                exception.localizedMessage ?: getString(R.string.favourites_error_message),
                Toast.LENGTH_LONG,
            ).show()
        } finally {
            setLoadingState(false)
        }
    }

    private fun loadFavourites() {
        favouritesRepository.fetchUserFavourites(
            onSuccess = { favouriteItems ->
                if (!isAdded) {
                    return@fetchUserFavourites
                }

                favouritesAdapter.updateItems(favouriteItems)
                binding.savedFavouritesRecyclerView.isVisible = favouriteItems.isNotEmpty()
                binding.savedFavouritesEmptyTextView.isVisible = favouriteItems.isEmpty()
            },
            onError = { exception ->
                if (!isAdded) {
                    return@fetchUserFavourites
                }

                favouritesAdapter.updateItems(emptyList())
                binding.savedFavouritesRecyclerView.isVisible = false
                binding.savedFavouritesEmptyTextView.isVisible = true
                Toast.makeText(
                    requireContext(),
                    exception.localizedMessage ?: getString(R.string.favourites_error_message),
                    Toast.LENGTH_LONG,
                ).show()
            },
        )
    }

    private fun loadRecentSearches() {
        recentSearchRepository.fetchRecentSearches(
            onSuccess = { recentSearches ->
                if (!isAdded) {
                    return@fetchRecentSearches
                }

                bindRecentSearches(recentSearches)
            },
            onError = { exception ->
                if (!isAdded) {
                    return@fetchRecentSearches
                }

                bindRecentSearches(emptyList())
                Toast.makeText(
                    requireContext(),
                    exception.localizedMessage ?: getString(R.string.recent_searches_error_message),
                    Toast.LENGTH_LONG,
                ).show()
            },
        )
    }

    private fun bindRecentSearches(recentSearches: List<RecentSearchItem>) {
        recentSearchAdapter.updateItems(recentSearches)
        binding.savedRecentSearchesRecyclerView.isVisible = recentSearches.isNotEmpty()
        binding.savedRecentSearchesEmptyTextView.isVisible = recentSearches.isEmpty()
    }

    private fun removeFavouriteItem(productName: String) {
        favouritesRepository.removeFavouriteItem(
            productName = productName,
            onSuccess = {
                if (!isAdded) {
                    return@removeFavouriteItem
                }

                loadFavourites()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.favourite_removed_message),
                    Toast.LENGTH_SHORT,
                ).show()
            },
            onError = { exception ->
                if (!isAdded) {
                    return@removeFavouriteItem
                }

                Toast.makeText(
                    requireContext(),
                    exception.localizedMessage ?: getString(R.string.favourites_error_message),
                    Toast.LENGTH_LONG,
                ).show()
            },
        )
    }

    private fun reopenSearch(productName: String) {
        val resultBundle = Bundle().apply {
            putString(BUNDLE_PRODUCT_NAME, productName)
        }
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY_OPEN_SEARCH,
            resultBundle,
        )
    }

    private fun openFavouriteDetail(productName: String) {
        val trimmedProductName = productName.trim()
        priceRepository.fetchProductsByName(
            normalizedQuery = SearchTextFormatter.normalizeQuery(trimmedProductName),
            onSuccess = { storePrices ->
                if (!isAdded) {
                    return@fetchProductsByName
                }

                val matchingOffer = storePrices.firstOrNull { offer ->
                    offer.productName.equals(trimmedProductName, ignoreCase = true)
                } ?: storePrices.firstOrNull()

                if (matchingOffer == null) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.product_detail_not_found_message),
                        Toast.LENGTH_LONG,
                    ).show()
                    return@fetchProductsByName
                }

                startActivity(
                    ProductDetailActivity.createIntent(
                        context = requireContext(),
                        productName = matchingOffer.productName,
                        storeName = matchingOffer.storeName,
                    ),
                )
            },
            onError = { exception ->
                if (!isAdded) {
                    return@fetchProductsByName
                }

                Toast.makeText(
                    requireContext(),
                    exception.localizedMessage ?: getString(R.string.product_detail_not_found_message),
                    Toast.LENGTH_LONG,
                ).show()
            },
        )
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.savedProgressBar.isVisible = isLoading
    }

    companion object {
        const val REQUEST_KEY_OPEN_SEARCH = "request_key_open_saved_search"
        const val BUNDLE_PRODUCT_NAME = "bundle_product_name"
    }
}
