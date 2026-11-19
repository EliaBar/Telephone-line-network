import java.util.Locale;

public class DiscreteAnova {
    private final int runsPerLevel;
    private final double observationDuration;

    public DiscreteAnova(int runsPerLevel, double observationDuration) {
        this.runsPerLevel = runsPerLevel;
        this.observationDuration = observationDuration;
    }

    public static void run(int runsPerLevel, double observationDuration) {
        new DiscreteAnova(runsPerLevel, observationDuration).runAnalysis();
    }

    public void runAnalysis() {
        int[] l1Levels = {8, 9, 10, 11, 12, 13, 14, 15};
        double[][] l1Stats = performOneWayAnova("External lines L1", l1Levels, true);

        int[] l2Levels = {1, 2, 3};
        double[][] l2Stats = performOneWayAnova("Internal lines L2", l2Levels, false);

        new ChartBuilder().buildAnovaChart(
                "anova_profiles.png",
                l1Levels, l1Stats[0], l1Stats[1], l1Stats[2], l1Stats[3],
                l2Levels, l2Stats[0], l2Stats[1], l2Stats[2], l2Stats[3]
        );
    }

    private double[][] performOneWayAnova(String factorName, int[] levels, boolean isL1) {
        int k = levels.length;
        double[][] dataP = new double[k][runsPerLevel];
        double[][] dataT = new double[k][runsPerLevel];
        NetworkConfig base = new NetworkConfig();

        for (int j = 0; j < k; j++) {
            int l1Val = isL1 ? levels[j] : base.getFreeL1Count();
            int l2Val = isL1 ? base.getFreeL2Count() : levels[j];
            NetworkConfig levelConfig = new NetworkConfig().setLines(l1Val, l2Val);

            for (int r = 0; r < runsPerLevel; r++) {
                NetworkMetrics m = SimulationRunner.runFromSnapshot(levelConfig, observationDuration);
                dataP[j][r] = m.getRetryProbability();
                dataT[j][r] = m.getTotalDuration();
            }
        }

        printAnovaTable(factorName, "P_retry", levels, dataP);
        printAnovaTable(factorName, "T_total", levels, dataT);

        double[][] stats = new double[4][k];
        for (int j = 0; j < k; j++) {
            stats[0][j] = calculateMean(dataP[j]);
            stats[1][j] = calculateStdDev(dataP[j], stats[0][j]);
            stats[2][j] = calculateMean(dataT[j]);
            stats[3][j] = calculateStdDev(dataT[j], stats[2][j]);
        }
        return stats;
    }

    private double calculateStdDev(double[] values, double mean) {
        double sqSum = 0.0;
        for (double v : values) {
            double diff = v - mean;
            sqSum += diff * diff;
        }
        return Math.sqrt(sqSum / (values.length - 1));
    }

    private void printAnovaTable(String factorName, String metricName, int[] levels, double[][] data) {
        int k = levels.length;
        int p = data[0].length;
        double[] levelMeans = new double[k];
        double grandSum = 0.0;

        for (int j = 0; j < k; j++) {
            levelMeans[j] = calculateMean(data[j]);
            grandSum += levelMeans[j];
        }
        double grandMean = grandSum / k;

        double sFact = 0.0;
        double sRes = 0.0;
        for (int j = 0; j < k; j++) {
            double diffMean = levelMeans[j] - grandMean;
            sFact += p * diffMean * diffMean;
            for (int r = 0; r < p; r++) {
                double diffObs = data[j][r] - levelMeans[j];
                sRes += diffObs * diffObs;
            }
        }

        int df1 = k - 1;
        int df2 = k * (p - 1);
        double dFact = sFact / df1;
        double dRes = sRes / df2;
        double fObs = dFact / dRes;
        double fCrit = getFisherCritical005(df1);

        System.out.println("=== One-way ANOVA: " + factorName + " -> " + metricName + " ===");
        for (int j = 0; j < k; j++) {
            System.out.printf(Locale.US, "Level %d: mean = %.4f\n", levels[j], levelMeans[j]);
        }
        System.out.printf(Locale.US, "d_fact = %.6f (df1 = %d), d_res = %.6f (df2 = %d)\n", dFact, df1, dRes, df2);
        System.out.printf(Locale.US, "F_obs = %.4f, F_crit (alpha = 0.05) = %.4f -> ", fObs, fCrit);
        if (fObs > fCrit) {
            System.out.println("Factor has a statistically significant effect\n");
        } else {
            System.out.println("Factor effect is statistically insignificant\n");
        }
    }

    private double getFisherCritical005(int f1) {
        double[] fTableInf = {
            0.00, 3.84, 3.00, 2.60, 2.37, 2.21, 2.10, 2.01,
            1.94, 1.88, 1.83, 1.79, 1.75, 1.72, 1.69, 1.67
        };
        if (f1 < 1) {
            return 3.84;
        }
        if (f1 < fTableInf.length) {
            return fTableInf[f1];
        }
        return 1.67;
    }

    private double calculateMean(double[] values) {
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }
}