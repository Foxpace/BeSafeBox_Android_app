package motionapps.besafebox.sensors;

import android.hardware.Sensor;


public enum Sensors{
    GRAVITY("AGG", Sensor.TYPE_GRAVITY),
    ACG("ACG",Sensor.TYPE_ACCELEROMETER),
    ACC("ACC",Sensor.TYPE_LINEAR_ACCELERATION),
    GYRO("GYRO",Sensor.TYPE_GYROSCOPE),
    MAGNET("MAGNET",Sensor.TYPE_MAGNETIC_FIELD),
    ROTATION("ROTATION",Sensor.TYPE_ROTATION_VECTOR);

    int sensor;
    String shortName;

    Sensors(String shortName, int sensor){
        this.sensor = sensor;
        this.shortName = shortName;
    }

    public int getSensor() {
        return sensor;
    }

}