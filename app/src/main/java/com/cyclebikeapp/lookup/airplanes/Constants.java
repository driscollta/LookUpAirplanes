package com.cyclebikeapp.lookup.airplanes;

/**
 * Created by TommyD on 8/4/2016.
 *
 */
final class Constants {

    public static final String ICAO24 = "icao24";

    //database key tags
    static final String AIRPLANE_TABLE = "airplaneTable";
    static final String AIRPLANE_DB = "airplaneDataBase";
    //database key for the icao24 hex string that identifies all aircraft
    static final String DB_KEY_ICAO24 = "air_icao24";
    //database key for the manufacturer, accessed by icao24
    static final String DB_KEY_AIR_MANUFACTURER = "air_manufacturer";
    //database key for the tail number
    static final String DB_KEY_AIR_REGISTRATION = "air_registration";
    //database key for the model, accessed via icao24
    static final String DB_KEY_AIR_MODEL = "air_model";
    //database key for the owner, accessed via icao24
    static final String DB_KEY_AIR_OWNER = "air_owner";
    static final String DB_KEY_AIR_SPECIES = "air_species";

    static final String ADF_KEY_TITLE = "adf_key_title";
    static final String ADF_KEY_NUM_SEARCH_RESULTS = "adf_key_num_search_results";
    static final String ADF_KEY_MANUFACTURER = "adf_key_manufacturer";
    static final String ADF_KEY_MODEL = "adf_key_model";
    static final String ADF_KEY_OWNER = "adf_key_owner";
    static final String ADF_KEY_REGISTRATION = "adf_key_registration";
    static final String ADF_KEY_ICAO24_NUMBER = "adf_key_icao24_number";
    static final String ADF_KEY_LINK = "adf_key_link";

    // SharedPrefs key to indicate we've read all the asset satellite files. If not, we'll try again the next time
    static final String PREF_KEY_MAX_RANGE = "key_max_range";
    static final String PREF_KEY_AIRFILES_READ_SUCCESS = "pref_key_airplanefiles_read_success";
    static final String PREF_KEY_READING_AIRFILES = "pref_key_reading_airplanefiles";
    static final String  PREF_KEY_AIRFILE_DOC_READ = "pref_key_airfile_docread";
    static final String MOBILE_DATA_SETTING_KEY = "mobile_data_setting_key";
    static final String[] maxRangeValuesMiles= {"10 mi","20 mi", "30 mi", "40 mi"};
    static final String[] maxRangeValuesMetric= {"20 km", "30 km", "40 km", "50 km"};
    // distance conversions
    static final double mph_per_mps = 2.23694;
    static final double kph_per_mps = 3.6;
    static final double km_per_meter = 0.001;
    static final double mile_per_meter = 0.00062137119224;
    static final double foot_per_meter = 3.28084;
    static final String FORMAT_4_0F = "%4.0f";
    static final String FORMAT_3_0F = "%3.0f";
    static final String FORMAT_2_0F = "%2.0f";
    static final String ZERO = "0";
    static final String KEY_REQUESTING_LOCATION_UPDATES = "requesting_location_updates";
    static final String RETURN_CHAR = "\n";
    static final String DOUBLE_DASH = "--";
    static final String MORE_INFO = "MORE INFO";

    private Constants() {}
    static final String SHARING_IMAGE_NAME = "LookUpInTheSky.jpg";
    // 2-line TLEs
    // for some reason this URL returns a JSON version of all current tles
    static final String OLD_DATE = "1970-01-01 00:00:00";
    static final String STRING_ZERO = "0";
    static final String PREFS_KEY_UNIT = "unit_default";
    static final String PREFS_KEY_MAG_DECLINATION = "prefs_key_mag_declination";
    static final float PREFS_DEFAULT_MAG_DECLINATION = 0f;
    static final String PREFS_KEY_ALTITUDE = "prefs_key_altitude";
    static final String PREFS_KEY_LATITUDE = "prefs_key_latitude";
    static final String PREFS_KEY_LONGITUDE = "prefs_key_longitude";
    static final String PREFS_KEY_TIME = "prefs_key_time";
    static final String PREFS_DEFAULT_ALTITUDE = "-621.1";
    static final String PREFS_DEFAULT_LATITUDE = "37.1";
    static final String PREFS_DEFAULT_LONGITUDE = "-122.1";
    static final long PREFS_DEFAULT_TIME = 123456;

    // name of the album for storing sharing image
    static final String LOOK_UP = "LookUp";

