package motionapps.besafebox.datatools.storage;


import java.util.HashMap;
import java.util.LinkedList;


/**
 * class to store all the data into HashMap with specific keys + IndexStorage to store all indexes
 */
public class DataCarrier {

    public static int
            MAGNITUDE = 0,
            NORMALIZED_TIME = 1,
            INTERESTING_MAGNITUDE = 2,
            INTERESTING = 3,
            INTERESTING_AFTER_FREE_FALL = 4;

    LinkedList<SensorOutput> values;
    private HashMap<Integer, LinkedList<?>> tempStorage;
    private IndexStorage indexStorage;

    DataCarrier() {}

    public DataCarrier(LinkedList<SensorOutput> sensorOutputs){
        values = sensorOutputs;
    }

    public LinkedList<SensorOutput> getValues() {
        return values;
    }

    public void addData(int key, LinkedList<?> data){
        if(tempStorage == null){
            tempStorage = new HashMap<>();
        }
        tempStorage.put(key, data);
    }

    public <T> LinkedList<T> getData(int key){
        return tempStorage != null ? (LinkedList<T>) tempStorage.get(key) : null;
    }

    public IndexStorage getIndexStorage() {
        if(indexStorage == null){
            indexStorage = new IndexStorage();
        }
        return indexStorage;
    }
}
