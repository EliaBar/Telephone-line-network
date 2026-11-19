import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Statistics {
    private final int sampleRuns;
    private final double observationDuration;
    private final int binsCount;

    public Statistics(int sampleRuns, double observationDuration) {
        this(sampleRuns, observationDuration, 30);
    }

    public Statistics(int sampleRuns, double observationDuration, int binsCount) {
        this.sampleRuns = sampleRuns;
        this.observationDuration = observationDuration;
        this.binsCount = binsCount;
    }

    public static int analyze(int sampleRuns, double observationDuration) {
        return new Statistics(sampleRuns, observationDuration).runAnalysis();
    }

    public int runAnalysis() {
        String[] metricNames = {
            "T_dial", "T_talk", "T_total", "U_L1", "U_L2", "P_retry"
        };
        String[] xAxisLabels = {
            "T_dial, min", "T_talk, min", "T_total, min", "U_L1", "U_L2", "P_retry"
        };
        double[][] allSamples = new double[metricNames.length][sampleRuns];
        NetworkConfig baseConfig = new NetworkConfig();

        for (int i = 0; i < sampleRuns; i++) {
            NetworkMetrics m = SimulationRunner.runFromSnapshot(baseConfig, observationDuration);
            double[] runMetrics = m.getAllMetrics();
            for (int k = 0; k < metricNames.length; k++) {
                allSamples[k][i] = runMetrics[k];
            }
        }

        printSummaryTable(metricNames, allSamples);

        new ChartBuilder().buildHistograms("histograms.png", metricNames, xAxisLabels, allSamples, binsCount);

        String[] hypotheses = {"norm", "lognorm", "exp"};
        for (int k = 0; k < metricNames.length; k++) {
            for (String dist : hypotheses) {
                performChiSquareTest(metricNames[k], allSamples[k], dist);
            }
        }

        return calculateRequiredRuns(allSamples[5], allSamples[2]);
    }

    private void printSummaryTable(String[] metricNames, double[][] allSamples) {
        System.out.println("=== Statistics (N = " + sampleRuns + ", T_obs = " + (int) observationDuration + ") ===");
        System.out.printf(Locale.US, "%-10s | %-10s | %-10s | %-10s | %-24s\n",
                "Metric", "Mean", "Variance", "StdDev", "95% Confidence Interval");
        System.out.println("---------------------------------------------------------------------------");

        for (int k = 0; k < metricNames.length; k++) {
            double mean = calculateMean(allSamples[k]);
            double var = calculateVariance(allSamples[k], mean);
            double std = Math.sqrt(var);
            double ciHalf = 1.96 * std / Math.sqrt(sampleRuns);

            System.out.printf(Locale.US, "%-10s | %-10.4f | %-10.6f | %-10.4f | [%.4f ; %.4f]\n",
                    metricNames[k], mean, var, std, mean - ciHalf, mean + ciHalf);
        }
        System.out.println();
    }

    private int calculateRequiredRuns(double[] samplesP, double[] samplesT) {
        double meanP = calculateMean(samplesP);
        double varP = calculateVariance(samplesP, meanP);
        double meanT = calculateMean(samplesT);
        double varT = calculateVariance(samplesT, meanT);

        double delta = 0.05;
        double epsP = delta * meanP;
        double epsT = delta * meanT;
        int pRunsP = (int) Math.ceil(varP / (epsP * epsP * 0.05));
        int pRunsT = (int) Math.ceil(varT / (epsT * epsT * 0.05));
        int requiredRuns = Math.max(pRunsP, pRunsT);

        System.out.printf(Locale.US, "P_retry: eps = %.4f (5%% of mean) -> p1 = %d runs\n", epsP, pRunsP);
        System.out.printf(Locale.US, "T_total: eps = %.4f (5%% of mean) -> p2 = %d runs\n", epsT, pRunsT);
        System.out.printf(Locale.US, "Accepted number of runs per experiment: p = %d\n\n", requiredRuns);

        return requiredRuns;
    }

    public void performChiSquareTest(String name, double[] data, String distType) {
        int n = data.length;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            if (data[i] < min) {
                min = data[i];
            }
            if (data[i] > max) {
                max = data[i];
            }
        }

        double margin = (max - min) * 0.001;
        min -= margin;
        max += margin;
        double h = (max - min) / binsCount;

        int[] counts = new int[binsCount];
        for (int i = 0; i < n; i++) {
            int idx = (int) ((data[i] - min) / h);
            if (idx < 0) {
                idx = 0;
            }
            if (idx >= binsCount) {
                idx = binsCount - 1;
            }
            counts[idx]++;
        }

        double mean = calculateMean(data);
        double std = Math.sqrt(calculateVariance(data, mean));

        double[] logData = new double[n];
        for (int i = 0; i < n; i++) {
            logData[i] = (data[i] > 0) ? Math.log(data[i]) : 0.0;
        }
        double logMean = calculateMean(logData);
        double logStd = Math.sqrt(calculateVariance(logData, logMean));
        double lambda = 1.0 / mean;

        double[] expected = new double[binsCount];
        for (int i = 0; i < binsCount; i++) {
            double left = min + i * h;
            double right = left + h;
            double prob = 0.0;

            if ("norm".equalsIgnoreCase(distType)) {
                double pL = (i == 0) ? 0.0 : normalCdf(left, mean, std);
                double pR = (i == binsCount - 1) ? 1.0 : normalCdf(right, mean, std);
                prob = pR - pL;
            } else if ("lognorm".equalsIgnoreCase(distType)) {
                double pL = (i == 0 || left <= 0) ? 0.0 : normalCdf(Math.log(left), logMean, logStd);
                double pR = (i == binsCount - 1) ? 1.0 : normalCdf(Math.log(right), logMean, logStd);
                prob = pR - pL;
            } else if ("exp".equalsIgnoreCase(distType)) {
                double pL = (i == 0) ? 0.0 : 1.0 - Math.exp(-lambda * Math.max(0.0, left));
                double pR = (i == binsCount - 1) ? 1.0 : 1.0 - Math.exp(-lambda * Math.max(0.0, right));
                prob = pR - pL;
            } else if ("uni".equalsIgnoreCase(distType)) {
                prob = 1.0 / binsCount;
            }

            expected[i] = n * prob;
        }

        List<Double> mLeft = new ArrayList<>();
        List<Double> mRight = new ArrayList<>();
        List<Integer> mObs = new ArrayList<>();
        List<Double> mExp = new ArrayList<>();

        double curLeft = min;
        int curObs = 0;
        double curExp = 0.0;

        for (int i = 0; i < binsCount; i++) {
            curObs += counts[i];
            curExp += expected[i];
            double curRight = min + (i + 1) * h;

            if (curExp >= 5.0 || i == binsCount - 1) {
                if (curExp < 5.0 && !mExp.isEmpty()) {
                    int last = mExp.size() - 1;
                    mRight.set(last, curRight);
                    mObs.set(last, mObs.get(last) + curObs);
                    mExp.set(last, mExp.get(last) + curExp);
                } else {
                    mLeft.add(curLeft);
                    mRight.add(curRight);
                    mObs.add(curObs);
                    mExp.add(curExp);
                }
                curLeft = curRight;
                curObs = 0;
                curExp = 0.0;
            }
        }

        int estimatedParams = 0;
        if ("norm".equalsIgnoreCase(distType) || "lognorm".equalsIgnoreCase(distType)) {
            estimatedParams = 2;
        } else if ("exp".equalsIgnoreCase(distType)) {
            estimatedParams = 1;
        }

        int finalGroups = mObs.size();
        int df = finalGroups - 1 - estimatedParams;
        if (df < 1) {
            System.out.println("Not enough degrees of freedom for " + name + " (" + distType + ")");
            return;
        }

        System.out.println("=== CHI-SQUARE TEST: " + name + " [Hypothesis: " + distType.toUpperCase() + "] ===");
        System.out.printf(Locale.US, "%-22s | %-10s | %-10s | %-12s\n",
                "Interval", "Observed", "Expected", "(O-E)^2 / E");
        System.out.println("----------------------------------------------------------------");

        double chiObserved = 0.0;
        for (int i = 0; i < finalGroups; i++) {
            double diff = mObs.get(i) - mExp.get(i);
            double term = (diff * diff) / mExp.get(i);
            chiObserved += term;
            System.out.printf(Locale.US, "[%8.4f ; %8.4f] | %-10d | %-10.2f | %-12.4f\n",
                    mLeft.get(i), mRight.get(i), mObs.get(i), mExp.get(i), term);
        }

        double chiCritical = getChiSquareCritical(df);
        System.out.println("----------------------------------------------------------------");
        System.out.printf(Locale.US, "Final groups: %d, Degrees of freedom: %d\n", finalGroups, df);
        System.out.printf(Locale.US, "Chi-Square observed = %.4f, Critical (alpha = 0.05) = %.4f\n",
                chiObserved, chiCritical);
        if (chiObserved < chiCritical) {
            System.out.println("Result: Hypothesis ACCEPTED\n");
        } else {
            System.out.println("Result: Hypothesis REJECTED\n");
        }
    }

    private double normalCdf(double x, double mean, double std) {
        double z = (x - mean) / (std * Math.sqrt(2.0));
        return 0.5 * (1.0 + erf(z));
    }

    private double erf(double x) {
        int sign = (x < 0) ? -1 : 1;
        x = Math.abs(x);
        double t = 1.0 / (1.0 + 0.3275911 * x);
        double y = 1.0 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t * Math.exp(-x * x);
        return sign * y;
    }

    private double getChiSquareCritical(int df) {
        double[] table = {
            0.00, 3.84, 5.99, 7.81, 9.49, 11.07,
            12.59, 14.07, 15.51, 16.92, 18.31, 19.68,
            21.03, 22.36, 23.68, 25.00, 26.30, 27.59
        };
        if (df < 1) {
            return 3.84;
        }
        if (df < table.length) {
            return table[df];
        }
        double z = 1.64485;
        return df * Math.pow(1.0 - (2.0 / (9.0 * df)) + z * Math.sqrt(2.0 / (9.0 * df)), 3);
    }

    private double calculateMean(double[] values) {
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    private double calculateVariance(double[] values, double mean) {
        double sqSum = 0.0;
        for (double v : values) {
            double diff = v - mean;
            sqSum += diff * diff;
        }
        return sqSum / (values.length - 1);
    }
}