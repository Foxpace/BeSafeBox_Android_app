package motionapps.besafebox.gps;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.jetbrains.annotations.NotNull;

/**
 * Callback for GPS registration
 */

public class GPSCallback extends LocationCallback {

    /**
     * Interface to implement with regular use of GPS
     */
    public interface OnLocationChangedCallback {
        void onLocationChanged(Location location);
        void onLastLocationSuccess(Location location);
        void onAvailabilityChanged(LocationAvailability locationAvailability);
    }

    /**
     * Interface to use just for last location
     */
    public interface OnLastLocation{
        void onLastLocation(Location location);
    }

    private final OnLocationChangedCallback callBack;
    private final FusedLocationProviderClient locationClient;
    private final String TAG  = "GPS_location";
    private Location lastLocation;
    private GPSParameters gpsParameters;
    private boolean registered = false;

    /**
     * @param context - any
     * @param locationChangedCallback - object with implemented interface
     * @param bLastLocation - send last location, if available
     */
    @SuppressLint("MissingPermission")
    public GPSCallback(Context context, OnLocationChangedCallback locationChangedCallback, boolean bLastLocation) {

        this.callBack = locationChangedCallback;
        locationClient = LocationServices.getFusedLocationProviderClient(context); // GPS client
        if (bLastLocation) { // last location
            locationClient.getLastLocation().addOnSuccessListener(location -> {
                lastLocation = location;
                callBack.onLastLocationSuccess(location);
            }).addOnFailureListener(e -> callBack.onLastLocationSuccess(null));
        }
    }

    /**
     * GPS is turned off and has to be started manually, if the new parameters should be used
     * @param gpsParameters - parameters, which will be used for GPS
     */
    public void setGpsParameters(GPSParameters gpsParameters) {
        if(registered){
            gpsOff();
        }
        this.gpsParameters = gpsParameters;
    }

    /**
     * Static method just for the last location
     * @param context any
     * @param onLastLocation - callback for last location
     */
    @SuppressLint("MissingPermission")
    public static void getLastLocation(Context context, OnLastLocation onLastLocation){
        FusedLocationProviderClient locationClient =
                LocationServices.getFusedLocationProviderClient(context);

        locationClient.getLastLocation()
                .addOnSuccessListener(onLastLocation::onLastLocation)
                .addOnFailureListener(e -> onLastLocation.onLastLocation(null));
    }


    /**
     * @param locationResult - result of GPS search - passing to callback
     */
    @Override
    public void onLocationResult(@NotNull LocationResult locationResult) {
        super.onLocationResult(locationResult);
        if(locationResult.getLocations().size() > 0){
            lastLocation = locationResult.getLastLocation();
            callBack.onLocationChanged(lastLocation);
        }
    }

    /**
     * @param locationAvailability - change in access to GPS - no connection, lost connection, ...
     */
    @Override
    public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
        super.onLocationAvailability(locationAvailability);
        callBack.onAvailabilityChanged(locationAvailability);
    }


    /**
     * turns off GPS completely
     */
    public void gpsOff(){
        Log.i(TAG, "Logging off location");
        locationClient.removeLocationUpdates(this);
        registered = false;
    }

    /**
     * GPS parameters from setter function are now used for registration of GPS for the app
     */
    @SuppressLint("MissingPermission")
    public void changeRequest(){

        if(registered){
            gpsOff();
        }

        if(gpsParameters == null){
            return;
        }

        Log.i(TAG, "Registering new request");
        locationClient.requestLocationUpdates(createRequest(),
                this, Looper.getMainLooper());
        registered = true;
    }

    /**
     * @return LocationRequest - creation of LocationRequest object
     */
    private LocationRequest createRequest(){

        LocationRequest locationRequest = LocationRequest.create()
        .setPriority(gpsParameters.getPriority())
        .setInterval(gpsParameters.getInterval())
        .setFastestInterval(gpsParameters.getFastestInteval())
        .setSmallestDisplacement(gpsParameters.getDistance());
        Log.i(TAG, "Registering: " +gpsParameters.toString());
        return locationRequest;

    }
}
