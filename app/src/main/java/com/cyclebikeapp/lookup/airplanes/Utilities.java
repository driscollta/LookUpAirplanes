package com.cyclebikeapp.lookup.airplanes;

import android.content.SharedPreferences;

import java.util.ArrayList;

import static com.cyclebikeapp.lookup.airplanes.Constants.PREF_KEY_AIRFILES_READ_SUCCESS;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREF_KEY_READING_AIRFILES;

/**
 * Created by TommyD on 4/18/2016.
 *
 */
class Utilities {

    static boolean airplaneFilesWereRead(SharedPreferences settings) {
        return settings.getBoolean(PREF_KEY_AIRFILES_READ_SUCCESS, false);
    }

    static boolean readingAirplaneFiles(SharedPreferences settings) {
        return settings.getBoolean(PREF_KEY_READING_AIRFILES, false);
    }

    static int findAirplaneIndex(String icao24, ArrayList<Airplane> mArrayList){
        // given the icao24 value from a new StateVector, find matching entry index in MyStateVector list.
        // if not found, return index = .size() + 1
        int index = 0;
        if (mArrayList == null){
            return  -1;
        }
        for (Airplane anAirplane:mArrayList){
            if (anAirplane.getSv().getIcao24().equals(icao24)){
                return index;
            }
            index++;
        }
        return index;
    }

    static String getMoreInfoURI(String s) {
        if (s == null){
            return null;
        }
        final String baseURIUS = "https://registry.faa.gov/aircraftinquiry/NNum_Results.aspx?NNumbertxt=" + s;
        final String baseURICanada = "http://wwwapps.tc.gc.ca/Saf-Sec-Sur/2/CCARCS-RIACC/RchAvcRes.aspx?m=%7c" +
                s.substring(s.indexOf("-") + 1) +
                "%7c&cn=%7c%7c&mn=%7c%7c&sn=%7c%7c&wf=0&wt=1000000&iy=%%&aac=%%&ne=%%&ce=%%&rp=%%&fr=%%&in=||&ay=%%&ac=%%&sc=%%&on=%7c%7c&tn=%7c%7c&c=%7c%7c&r=%25%25&p=%25%25&pc=%7c%7c&mo=A";
        final String baseURIDefault = "https://www.flightradar24.com/data/aircraft/" + s;
        if (s.toUpperCase().startsWith("N")){
            return baseURIUS;
        } else if (s.toUpperCase().startsWith("C")){
            return baseURICanada;
        } else {
            return baseURIDefault;
        }
    }

}
