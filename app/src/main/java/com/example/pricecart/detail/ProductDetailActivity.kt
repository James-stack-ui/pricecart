package com.example.pricecart.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pricecart.R
import com.example.pricecart.data.FavouritesRepository
import com.example.pricecart.data.PriceRepository
import com.example.pricecart.data.model.ProductOfferDetails
import com.example.pricecart.data.model.UserCoordinates
import com.example.pricecart.databinding.ActivityProductDetailBinding
import com.example.pricecart.favourites.FavouriteItemHelper
import com.example.pricecart.favourites.FavouriteToggleAction
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DateFormat
import java.util.Date

class ProductDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductDetailBinding

    private val priceRepository = PriceRepository(firestore = FirebaseFirestore.getInstance())
    private val favouritesRepository = FavouritesRepository()

    private lateinit var productName: String
    private lateinit var storeName: String
    private var userCoordinates: UserCoordinates? = null
    private var isCurrentItemSaved: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        productName = intent.getStringExtra(EXTRA_PRODUCT_NAME).orEmpty()
        storeName = intent.getStringExtra(EXTRA_STORE_NAME).orEmpty()
        userCoordinates = extractUserCoordinates(intent)

        binding.detailToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.detailFavouriteButton.setOnClickListener {
            toggleFavourite()
        }
        binding.compareMoreButton.setOnClickListener {
            finish()
        }

        if (productName.isBlank() || storeName.isBlank()) {
            Toast.makeText(this, getString(R.string.product_detail_not_found_message), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        loadOfferDetails()
    }

    override fun onResume() {
        super.onResume()
        refreshFavouriteState()
    }

    private fun loadOfferDetails() {
        priceRepository.getProductOfferDetails(
            productName = productName,
            storeName = storeName,
            userCoordinates = userCoordinates,
            onSuccess = { offerDetails ->
                if (offerDetails == null) {
                    Toast.makeText(this, getString(R.string.product_detail_not_found_message), Toast.LENGTH_LONG).show()
                    finish()
                    return@getProductOfferDetails
                }

                bindOfferDetails(offerDetails)
                refreshFavouriteState()
            },
            onError = {
                Toast.makeText(this, getString(R.string.product_detail_not_found_message), Toast.LENGTH_LONG).show()
                finish()
            },
        )
    }

    private fun bindOfferDetails(offerDetails: ProductOfferDetails) {
        binding.detailToolbar.title = offerDetails.productName
        binding.detailProductNameTextView.text = offerDetails.productName
        binding.detailStoreNameTextView.text = offerDetails.storeName
        binding.detailPriceTextView.text = getString(
            R.string.price_format,
            offerDetails.currency,
            offerDetails.price,
        )
        binding.detailRankBadgeTextView.text = if (offerDetails.rank == 1) {
            getString(R.string.best_match_badge)
        } else {
            getString(R.string.rank_badge, offerDetails.rank)
        }
        binding.detailDistanceTextView.text = offerDetails.distanceKilometers?.let { distance ->
            getString(R.string.distance_kilometers, distance)
        } ?: getString(R.string.distance_unavailable)
        binding.detailUpdatedAtTextView.text = offerDetails.updatedAt?.let { updatedAt ->
            getString(
                R.string.updated_at_format,
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(updatedAt)),
            )
        } ?: getString(R.string.updated_at_unavailable)
        binding.detailStoreNameSnapshotTextView.text = getString(
            R.string.store_snapshot_store_name_format,
            storeSnapshotValue(offerDetails.storeName),
        )
        binding.detailStoreNeighborhoodTextView.text = getString(
            R.string.store_snapshot_neighborhood_format,
            storeSnapshotValue(offerDetails.storeDetails.neighborhood),
        )
        binding.detailStoreAddressTextView.text = getString(
            R.string.store_snapshot_address_format,
            storeSnapshotValue(offerDetails.storeDetails.address),
        )
        binding.detailStoreCoordinatesTextView.text = formatStoreCoordinates(offerDetails)
        binding.detailStoreHoursTextView.text = getString(
            R.string.store_hours_format,
            storeSnapshotValue(offerDetails.storeDetails.hours),
        )
        binding.detailStorePhoneTextView.text = getString(
            R.string.store_phone_format,
            storeSnapshotValue(offerDetails.storeDetails.phoneNumber),
        )
        binding.detailStoreNoteTextView.text = getString(
            R.string.store_snapshot_note_format,
            storeSnapshotValue(offerDetails.storeDetails.note),
        )
        binding.detailComparisonTitleTextView.text = getString(
            R.string.store_rank_summary,
            offerDetails.rank,
            offerDetails.totalOfferCount,
        )
        binding.detailComparisonBodyTextView.text = if (offerDetails.savingsComparedToBest <= 0.0) {
            getString(R.string.best_price_detail_message)
        } else {
            getString(
                R.string.price_gap_detail_message,
                offerDetails.currency,
                offerDetails.savingsComparedToBest,
                offerDetails.currency,
                offerDetails.bestPrice,
            )
        }
    }

    private fun storeSnapshotValue(rawValue: String): String {
        return rawValue.trim().ifBlank {
            getString(R.string.store_snapshot_value_unavailable)
        }
    }

    private fun formatStoreCoordinates(offerDetails: ProductOfferDetails): String {
        val latitude = offerDetails.storeLatitude
        val longitude = offerDetails.storeLongitude
        return if (latitude != null && longitude != null) {
            getString(R.string.store_snapshot_coordinates_format, latitude, longitude)
        } else {
            getString(R.string.store_snapshot_coordinates_unavailable)
        }
    }

    private fun refreshFavouriteState() {
        favouritesRepository.isFavouriteItemSaved(
            productName = productName,
            onSuccess = { isSaved ->
                isCurrentItemSaved = isSaved
                updateFavouriteButton()
            },
            onError = {
                isCurrentItemSaved = false
                updateFavouriteButton()
            },
        )
    }

    private fun toggleFavourite() {
        when (FavouriteItemHelper.resolveToggleAction(isCurrentItemSaved)) {
            FavouriteToggleAction.SAVE -> {
                favouritesRepository.saveFavouriteItem(
                    productName = productName,
                    onSuccess = {
                        isCurrentItemSaved = true
                        updateFavouriteButton()
                        Toast.makeText(
                            this,
                            getString(R.string.favourite_saved_message),
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    onError = { exception ->
                        Toast.makeText(
                            this,
                            exception.localizedMessage ?: getString(R.string.favourites_error_message),
                            Toast.LENGTH_LONG,
                        ).show()
                    },
                )
            }

            FavouriteToggleAction.REMOVE -> {
                favouritesRepository.removeFavouriteItem(
                    productName = productName,
                    onSuccess = {
                        isCurrentItemSaved = false
                        updateFavouriteButton()
                        Toast.makeText(
                            this,
                            getString(R.string.favourite_removed_message),
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    onError = { exception ->
                        Toast.makeText(
                            this,
                            exception.localizedMessage ?: getString(R.string.favourites_error_message),
                            Toast.LENGTH_LONG,
                        ).show()
                    },
                )
            }
        }
    }

    private fun updateFavouriteButton() {
        binding.detailFavouriteButton.text = getString(
            if (isCurrentItemSaved) {
                R.string.saved_button
            } else {
                R.string.save_button
            },
        )
    }

    private fun extractUserCoordinates(intent: Intent): UserCoordinates? {
        val latitude = intent.getDoubleExtra(EXTRA_USER_LATITUDE, Double.NaN)
        val longitude = intent.getDoubleExtra(EXTRA_USER_LONGITUDE, Double.NaN)
        return if (!latitude.isNaN() && !longitude.isNaN()) {
            UserCoordinates(latitude = latitude, longitude = longitude)
        } else {
            null
        }
    }

    companion object {
        private const val EXTRA_PRODUCT_NAME = "extra_product_name"
        private const val EXTRA_STORE_NAME = "extra_store_name"
        private const val EXTRA_USER_LATITUDE = "extra_user_latitude"
        private const val EXTRA_USER_LONGITUDE = "extra_user_longitude"

        fun createIntent(
            context: Context,
            productName: String,
            storeName: String,
            userCoordinates: UserCoordinates? = null,
        ): Intent {
            return Intent(context, ProductDetailActivity::class.java).apply {
                putExtra(EXTRA_PRODUCT_NAME, productName)
                putExtra(EXTRA_STORE_NAME, storeName)
                userCoordinates?.let { coordinates ->
                    putExtra(EXTRA_USER_LATITUDE, coordinates.latitude)
                    putExtra(EXTRA_USER_LONGITUDE, coordinates.longitude)
                }
            }
        }
    }
}
