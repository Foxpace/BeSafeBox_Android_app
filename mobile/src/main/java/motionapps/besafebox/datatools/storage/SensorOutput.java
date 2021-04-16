package motionapps.besafebox.datatools.storage;

import android.hardware.SensorEvent;

/**
 * main sensor output object
 */

public class SensorOutput {

    public long time = 0L;
    public float[] values = new float[]{0,0,0};
    public int accuracy = 0, type = 0;

    public SensorOutput(){}

    public SensorOutput(int type, long time, float[] values, int acc){
        this.time = time;
        this.values = values;
        this.accuracy = acc;
        this.type = type;
    }

    public SensorOutput(SensorEvent sensorEvent) {
        time = System.currentTimeMillis(); // usage of milliseconds
        values = sensorEvent.values.clone();
        accuracy = sensorEvent.accuracy;
        type = sensorEvent.sensor.getType();
    }
}

