package motionapps.besafebox.gps;


import com.google.android.gms.location.LocationRequest;

/**
 * options for the GPS registration, which can be used uwith detectors
 */

public enum GPSParameters {


    WALK_PARAMS(LocationRequest.PRIORITY_NO_POWER, 60000L, 30000L, 100),
    CAR_PARAMS_LOW(LocationRequest.PRIORITY_HIGH_ACCURACY, 20000L, 5000L, 100),
    CAR_PARAMS_HIGH(LocationRequest.PRIORITY_HIGH_ACCURACY, 10000L, 5000L, 20);


    private final long interval, fastestInteval;
    private final int distance, priority;

    GPSParameters(int priority, long interval, long fastestInteval, int distance){
        this.interval = interval;
        this.priority = priority;
        this.fastestInteval = fastestInteval;
        this.distance = distance;
    }

    public int getDistance() {
        return distance;
    }

    public long getFastestInteval() {
        return fastestInteval;
    }

    public long getInterval() {
        return interval;
    }

    public int getPriority() {
        return priority;
    }

}
