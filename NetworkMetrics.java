import java.util.Locale;

public class NetworkMetrics {
    private final PetriNet net;
    private final PetriSim sim;
    private final int l1;
    private final int l2;
    private int served;
    private int retries;
    private double lambda;
    private double loadL1;
    private double loadL2;
    private double retryProbability;
    private double dialDuration;
    private double talkDuration;
    private double totalDuration;

    public NetworkMetrics(PetriSim sim, int l1, int l2) {
        if (sim == null) {
            throw new IllegalArgumentException("PetriSim cannot be null");
        }
        this.sim = sim;
        this.net = sim.getNet();
        this.l1 = l1;
        this.l2 = l2;
        this.calculateAll();
    }

    public void calculateAll() {
        this.served = this.net.getCurrentMark("P_Served");
        this.retries = this.net.getCurrentMark("P_RetryCount");
        int totalArrived = this.net.getCurrentMark("P_TotalArrived");

        double currentTime = this.sim.getCurrentTime();

        double lambdaIn;
        if (totalArrived > 0 && currentTime > 0.0) {
            lambdaIn = (double) totalArrived / currentTime;
        } else {
            lambdaIn = 0.0;
        }

        if (this.served > 0 && currentTime > 0.0) {
            this.lambda = (double) this.served / currentTime;
        } else {
            this.lambda = 0.0;
        }

        double meanFreeL1 = this.net.getMeanMark("P_FreeL1");
        double meanFreeL2 = this.net.getMeanMark("P_FreeL2");

        if (this.l1 > 0) {
            this.loadL1 = ((double) this.l1 - meanFreeL1) / (double) this.l1;
        } else {
            this.loadL1 = 0.0;
        }

        if (this.l2 > 0) {
            this.loadL2 = ((double) this.l2 - meanFreeL2) / (double) this.l2;
        } else {
            this.loadL2 = 0.0;
        }

        int totalAttempts = this.served + this.retries;
        if (totalAttempts > 0) {
            this.retryProbability = (double) this.retries / (double) totalAttempts;
        } else {
            this.retryProbability = 0.0;
        }

        double meanBufferDial = this.net.getMeanBuffer("T_Dial");

        if (this.lambda > 0.0) {
            this.dialDuration = meanBufferDial / this.lambda;
        } else {
            this.dialDuration = 0.0;
        }

        double meanBufferTalk = this.net.getMeanBuffer("T_Talk");

        if (this.lambda > 0.0) {
            this.talkDuration = meanBufferTalk / this.lambda;
        } else {
            this.talkDuration = 0.0;
        }

        double totalMeanTokens = getStepActiveClients();

        if (this.lambda > 0.0) {
            this.totalDuration = totalMeanTokens / lambdaIn;
        } else {
            this.totalDuration = 0.0;
        }
    }

    public double getStepActiveClients() {
        return this.net.getMeanMark("P_QueueL1")
                + this.net.getMeanMark("P_ConnectQueue")
                + this.net.getMeanMark("P_WaitDial")
                + this.net.getMeanMark("P_WaitTalk")
                + this.net.getMeanBuffer("T_Retry")
                + this.net.getMeanBuffer("T_Dial")
                + this.net.getMeanBuffer("T_Talk");
    }

    public double getCumulativeTotalDuration(double cumulativeBusyArea) {
        int totalArrived = this.net.getCurrentMark("P_TotalArrived");
        double currentTime = this.sim.getCurrentTime();
        if (totalArrived > 0 && currentTime > 0.0) {
            double cumulativeMeanL = cumulativeBusyArea / currentTime;
            double lambdaIn = (double) totalArrived / currentTime;
            return cumulativeMeanL / lambdaIn;
        }
        return 0.0;
    }

    public int getServed() {
        return this.served;
    }

    public int getRetries() {
        return this.retries;
    }

    public double getLambda() {
        return this.lambda;
    }

    public double getLoadL1() {
        return this.loadL1;
    }

    public double getLoadL2() {
        return this.loadL2;
    }

    public double getRetryProbability() {
        return this.retryProbability;
    }

    public double getDialDuration() {
        return this.dialDuration;
    }

    public double getTalkDuration() {
        return this.talkDuration;
    }

    public double getTotalDuration() {
        return this.totalDuration;
    }

    public double[] getAllMetrics() {
        double[] metrics = new double[6];
        metrics[0] = this.dialDuration;
        metrics[1] = this.talkDuration;
        metrics[2] = this.totalDuration;
        metrics[3] = this.loadL1;
        metrics[4] = this.loadL2;
        metrics[5] = this.retryProbability;
        return metrics;
    }

    public String toCsvString() {
        return String.format(Locale.US, "%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f",
                this.served, this.retries, this.dialDuration, this.talkDuration, this.totalDuration, this.loadL1, this.loadL2, this.retryProbability);
    }

    public String toTableRow(int expNo) {
        return String.format(Locale.US, "%-3d | %-3d | %-3d | %-7d | %-7d | %-8.4f | %-8.4f | %-9.4f | %-7.4f | %-7.4f | %-8.4f",
                expNo, this.l1, this.l2, this.served, this.retries, this.dialDuration, this.talkDuration, this.totalDuration, this.loadL1, this.loadL2, this.retryProbability);
    }
}