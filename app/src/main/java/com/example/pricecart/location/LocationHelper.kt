package com.example.pricecart.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.pricecart.data.model.UserCoordinates
import com.example.pricecart.data.model.UserLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.io.IOException
import java.util.Locale

class LocationHelper(context: Context) {
    private val appContext = context.applicationContext
    private val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(appContext)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun fetchCurrentLocation(
        onSuccess: (UserLocation) -> Unit,
        onUnavailable: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        if (!hasLocationPermission()) {
            dispatchUnavailable(onUnavailable)
            return
        }

        val cancellationTokenSource = CancellationTokenSource()
        try {
            fusedLocationProviderClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token,
            ).addOnSuccessListener { location ->
                if (location != null) {
                    buildUserLocation(
                        coordinates = UserCoordinates(location.latitude, location.longitude),
                        onSuccess = onSuccess,
                    )
                } else {
                    fetchLastKnownLocation(onSuccess, onUnavailable, onError)
                }
            }.addOnFailureListener {
                fetchLastKnownLocation(onSuccess, onUnavailable, onError)
            }
        } catch (exception: SecurityException) {
            dispatchError(exception, onError)
        }
    }

    private fun fetchLastKnownLocation(
        onSuccess: (UserLocation) -> Unit,
        onUnavailable: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        if (!hasLocationPermission()) {
            dispatchUnavailable(onUnavailable)
            return
        }

        try {
            fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        buildUserLocation(
                            coordinates = UserCoordinates(location.latitude, location.longitude),
                            onSuccess = onSuccess,
                        )
                    } else {
                        dispatchUnavailable(onUnavailable)
                    }
                }
                .addOnFailureListener { exception ->
                    dispatchError(exception, onError)
                }
        } catch (exception: SecurityException) {
            dispatchError(exception, onError)
        }
    }

    private fun buildUserLocation(
        coordinates: UserCoordinates,
        onSuccess: (UserLocation) -> Unit,
    ) {
        reverseGeocode(
            coordinates = coordinates,
            onResolved = { formattedAddress ->
                dispatchSuccess(
                    UserLocation(
                        coordinates = coordinates,
                        formattedAddress = formattedAddress,
                    ),
                    onSuccess,
                )
            },
        )
    }

    private fun dispatchSuccess(
        userLocation: UserLocation,
        onSuccess: (UserLocation) -> Unit,
    ) {
        mainHandler.post { onSuccess(userLocation) }
    }

    private fun dispatchUnavailable(onUnavailable: () -> Unit) {
        mainHandler.post(onUnavailable)
    }

    private fun dispatchError(
        exception: Exception,
        onError: (Exception) -> Unit,
    ) {
        mainHandler.post { onError(exception) }
    }

    private fun reverseGeocode(
        coordinates: UserCoordinates,
        onResolved: (String?) -> Unit,
    ) {
        if (!Geocoder.isPresent()) {
            onResolved(null)
            return
        }

        val geocoder = Geocoder(appContext, Locale.getDefault())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(
                coordinates.latitude,
                coordinates.longitude,
                1,
            ) { addresses ->
                onResolved(addresses.firstOrNull()?.getAddressLine(0))
            }
            return
        }

        try {
            @Suppress("DEPRECATION")
            val address = geocoder.getFromLocation(
                coordinates.latitude,
                coordinates.longitude,
                1,
            )?.firstOrNull()
            onResolved(address?.getAddressLine(0))
        } catch (_: IOException) {
            onResolved(null)
        } catch (_: IllegalArgumentException) {
            onResolved(null)
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        return fineLocationPermission == PackageManager.PERMISSION_GRANTED ||
            coarseLocationPermission == PackageManager.PERMISSION_GRANTED
    }
}
