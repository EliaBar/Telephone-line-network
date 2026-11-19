import java.util.ArrayList;

public class Transition {

    private String name;
    private int number;

    private final ArrayList<Integer> inP = new ArrayList<>();
    private final ArrayList<Integer> outP = new ArrayList<>();
    private final ArrayList<Double> timeOut = new ArrayList<>();

    private int buffer;            // Кількість маркерів, що обслуговуються в даний момент
    private int priority;
    private double minTime;        // Найближчий момент завершення обслуговування серед усіх каналів
    private double parametr;       // Середнє значення часу обслуговування
    private double paramDeviation; // Середньоквадратичне відхилення часу обслуговування
    private double minLimit;       // Нижня межа розподілу
    private double maxLimit;       // Верхня межа розподілу
    private String distribution;
    private int num;               // Номер каналу багатоканального переходу, що відповідає найближчій події
    private double mean;
    private int observedMax;
    private int observedMin;
    private double observedMinDelay = Double.MAX_VALUE;
    private double observedMaxDelay = 0.0;
    private static int next = 0;

    public Transition(String name, double delay) {
        this.name = name;
        this.number = next++;
        this.parametr = delay;
        this.paramDeviation = 0;
        this.minLimit = 0;
        this.maxLimit = Double.MAX_VALUE;
        this.distribution = null;
        this.buffer = 0;
        this.minTime = Double.MAX_VALUE; 
        this.num = 0;
        this.mean = 0;
        this.observedMax = 0;
        this.observedMin = 0;
        this.priority = 0;

        this.timeOut.add(Double.MAX_VALUE); 
        this.minEvent();
    }

    public Transition(String name, double delay, int priority) {
        this(name, delay);
        this.priority = priority;
    }

    public Transition(String name, String distribution, double meanParam, double stdDev, double minLimit, double maxLimit, int priority) {
        this(name, meanParam, priority);
        this.distribution = distribution;
        this.paramDeviation = stdDev;
        this.minLimit = minLimit;
        this.maxLimit = maxLimit;
    }

    public static void initNext() { 
        next = 0;
    }

    public void changeMean(double timeWeight) {
        this.mean = this.mean + (this.buffer - this.mean) * timeWeight;
    }

    public ArrayList<Double> getTimeOut() {
        return this.timeOut;
    }

    public double getMean() {
        return this.mean;
    }

    public int getObservedMax() {
        return this.observedMax;
    }

    public int getObservedMin() {
        return this.observedMin;
    }

    public double getObservedMinDelay() {
        return observedMinDelay;
    }

    public double getObservedMaxDelay() {
        return observedMaxDelay;
    }

    public int getPriority() {
        return this.priority;
    }

    public int getBuffer() {
        return this.buffer;
    }

    public double generateTimeServ() {
        double delay;
        if (distribution == null) {
            delay = parametr;
        } else if (distribution.equalsIgnoreCase("exp")) {
            delay = FunRand.exp(parametr, minLimit, maxLimit);
        } else if (distribution.equalsIgnoreCase("norm")) {
            delay = FunRand.norm(parametr, paramDeviation, minLimit, maxLimit);
        } else {
            delay = parametr;
        }

        if (delay < observedMinDelay) {
            observedMinDelay = delay;
        }
        if (delay > observedMaxDelay) {
            observedMaxDelay = delay;
        }

        return delay;
    }

    public void resetStats() {
        this.mean = 0.0;
        this.observedMax = 0;
        this.observedMin = 0;
        this.observedMinDelay = Double.MAX_VALUE;
        this.observedMaxDelay = 0.0;
    }

    public double getParameter() {
        return parametr;
    }

    public String getName() {
        return name;
    }

    public double getMinTime() {
        this.minEvent();
        return minTime;
    }

    public int getNum() {
        return num;
    }

    public int getNumber() {
        return number;
    }

    public void createInP(ArcIn[] arcs) {
        inP.clear();
        for (ArcIn arc : arcs) {
            if (arc.getNumT() == this.number) {
                inP.add(arc.getNumP());
            }
        }
    }

    public void createOutP(ArcOut[] arcs) {
        outP.clear();
        for (ArcOut arc : arcs) {
            if (arc.getNumT() == this.number) {
                outP.add(arc.getNumP());
            }
        }
    }

    public boolean condition(Place[] pp) {
        for (int i = 0; i < inP.size(); i++) {
            if (pp[inP.get(i)].getMark() == 0) {
                        return false;
                    }
        }
        return true;
    }

    public void actIn(Place[] pp, double currentTime) {
        if (this.condition(pp)) {
            for (int i = 0; i < inP.size(); i++) {
                pp[inP.get(i)].decreaseMark();
            }
            double eventTime = currentTime + generateTimeServ();
            if (buffer == 0) {
                timeOut.set(0, eventTime);
            } else {
                timeOut.add(eventTime);
            }
            buffer++;
            if (observedMax < buffer) {
                observedMax = buffer;
            }
            this.minEvent();
        }
    }

    public void actOut(Place[] pp) {
        if (buffer > 0) {
            for (int j = 0; j < outP.size(); j++) {
                pp[outP.get(j)].increaseMark();
            }
            if (num == 0 && (timeOut.size() == 1)) {
                timeOut.set(0, Double.MAX_VALUE);
            } else {
                timeOut.remove(num);
            }
            buffer--;
            if (observedMin > buffer) {
                observedMin = buffer;
            }
             this.minEvent();
        }
    }

    public final void minEvent() {
        minTime = Double.MAX_VALUE;
        num = 0;
        for (int i = 0; i < timeOut.size(); i++) {
            if (timeOut.get(i) < minTime) {
                minTime = timeOut.get(i);
                num = i;
            }
        }
    }

    public ArrayList<Integer> getInP() {
        return inP;
    }

    public ArrayList<Integer> getOutP() {
        return outP;
    }

    public boolean hasConsumingInput() {
        return !inP.isEmpty();
    }

    public boolean isEmptyOutputPlacesList() {
        return outP.isEmpty();
    }

    public String getDistribution() {
        return distribution;
    }

    public double getParamDeviation() {
        return paramDeviation;
    }
}