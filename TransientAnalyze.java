import java.util.Locale;

public class TransientAnalyze{
    private final int runs;
    private final int maxTime;
    private final int step;
    private final int requiredHoldSteps;

    public TransientAnalyze() {
        this(4, 20000, 100, 10);
    }

    public TransientAnalyze(int runs, int maxTime, int step, int requiredHoldSteps) {
        this.runs = runs;
        this.maxTime = maxTime;
        this.step = step;
        this.requiredHoldSteps = requiredHoldSteps;
    }

    public static double analyze() {
        return new TransientAnalyze().runAnalysis();
    }

    public double runAnalysis() {
        int stepsCount = maxTime / step;
        double[][] historyP = new double[runs][stepsCount];
        double[][] historyT = new double[runs][stepsCount];
        PetriNet[] nets = new PetriNet[runs];
        NetworkConfig baseConfig = new NetworkConfig();

        for (int r = 0; r < runs; r++) {
            PetriNet net = PhoneNetworkModel.createNet(baseConfig);
            PetriSim sim = new PetriSim(net);
            double cumulativeBusyArea = 0.0;

            for (int s = 0; s < stepsCount; s++) {
                double targetTime = (s + 1) * step;
                sim.setSimulationTime(targetTime);
                sim.go();

                NetworkMetrics metrics = new NetworkMetrics(sim, baseConfig.getFreeL1Count(), baseConfig.getFreeL2Count());
                cumulativeBusyArea += metrics.getStepActiveClients() * step;
                historyP[r][s] = metrics.getRetryProbability();
                historyT[r][s] = metrics.getCumulativeTotalDuration(cumulativeBusyArea);
            }
            nets[r] = net;
        }

        printProgressTable(historyP, historyT, stepsCount);

        int plateauStart = stepsCount / 2;
        double meanP = calculatePlateauMean(historyP, plateauStart);
        double stdP = calculatePlateauStdDev(historyP, plateauStart, meanP);
        double lowP = meanP - 3.0 * stdP;
        double highP = meanP + 3.0 * stdP;

        double meanT = calculatePlateauMean(historyT, plateauStart);
        double stdT = calculatePlateauStdDev(historyT, plateauStart, meanT);
        double lowT = meanT - 3.0 * stdT;
        double highT = meanT + 3.0 * stdT;

        int idxP = findConfidenceEntryIndex(historyP, lowP, highP, requiredHoldSteps);
        int idxT = findConfidenceEntryIndex(historyT, lowT, highT, requiredHoldSteps);
        int finalIdx = Math.max(idxP, idxT);

        double t1 = (idxP + 1) * step;
        double t2 = (idxT + 1) * step;
        double transientT = (finalIdx + 1) * step;

        int bestRunIdx = findMostRepresentativeRun(historyP, historyT, meanP, meanT, stepsCount - 1);

        System.out.println("-------------------------------------------------------------------------------------------------");
        System.out.printf(Locale.US, "P_retry: [%.4f ; %.4f] (mean=%.4f, sigma=%.4f) -> T1 = %.0f\n", lowP, highP, meanP, stdP, t1);
        System.out.printf(Locale.US, "T_total: [%.4f ; %.4f] (mean=%.4f, sigma=%.4f) -> T2 = %.0f\n", lowT, highT, meanT, stdT, t2);
        System.out.printf(Locale.US, "Accepted transient period: T = %.0f time units (snapshot from run #%d)\n\n",
                transientT, bestRunIdx + 1);

        new ChartBuilder().buildTransientChart(
                "transient_period.png", historyP, historyT, step, t1, t2, lowP, highP, lowT, highT
        );
        SimulationRunner.saveSnapshot(nets[bestRunIdx], transientT);
        verifyWarmStart(lowP, highP, lowT, highT);

        return transientT;
    }

