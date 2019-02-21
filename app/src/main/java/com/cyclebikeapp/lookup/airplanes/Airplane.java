package com.cyclebikeapp.lookup.airplanes;

import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import opensky.StateVector;

import static com.cyclebikeapp.lookup.airplanes.Constants.DB_KEY_AIR_REGISTRATION;

class Airplane {
    private static final int _360 = 360;
    private StateVector sv;
    private long timeTagSysCurrentTime;
    private double lookAngleAz;
    private double lookAngleEl;
    // from Location.distanceTo, in meters
    private float distance;
    private double altitude;
    private String name;
    private String airline;
    private int species;

    public Airplane(StateVector sv) {
        this.sv = sv;
    }

    public static double convertMeters2Latitude(double meters) {
        return meters / 111111;
    }

    public static double convertMeters2Longitude(double meters, double latitudeDeg) {
        if (Math.abs(latitudeDeg) < 90) {
            return meters / Math.cos(Math.toRadians(latitudeDeg)) / 111111;
        } else {
            return 0.;
        }
    }

    public void recalculatePosition(long deltaTimeMSec, Location myPlace) {
        // will have an airplane lat/long location before calling this
        // find new altitude, lat, long using deltaTime (now - sv.lastPositionUpdate) in milli-seconds,
        // vertical rate and horizontal velocity and bearing
        timeTagSysCurrentTime = System.currentTimeMillis();
        double deltaTimeSec = deltaTimeMSec / 1000.;
        double newLatitude = myPlace.getLatitude();
        double newLongitude = myPlace.getLongitude();

        double geoAltitude = sv.getGeoAltitude() != null ? sv.getGeoAltitude() : 0;
        double baroAltitude = sv.getBaroAltitude() != null ? sv.getBaroAltitude() : 0;
        double verticalRate = sv.getVerticalRate() != null ? sv.getVerticalRate() : 0;
        this.altitude = (geoAltitude > baroAltitude) ? geoAltitude : baroAltitude + (deltaTimeSec * verticalRate);
        if (sv.isOnGround()) {
            this.altitude = 0.;
        }
        if (sv.getHeading() != null && sv.getVelocity() != null) {
            // meters traveled east during deltaTime using heading (degrees) and velocity (m/sec)
            Double eastDistanceTraveled = Math.sin(Math.toRadians(sv.getHeading())) * sv.getVelocity() * deltaTimeSec;
            newLongitude = sv.getLongitude() + convertMeters2Longitude(eastDistanceTraveled, sv.getLatitude());
            Double northDistanceTraveled = Math.cos(Math.toRadians(sv.getHeading())) * sv.getVelocity() * deltaTimeSec;
            newLatitude = sv.getLatitude() + convertMeters2Latitude(northDistanceTraveled);
        }
        Location svLoc = new Location(LocationManager.GPS_PROVIDER);
        svLoc.setLatitude(newLatitude);
        svLoc.setLongitude(newLongitude);
        lookAngleAz = (myPlace.bearingTo(svLoc) + _360) % _360;
        double deltaElevation = this.altitude - myPlace.getAltitude();
        distance = myPlace.distanceTo(svLoc);
        lookAngleEl = Math.toDegrees(Math.atan2(deltaElevation, myPlace.distanceTo(svLoc)));
    }

    public StateVector getSv() {
        return sv;
    }

    public void setSv(StateVector sv) {
        this.sv = sv;
    }

    public double getLookAngleAz() {
        return lookAngleAz;
    }

    public double getLookAngleEl() {
        return lookAngleEl;
    }

    public long getTimeTagSysCurrentTime() {
        return timeTagSysCurrentTime;
    }

    public float getDistance() {
        return distance;
    }

    public double getAltitude() {
        return altitude;
    }

    public String getName() {
        return name;
    }

    public void setName(StateVector aState, AirplaneDBAdapter db) {
        String indentifier;
        String callsign = aState.getCallsign();
        String tailNumber = null;
        if (db != null && !db.isClosed()) {
            tailNumber = db.fetchAirplaneData(aState.getIcao24()).get(DB_KEY_AIR_REGISTRATION);
            if (tailNumber == null && MainActivity.DEBUG) {
                Log.i("findAirplaneIdentifier",
                        String.format("no tail number for icao24 %s", aState.getIcao24()));
            }
        }
        if (callsign != null && callsign.length() > 2) {
            indentifier = callsign;
        } else if (tailNumber != null && tailNumber.length() > 2) {
            indentifier = tailNumber;
        } else {
            indentifier = aState.getIcao24();
        }
        this.name = indentifier;
    }

    public String getAirline() {
        return airline;
    }

    public void setAirline(String airline) {
        this.airline = airline;
    }

    public int getSpecies() {
        return species;
    }

    public void setSpecies(int species) {
        this.species = species;
    }
}
