public class Place{
    private final String name;
    private final int number;
    private int mark;
    private double mean;
    private static int next = 0;
    private int observedMax;
    private int observedMin;

    public Place(String name, int markersQuantity) {
        if (markersQuantity < 0) {
            throw new IllegalArgumentException("Quantity of markers can not be negative");
        }
        this.name = name;
        number = next;
        next++;
        mark = markersQuantity;
        mean = 0;
        observedMax = markersQuantity;
        observedMin = markersQuantity;
    }
    
    public Place(String name) {
        this(name, 0);
    }

    public Place(int markersQuantity) {
        this("Place_"+next, markersQuantity);
    }
    
    public Place() {
        this("Place_"+next, 0);
    }

    public static void initNext(){ 
        next = 0;
    }

    public void changeMean(double timeWeight) {
        mean = mean + (mark - mean) * timeWeight;
    }

    public double getMean() {
        return mean;
    }

    public void increaseMark() {
        mark++;
        findMinMax();
    }

    public void decreaseMark() {
        mark--;
        findMinMax();
    }
   
    private void findMinMax(){
        if (observedMax < mark) {
            observedMax = mark;
        }
        if (observedMin > mark) {
            observedMin = mark;
        }
    }

    public int getMark() {
        return mark;
    }

    public void setMark(int a) {
        mark = a;
        findMinMax();
    }

    public int getObservedMax() {
        return observedMax;
    }

    public int getObservedMin() {
        return observedMin;
    }

    public String getName() {
        return name;
    }

    public int getNumber() {
        return number;
    }
}