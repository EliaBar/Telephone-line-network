public class NetworkConfig {
    private int initialUsersReady;
    private int initialQueueL1;
    private int freeL1Count;
    private int freeL2Count;

    private double arrivalMean;
    private double arrivalMin;
    private double arrivalMax;

    private double retryMean;
    private double retryDev;
    private double retryMin;
    private double retryMax;

    private double dialMean;
    private double dialMin;
    private double dialMax;

    private double talkMean;
    private double talkMin;
    private double talkMax;

    private int priorityTakeL1;
    private int priorityRetry;

    public NetworkConfig() {
        this.initialUsersReady = 1;
        this.initialQueueL1 = 0;
        this.freeL1Count = 10;
        this.freeL2Count = 1;

        this.arrivalMean = 1.0;
        this.arrivalMin = 0.1;
        this.arrivalMax = 5.0;

        this.retryMean = 15.0;
        this.retryDev = 2.0;
        this.retryMin = 0.0;
        this.retryMax = 60.0;

        this.dialMean = 0.2;
        this.dialMin = 0.05;
        this.dialMax = 1.0;

        this.talkMean = 10.0;
        this.talkMin = 1.0;
        this.talkMax = 30.0;

        this.priorityTakeL1 = 1;
        this.priorityRetry = 0;
    }

    public NetworkConfig setInitialState(int initialUsersReady, int initialQueueL1) {
        this.initialUsersReady = initialUsersReady;
        this.initialQueueL1 = initialQueueL1;
        return this;
    }

    public NetworkConfig setLines(int freeL1Count, int freeL2Count) {
        this.freeL1Count = freeL1Count;
        this.freeL2Count = freeL2Count;
        return this;
    }

    public NetworkConfig setArrival(double mean, double min, double max) {
        this.arrivalMean = mean;
        this.arrivalMin = min;
        this.arrivalMax = max;
        return this;
    }

    public NetworkConfig withArrivalMean(double mean) {
        double scale = mean / this.arrivalMean;
        this.arrivalMean = mean;
        this.arrivalMin *= scale;
        this.arrivalMax *= scale;
        return this;
    }

    public NetworkConfig setRetry(double mean, double dev, double min, double max) {
        this.retryMean = mean;
        this.retryDev = dev;
        this.retryMin = min;
        this.retryMax = max;
        return this;
    }

    public NetworkConfig withRetryMean(double mean) {
        double scale = mean / this.retryMean;
        this.retryMean = mean;
        this.retryDev *= scale;
        this.retryMin *= scale;
        this.retryMax *= scale;
        return this;
    }

    public NetworkConfig setDial(double mean, double min, double max) {
        this.dialMean = mean;
        this.dialMin = min;
        this.dialMax = max;
        return this;
    }

    public NetworkConfig withDialMean(double mean) {
        double scale = mean / this.dialMean;
        this.dialMean = mean;
        this.dialMin *= scale;
        this.dialMax *= scale;
        return this;
    }

    public NetworkConfig setTalk(double mean, double min, double max) {
        this.talkMean = mean;
        this.talkMin = min;
        this.talkMax = max;
        return this;
    }

    public NetworkConfig withTalkMean(double mean) {
        double scale = mean / this.talkMean;
        this.talkMean = mean;
        this.talkMin *= scale;
        this.talkMax *= scale;
        return this;
    }

    public NetworkConfig setPriorities(int priorityTakeL1, int priorityRetry) {
        this.priorityTakeL1 = priorityTakeL1;
        this.priorityRetry = priorityRetry;
        return this;
    }

    public int getInitialUsersReady() {
        return initialUsersReady;
    }

    public int getInitialQueueL1() {
        return initialQueueL1;
    }

    public int getFreeL1Count() {
        return freeL1Count;
    }

    public int getFreeL2Count() {
        return freeL2Count;
    }

    public double getArrivalMean() {
        return arrivalMean;
    }

    public double getArrivalMin() {
        return arrivalMin;
    }

    public double getArrivalMax() {
        return arrivalMax;
    }

    public double getRetryMean() {
        return retryMean;
    }

    public double getRetryDev() {
        return retryDev;
    }

    public double getRetryMin() {
        return retryMin;
    }

    public double getRetryMax() {
        return retryMax;
    }

    public double getDialMean() {
        return dialMean;
    }

    public double getDialMin() {
        return dialMin;
    }

    public double getDialMax() {
        return dialMax;
    }

    public double getTalkMean() {
        return talkMean;
    }

    public double getTalkMin() {
        return talkMin;
    }

    public double getTalkMax() {
        return talkMax;
    }

    public int getPriorityTakeL1() {
        return priorityTakeL1;
    }

    public int getPriorityRetry() {
        return priorityRetry;
    }
}