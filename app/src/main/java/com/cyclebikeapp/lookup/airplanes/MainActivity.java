package com.cyclebikeapp.lookup.airplanes;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SoundEffectConstants;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.PopupMenu;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import opensky.OpenSkyApi;
import opensky.OpenSkyStates;
import opensky.StateVector;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.cyclebikeapp.lookup.airplanes.Constants.CC_MAP_KEY_AIRPLANE_NAME;
import static com.cyclebikeapp.lookup.airplanes.Constants.CC_MAP_KEY_ICAO24;
import static com.cyclebikeapp.lookup.airplanes.Constants.DB_KEY_AIR_REGISTRATION;
import static com.cyclebikeapp.lookup.airplanes.Constants.DEF_LOS_AZIMUTH;
import static com.cyclebikeapp.lookup.airplanes.Constants.DEF_LOS_ELEV;
import static com.cyclebikeapp.lookup.airplanes.Constants.DISTANCE_TYPE_MILE;
import static com.cyclebikeapp.lookup.airplanes.Constants.FORMAT_3_2F;
import static com.cyclebikeapp.lookup.airplanes.Constants.LOCATION_IS_OLD;
import static com.cyclebikeapp.lookup.airplanes.Constants.LOOKAFTERSTUFF_INITIAL_DELAY_TIME;
import static com.cyclebikeapp.lookup.airplanes.Constants.LOOKAFTERSTUFF_REPEAT_TIME;
import static com.cyclebikeapp.lookup.airplanes.Constants.MAX_CLICK_DISTANCE;
import static com.cyclebikeapp.lookup.airplanes.Constants.MAX_CLICK_DURATION;
import static com.cyclebikeapp.lookup.airplanes.Constants.MAX_ELEV;
import static com.cyclebikeapp.lookup.airplanes.Constants.MAX_LIVE_ZOOM;
import static com.cyclebikeapp.lookup.airplanes.Constants.MAX_TOTAL_ZOOM;
import static com.cyclebikeapp.lookup.airplanes.Constants.MIN_TOTAL_ZOOM;
import static com.cyclebikeapp.lookup.airplanes.Constants.MOBILE_DATA_SETTING_KEY;
import static com.cyclebikeapp.lookup.airplanes.Constants.NAV_DRAWER_LIVE_MODE_KEY;
import static com.cyclebikeapp.lookup.airplanes.Constants.PAID_VERSION;
import static com.cyclebikeapp.lookup.airplanes.Constants.PERMISSIONS_REQUEST_CAMERA;
import static com.cyclebikeapp.lookup.airplanes.Constants.PERMISSIONS_REQUEST_LOCATION;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREFS_KEY_LIVE_MODE;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREFS_KEY_LOSAZ;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREFS_KEY_LOSEL;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREFS_KEY_PANAZ;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREFS_KEY_PANEL;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREFS_KEY_SHOWN_LIVEMODE_HINT;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREFS_KEY_SHOWN_PAUSEDMODE_HINT;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREFS_KEY_TEMP_PANAZ;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREFS_KEY_TEMP_PANEL;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREFS_KEY_UNIT;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREFS_NAME;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREF_KEY_MAX_RANGE;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREF_KEY_READING_AIRFILES;
import static com.cyclebikeapp.lookup.airplanes.Constants.RC_NAV_DRAWER;
import static com.cyclebikeapp.lookup.airplanes.Constants.RECALC_LOOKANGLES_INITIAL_DELAY_TIME;
import static com.cyclebikeapp.lookup.airplanes.Constants.RECALC_LOOKANGLES_REPEAT_TIME;
import static com.cyclebikeapp.lookup.airplanes.Constants.REQUEST_CHANGE_LOCATION_SETTINGS;
import static com.cyclebikeapp.lookup.airplanes.Constants.REQUEST_CHANGE_WIFI_SETTINGS;
import static com.cyclebikeapp.lookup.airplanes.Constants.REQUEST_CHECK_SETTINGS;
import static com.cyclebikeapp.lookup.airplanes.Constants.ZERO;
import static com.cyclebikeapp.lookup.airplanes.Constants.km_per_meter;
import static com.cyclebikeapp.lookup.airplanes.Constants.maxRangeValuesMetric;
import static com.cyclebikeapp.lookup.airplanes.Constants.maxRangeValuesMiles;
import static com.cyclebikeapp.lookup.airplanes.Constants.mile_per_meter;
import static com.cyclebikeapp.lookup.airplanes.Constants.nanosPerMillis;
import static com.cyclebikeapp.lookup.airplanes.Utilities.airplaneFilesWereRead;
import static com.cyclebikeapp.lookup.airplanes.Utilities.getMoreInfoURI;
import static com.cyclebikeapp.lookup.airplanes.Utilities.readingAirplaneFiles;

