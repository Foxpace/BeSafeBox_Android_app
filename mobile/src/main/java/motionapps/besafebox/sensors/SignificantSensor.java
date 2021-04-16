package motionapps.besafebox.sensors;

import android.hardware.Sensor;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;


public class SignificantSensor extends TriggerEventListener {

    private Sensor sensor;

    public interface onMotionListener{
        void onMotion();
    }

    private onMotionListener listener;

    @Override
    public void onTrigger(TriggerEvent event) {
        listener.onMotion();
    }

    public void setListener(SignificantSensor.onMotionListener listener) {
        this.listener = listener;
    }

    public void setSensor(Sensor sensor) {
        this.sensor = sensor;
    }

    public Sensor getSensor() {
        return sensor;
    }
}
