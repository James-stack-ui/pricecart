package com.example.pricecart.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pricecart.R
import com.example.pricecart.data.IncompleteCatalogDataException
import com.example.pricecart.data.PriceRepository
import com.example.pricecart.data.RecentSearchRepository
import com.example.pricecart.data.RecentlyViewedRepository
import com.example.pricecart.data.model.StoreOfferResult
import com.example.pricecart.data.model.UserCoordinates
import com.example.pricecart.data.model.UserLocation
import com.example.pricecart.databinding.DialogFiltersBinding
import com.example.pricecart.databinding.FragmentHomeBinding
import com.example.pricecart.detail.ProductDetailActivity
import com.example.pricecart.favourites.FavouriteItemHelper
import com.example.pricecart.location.LocationHelper
import com.example.pricecart.ranking.StoreRankingCalculator
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.example.pricecart.search.ProductSearchManager
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.ceil

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val priceRepository = PriceRepository(firestore = FirebaseFirestore.getInstance())
    private val recentSearchRepository = RecentSearchRepository()
    private val recentlyViewedRepository = RecentlyViewedRepository()
    private lateinit var productSearchManager: ProductSearchManager
    private lateinit var locationHelper: LocationHelper
    private lateinit var storePriceAdapter: StorePriceAdapter

    private var currentSearchDisplayName: String = ""
    private var currentSearchNormalized: String = ""
    private var currentUserCoordinates: UserCoordinates? = null
    private var currentSearchResults: List<StoreOfferResult> = emptyList()
    private var filteredSearchResults: List<StoreOfferResult> = emptyList()
    private var emptyStateMode: SearchEmptyStateMode = SearchEmptyStateMode.INITIAL
    private var selectedStores = linkedSetOf<String>()
    private var selectedMaxPrice: Double? = null
    private var selectedMaxDistanceKm: Double? = null
    private var filterDialog: BottomSheetDialog? = null
    private var filterDialogBinding: DialogFiltersBinding? = null
    private var pendingSearchQuery: String? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) {
            fetchCurrentLocation()
        } else {
            handleLocationRequiredFailure()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        productSearchManager = ProductSearchManager(priceRepository)
        locationHelper = LocationHelper(requireContext())
        storePriceAdapter = StorePriceAdapter(::openProductDetail)

        binding.searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = storePriceAdapter
        }

        binding.searchButton.setOnClickListener {
            searchWithLocationRequirement(binding.searchEditText.text?.toString().orEmpty())
        }
        binding.searchEditText.setOnEditorActionListener { _, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || keyEvent?.keyCode == KeyEvent.KEYCODE_ENTER) {
                searchWithLocationRequirement(binding.searchEditText.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }
        binding.locationButton.setOnClickListener {
            requestLocationWithPermissionCheck()
        }
        binding.filterToggleButton.setOnClickListener {
            showFilterPopup()
        }

        parentFragmentManager.setFragmentResultListener(
            REQUEST_KEY_SEARCH_FROM_SAVED,
            viewLifecycleOwner,
        ) { _, result ->
            val productName = result.getString(BUNDLE_QUERY).orEmpty()
            binding.searchEditText.setText(productName)
            searchWithLocationRequirement(productName)
        }

        showInitialState()
        refreshQuickPicks()
    }

    override fun onResume() {
        super.onResume()
        refreshQuickPicks()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        filterDialog?.dismiss()
        filterDialog = null
        filterDialogBinding = null
        _binding = null
    }

    private fun performSearch(rawQuery: String) {
        val queryForDisplay = rawQuery.trim()
        binding.searchInputLayout.error = null
        setSearchLoadingState(true)

        val searchStarted = productSearchManager.searchProductsByName(
            rawQuery = rawQuery,
            onBlankQuery = {
                currentSearchDisplayName = ""
                currentSearchNormalized = ""
                currentSearchResults = emptyList()
                filteredSearchResults = emptyList()
                storePriceAdapter.updateItems(emptyList())
                binding.searchInputLayout.error = getString(R.string.search_required_error)
                showInitialState()
            },
            onSuccess = { normalizedQuery, storePrices ->
                if (!isAdded) {
                    return@searchProductsByName
                }

                currentSearchDisplayName = queryForDisplay
                currentSearchNormalized = normalizedQuery
                currentSearchResults = StoreRankingCalculator.rankStoreOffers(
                    storePrices = storePrices,
                    userCoordinates = currentUserCoordinates,
                )
                recentSearchRepository.recordSearch(queryForDisplay)
                emptyStateMode = if (currentSearchResults.isEmpty()) {
                    SearchEmptyStateMode.NO_MATCHES
                } else {
                    SearchEmptyStateMode.INITIAL
                }
                resetResultFilters()
                renderFilteredResults()
                refreshQuickPicks()
                setSearchLoadingState(false)
            },
            onError = { exception ->
                if (!isAdded) {
                    return@searchProductsByName
                }

                currentSearchDisplayName = queryForDisplay
                currentSearchNormalized = FavouriteItemHelper.normalizeProductName(queryForDisplay)
                currentSearchResults = emptyList()
                filteredSearchResults = emptyList()
                emptyStateMode = if (exception is IncompleteCatalogDataException) {
                    SearchEmptyStateMode.CATALOG_UNAVAILABLE
                } else {
                    SearchEmptyStateMode.ERROR
                }
                storePriceAdapter.updateItems(emptyList())
                showSearchResultsState(hasMatches = false)
                setSearchLoadingState(false)
                Toast.makeText(
                    requireContext(),
                    exception.localizedMessage ?: getString(R.string.search_error_message),
                    Toast.LENGTH_LONG,
                ).show()
            },
        )

        if (!searchStarted) {
            setSearchLoadingState(false)
            return
        }

        binding.resultHeaderTextView.text = getString(R.string.search_result_header, queryForDisplay)
        binding.resultHeaderContainer.isVisible = true
    }

    private fun searchWithLocationRequirement(rawQuery: String) {
        if (currentUserCoordinates != null) {
            performSearch(rawQuery)
            return
        }

        pendingSearchQuery = rawQuery
        requestLocationWithPermissionCheck()
    }

    private fun requestLocationWithPermissionCheck() {
        if (hasLocationPermission()) {
            fetchCurrentLocation()
            return
        }

        Toast.makeText(requireContext(), getString(R.string.location_permission_message), Toast.LENGTH_SHORT)
            .show()
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    private fun hasLocationPermission(): Boolean {
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        return fineLocationPermission == PackageManager.PERMISSION_GRANTED ||
            coarseLocationPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun fetchCurrentLocation() {
        binding.locationStatusTextView.text = getString(R.string.location_fetching)
        locationHelper.fetchCurrentLocation(
            onSuccess = { userLocation ->
                if (!isAdded) {
                    return@fetchCurrentLocation
                }

                applyLocationSuccess(userLocation)
            },
            onUnavailable = {
                if (!isAdded) {
                    return@fetchCurrentLocation
                }
                if (pendingSearchQuery != null) {
                    handleLocationRequiredFailure()
                } else {
                    showPriceOnlyLocationState(showMessage = false)
                }
            },
            onError = {
                if (!isAdded) {
                    return@fetchCurrentLocation
                }
                if (pendingSearchQuery != null) {
                    handleLocationRequiredFailure()
                } else {
                    showPriceOnlyLocationState(showMessage = true)
                }
            },
        )
    }

    private fun applyLocationSuccess(userLocation: UserLocation) {
        currentUserCoordinates = userLocation.coordinates
        binding.locationStatusTextView.text = userLocation.formattedAddress
            ?: getString(R.string.location_enabled_without_address)
        val pendingQuery = pendingSearchQuery
        pendingSearchQuery = null
        if (pendingQuery != null) {
            performSearch(pendingQuery)
        } else {
            rerankCurrentResults()
        }
    }

    private fun rerankCurrentResults() {
        if (currentSearchNormalized.isBlank()) {
            return
        }

        priceRepository.fetchProductsByName(
            normalizedQuery = currentSearchNormalized,
            onSuccess = { storePrices ->
                if (!isAdded) {
                    return@fetchProductsByName
                }

                currentSearchResults = StoreRankingCalculator.rankStoreOffers(
                    storePrices = storePrices,
                    userCoordinates = currentUserCoordinates,
                )
                syncDistanceFilterWithCurrentLocation()
                renderFilteredResults()
            },
            onError = {
                if (!isAdded) {
                    return@fetchProductsByName
                }
                binding.locationStatusTextView.text = getString(R.string.location_price_only)
            },
        )
    }

    private fun showPriceOnlyLocationState(showMessage: Boolean) {
        currentUserCoordinates = null
        selectedMaxDistanceKm = null
        binding.locationStatusTextView.text = getString(R.string.location_price_only)
        if (showMessage) {
            Toast.makeText(
                requireContext(),
                getString(R.string.location_error_message),
                Toast.LENGTH_SHORT,
            ).show()
        }
        renderFilteredResults()
        rerankCurrentResults()
    }

    private fun handleLocationRequiredFailure() {
        pendingSearchQuery = null
        currentUserCoordinates = null
        selectedMaxDistanceKm = null
        binding.locationStatusTextView.text = getString(R.string.location_price_only)
        Toast.makeText(
            requireContext(),
            getString(R.string.location_required_for_search_message),
            Toast.LENGTH_SHORT,
        ).show()
        renderFilteredResults()
    }

    private fun setSearchLoadingState(isLoading: Boolean) {
        binding.searchProgressBar.isVisible = isLoading
        binding.searchButton.isEnabled = !isLoading
        binding.searchEditText.isEnabled = !isLoading
        binding.locationButton.isEnabled = !isLoading
    }

    private fun showInitialState() {
        binding.resultHeaderContainer.isVisible = false
        binding.resultCountTextView.text = getString(R.string.quick_compare_prompt)
        binding.emptyStateTextView.isVisible = true
        binding.emptyStateTextView.text = getString(R.string.empty_search_message)
        emptyStateMode = SearchEmptyStateMode.INITIAL
        binding.searchResultsRecyclerView.isVisible = false
        updateFilterButtonVisibility()
        setSearchLoadingState(false)
    }

    private fun showSearchResultsState(
        hasMatches: Boolean,
    ) {
        binding.resultHeaderContainer.isVisible = currentSearchDisplayName.isNotBlank()
        binding.resultHeaderTextView.text = getString(
            R.string.search_result_header,
            currentSearchDisplayName,
        )
        binding.resultCountTextView.text = if (hasMatches) {
            getString(R.string.results_count_label, filteredSearchResults.size)
        } else {
            getString(R.string.no_results_title)
        }
        binding.searchResultsRecyclerView.isVisible = hasMatches
        binding.emptyStateTextView.isVisible = !hasMatches
        binding.emptyStateTextView.text = if (hasMatches) "" else getEmptyStateMessage()
        updateFilterButtonVisibility()
    }

    private fun resetResultFilters() {
        selectedStores.clear()
        selectedMaxPrice = null
        selectedMaxDistanceKm = null
        renderFilterControls()
    }

    private fun syncDistanceFilterWithCurrentLocation() {
        if (currentUserCoordinates == null) {
            selectedMaxDistanceKm = null
        }
        renderFilterControls()
    }

    private fun renderFilteredResults() {
        filteredSearchResults = currentSearchResults
            .filter { storeOfferResult ->
                selectedStores.isEmpty() || selectedStores.contains(storeOfferResult.storeName)
            }
            .filter { storeOfferResult ->
                selectedMaxPrice == null || storeOfferResult.price <= selectedMaxPrice!!
            }
            .filter { storeOfferResult ->
                when {
                    selectedMaxDistanceKm == null -> true
                    storeOfferResult.distanceKilometers == null -> false
                    else -> storeOfferResult.distanceKilometers <= selectedMaxDistanceKm!!
                }
            }

        renderFilterControls()
        storePriceAdapter.updateItems(filteredSearchResults)
        showSearchResultsState(hasMatches = filteredSearchResults.isNotEmpty())
    }

    private fun renderFilterControls() {
        renderStoreFilterChips()
        renderPriceFilterChips()
        renderDistanceFilterChips()
        updateFilterButtonVisibility()
    }

    private fun updateFilterButtonVisibility() {
        val hasResultsToFilter = currentSearchResults.isNotEmpty()
        binding.filterToggleButton.isVisible = hasResultsToFilter
        binding.filterToggleButton.text = getString(R.string.filter_button_show)
    }

    private fun renderStoreFilterChips() {
        val chipGroup = filterDialogBinding?.storeFilterChipGroup ?: return
        chipGroup.removeAllViews()
        val storeNames = currentSearchResults.map { it.storeName }.distinct()
        chipGroup.isVisible = storeNames.isNotEmpty()
        storeNames.forEach { storeName ->
            val chip = Chip(requireContext()).apply {
                text = storeName
                isCheckable = true
                isChecked = selectedStores.contains(storeName)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedStores.add(storeName)
                    } else {
                        selectedStores.remove(storeName)
                    }
                    renderFilteredResults()
                }
            }
            chipGroup.addView(chip)
        }
    }

    private fun renderPriceFilterChips() {
        val chipGroup = filterDialogBinding?.priceFilterChipGroup ?: return
        chipGroup.removeAllViews()
        val priceThresholds = currentSearchResults.map { it.price }.distinct().sorted()
        addSingleSelectFilterChip(
            chipGroup = chipGroup,
            title = getString(R.string.filter_any_price),
            isSelected = selectedMaxPrice == null,
        ) {
            selectedMaxPrice = null
            renderFilteredResults()
        }
        priceThresholds.forEach { price ->
            addSingleSelectFilterChip(
                chipGroup = chipGroup,
                title = getString(R.string.filter_price_up_to, getString(R.string.price_format, "JMD", price)),
                isSelected = selectedMaxPrice == price,
            ) {
                selectedMaxPrice = price
                renderFilteredResults()
            }
        }
    }

    private fun renderDistanceFilterChips() {
        val dialogBinding = filterDialogBinding ?: return
        dialogBinding.distanceFilterChipGroup.removeAllViews()
        val hasLocation = currentUserCoordinates != null
        dialogBinding.distanceFilterLabel.text = getString(
            if (hasLocation) {
                R.string.filter_distance_label
            } else {
                R.string.filter_distance_label_disabled
            },
        )
        if (!hasLocation) {
            dialogBinding.distanceFilterChipGroup.isVisible = false
            return
        }

        val distanceThresholds = currentSearchResults.mapNotNull { it.distanceKilometers }
            .map { ceil(it).coerceAtLeast(1.0) }
            .distinct()
            .sorted()
        dialogBinding.distanceFilterChipGroup.isVisible = true
        addSingleSelectFilterChip(
            chipGroup = dialogBinding.distanceFilterChipGroup,
            title = getString(R.string.filter_any_distance),
            isSelected = selectedMaxDistanceKm == null,
        ) {
            selectedMaxDistanceKm = null
            renderFilteredResults()
        }
        distanceThresholds.forEach { distance ->
            addSingleSelectFilterChip(
                chipGroup = dialogBinding.distanceFilterChipGroup,
                title = getString(R.string.filter_distance_up_to, distance.toInt()),
                isSelected = selectedMaxDistanceKm == distance,
            ) {
                selectedMaxDistanceKm = distance
                renderFilteredResults()
            }
        }
    }

    private fun showFilterPopup() {
        if (currentSearchResults.isEmpty()) {
            return
        }

        val existingDialog = filterDialog
        if (existingDialog?.isShowing == true) {
            renderFilterControls()
            return
        }

        val dialogBinding = DialogFiltersBinding.inflate(layoutInflater)
        filterDialogBinding = dialogBinding
        filterDialog = BottomSheetDialog(requireContext()).apply {
            setContentView(dialogBinding.root)
            setOnDismissListener {
                filterDialogBinding = null
                filterDialog = null
            }
        }
        renderFilterControls()
        filterDialog?.show()
    }

    private fun addSingleSelectFilterChip(
        chipGroup: com.google.android.material.chip.ChipGroup,
        title: String,
        isSelected: Boolean,
        onSelected: () -> Unit,
    ) {
        val chip = Chip(requireContext()).apply {
            text = title
            isCheckable = true
            isChecked = isSelected
            setOnClickListener { onSelected() }
        }
        chipGroup.addView(chip)
    }

    private fun refreshQuickPicks() {
        priceRepository.fetchCatalogProductNames(
            limit = MAX_QUICK_PICKS,
            onSuccess = { productNames ->
                if (!isAdded) {
                    return@fetchCatalogProductNames
                }
                showQuickPicks(productNames)
            },
            onError = {
                if (!isAdded) {
                    return@fetchCatalogProductNames
                }
                showQuickPicks(emptyList())
            },
        )
    }

    private fun showQuickPicks(quickPickNames: List<String>) {
        binding.quickPicksChipGroup.removeAllViews()
        binding.quickPicksEmptyTextView.isVisible = quickPickNames.isEmpty()
        quickPickNames.forEach { quickPickName ->
            val chip = Chip(requireContext()).apply {
                text = quickPickName
                isCheckable = false
                isClickable = true
                setOnClickListener {
                    binding.searchEditText.setText(quickPickName)
                    searchWithLocationRequirement(quickPickName)
                }
            }
            binding.quickPicksChipGroup.addView(chip)
        }
    }

    private fun openProductDetail(storeOfferResult: StoreOfferResult) {
        recentlyViewedRepository.recordViewedOffer(
            storeOfferResult = storeOfferResult,
            userCoordinates = currentUserCoordinates,
        )
        startActivity(
            ProductDetailActivity.createIntent(
                context = requireContext(),
                productName = storeOfferResult.productName,
                storeName = storeOfferResult.storeName,
                userCoordinates = currentUserCoordinates,
            ),
        )
    }

    companion object {
        private const val MAX_QUICK_PICKS = 6
        const val REQUEST_KEY_SEARCH_FROM_SAVED = "request_key_search_from_saved"
        const val BUNDLE_QUERY = "bundle_query"
    }

    private fun getEmptyStateMessage(): String {
        return when (emptyStateMode) {
            SearchEmptyStateMode.INITIAL -> getString(R.string.empty_search_message)
            SearchEmptyStateMode.NO_MATCHES -> getString(R.string.no_matches_message)
            SearchEmptyStateMode.CATALOG_UNAVAILABLE -> getString(R.string.catalog_unavailable_message)
            SearchEmptyStateMode.ERROR -> getString(R.string.search_error_message)
        }
    }

    private enum class SearchEmptyStateMode {
        INITIAL,
        NO_MATCHES,
        CATALOG_UNAVAILABLE,
        ERROR,
    }
}
