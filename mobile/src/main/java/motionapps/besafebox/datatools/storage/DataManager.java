package motionapps.besafebox.datatools.storage;

import android.hardware.Sensor;
import android.location.Location;

import java.util.HashMap;
import java.util.Objects;

import motionapps.besafebox.activities.options.preferences.PreferencesHolder;
import motionapps.besafebox.model_managers.ModelManager;


/**
 * class to store all the sensor samples, which can be than accessed
 */
public class DataManager {

    private final HashMap<Integer, DataStorage> data = new HashMap<>(); // stores data in DataStorage object by id of sensor
    private Location lastLocation;

    public DataManager(ModelManager modelManager, Sensor[] sensors) { // needed sensors to init storage
        for(Sensor sensor : sensors){
            // uses number of samples obrained from Sampler service
            int limit = PreferencesHolder.getInt(modelManager.getContext(),
                    sensor.getType() +"_MAX", 250);
            if(limit < 0){
                continue;
            }
            data.put(sensor.getType(), new DataStorage(limit));
        }
    }


    /**
     * @param sensorEvent object derived from SensorEvent, which duplicates data to store it
     *                    SensorEvent behaves as static one object, so it needs to be duplicated to store it
     */
    public void postSensorEvent(SensorOutput sensorEvent) {
        Objects.requireNonNull(data.get(sensorEvent.type)).setEvent(sensorEvent);
    }

    /**
     * @param type - id of sensor
     * @param lastTenSeconds - if we want only last 10 seconds
     * @return DataCarrier with the sensor data + place for indexes
     */
    public DataCarrier getData(int type, boolean lastTenSeconds){
        if(Objects.requireNonNull(data.get(type)).isReady()){
            return Objects.requireNonNull(data.get(type)).packValues(lastTenSeconds);
        }else{
            return null;
        }
    }

    public void setLastLocation(Location location) {
        this.lastLocation = location;
    }

    public Location getLastLocation() {
        return lastLocation;
    }
}