    // Unique tag for the error dialog fragment
    static final String DIALOG_ERROR = "dialog_error";
    static final String STATE_RESOLVING_ERROR = "resolving_error";
    static final String PREFS_NAME = "look_airplanes_shared_prefs";
    // Hashmap tags for satellite-click pop-up window
    static final String CC_MAP_KEY_ICAO24 = "cc_map_key_icao24";
    static final String CC_MAP_KEY_AIRPLANE_NAME = "cc_map_key_airplane_name";
    static final long nanosPerMillis = 1000000;
    private static final long ONE_SECOND = 1000;
    static final long ONE_MINUTE = 60 * ONE_SECOND;
    static final long ONE_HOUR = 60 * ONE_MINUTE;
    static final long TWENTY_FOUR_HOURS = 24 * 60 * ONE_MINUTE;
    // TLE refresh Times for LEO, GEO and DEEP satellite kinds
    static final long ONE_WEEK = 7 * TWENTY_FOUR_HOURS;
    static final long SIX_WEEKS = 6 * ONE_WEEK;
    static final long MILLISEC_PER_DAY = 24 * ONE_HOUR;

    static final long LOOKAFTERSTUFF_INITIAL_DELAY_TIME = 5 * ONE_SECOND;
    static final long LOOKAFTERSTUFF_REPEAT_TIME = 60 * ONE_SECOND;
    static final long RECALC_LOOKANGLES_REPEAT_TIME = 250;
    static final long RECALC_LOOKANGLES_INITIAL_DELAY_TIME = ONE_SECOND;
    // Max allowed duration for a "click", in milliseconds.
    static final int MAX_CLICK_DURATION = 650;
    //Max allowed distanceSwiped to move during a "click", in DP.
    static final int MAX_CLICK_DISTANCE = 15;
    static final long LOCATION_INTERVAL = ONE_HOUR;
    static final long LOCATION_IS_OLD = 2* ONE_HOUR;

    static final String PREFS_KEY_SHOWN_PAUSEDMODE_HINT = "prefs_key_shown_pausedmode_hint";
    static final String PREFS_KEY_SHOWN_LIVEMODE_HINT = "prefs_key_shown_livemode_hint";
    static final String NAV_DRAWER_LIVE_MODE_KEY = "nav_drawer_live_mode_key";
    static final String PREFS_KEY_LIVE_MODE = "prefs_key_livemode";
    static final String PREFS_KEY_LOSAZ = "prefs_key_losaz";
    static final String PREFS_KEY_LOSEL = "prefs_key_losel";
    static final String PREFS_KEY_PANAZ = "prefs_key_panaz";
    static final String PREFS_KEY_PANEL = "prefs_key_panel";
    static final String PREFS_KEY_TEMP_PANAZ = "prefs_key_temp_panaz";
    static final String PREFS_KEY_TEMP_PANEL = "prefs_key_temp_panel";

    // codes indicating which Activity was launched and returned
    static final int PERMISSIONS_REQUEST_LOCATION = 51;
    static final int PERMISSIONS_REQUEST_CAMERA = 52;
    static final int REQUEST_CHECK_SETTINGS = 94;
    static final int REQUEST_CHANGE_LOCATION_SETTINGS = 92;
    static final int REQUEST_CHANGE_WIFI_SETTINGS = 93;

    // Request code to use when launching the navigation drawer activity
    static final int RC_NAV_DRAWER = 89;

    static final int PAID_VERSION = 5431;
    static final int FREE_VERSION = 1345;
    static final String FORMAT_3_1F = "%3.1f";
    static final String FORMAT_4_3F = "%4.3f";
    static final String FORMAT_3_2F = "%3.2f";

    static final int MAX_ELEV = 90;
    static final double MAX_TOTAL_ZOOM = 6.;
    static final double MIN_TOTAL_ZOOM = 1.;
    static final double MAX_LIVE_ZOOM = 3.3;
    static final float DEF_LOS_AZIMUTH = 180f;
    static final float DEF_LOS_ELEV = 55f;
    static final String MILE = "mi";
    static final String KM = "km";
    static final String METER = "m";
    static final String FOOT = "ft";
    static final int DISTANCE_TYPE_MILE = 0;
    static final int DISTANCE_TYPE_METRIC = 1;
    // 30 miles is 48280 m is half width & length of box for viewable airplanes
    static final double MAX_AIRPLANE_DISTANCE = 48280.3;
    static final int SPECIES_LAND_PLANE = 1;
    static final int SPECIES_HELICOPTER = 4;
    static final int SPECIES_HOT_AIR_BALLOON = 9;
    static final int SPECIES_BLIMP = 10;
}