@SuppressWarnings("ConstantConditions")
public class MainActivity extends AppCompatActivity
        implements SurfaceHolder.Callback, SensorEventListener, PopupMenu.OnMenuItemClickListener, SharedPreferences.OnSharedPreferenceChangeListener {
    // ** change these values depending on APK type (DEVELOPMENT OR PRODUCTION)
    public static final int version = PAID_VERSION;
    public static final boolean DEBUG = false;
    // Timers and Handlers for periodic tasks
    private Handler lookAfterStuffHandler;
    private Timer lookAfterStuffTimer;
    private TimerTask lookAfterStuff;
    private Handler recalcLookAnglesHandler;
    private Timer recalcLookAnglesTimer;
    private TimerTask recalcLookAnglesTask;
    // all the Location functions
    LocationHelper mLocationHelper;
    // A handle to the View in which the satellites are plotted
    private GridSatView mGridSatView;
    // this is where we put the camera preview
    private SurfaceHolder mHolder;
    // a Class to set-up and maintain the camera preview in LiveMode
    private CameraPreview mCameraPreview;
    private SensorManager mSensorManager;
    private Sensor sensorGravity;
    private Sensor sensorAccel;
    private Sensor sensorMagField;
    // data from Sensors
    private float[] mGravityVector;
    private float[] mMagFieldValues;
    // some Android devices don't have a compass
    private boolean hasMagSensor;
    // gesture detector to handle pinch zooming for camera and satellite canvas
    private ScaleGestureDetector mScaleDetector;

    // low pass filter parameter, depending on gravity- or accelerometer-type sensor
    private double accelFilterAlpha;
    // display once-per-app-launch user-requests about Services' configuration
    private boolean complainedLocationOld;
    private boolean complainedMagSensor;
    private boolean complainedCameraPermission;
    // pop-up windows directing user to make System Settings changes
    private Snackbar mMobileDataPermissionSnackbar;
    private Snackbar mWirelessSettingsSnackbar;
    private Snackbar mHintSnackBar;
    private Snackbar mLocationSettingsSnackBar;
    private Snackbar mCameraPermissionSnackBar;
    private ArrayList<Snackbar> snackBarList;
    private OpenSkyApi osAPI;
    static final  String openskyRoot = "https://opensky-network.org/api";
    private static final  String C61JDI71MITZ5JIZW6U1 = "TommyD";
    private static final  String WLWSEOO1P6G5G7HHIPVB = "sweeney";
    // all the database functions
    private AirplaneDBAdapter dataBaseAdapter = null;
    private Double latMin;
    private Double latMax;
    private Double longMin;
    private Double longMax;
    static final int MY_PERMISSIONS_REQUEST_WRITE = 824;
    private int readStateVectorsCounter = 0;
    private boolean openskyAccessOkay = true;

    /**
     * Invoked when the Activity is created.
     *
     * @param savedInstanceState a Bundle containing state saved from a previous
     *        execution, or null if this is a new execution
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGridSatView = new GridSatView(this, null);

        osAPI = new OpenSkyApi(C61JDI71MITZ5JIZW6U1, WLWSEOO1P6G5G7HHIPVB);
        mLocationHelper = new LocationHelper(getApplicationContext());
        dataBaseAdapter = new AirplaneDBAdapter(getApplicationContext());
        // Handlers and Timers for repeating tasks
        lookAfterStuffHandler = new Handler();
        lookAfterStuffTimer = new Timer();
        recalcLookAnglesHandler = new Handler();
        recalcLookAnglesTimer = new Timer();
        snackBarList = new ArrayList<>();
        //test if GooglePlay Services is available and up to date
        googlePlayAvailable(getApplicationContext());
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mCameraPreview = new CameraPreview(this);
        mHolder = mCameraPreview.getHolder();
        // we handle all the changes to the camera preview surface from MainActivity
        mHolder.addCallback(this);
        setContentView(R.layout.activity_main);
        addContentView(mCameraPreview, new FrameLayout.LayoutParams
                (FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        addContentView(mGridSatView, new FrameLayout.LayoutParams
                (FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        mGridSatView.invalidate();
        // "complaining" means showing a Snackbar dialog
        complainedLocationOld = false;
        // some devices don't have a magnetic compass; we'll pop-up a dialog to complain that Live Mode doesn't work
        complainedMagSensor = false;
        complainedCameraPermission = false;
        //handles pinch zoom input
        mScaleDetector = new ScaleGestureDetector(getApplicationContext(), scaleGestureListener());
        new ThreadPerTaskExecutor().execute(readAirlineCodesFromAssetsRunnable);
        dataBaseAdapter.open();
        dataBaseAdapter.readAirplaneFileFromAssetAsync();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // handle the preference change here
        if (DEBUG) { Log.i(this.getClass().getSimpleName(), "onSharedPreferenceChanged() - key: " + key); }
        switch (key) {
            case PREFS_KEY_UNIT:
                if (DEBUG) { Log.i(this.getClass().getSimpleName(), "onSharedPreferenceChanged() - UNITS"); }
                mGridSatView.distanceUnit = sharedPreferences.getString(PREFS_KEY_UNIT, ZERO);
            break;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {

            case PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationHelper.stopLocationUpdates();
                    mLocationHelper.startLocationUpdates();
                }
            }//location permissions case
            break;
            case PERMISSIONS_REQUEST_CAMERA: {
                // restart camera
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mCameraPreview.stopPreview();
                    if (mCameraPreview.mCamera == null) {
                        mCameraPreview.connectCamera(mHolder);
                    }
                    mCameraPreview.setPreviewSize();
                    // we're told to find camera zoom ratios after changing the Preview Size; zoom ratios may be different for different sizes
                    mCameraPreview.configureCamera();
                    if (mGridSatView.isLiveMode()) {
                        if (DEBUG) {Log.w(this.getClass().getName(), "starting camera preview");}
                        mCameraPreview.startPreview();
                    }
                }
            }// camera permissions case
            break;
        }// switch
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        switch (requestCode) {
            case RC_NAV_DRAWER:
                if (DEBUG) {
                    Log.w(this.getClass().getName(), "returning from Nav_Drawer Activity");
                }
                if (resultCode == RESULT_OK) {
                    boolean liveMode = data.getExtras().getBoolean(NAV_DRAWER_LIVE_MODE_KEY);
                    mGridSatView.setLiveMode(liveMode);
                    SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, 0).edit();
                    editor.putBoolean(PREFS_KEY_LIVE_MODE, mGridSatView.isLiveMode()).apply();
                }
                break;
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made user changed location settings
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change Location settings, but chose not to
                        break;
                    default:
                        break;
                }
                break;
            case REQUEST_CHANGE_LOCATION_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        mLocationHelper.stopLocationUpdates();
                        mLocationHelper.startLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change Location settings, but chose not to
                        break;
                    default:
                        break;
                }
                break;
            case REQUEST_CHANGE_WIFI_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change WiFi settings, but chose not to
                        // pretend that we've updated TLEs today so LookAfterStuff doesn't keep asking
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    /**
     * User has clicked in the Menu floating action button;
     * 1) save a previewImage if we're in LiveMode
     * 2) save the sharingImage that looks like the screen without FABs
     * 3) start the NavigationDrawer Activity
     */
    private void doMenuClick() {
        //start NavigationDrawerActivity
        if (mGridSatView.isLiveMode()) {
            // we are live, capture previewImage and save sharingImage
            mCameraPreview.startPreviewCallback(mGridSatView);
            // set flag if previewImage is portrait mode
            setPreviewImageRotation();
        } else {
            // we are paused, just save the sharingImage, don't have to capture a new one
            mGridSatView.saveSharingImage = true;
            mGridSatView.invalidate();
        }
        mGridSatView.setLiveMode(false);
        final Intent navigationDrawerIntent = new Intent(this, NavDrawerFabActivity.class);
        // delay navigationDrawer activity until sharing image is saved
        mGridSatView.postDelayed(new Runnable() {
            @Override
            public void run() {
                navigationDrawerIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivityForResult(navigationDrawerIntent, RC_NAV_DRAWER);
            }
        }, 100);
    }

    /**
     * If we're in Portrait Mode, the previewImage is rotated on its side
     * we'll have to correct this when using the preview image as background or for the sharingImage
     */
    private void setPreviewImageRotation() {
        boolean previewImageRotation = false;
        if (getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_0) {
            previewImageRotation = true;
        }
        mGridSatView.setPreviewImageRotation(previewImageRotation);
    }

    /**
     * User clicked in the Play (or Pause) button
     * If we were Live, save a previewImage. If we were Paused,
     * start the camera Preview Mode and reset pan and zoom parameters
     */
    private void doPlayPauseClick() {
        if (mGridSatView.isLiveMode()) {
            //capture a previewImage to use for sharingImage and under canvas when panning and zooming
            mCameraPreview.startPreviewCallback(mGridSatView);
            // set flag if previewImage is portrait mode
            setPreviewImageRotation();
            mGridSatView.setLiveMode(false);
        } else {// we will be Live
            mCameraPreview.startPreview();
            mGridSatView.setLiveMode(true);
            // now in liveMode where we don't pan, so reset pan parameters
            mGridSatView.resetPan();
            // now in LiveMode only zoom using the camera; set pausedZoom = 1;
            mGridSatView.setPausedZoomFactor(1.);
            mGridSatView.invalidate();
        }
    }

    @Override
    protected void onDestroy() {
        if (DEBUG){Log.w(this.getClass().getName(), "onDestroy()");}
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        if (DEBUG){Log.w(this.getClass().getName(), "onResume()");}
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        // if we don't have an internet connection, WiFi or Mobile, but we have fine location permission and GPS location enabled,
        // try to get location from GPS; default is location from Network provider.
        // If that is disabled and can't get GPS Location, we'll ask to correct later.
        initializeSnackBars();
        if (askLocationPermission()) {
            mLocationHelper.stopLocationUpdates();
            mLocationHelper.startLocationUpdates();
        }
        // we close the database during onStop to release resources; reconnect to DB here if it's closed
        try {
            if (dataBaseAdapter != null && dataBaseAdapter.isClosed()) {
                dataBaseAdapter.open();
            }
        } catch (SQLException ignored) {
        }
        // may have to connect camera if mCamera is null? or disconnected
/*        if (mCameraPreview.mCamera == null){
            mCameraPreview.connectCamera(mHolder);
        }*/
        setLocationStatus();
        stopLookingAfterStuff();
        stopRecalculatingLookAngles();
        startLookingAfterStuff();
        startRecalculatingLookAngles();
        restoreSharedPreferences();
        registerSensorListeners(SensorManager.SENSOR_DELAY_GAME);
    }

    private boolean askLocationPermission() {
        if (Util.hasFineLocationPermission(getApplicationContext())) {
            if (DEBUG) Log.i(this.getClass().getName(), "ask Location permission: has location permission");
            return true;
        } else {
            // Request permission
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_LOCATION);
            return false;
        }
    }

    private void restoreSharedPreferences() {

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        mGridSatView.setLiveMode(settings.getBoolean(PREFS_KEY_LIVE_MODE, true));
        // GEO satellites will be at 180 for Northern Latitudes; use that as default
        mGridSatView.setLosAzDeg(settings.getFloat(PREFS_KEY_LOSAZ, DEF_LOS_AZIMUTH));
        mGridSatView.setLosElDeg(settings.getFloat(PREFS_KEY_LOSEL, DEF_LOS_ELEV));
        mGridSatView.tempPanAz = settings.getFloat(PREFS_KEY_TEMP_PANAZ, 0f);
        mGridSatView.tempPanEl = settings.getFloat(PREFS_KEY_TEMP_PANEL, 0f);
        mGridSatView.panAz = settings.getFloat(PREFS_KEY_PANAZ, 0f);
        mGridSatView.panEl = settings.getFloat(PREFS_KEY_PANEL, 0f);
        // copy magDeclination to GridSatView so it can correct longitude line labels
        mGridSatView.magDeclination = mLocationHelper.getMagDeclination();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mGridSatView.distanceUnit = sharedPref.getString(PREFS_KEY_UNIT, Constants.ZERO);
    }

    /**
     * This is how we'll find airplanes given a click on the canvas.
     * If there is only one satellite within the click-tolerance, we'll pop-up the airplane info.
     * If there are more than one, pop-up a context menu listing the airplane names. When user chooses,
     * get icao24 and fetchDeviceData to pop-up airplane info.
     * Feed this from onTouchListener where lookAngAz, lookAngEl are provided. tolerance depends on screen pixel density
     */
    private void handleAirplaneCanvasClick(float lookAngAz, float lookAngEl, float tolerance) {
        final ArrayList<HashMap<String, String>> clickAirList =
                mGridSatView.getCanvasClickAirplanes(lookAngAz, lookAngEl, tolerance);
        // how many airplanes did I find?  print Name, icao24
        if (clickAirList.size() > 0) {
            if (clickAirList.size() > 1){
                //situate a View in the Layout to anchor this pop-up
                final View aView = findViewById(R.id.popup_anchor);
                if (aView != null) {
                    aView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showPopupMenu(aView, clickAirList);
                        }
                    },50);
                }
            } else {
                int selectedItem = 0;
                String icao24 = (clickAirList.get(selectedItem).get(CC_MAP_KEY_ICAO24));
                mGridSatView.setSelectedAirplaneicao24(icao24);
                int airplaneIndex = Utilities.findAirplaneIndex(icao24, mGridSatView.mAirplanes);
                if (airplaneIndex >= mGridSatView.mAirplanes.size() || airplaneIndex < 0) {
                    return;
                }
                mGridSatView.airplaneData = mGridSatView.mAirplanes.get(airplaneIndex);
                mGridSatView.airplaneDBData = dataBaseAdapter.fetchAirplaneData(icao24);
                mGridSatView.invalidate();
            }
        } else {
            // no airplanes selected
            mGridSatView.setSelectedAirplaneicao24(ZERO);
        }
    }

    private void showPopupMenu(View v, ArrayList<HashMap<String, String>> clickAirList) {
        // Don't leave a pop-up on screen if we're leaving MainActivity
        if (MainActivity.this.isFinishing()) {
            return;
        }
        PopupMenu popup = new PopupMenu(MainActivity.this, v);
        // This activity implements OnMenuItemClickListener
        popup.setOnMenuItemClickListener(MainActivity.this);
        //add items to pop-up depending on # satellites
        int order = 1;
        // only 1 group
        int groupId = 0;
        for (HashMap<String, String> hm : clickAirList) {
            // use "itemId" entry to store icao24 number.
            // When user clicks on List we're given itemId and we can then retrieve icao24
            int itemId = Integer.parseInt(hm.get(CC_MAP_KEY_ICAO24), 16);
            popup.getMenu().add(groupId, itemId, order, hm.get(CC_MAP_KEY_AIRPLANE_NAME));
            order++;
        }
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.actions, popup.getMenu());
        popup.show();
    }

    /**
     * User clicked on the pop-up list of airplanes where user clicked on canvas
     *
     * @param item the item number clicked
     * @return true to say this pop-up has been dismissed
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        // need to convert back to hex string to access database
        String icao24 = Integer.toString(item.getItemId(), 16);
        mGridSatView.setSelectedAirplaneicao24(icao24);
        int airplaneIndex = Utilities.findAirplaneIndex(icao24, mGridSatView.mAirplanes);
        if (airplaneIndex >= mGridSatView.mAirplanes.size() || airplaneIndex < 0) {
            return true;
        }
        mGridSatView.airplaneData = mGridSatView.mAirplanes.get(airplaneIndex);
        mGridSatView.airplaneDBData = dataBaseAdapter.fetchAirplaneData(icao24);
        mGridSatView.invalidate();
        return true;
    }

    /**
     * Invoked when the Activity loses user focus.
     */
    @Override
    protected void onPause() {
        if (DEBUG){Log.w(this.getClass().getName(), "onPause()");}
        super.onPause();
        unRegisterSensorListeners();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREFS_KEY_LIVE_MODE, mGridSatView.isLiveMode());
        editor.putFloat(PREFS_KEY_LOSAZ, mGridSatView.getLosAzDeg());
        editor.putFloat(PREFS_KEY_LOSEL,mGridSatView.getLosElDeg());
        editor.putFloat(PREFS_KEY_TEMP_PANAZ, mGridSatView.tempPanAz);
        editor.putFloat(PREFS_KEY_TEMP_PANEL, mGridSatView.tempPanEl);
        editor.putFloat(PREFS_KEY_PANAZ, mGridSatView.panAz);
        editor.putFloat(PREFS_KEY_PANEL, mGridSatView.panEl).apply();
    }

    @Override
    protected void onStop() {
        if (DEBUG){Log.w(this.getClass().getName(), "onStop()");}
        super.onStop();
        if (dataBaseAdapter != null) {
            dataBaseAdapter.close();
        }
        mLocationHelper.stopLocationUpdates();
        stopLookingAfterStuff();
        stopRecalculatingLookAngles();
        mCameraPreview.stopPreview();
        mCameraPreview.releaseCamera();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * recalculate look angles for fast-moving airplanes; only in paid version
     */
    private void startRecalculatingLookAngles() {
        recalcLookAnglesTask = new TimerTask() {
            @Override
            public void run() {
                recalcLookAnglesHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // get current UTC time
                        long currentUTC = SystemClock.elapsedRealtimeNanos() / nanosPerMillis - mLocationHelper.getSystemTimeOffset();
                        //Log.w(this.getClass().getName(), String.format("recalc - currentUTC: %d currentTimeMillis(): %d ", currentUTC, System.currentTimeMillis()));
                        for (Airplane mAirplane : mGridSatView.mAirplanes) {
                            // get time difference between currentUTC time and airplace lastPositionUpdate time in milliseconds
                            // recalculate airplane new location and altitude, and look angles from myLocation
                            if (mAirplane.getSv().getLastPositionUpdate() != null) {
                                long deltaTime = (long) (currentUTC - mAirplane.getSv().getLastPositionUpdate() * 1000);
                                mAirplane.recalculatePosition(deltaTime, mLocationHelper.getMyLocation());
                            }
                        }
                        readStateVectorsCounter++;
                        if (readStateVectorsCounter > 5010 / RECALC_LOOKANGLES_REPEAT_TIME){
                            readStateVectorsCounter = 0;
                            if (DEBUG) {
                                Log.w(this.getClass().getName(), "lookAfterStuff -airplane files were read: "
                                        + (airplaneFilesWereRead(getSharedPreferences(PREFS_NAME, 0)) ? "yes" : "no")
                                        + " reading  airplane files: "
                                        + (getSharedPreferences(PREFS_NAME, 0).getBoolean(PREF_KEY_READING_AIRFILES, false) ? "yes" : "no"));
                            }
                            if (!airplaneFilesWereRead(getSharedPreferences(PREFS_NAME, 0))
                                    && !readingAirplaneFiles(getSharedPreferences(PREFS_NAME, 0))) {
                                if (DEBUG) {
                                    Log.w(this.getClass().getName(), "re-reading airplaneFile...");
                                }
                                dataBaseAdapter.readAirplaneFileFromAssetAsync();
                            } else {
                                boolean hasWifi = Util.hasWifiInternetConnection(getApplicationContext());
                                boolean hasConnection = Util.hasInternetConnection(getApplicationContext());
                                boolean hasPermission = Util.hasMobileDataPermission(getApplicationContext());
                                if (hasWifi || (hasConnection && hasPermission)) {
                                    new RetrieveNewStateVectorsBackground().execute();
                                }
                                mGridSatView.loadingAirplanes = false;
                            }

                        }
                        //it's nice to update locationStatus quickly here
                        mGridSatView.setLocationStatus(testSharedPrefsLocationIsCurrent());
                        // redraw the airplanes
                        mGridSatView.invalidate();
                    }
                });// Runnable
            }
        };
        recalcLookAnglesTimer.schedule(recalcLookAnglesTask, RECALC_LOOKANGLES_INITIAL_DELAY_TIME, RECALC_LOOKANGLES_REPEAT_TIME);
    }

    private void stopRecalculatingLookAngles() {
        recalcLookAnglesHandler.removeCallbacksAndMessages(null);
        if (recalcLookAnglesTask != null) {
            recalcLookAnglesTask.cancel();
            recalcLookAnglesTask = null;
        }
    }

    private class RetrieveNewStateVectorsBackground extends AsyncTask<Void, Void, OpenSkyStates> {

        @Override
        protected OpenSkyStates doInBackground(Void... params) {
            OpenSkyStates os = null;
            try {
                calculateStateVectorBounds(mLocationHelper.getMyLocation());
                os = osAPI.getStates(0, null,
                        new OpenSkyApi.BoundingBox(latMin, latMax, longMin, longMax));

            } catch (IOException e) {
                if (DEBUG){e.printStackTrace();}
            }
            return os;
        }

        @Override
        protected void onPostExecute(OpenSkyStates os) {
            super.onPostExecute(os);
            if (os != null) {
                openskyAccessOkay = true;
                mGridSatView.mergeNewStates(os, dataBaseAdapter);
            } else {
                if (DEBUG){Log.wtf(this.getClass().getSimpleName(), "os is null");}
                openskyAccessOkay = false;
            }
            mGridSatView.purgeOldStateVectors();
        }
    }

    private final Runnable readAirlineCodesFromAssetsRunnable = new Runnable() {
        /**
         * Read airline Codes file from assets and add to HashMap.
         */
        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            try {
                // read-in any .csv file in assets folder
                String[] fileList = getAssets().list("codes");
                // test for .csv extension, open files in for loop
                for (String airCodeFileName : fileList) {
                    if (DEBUG) {
                        Log.w(this.getClass().getName(), "adding file " + airCodeFileName + " to Hashmap");
                    }
                    readAirFileCodes("codes/" + airCodeFileName);
                }
            } catch (IOException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
            if (DEBUG) {
                Log.w(this.getClass().getName(), "reading airCode files Runnable took "
                        + String.format("%3.1f", (System.currentTimeMillis() - startTime) / 1000.) + " sec");
            }
        }
    };

    private void readAirFileCodes(String airFileName) {
        InputStream is = null;
        try {
            is = getAssets().open(airFileName);
        } catch (IOException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
        BufferedReader reader = null;
        if (is != null) {
            reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")), 65536);
        }
        String line;
        StringTokenizer st;
        int lineNum = 0;
        try {
            if (reader != null) {
                //read sharedPrefs for current document num for satFileName file
                while ((line = reader.readLine()) != null) {
                    // tab-delimited text file
                    lineNum++;
                    st = new StringTokenizer(line, "\t");
                    mGridSatView.airlineCodes.put(st.nextToken().trim().toUpperCase(), st.nextToken().trim());
                }
            }
        } catch (IOException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        } finally {
            if (DEBUG) {Log.i(this.getClass().getName(), String.format("read: %d lines from aircodes file", lineNum));}
        }
    }

    /**
     * 1) check location is current and if not, that we have a Provider connection
     * 2) complain about camera permission, once
     * 3) show hints
     */
    private void startLookingAfterStuff() {
        if (lookAfterStuff != null) {
            return;
        }
        lookAfterStuff = new TimerTask() {

            @Override
            public void run() {
                lookAfterStuffHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        testLocationIsCurrent();
                        setLocationStatus();
                        checkDataConnection();
                        complainCameraPermission();
                        showSnackBarHints();
                    }
                });//Runnable
            }

            private void checkDataConnection() {
                if (Util.hasWifiInternetConnection(getApplicationContext())) {
                    // update StateVectors over WiFi without asking for permission
                    // update dataConnectionStatus
                    mGridSatView.dataConnectionStatus = GridSatView.DATA_CONNECTION_STATUS_OKAY;
                } else if (Util.hasInternetConnection(getApplicationContext())) {
                    if (!Util.hasMobileDataPermission(getApplicationContext())) {
                        // doesn't have WiFi data, but has Internet, ask for permission
                        mGridSatView.dataConnectionStatus = GridSatView.DATA_CONNECTION_STATUS_NONE;
                        // get size of required data to alert user
                        Util.closeAllSnackbars(snackBarList);
                        double dataSize = StateVector.dataSize / 5000.;
                        StringBuilder snackBarString = new StringBuilder(getString(R.string.ask_mobile_data_permission));
                        snackBarString.append(String.format(Locale.getDefault(), FORMAT_3_2F, dataSize));
                        snackBarString.append(" kbps");
                        // show snackbar to ask permission to update StateVectors over mobile given data size
                        mMobileDataPermissionSnackbar = Snackbar.make(
                                mGridSatView,
                                snackBarString,
                                Snackbar.LENGTH_INDEFINITE);
                        mMobileDataPermissionSnackbar.setAction(getString(R.string.allow), new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                SharedPreferences settings = getDefaultSharedPreferences(getApplicationContext());
                                SharedPreferences.Editor editor = settings.edit();
                                //change hasMobileDataPermission
                                editor.putString(MOBILE_DATA_SETTING_KEY, "1").apply();
                                // now have permission to get StateVector data over mobile network
                                mGridSatView.dataConnectionStatus = GridSatView.DATA_CONNECTION_STATUS_OKAY;
                            }
                        }).show();
                    } else { //have Mobile Data & permission
                        mGridSatView.dataConnectionStatus = GridSatView.DATA_CONNECTION_STATUS_OKAY;
                    }
                } else {
                    Util.closeAllSnackbars(snackBarList);
                    mGridSatView.dataConnectionStatus = GridSatView.DATA_CONNECTION_STATUS_NONE;
                    // no data connection-> complain and show snackbar with wirelessSettings Intent
                    mWirelessSettingsSnackbar.setAction(getString(R.string.open), new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            Intent viewIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                            startActivityForResult(viewIntent, REQUEST_CHANGE_WIFI_SETTINGS);
                        }
                    }).show();
                }
            }

            private void complainCameraPermission() {
                // if not Android M or greater, return
                // if we have camera permission, return
                // if any SnackBar is showing, return
                // if we've already complained, return
                // if we haven't complained about current Location or current TLEs, return

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Util.hasCameraPermission(MainActivity.this)) {
                    complainedCameraPermission = true;
                    return;
                }
                // wait until we've complained about Location and have internet
                // if we've already complained once since the app was first launched
                if (!complainedLocationOld || complainedCameraPermission
                        || !Util.hasInternetConnection(getApplicationContext())
                        || Util.isAnySnackBarVisible(snackBarList)) {
                    return;
                }
                complainedCameraPermission = true;
                // show snack bar explaining why camera is needed, ask permission
                //if (DEBUG) { Log.w(this.getClass().getName(), "Complaining about camera Permission"); }
                mCameraPermissionSnackBar.setAction(getString(R.string.allow), new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // Show the request permissions dialog
                        // and check the result in onRequestPermissionsResult()
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                PERMISSIONS_REQUEST_CAMERA);
                    }
                }).show();
            }

             private void showSnackBarHints() {
                // 1a) if we've shown liveModeHint and we're in liveMode return
                // 1b) if we've shown pausedModeHint and we're in pausedMode return
                // 2) if we haven't complained that the location is old with a snackBar, return
                // 3) if we haven't complained about camera permission in Android M, return
                // 4) if liveMode, show liveModeHintText, set liveModeHint SharedPref = true
                // 5) if pausedMode, show pausedModeHintText, set pausedModeHint SharedPref = true
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                if ((settings.getBoolean(PREFS_KEY_SHOWN_LIVEMODE_HINT, false)
                        && mGridSatView.isLiveMode())
                        || (settings.getBoolean(PREFS_KEY_SHOWN_PAUSEDMODE_HINT, false)
                        && !mGridSatView.isLiveMode())) {
                    return;
                }
                if (!complainedLocationOld || !complainedCameraPermission) {
                    return;
                }
                // don't over-write any other snackBars that haven't been dismissed
                if (Util.isAnySnackBarVisible(snackBarList)) {
                    return;
                }
                SharedPreferences.Editor editor = settings.edit();
                String snackBarText;
                if (mGridSatView.isLiveMode()) {
                    editor.putBoolean(PREFS_KEY_SHOWN_LIVEMODE_HINT, true).apply();
                    snackBarText = getResources().getString(R.string.livemode_hint);
                } else {
                    editor.putBoolean(PREFS_KEY_SHOWN_PAUSEDMODE_HINT, true).apply();
                    snackBarText = getResources().getString(R.string.pausedmode_hint);
                }
                // pass GridSatView so snackBar shows over surface
                mHintSnackBar = Snackbar.make(
                        mGridSatView, snackBarText,
                        Snackbar.LENGTH_INDEFINITE);
                mHintSnackBar.setAction(getString(R.string.ok), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mHintSnackBar.dismiss();
                    }
                }).show();
            }
        };
        lookAfterStuffTimer.schedule(lookAfterStuff, LOOKAFTERSTUFF_INITIAL_DELAY_TIME, LOOKAFTERSTUFF_REPEAT_TIME);
    }

    private void stopLookingAfterStuff() {
        lookAfterStuffHandler.removeCallbacksAndMessages(null);
        if (lookAfterStuff != null) {
            lookAfterStuff.cancel();
            lookAfterStuff = null;
        }
    }

    private void googlePlayAvailable(Context context) {
        if (!Util.hasWifiInternetConnection(context)){
            return;
        }
        int googlePlayAvailableResponse = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
        if (googlePlayAvailableResponse != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, googlePlayAvailableResponse, 0).show();
        }
    }

    private void checkLocationSettings(){
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationHelper.createLocationRequest());
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                mLocationHelper.stopLocationUpdates();
                mLocationHelper.startLocationUpdates();
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }

    private void testLocationIsCurrent() {
        //for debugging get locationStatus from mGridStatView
        // for debugging, Log complainedLocationOld, hasWifiInternetConnection, hasInternetConnection,
        // isGPSLocationEnabled, isNetworkLocationEnabled, locationStatus
/*        if (DEBUG) {
            Log.w(this.getClass().getName(), "testLocationIsCurrent() - complainedLocationOld: " + (complainedLocationOld ? "yes" : "no")
                    + " hasWifiInternetConnection: " + (Util.hasWifiInternetConnection(getApplicationContext()) ? "yes" : "no")
                    + " hasInternetConnection: " + (Util.hasInternetConnection(getApplicationContext()) ? "yes" : "no")
                    + " isGPSLocationEnabled: " + (Util.isGPSLocationEnabled(getApplicationContext()) ? "yes" : "no")
                    + " isNetworkLocationEnabled: " + (Util.isNetworkLocationEnabled(getApplicationContext()) ? "yes" : "no")
                    + " locationStatus: " + Util.returnStringLocationStatus(mGridSatView.locationStatus));
            Log.w(this.getClass().getName(), "testLocationIsCurrent() - is Location null?"
                    + (mLocationHelper.getMyLocation() == null ? " yes" : " no"));
        }*/
        // check if have Location permission. If Location old, null or default, check Location Settings
        if (askLocationPermission() && mGridSatView.locationStatus != GridSatView.LOCATION_STATUS_OKAY) {
            checkLocationSettings();
        }//locationStatus not Okay
        else {
            // don't need to complain
            complainedLocationOld = true;
        }
    }

    private void setLocationStatus() {
        int locationStatus;
        if (Util.locationIsDefault(mLocationHelper.getMyLocation())) {
            locationStatus = GridSatView.LOCATION_STATUS_NONE;
        } else if (System.currentTimeMillis() - mLocationHelper.getMyLocation().getTime() > LOCATION_IS_OLD) {
            locationStatus = GridSatView.LOCATION_STATUS_OLD;
        } else {
            locationStatus = GridSatView.LOCATION_STATUS_OKAY;
        }
        mGridSatView.setLocationStatus(locationStatus);
/*        if (DEBUG) {
            Log.i(this.getClass().getName(), "setLocationStatus(): long - " +
                    String.format("%7.4f", mLocationHelper.getMyLocation().getLongitude())
                    + " lat - " + String.format("%7.4f", mLocationHelper.getMyLocation().getLatitude())
                    + " alt - " + String.format("%3.1f", mLocationHelper.getMyLocation().getAltitude())
                    + " time - " + new Date(mLocationHelper.getMyLocation().getTime()));
        }*/
    }

    /**
     * Query the condition of the last known Location: "none", "old", or "okay"
     *
     * @return the integer value of LocationStatus
     */
    private int testSharedPrefsLocationIsCurrent() {
        Location aLoc = Util.getLocFromSharedPrefs(getApplicationContext());
        int locationStatus;
        if (Util.locationIsDefault(aLoc)) {
            locationStatus = GridSatView.LOCATION_STATUS_NONE;
            if (DEBUG){Log.i(this.getClass().getName(), "locationIsDefault");}
        } else if (System.currentTimeMillis() - aLoc.getTime() > LOCATION_IS_OLD) {
            locationStatus = GridSatView.LOCATION_STATUS_OLD;
            if (DEBUG){Log.i(this.getClass().getName(), "locationIsOld");}
        } else {
            locationStatus = GridSatView.LOCATION_STATUS_OKAY;
/*            if (DEBUG){Log.i(this.getClass().getName(), "locationIsOkay "
                    + String.format(FORMAT_4_3F, mLocationHelper.getMyLocation().getLatitude())
            + " / " + String.format(FORMAT_4_3F, mLocationHelper.getMyLocation().getLongitude()));}*/
        }
        return locationStatus;
    }

    /**
     * This is called immediately after the surface is first created.
     * Implementations of this should start up whatever rendering code
     * they desire.  Note that only one thread can ever draw into
     * a {@link }, so you should not draw into the Surface here
     * if your normal rendering will be in another thread.
     *
     * @param holder The SurfaceHolder whose surface is being created.
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mGridSatView.setSurfaceSize(holder.getSurfaceFrame().width(), holder.getSurfaceFrame().height());
        mGridSatView.setFOVScale(mCameraPreview.getFOV());
    }

    /**
     * This is called immediately after any structural changes (format or
     * size) have been made to the surface.  You should at this point update
     * the imagery in the surface.  This method is always called at least
     * once, after {@link #surfaceCreated}.
     *
     * @param holder The SurfaceHolder whose surface has changed.
     * @param format The new PixelFormat of the surface.
     * @param width  The new width of the surface.
     * @param height The new height of the surface.
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }
        int cameraSensorOrientation = 90;
        //use CameraInfo21 to see if camera sensor is upside down and modify surface orientation; write debug code to display parameters
        //todo does surfaceChanged get called after onRequestPermissionsResult? if not, we'll have to save cameraSensorOrientation
        // todo and adjust setAzelConfigCorrection() when camera is on/off
        // if Build number > Lollipop can use CameraInfo21; get cameraSensorOrientation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            CameraInfo21 mCamInfo21 = new CameraInfo21(getApplicationContext());
            cameraSensorOrientation = mCamInfo21.findBackFacingCameraOrientation();
            if (DEBUG) {
                Log.e(this.getClass().getName(), "reading cameraSensorOrientation for Android M "
                        + " cameraSensorOrientation: " + cameraSensorOrientation);
            }
        }
        mGridSatView.setSurfaceSize(width, height);
        mGridSatView.setFOVScale(mCameraPreview.getFOV());
        mCameraPreview.stopPreview();
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int displayRotationDegrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                displayRotationDegrees = 0;
                break; // Natural orientation
            case Surface.ROTATION_90:
                displayRotationDegrees = 90;
                break; // Landscape left
            case Surface.ROTATION_180:
                displayRotationDegrees = 180;
                break;// Upside down
            case Surface.ROTATION_270:
                displayRotationDegrees = 270;
                break;// Landscape right
        }
        int displayRotationChange = (mCameraPreview.getCameraRotationOffset() - displayRotationDegrees + 360) % 360;
        mCameraPreview.setDisplayOrientation(displayRotationChange);
        mGridSatView.setAzelConfigCorrection(displayRotationChange - cameraSensorOrientation);
        if (DEBUG) {
            Log.e(this.getClass().getName(), "surfaceChanged() liveMode?" + (mGridSatView.isLiveMode() ? " yes" : " no")
                    + " displayRotation: " + displayRotationDegrees
                    + " displayRotationChange: " + displayRotationChange
                    + " cameraSensorOrientation: " + cameraSensorOrientation);
        }
        // if sensor orientation is not 0, add "so: " + cameraSensorOrientation to debug text
        // also add " dispRot: " + displayRotationDegrees
        // also add " dispRotChange: " + displayRotationChange
        mGridSatView.debugText = "camSO: " + cameraSensorOrientation
                + " dispRot: " + displayRotationDegrees
                + " dispRotChange: " + displayRotationChange;

        if (!mGridSatView.isLiveMode()){
            // gravity sensor would determine this in LiveMode
            //todo need to preserve screenOrientation from accel sensor so grid lines are the same as in LiveMode
            mGridSatView.setScreenRotation(displayRotationDegrees);
        }
        try {
            mCameraPreview.mCamera.setPreviewDisplay(mHolder);
            // Exception is most likely no camera permission
        } catch (Exception ignore) {

        }
        mCameraPreview.setPreviewSize();
        // we're told to find camera zoom ratios after changing the Preview Size; zoom ratios may be different for different sizes
        mCameraPreview.configureCamera();
        if (mGridSatView.isLiveMode()) {
            mCameraPreview.startPreview();
        }
        // we've re-oriented the screen, re-draw the grid and satellites.
        mGridSatView.invalidate();
    }

    /**
     * This is called immediately before a surface is being destroyed. After
     * returning from this call, you should no longer try to access this
     * surface.  If you have a rendering thread that directly accesses
     * the surface, you must ensure that thread is no longer touching the
     * Surface before returning from this function.
     *
     * @param holder The SurfaceHolder whose surface is being destroyed.
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    /**
     * Need magnetic field sensor to determine which way screen is pointing.
     * Need either a gravity sensor or an accelerometer sensor to find screen orientation and elevation angle of z-axis
     * If we find a gravity sensor, use that. If there is no gravity sensor, use an accelerometer
     *
     * @param sensorSpeed desired update rate for sensors
     */
    private void registerSensorListeners(int sensorSpeed) {
        mGravityVector = new float[3];
        sensorGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        // the Gravity Sensor already filters raw data, so only use a mild low-pass filter
        accelFilterAlpha = .4;
        if (sensorGravity == null) {
            sensorAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (sensorAccel != null) {
                mSensorManager.registerListener(this, sensorAccel, sensorSpeed);
                // the Accelerometer Sensor is noisy, so use an agressive low-pass filter
                accelFilterAlpha = .96;
            }
        } else {
            // prefer to use the Gravity Sensor
            mSensorManager.registerListener(this, sensorGravity, sensorSpeed);
        }
        mMagFieldValues = new float[3];
        sensorMagField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        if (sensorMagField != null) {
            hasMagSensor = mSensorManager.registerListener(this, sensorMagField, sensorSpeed);
        }
        if (!hasMagSensor && !complainedMagSensor) {
            complainedMagSensor = true;
            if (mGridSatView.isLiveMode()) {
                mGridSatView.setLosAzDeg(180f);
            }
            noCompassDialog(getResources().getString(R.string.no_compass_title), getResources().getString(R.string.no_compass_message));
        }
    }

    private void unRegisterSensorListeners() {
        mGravityVector = null;
        mMagFieldValues = null;
        mSensorManager.unregisterListener(this);
        if (sensorGravity == null) {
            mSensorManager.unregisterListener(this, sensorAccel);
        } else {
            mSensorManager.unregisterListener(this, sensorGravity);
        }
        if (sensorMagField != null) {
            mSensorManager.unregisterListener(this, sensorMagField);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // dismiss any snackbars showing when touchEvent occurs
        if (mWirelessSettingsSnackbar != null && mWirelessSettingsSnackbar.isShown()){
            mWirelessSettingsSnackbar.dismiss();
        } else if (mMobileDataPermissionSnackbar != null && mMobileDataPermissionSnackbar.isShown()){
            mMobileDataPermissionSnackbar.dismiss();
        } else  if (mLocationSettingsSnackBar != null && mLocationSettingsSnackBar.isShown()){
            mLocationSettingsSnackBar.dismiss();
        } else  if (mCameraPermissionSnackBar != null && mCameraPermissionSnackBar.isShown()){
            mCameraPermissionSnackBar.dismiss();
        }
        //handle a zoom event with the ScaleDetector
        mScaleDetector.onTouchEvent(event);
        // don't try to pan or click if we're zooming
        if (!mScaleDetector.isInProgress()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mGridSatView.pressStartTime = System.currentTimeMillis();
                    mGridSatView.stayedWithinClickDistance = true;
                    mGridSatView.touchDownX = event.getX();
                    mGridSatView.touchDownY = event.getY();
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    //this event is intercepted by mScaleDetector
                    mGridSatView.isZooming = true;
                    break;
                case MotionEvent.ACTION_UP:
                    // detect click if ACTION_DOWN coords are not much different than ACTION_UP coords and timing is short
                    long pressDuration = System.currentTimeMillis() - mGridSatView.pressStartTime;
                    if (pressDuration < MAX_CLICK_DURATION && mGridSatView.stayedWithinClickDistance) {
                        // Click event has occurred. Just call these to perform feedback that user has specified
                        handleClickEvent();
                    } else {
                        //was not a click event, handle the end of a pan event
                        if (!mGridSatView.isZooming) {
                            handlePanEventEnd(event);
                        }
                    }
                    mGridSatView.isZooming = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    //if I was within clickDistance and now I've moved too far, reject this as a click
                    if (mGridSatView.stayedWithinClickDistance
                            && distanceSwiped(mGridSatView.touchDownX, mGridSatView.touchDownY,
                            event.getX(), event.getY()) > MAX_CLICK_DISTANCE) {
                        mGridSatView.stayedWithinClickDistance = false;
                    }
                    pressDuration = System.currentTimeMillis() - mGridSatView.pressStartTime;
                    if (pressDuration > MAX_CLICK_DURATION
                            || !mGridSatView.stayedWithinClickDistance) {
                        // either we've moved out of the "click distance"
                        // or waited until we're sure this is not a click event, then start panning if not in LiveMode
                        if (!mGridSatView.isZooming) {
                            handleOngoingPanEvent(event);
                        }
                    }
                    break;
            }
        } else {
            // scaleDetector is handling the touch event
            mGridSatView.isZooming = true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * Touch event was click, handle menu button, play/pause button or airplane clicks
     */
    private void handleClickEvent() {
        mGridSatView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        mGridSatView.playSoundEffect(SoundEffectConstants.CLICK);
        if (mGridSatView.clickIsInPlayPauseButton()) {
            doPlayPauseClick();
        } else if (mGridSatView.clickIsInMenuButton()) {
            doMenuClick();
        } else if (mGridSatView.isClickInAirplaneInfoCloseBox()){
            mGridSatView.setSelectedAirplaneicao24(ZERO);
            mGridSatView.invalidate();
        } else if (mGridSatView.isClickInMoreInfoBox() && !(ZERO).equals(mGridSatView.getSelectedAirplaneicao24())) {
            //todo draw "MORE INFO" with gray textPaint to blink selection
            String url = getMoreInfoURI(mGridSatView.airplaneDBData.get(DB_KEY_AIR_REGISTRATION));
            if (url != null) {
                final Intent browseIntent = new Intent(Intent.ACTION_VIEW);
                browseIntent.setData(Uri.parse(url));
                startActivity(browseIntent);
            }
        } else {
            // click was somewhere in canvas, maybe in an airplane
            // convert click coords to az,el coords - depends on scale and screenRotation
            float[] clickAzEl = mGridSatView.convertClickXYToAzEl();
            float clickTolerance = 1.5f * GridSatView.ICON_SIZE / mGridSatView.getPixelPerDegree();
            //clicking is shakier in liveMode
            if (mGridSatView.isLiveMode()) {
                clickTolerance *= 2;
            }
            // find what airplanes are at this click location, and either present popup, or airplane info
            handleAirplaneCanvasClick(clickAzEl[0], clickAzEl[1], clickTolerance);
        }
    }

    /**
     * Touch event was pan, calculate az, el pan distance and re-draw canvas
     *
     * @param event the data associated with this Touch Event
     */
    private void handleOngoingPanEvent(MotionEvent event) {
        // do panning if not LiveMode, so set panX, panY and call invalidate()
        if (mGridSatView.isLiveMode()) {
            return;
        }
        // calculate delta between current coords and ACTION_DOWN coords to set GRIDSatView panAz and panEl
        mGridSatView.panAz = (mGridSatView.tempPanAz + (mGridSatView.touchDownX - event.getX()) / mGridSatView.getPixelPerDegree());
        // restrict total panning in elevation so that el > 90 or el < -10 doesn't show on screen
        float proposedTempPanEl = (event.getY() - mGridSatView.touchDownY) / mGridSatView.getPixelPerDegree();
        float newLosEl = mGridSatView.getLosElDeg() + mGridSatView.tempPanEl + proposedTempPanEl;
        float halfCanvasHDeg = mGridSatView.mCanvasHeight / (2 * mGridSatView.getPixelPerDegree());
        if (newLosEl > halfCanvasHDeg - 10 && newLosEl < MAX_ELEV - halfCanvasHDeg) {
            mGridSatView.panEl = mGridSatView.tempPanEl + proposedTempPanEl;
        }
        // in LiveMode sensor event calls invalidate; need to do it here to update canvas during pan
        mGridSatView.invalidate();
    }

    /**
     * We're finished with this pan event. Save the distance panned so the next pan event
     * starts where we finished this pan. Also limit elevation pan to -10 to +90 degrees
     *
     * @param event the pan event to handle
     */
    private void handlePanEventEnd(MotionEvent event) {
        if (mGridSatView.isLiveMode()) {
            return;
        }
        float halfCanvasHDeg = mGridSatView.mCanvasHeight / (2 * mGridSatView.getPixelPerDegree());
        mGridSatView.tempPanAz += (mGridSatView.touchDownX - event.getX()) / mGridSatView.getPixelPerDegree();
        mGridSatView.tempPanAz = (mGridSatView.tempPanAz + 360) % 360;
        float proposedTempPanEl = (event.getY() - mGridSatView.touchDownY) / mGridSatView.getPixelPerDegree();
        float newLosEl = mGridSatView.getLosElDeg() + mGridSatView.tempPanEl + proposedTempPanEl;
        if (newLosEl > halfCanvasHDeg - 10 && newLosEl < MAX_ELEV - halfCanvasHDeg) {
            mGridSatView.tempPanEl += proposedTempPanEl;
            //restrict total panning in elevation so that el > 90 doesn't show on screen
        } else if (newLosEl >= MAX_ELEV - halfCanvasHDeg) {
            mGridSatView.tempPanEl += (MAX_ELEV - mGridSatView.getLosElDeg() - mGridSatView.tempPanEl - halfCanvasHDeg);
        } else {
            mGridSatView.tempPanEl += (-mGridSatView.getLosElDeg() - mGridSatView.tempPanEl + halfCanvasHDeg - 10);
        }
    }

    /**
     * Calculate distance swiped to see if we stayed within a small distance. If so
     * it's a click, if not it's a pan
     *
     * @param x1 start horizontal location in pixels
     * @param y1 start vertical location in pixels
     * @param x2 end horizontal location in pixels
     * @param y2 end vertical location in pixels
     * @return total distance in density dependent pixels
     */
    private float distanceSwiped(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        float distanceInPixels = (float) Math.sqrt(dx * dx + dy * dy);
        return pixelToDp(distanceInPixels);
    }

    /**
     * convert a distance in pixels to a distance in dp: density dependent pixels
     * to determine whether touch event is a click or a pan
     *
     * @param pixel distance the touch event moved, in pixels
     * @return equivalent touch distance in dp
     */
    private float pixelToDp(float pixel) {
        return pixel / getResources().getDisplayMetrics().density;
    }

    /**
     * Handle a pinch zoom event to change the camera zoom factor and grid/satellite view
     * @return false to say we've handled the event
     */
    @NonNull
    private ScaleGestureDetector.OnScaleGestureListener scaleGestureListener() {
        return new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {

                if (mGridSatView.isLiveMode()) {
                    mGridSatView.setPreviewImageZoomScaleFactor(mGridSatView.getTempCameraZoomFactor());
                } else{
                    mGridSatView.setPausedZoomFactor(mGridSatView.getTempPausedZoomFactor());
                }
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                mGridSatView.setTempCameraZoomFactor(MIN_TOTAL_ZOOM);
                mGridSatView.setTempPausedZoomFactor(MIN_TOTAL_ZOOM);
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                // if in liveMode set maximum zoom factor to be about 3.33 because azimuth mag field shakes too much
                // if camera doesn't zoom and we're in LiveMode, just return
                if (mGridSatView.isLiveMode() && mCameraPreview.zoomRatios == null) {
                    return false;
                }
                // in LiveMode
                // 0) set pausedZoomFactor = 1;
                // 1) multiply previous cameraZoomFactor by currentScaleFactor
                // 2) if newZoomFactor > 3.3 newZoomFactor = 3.3; if newZoomFactor < 1, newZoomFactor = 1
                // 3) find corresponding zoomNumber for the camera, set zoomNumber and cameraZoomFactor;
                // set camera parameters, store cameraZoomFactor in GridSatView
                // detector.getScaleFactor is a relative # from 1 - 3 for this onScale event
                double detectorScaleFactor = detector.getScaleFactor();

                if (mGridSatView.isLiveMode()) {
                    mGridSatView.setPausedZoomFactor(MIN_TOTAL_ZOOM);
                    double newZoomFactor = mGridSatView.getPreviewImageZoomScaleFactor() * detectorScaleFactor;
                    if (newZoomFactor > MAX_LIVE_ZOOM) {
                        newZoomFactor = MAX_LIVE_ZOOM;
                    }
                    if (newZoomFactor < MIN_TOTAL_ZOOM) {
                        newZoomFactor = MIN_TOTAL_ZOOM;
                    }
                    double newCameraZoomFactor = mCameraPreview.findNewZoomFactor(newZoomFactor);
                    mGridSatView.setTempCameraZoomFactor(newCameraZoomFactor);

                    // recalculate the total zoomFactor with new camera zoom factor
                    mGridSatView.setZoomScaleFactor(newCameraZoomFactor);
                    mGridSatView.setFOVScale(mCameraPreview.getFOV());
                } else {
                    //in PausedMode
                    // 1) newPausedZoomFactor = pausedZoomFactor * currentScaleFactor; newZoomFactor = newPausedZoomFactor*cameraZoomFactor
                    // 2) if newZoomFactor > 6 newZoomFactor = 6; if newZoomFactor < 1, newZoomFactor = 1
                    // 3) pausedZoomFactor = newZoomFactor / cameraZoomFactor
                    // save pausedZoomFactor in GridSatView; save zoomScaleFactor in GridSatView (= newZoomFactor)
                    double newPausedZoomFactor = mGridSatView.getPausedZoomFactor()* detectorScaleFactor;
                    if (newPausedZoomFactor * mGridSatView.getPreviewImageZoomScaleFactor() > MAX_TOTAL_ZOOM) {
                        newPausedZoomFactor = MAX_TOTAL_ZOOM / mGridSatView.getPreviewImageZoomScaleFactor();
                    }
                    // total zoom can't be less than 1
                    if (newPausedZoomFactor * mGridSatView.getPreviewImageZoomScaleFactor() < MIN_TOTAL_ZOOM) {
                        newPausedZoomFactor = MIN_TOTAL_ZOOM / mGridSatView.getPreviewImageZoomScaleFactor();
                    }
                    mGridSatView.setTempPausedZoomFactor(newPausedZoomFactor);
                    mGridSatView.setZoomScaleFactor(newPausedZoomFactor * mGridSatView.getPreviewImageZoomScaleFactor());
                    mGridSatView.setFOVScale(mCameraPreview.getFOV());
                    mGridSatView.invalidate();
                }
                return false;
            }
        };
    }

    /**
     * Called when sensor values have changed.
     * <p>See {@link SensorManager SensorManager}
     * for details on possible sensor types.
     * <p>See also {@link SensorEvent SensorEvent}.
     * <p/>
     * <p><b>NOTE:</b> The application doesn't own the
     * {@link SensorEvent event}
     * object passed as a parameter and therefore cannot hold on to it.
     * The object may be part of an internal pool and may be reused by
     * the framework.
     *
     * @param event the {@link SensorEvent SensorEvent}.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_MAGNETIC_FIELD:
                if (mGridSatView.isLiveMode()) {
                    lowPassFilterMag(event.values.clone());
                    // correct for declination
                    mGridSatView.setLosAzDeg(calcAz() - mLocationHelper.getMagDeclination());
                }
                break;
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_GRAVITY:
                if (mGridSatView.isLiveMode()) {
                    lowPassFilterAccel(event.values.clone());
                    calcRotEl();
                    //only redraw screen when we get new gravity values, not both mag & gravity
                    mGridSatView.invalidate();
                }
                break;
        }//switch
    }

    /**
     * Called when the accuracy of the registered sensor has changed.
     * <p/>
     * <p>See the SENSOR_STATUS_* constants in
     * {@link SensorManager SensorManager} for details.
     *
     * @param sensor the sensor whose accuracy changed
     * @param accuracy The new accuracy of this sensor, one of
     *                 {@code SensorManager.SENSOR_STATUS_*}
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        String sensorAccuracy;
        switch (accuracy){
            case 3:
                sensorAccuracy = "SENSOR_STATUS_ACCURACY_HIGH";
                break;
            case 2:
                sensorAccuracy = "SENSOR_STATUS_ACCURACY_MED";
                break;
            case 1:
                sensorAccuracy = "SENSOR_STATUS_ACCURACY_LOW";
                break;
            default:
                sensorAccuracy = "SENSOR_STATUS_UNRELIABLE";
        }
        //TODO if magnetic field sensor accuracy is low, alert user to re-calibrate
        if (DEBUG){Log.e(this.getClass().getName(), "onAccuracyChanged()- sensor: " + sensor.getName() + " new accuracy: " + sensorAccuracy);}
    }

    private void calcRotEl() {
        // this only depends on gravity (or accelerometer) so just call it when those sensors give a new value
        float gravNorm = (float) Math.sqrt(mGravityVector[0] * mGravityVector[0]
                + mGravityVector[1] * mGravityVector[1]
                + mGravityVector[2] * mGravityVector[2]);
        float screenRotation = (float) (Math.toDegrees(Math.atan2(mGravityVector[0] / gravNorm, mGravityVector[1] / gravNorm)));
        mGridSatView.setScreenRotation(screenRotation);
        float losEl = (float) (Math.toDegrees(Math.acos(mGravityVector[2] / gravNorm) - Math.PI / 2));
        mGridSatView.setLosElDeg(losEl);
    }

    private void lowPassFilterAccel(float[] accelValues) {
        mGravityVector[0] = (float) (accelFilterAlpha * mGravityVector[0] + (1-accelFilterAlpha) * accelValues[0]);
        mGravityVector[1] = (float) (accelFilterAlpha * mGravityVector[1] + (1-accelFilterAlpha) * accelValues[1]);
        mGravityVector[2] = (float) (accelFilterAlpha * mGravityVector[2] + (1-accelFilterAlpha) * accelValues[2]);
    }

    private void lowPassFilterMag(float[] magValues) {
        final double alpha = 0.97;
        mMagFieldValues[0] = (float) (alpha * mMagFieldValues[0] + (1-alpha) * magValues[0]);
        mMagFieldValues[1] = (float) (alpha * mMagFieldValues[1] + (1-alpha) * magValues[1]);
        mMagFieldValues[2] = (float) (alpha * mMagFieldValues[2] + (1-alpha) * magValues[2]);
    }

    private float calcAz() {
        //this only depends on magnetic field value (and screen rotation & losEl) but only call this when we get a new mag field value
        // since the losAz shouldn't change when we change the elevation or screen rotation
        // use for losEl between +/- 70 degrees
        double cos = Math.cos(Math.toRadians(mGridSatView.getScreenRotation()));
        double sin = Math.sin(Math.toRadians(mGridSatView.getScreenRotation()));
        // correct screen rotation by rotating x-y axes around z;
        // this puts x in plane containing magnetic field
        double x = mMagFieldValues[0]*cos - mMagFieldValues[1]*sin;
        double y = mMagFieldValues[0]*sin + mMagFieldValues[1]*cos;
        // now rotate z-azis around x to correct for los elevation
        // this put z in plane containing magnetic field
        // x mag field doesn't change
        cos = Math.cos(Math.toRadians(mGridSatView.getLosElDeg()));
        sin = Math.sin(Math.toRadians(mGridSatView.getLosElDeg()));
        double z = y*sin + mMagFieldValues[2]*cos;
        double norm = Math.sqrt(x*x + z*z);
        return (float) Math.toDegrees(Math.atan2(x / norm, z / norm) + Math.PI);
    }
    private void noCompassDialog(String dialog_title, String dialog_message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(dialog_message)
                .setTitle(dialog_title)
                .setIcon(R.drawable.ic_compass_icon)
                // Add the buttons
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK button, just exit dialog
                    }
                }).show();
    }
    private void initializeSnackBars() {
        mMobileDataPermissionSnackbar = Snackbar.make(
                mGridSatView,
                getString(R.string.ask_mobile_internet_permission),
                Snackbar.LENGTH_INDEFINITE);
        mCameraPermissionSnackBar = Snackbar.make(
                mGridSatView,
                getString(R.string.allow_camera),
                Snackbar.LENGTH_INDEFINITE);
        mLocationSettingsSnackBar = Snackbar.make(
                mGridSatView,
                getString(R.string.ask_location_permission),
                Snackbar.LENGTH_INDEFINITE);
        mWirelessSettingsSnackbar = Snackbar.make(
                mGridSatView,
                getString(R.string.open_wifi_settings),
                Snackbar.LENGTH_INDEFINITE);
        mHintSnackBar = Snackbar.make(
                mGridSatView, getString(R.string.livemode_hint),
                Snackbar.LENGTH_INDEFINITE);
        snackBarList.clear();
        snackBarList.add(mMobileDataPermissionSnackbar);
        snackBarList.add(mWirelessSettingsSnackbar);
        snackBarList.add(mHintSnackBar);
        snackBarList.add(mLocationSettingsSnackBar);
        snackBarList.add(mCameraPermissionSnackBar);
    }

    private void calculateStateVectorBounds(Location myLocation) {
        //convert from meters to delta Latitude and delta longitude
        // https://gis.stackexchange.com/questions/5821/calculating-latitude-longitude-x-miles-from-point
        // get max range from default shared prefs
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        int prefMaxRangeIndex = sharedPref.getInt(PREF_KEY_MAX_RANGE, maxRangeValuesMiles.length - 1);
        boolean distanceIsMile = Integer.parseInt(mGridSatView.distanceUnit) == DISTANCE_TYPE_MILE;
        if (prefMaxRangeIndex >= maxRangeValuesMiles.length){
            prefMaxRangeIndex = maxRangeValuesMiles.length - 1;
        }
        String prefMaxRange = (distanceIsMile?maxRangeValuesMiles[prefMaxRangeIndex]:maxRangeValuesMetric[prefMaxRangeIndex]).substring(0,2);
        double maxRange = 1.0 * Integer.parseInt(prefMaxRange);
        if (distanceIsMile) {
            maxRange = maxRange / mile_per_meter;
        } else {
            maxRange = maxRange / km_per_meter;
        }
        double deltaLat = Airplane.convertMeters2Latitude(maxRange);
        double deltaLong = Airplane.convertMeters2Longitude(maxRange, myLocation.getLatitude());
        latMin = (myLocation.getLatitude() - deltaLat);
        latMax = (myLocation.getLatitude() + deltaLat);
        if (latMax > 90){
            latMax = 89.9;
        }
        if (latMin < -90){
            latMin = -89.9;
        }
        longMin = (myLocation.getLongitude() - deltaLong);
        longMax = (myLocation.getLongitude() + deltaLong);
        if (longMax > 180){
            longMax = 179.9;
        }
        if (longMin < -180){
            longMin = -179.9;
        }

    }

}
