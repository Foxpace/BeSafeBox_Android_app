package motionapps.besafebox.datatools.storage;


/**
 * get and set methods of all indexes
 */
public class IndexStorage {

    private int beginIndex, maxValueIndex, endIndex, endFree = -1;
    private double beginValue, maxValue, endValue, minValue;
    private long beginTime, maxValueTime, endTime;
    private boolean isFreeFall = false;

    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }

    public void setMaxValueIndex(int maxValueIndex) {
        this.maxValueIndex = maxValueIndex;
    }

    public void setMaxValueTime(long maxValueTime) {
        this.maxValueTime = maxValueTime;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public int getMaxValueIndex() {
        return maxValueIndex;
    }

    public long getMaxValueTime() {
        return maxValueTime;
    }

    public double getBeginValue() {
        return beginValue;
    }

    public double getEndValue() {
        return endValue;
    }

    public int getBeginIndex() {
        return beginIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public long getBeginTime() {
        return beginTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setBeginIndex(int beginIndex) {
        this.beginIndex = beginIndex;
    }

    public void setBeginTime(long beginTime) {
        this.beginTime = beginTime;
    }

    public void setBeginValue(double beginValue) {
        this.beginValue = beginValue;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void setEndValue(double endValue) {
        this.endValue = endValue;
    }

    public void setEndFree(int endFree) {
        this.endFree = endFree;
    }

    public int getEndFree() {
        return endFree == -1 ? beginIndex : endFree;
    }

    public void setFreeFall(boolean freeFall) {
        isFreeFall = freeFall;
    }

    public boolean isFreeFall() {
        return isFreeFall;
    }

    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }

    public double getMinValue() {
        return minValue;
    }
}
