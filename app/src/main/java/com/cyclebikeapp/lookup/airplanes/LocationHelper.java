package com.cyclebikeapp.lookup.airplanes;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import static android.content.Context.LOCATION_SERVICE;
import static com.cyclebikeapp.lookup.airplanes.Constants.LOCATION_INTERVAL;
import static com.cyclebikeapp.lookup.airplanes.Constants.ONE_MINUTE;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREFS_DEFAULT_TIME;
import static com.cyclebikeapp.lookup.airplanes.Constants.nanosPerMillis;

/**
 * Created by TommyD on 3/4/2016.
 * Use Google Play Service FusedLocationApi to get coarse user Location for airplane look angle calculations
 * Only need one coarse Location; altitude doesn't matter. First get the Last Known Location and save it in my Prefs.
 * If we don't have data connection, at least we'll have a location.
 * If user has Location Services set to allow fine Location using GPS, we'll try to use that. Otherwise try to get Location
 * over WiFi or Mobile. Once we have a new Location, save it in my Prefs and call stopLocationUpdates.
 */
class LocationHelper {
    private final MyLocationListener mLocationListener;
    private final LocationManager mLocationManager;
    private long systemTimeOffset;
    private final Context mContext;
    private Location myLocation;
    private float magDeclination;
    private final LocationCallback mLocationCallback;
    private final FusedLocationProviderClient mFusedLocationClient;

    LocationHelper(Context context) {
        systemTimeOffset = android.os.SystemClock.elapsedRealtimeNanos() / nanosPerMillis - System.currentTimeMillis();
        mContext = context;
        mLocationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        mLocationListener = new MyLocationListener();
        magDeclination = Util.getDeclinationFromSharedPrefs(context);
        setMyLocation(Util.getLocFromSharedPrefs(context));
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                long thisLocationTime = PREFS_DEFAULT_TIME;
                for (Location location : locationResult.getLocations()) {
                    // if more than one location returned only use latest one
                    if (location.getTime() > thisLocationTime) {
                        setMagDeclination(location);
                        thisLocationTime = location.getTime();
                        setMyLocation(location);
                    }
                }
            }
        };
    }

    public long getSystemTimeOffset() {
        return systemTimeOffset;
    }

    /**
     * Use Google Location API to get a user location
     */
    void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.requestLocationUpdates(createLocationRequest(),
                mLocationCallback,
                null);
        try {
            if (mLocationManager != null) {
                mLocationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, ONE_MINUTE, 0, mLocationListener);
            }
        } catch (IllegalStateException ignore) {
        }
    }

    /**
     * stop location updates in onStop() and before starting location updates
     */
    void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(LOCATION_INTERVAL);
        mLocationRequest.setFastestInterval(LOCATION_INTERVAL / 2);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    Location getMyLocation() {
        return myLocation;
    }

    private void setMyLocation(Location loc) {
        myLocation = loc;
        Util.saveLocSharedPrefs(loc, mContext);
    }

    float getMagDeclination() {
        return magDeclination;
    }

    private void setMagDeclination(Location location) {
        GeomagneticField gmf = new GeomagneticField((float) location.getLatitude(),
                (float) location.getLongitude(), (float) location.getAltitude(),
                location.getTime());
        magDeclination = gmf.getDeclination();
    }

    class MyLocationListener implements LocationListener {
        /**
         * Called when the location has changed.
         * We use this only to correct the System clock, not for Location
         * <p> There are no restrictions on the use of the supplied Location object.
         *
         * @param location The new location, as a Location object.
         */
        @Override
        public void onLocationChanged(Location location) {
            systemTimeOffset = android.os.SystemClock.elapsedRealtimeNanos() / nanosPerMillis - location.getTime();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    }

}