    public void verifyWarmStart(double lowP, double highP, double lowT, double highT) {
        int validationTime = 6000;
        int stepsCount = validationTime / step;
        double[][] historyP = new double[runs][stepsCount];
        double[][] historyT = new double[runs][stepsCount];
        NetworkConfig baseConfig = new NetworkConfig();

        for (int r = 0; r < runs; r++) {
            PetriNet net = SimulationRunner.prepareNetFromSnapshot(baseConfig, SimulationRunner.file);
            PetriSim sim = new PetriSim(net);
            double cumulativeBusyArea = 0.0;

            for (int s = 0; s < stepsCount; s++) {
                double targetTime = (s + 1) * step;
                sim.setSimulationTime(targetTime);
                sim.go();

                NetworkMetrics metrics = new NetworkMetrics(sim, baseConfig.getFreeL1Count(), baseConfig.getFreeL2Count());
                cumulativeBusyArea += metrics.getStepActiveClients() * step;
                historyP[r][s] = metrics.getRetryProbability();
                historyT[r][s] = metrics.getCumulativeTotalDuration(cumulativeBusyArea);
            }
        }

        int idxP = findConfidenceEntryIndex(historyP, lowP, highP, requiredHoldSteps);
        int idxT = findConfidenceEntryIndex(historyT, lowT, highT, requiredHoldSteps);
        double warmT1 = (idxP + 1) * step;
        double warmT2 = (idxT + 1) * step;

        System.out.println("=== Warm-start steady-state verification ===");
        System.out.printf(Locale.US, "P_retry enters steady corridor at t = %.0f (cold start was T1)\n", warmT1);
        System.out.printf(Locale.US, "T_total enters steady corridor at t = %.0f (cold start was T2)\n\n", warmT2);

        new ChartBuilder().buildTransientChart(
                "warm_start_validation.png", historyP, historyT, step, warmT1, warmT2, lowP, highP, lowT, highT
        );
    }

    private int findMostRepresentativeRun(double[][] historyP, double[][] historyT,
                                          double meanP, double meanT, int lastStep) {
        int bestIdx = 0;
        double minNormalizedError = Double.MAX_VALUE;
        for (int r = 0; r < runs; r++) {
            double errP = Math.abs(historyP[r][lastStep] - meanP) / meanP;
            double errT = Math.abs(historyT[r][lastStep] - meanT) / meanT;
            double totalErr = errP + errT;
            if (totalErr < minNormalizedError) {
                minNormalizedError = totalErr;
                bestIdx = r;
            }
        }
        return bestIdx;
    }

    private int findConfidenceEntryIndex(double[][] history, double lowBound, double highBound, int holdSteps) {
        int totalRuns = history.length;
        int steps = history[0].length;

        for (int s = 0; s <= steps - holdSteps; s++) {
            boolean allInsideWindow = true;
            for (int k = s; k < s + holdSteps; k++) {
                for (int r = 0; r < totalRuns; r++) {
                    double val = history[r][k];
                    if (val < lowBound || val > highBound) {
                        allInsideWindow = false;
                        break;
                    }
                }
                if (!allInsideWindow) {
                    break;
                }
            }
            if (allInsideWindow) {
                return s;
            }
        }
        return steps - 1;
    }

    private void printProgressTable(double[][] historyP, double[][] historyT, int stepsCount) {
        System.out.println("=== Transient period analysis ===");
        System.out.printf(Locale.US, "%-7s | %-27s | %-8s | %-31s | %-8s\n",
                "Time", "P_retry", "Spread_P", "T_total", "Spread_T");
        System.out.println("-------------------------------------------------------------------------------------------------");

        for (int s = 0; s < stepsCount; s++) {
            double minP = Double.MAX_VALUE;
            double maxP = -Double.MAX_VALUE;
            double minT = Double.MAX_VALUE;
            double maxT = -Double.MAX_VALUE;

            for (int r = 0; r < runs; r++) {
                if (historyP[r][s] < minP) {
                    minP = historyP[r][s];
                }
                if (historyP[r][s] > maxP) {
                    maxP = historyP[r][s];
                }
                if (historyT[r][s] < minT) {
                    minT = historyT[r][s];
                }
                if (historyT[r][s] > maxT) {
                    maxT = historyT[r][s];
                }
            }

            double spreadP = maxP - minP;
            double spreadT = maxT - minT;

            if ((s + 1) % 5 == 0 || s == 0) {
                System.out.printf(Locale.US, "%-7d | %.3f %.3f %.3f %.3f   | %-8.4f | %5.2f %5.2f %5.2f %5.2f       | %-8.4f\n",
                        (s + 1) * step,
                        historyP[0][s], historyP[1][s], historyP[2][s], historyP[3][s], spreadP,
                        historyT[0][s], historyT[1][s], historyT[2][s], historyT[3][s], spreadT);
            }
        }
    }

    private double calculatePlateauMean(double[][] history, int startStep) {
        int totalRuns = history.length;
        int steps = history[0].length;
        double sum = 0.0;
        int count = 0;

        for (int r = 0; r < totalRuns; r++) {
            for (int s = startStep; s < steps; s++) {
                sum += history[r][s];
                count++;
            }
        }
        return sum / count;
    }

    private double calculatePlateauStdDev(double[][] history, int startStep, double mean) {
        int totalRuns = history.length;
        int steps = history[0].length;
        double sqSum = 0.0;
        int count = 0;

        for (int r = 0; r < totalRuns; r++) {
            for (int s = startStep; s < steps; s++) {
                double diff = history[r][s] - mean;
                sqSum += diff * diff;
                count++;
            }
        }
        return Math.sqrt(sqSum / (count - 1));
    }
}
