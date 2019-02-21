package com.cyclebikeapp.lookup.airplanes;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.cyclebikeapp.lookup.airplanes.Constants.MOBILE_DATA_SETTING_KEY;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREFS_DEFAULT_ALTITUDE;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREFS_DEFAULT_LATITUDE;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREFS_DEFAULT_LONGITUDE;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREFS_DEFAULT_MAG_DECLINATION;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREFS_DEFAULT_TIME;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREFS_KEY_ALTITUDE;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREFS_KEY_LATITUDE;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREFS_KEY_LONGITUDE;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREFS_KEY_MAG_DECLINATION;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREFS_KEY_TIME;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREFS_NAME;
import static com.cyclebikeapp.lookup.airplanes.Constants.STRING_ZERO;

/**
 * Created by TommyD on 4/18/2016.
 *
 */
class Util {


    static boolean hasInternetConnection(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = null;
        if (cm != null) {
            activeNetwork = cm.getActiveNetworkInfo();
        }
        boolean hasInternetPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED;
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting() && hasInternetPermission;
    }

    static boolean hasMobileDataPermission(Context mContext) {
        SharedPreferences settings = getDefaultSharedPreferences(mContext);
        return Integer.parseInt(settings.getString(MOBILE_DATA_SETTING_KEY, STRING_ZERO)) == 1;
    }

    static boolean hasWifiInternetConnection(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = null;
        if (cm != null) {
            activeNetwork = cm.getActiveNetworkInfo();
        }
        boolean hasInternetPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED;
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting() && hasInternetPermission
                && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
    }

    static boolean isGPSLocationEnabled(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        try {
            if (lm != null) {
                gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            }
        } catch (SecurityException ignored) {
        }
        return gps_enabled && hasLocationPermission(context);
    }

    static boolean isNetworkLocationEnabled(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean network_enabled = false;
        try {
            if (lm != null) {
                network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            }
        } catch (SecurityException ignored) {
        }
        return network_enabled;
    }

    /* Checks if external storage is available for read and write */
    static boolean isExternalStorageWritable(Context context) {
        String state = Environment.getExternalStorageState();
        boolean hasWritePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (MainActivity.DEBUG) { Log.i("Util", "isExternalStorageWritable? "
                + (Environment.MEDIA_MOUNTED.equals(state) && hasWritePermission?"yes":"no"));}
        return Environment.MEDIA_MOUNTED.equals(state) && hasWritePermission;
    }

    /**
     * Test if the Location is the program default, meaning we have never received a Location
     *
     * @param aLoc the location to test
     * @return true if the location is the default
     */

    static boolean locationIsDefault(Location aLoc) {
        return aLoc.getTime() == PREFS_DEFAULT_TIME
                && aLoc.getLatitude() == Double.parseDouble(PREFS_DEFAULT_LATITUDE)
                && aLoc.getLongitude() == Double.parseDouble(PREFS_DEFAULT_LONGITUDE)
                && aLoc.getAltitude() == Double.parseDouble(PREFS_DEFAULT_ALTITUDE);
    }
    static void saveLocSharedPrefs(Location location, Context mContext) {
        SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(PREFS_KEY_TIME, location.getTime());
        editor.putString(PREFS_KEY_ALTITUDE, String.valueOf(location.getAltitude()));
        editor.putString(PREFS_KEY_LATITUDE, String.valueOf(location.getLatitude()));
        editor.putString(PREFS_KEY_LONGITUDE, String.valueOf(location.getLongitude())).apply();
        // save magnetic declination to sharedPrefs for this Location
        saveMagDeclinationSharedPrefs(location, editor);
        // only call saveLocSharedPrefs when new Location received, so we should indicate rebuildGEOSatellites with new Location
    }
    static Location getLocFromSharedPrefs(Context mContext) {
        Location aLoc = new Location(LocationManager.NETWORK_PROVIDER);
        SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
        aLoc.setLongitude(Double.parseDouble(settings.getString(PREFS_KEY_LONGITUDE, PREFS_DEFAULT_LONGITUDE)));
        aLoc.setLatitude(Double.parseDouble(settings.getString(PREFS_KEY_LATITUDE, PREFS_DEFAULT_LATITUDE)));
        aLoc.setAltitude(Double.parseDouble(settings.getString(PREFS_KEY_ALTITUDE, PREFS_DEFAULT_ALTITUDE)));
        // this is just temporary until we get a location from LocationHelper
        aLoc.setTime(settings.getLong(PREFS_KEY_TIME, PREFS_DEFAULT_TIME));
        return aLoc;
    }
    static float getDeclinationFromSharedPrefs(Context mContext) {
        SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
        return settings.getFloat(PREFS_KEY_MAG_DECLINATION, PREFS_DEFAULT_MAG_DECLINATION);
    }

    private static void saveMagDeclinationSharedPrefs(Location location, SharedPreferences.Editor editor) {
        GeomagneticField gmf = new GeomagneticField((float) location.getLatitude(),
                (float) location.getLongitude(), (float) location.getAltitude(),
                location.getTime());
        editor.putFloat(PREFS_KEY_MAG_DECLINATION, gmf.getDeclination()).apply();
    }

    static boolean hasCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    static boolean hasFineLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    static boolean hasStoragePermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    static File getAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), albumName);
        if (!file.exists()) {
            if (!file.mkdirs()){
                if (MainActivity.DEBUG) {
                    Log.w("getAlbumStorageDir", "directory " + file.toString() + " not created");}
            } else{
                if (MainActivity.DEBUG) {
                    Log.w("getAlbumStorageDir", "directory " + file.toString() + " created");}
            }
        } else {
            if (MainActivity.DEBUG) {
                Log.w("getAlbumStorageDir", "directory " + file.toString() + " already exists");}
        }
        return file;
    }

    static boolean isAnySnackBarVisible(ArrayList<Snackbar> snackbars){
        for (Snackbar sb :snackbars){
            if (sb != null && sb.isShown()){
                return  true;
            }
        }
        return  false;
    }
    static void closeAllSnackbars(ArrayList<Snackbar> snackbars){
        for (Snackbar sb :snackbars){
            if (sb != null && sb.isShown()){
                sb.dismiss();
            }
        }
    }

}
