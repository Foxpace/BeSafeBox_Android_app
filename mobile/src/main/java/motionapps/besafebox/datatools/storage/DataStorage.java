package motionapps.besafebox.datatools.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

/**
 * storage for one specific sensor
 */
public class DataStorage extends DataCarrier {

    /**
     * @param maxMemory - number of samples to store maximally
     */
    DataStorage(int maxMemory){
        values = new LinkedList<>(Collections.nCopies(maxMemory, new SensorOutput()));
    }

    /**
     * @param sensorEvent - our object for sensor - it is stored into LinkedList to exploit O(1) to store it
     */
    public void setEvent(SensorOutput sensorEvent) {
        values.addLast(sensorEvent);
        values.removeFirst();
    }

    boolean isReady(){
        return values.get(values.size()-1).time != 0L && values.get(0).time != 0L;
    }

    /**
     * @param lastTenSeconds - boolean, to get last 10 seconds only
     * @return - DataCarrier - packed sensor info, so they can be used on other threads
     */
    DataCarrier packValues(boolean lastTenSeconds) {
        if(lastTenSeconds){
            ArrayList<SensorOutput> outputs = new ArrayList<>(values);
            LinkedList<SensorOutput> toAnalyse = new LinkedList<>();
            long begin = outputs.get(outputs.size()-1).time;
            for (int i = outputs.size()-1; i > 0; i--) {
                if(Math.abs(begin-outputs.get(i).time) > 10_000L){
                    break;
                }
                toAnalyse.addFirst(outputs.get(i));
            }
            return new DataCarrier(toAnalyse);
        }else{
            return new DataCarrier(new LinkedList<>(values));
        }
    }


}
