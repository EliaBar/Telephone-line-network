import java.util.Locale;

public class FactorialRegression {
    private final int runsPerPoint;
    private final double observationDuration;
    private final double variationStep;

    public FactorialRegression(int runsPerPoint, double observationDuration) {
        this(runsPerPoint, observationDuration, 0.03);
    }

    public FactorialRegression(int runsPerPoint, double observationDuration, double variationStep) {
        this.runsPerPoint = runsPerPoint;
        this.observationDuration = observationDuration;
        this.variationStep = variationStep;
    }

    public static void run(int runsPerPoint, double observationDuration) {
        new FactorialRegression(runsPerPoint, observationDuration).runAnalysis();
    }

    public void runAnalysis() {
        int experimentsCount = 8;
        NetworkConfig base = new NetworkConfig();

        double[] lowLevels = {
            base.getArrivalMean() * (1.0 - variationStep),
            base.getTalkMean() * (1.0 - variationStep),
            base.getRetryMean() * (1.0 - variationStep)
        };
        double[] highLevels = {
            base.getArrivalMean() * (1.0 + variationStep),
            base.getTalkMean() * (1.0 + variationStep),
            base.getRetryMean() * (1.0 + variationStep)
        };

        int[][] xMatrix = buildDesignMatrix(experimentsCount);

        double[] meanY1 = new double[experimentsCount];
        double[] varY1 = new double[experimentsCount];
        double[] meanY2 = new double[experimentsCount];
        double[] varY2 = new double[experimentsCount];

        System.out.println("=== Factorial experiment 2^3 (p = " + runsPerPoint + " runs per point, T_obs = " + (int) observationDuration + ") ===");
        System.out.println("-------------------------------------------------------------------------------------------------------------------");
        System.out.printf(Locale.US, "%-4s | %-3s %-3s %-3s %-3s %-4s %-4s %-4s %-5s | %-6s %-6s %-6s | %-10s %-10s | %-10s %-10s\n",
                "Exp", "x0", "x1", "x2", "x3", "x12", "x13", "x23", "x123", "t_arr", "T_talk", "T_ret", "Mean_P", "Var_P", "Mean_T", "Var_T");
        System.out.println("-------------------------------------------------------------------------------------------------------------------");

        for (int u = 0; u < experimentsCount; u++) {
            double arrVal = (xMatrix[u][1] == -1) ? lowLevels[0] : highLevels[0];
            double talkVal = (xMatrix[u][2] == -1) ? lowLevels[1] : highLevels[1];
            double retVal = (xMatrix[u][3] == -1) ? lowLevels[2] : highLevels[2];

            NetworkConfig expConfig = new NetworkConfig()
                    .withArrivalMean(arrVal)
                    .withTalkMean(talkVal)
                    .withRetryMean(retVal);

            double[] runP = new double[runsPerPoint];
            double[] runT = new double[runsPerPoint];

            for (int r = 0; r < runsPerPoint; r++) {
                NetworkMetrics m = SimulationRunner.runFromSnapshot(expConfig, observationDuration);
                runP[r] = m.getRetryProbability();
                runT[r] = m.getTotalDuration();
            }

            meanY1[u] = calculateMean(runP);
            varY1[u] = calculateVariance(runP, meanY1[u]);
            meanY2[u] = calculateMean(runT);
            varY2[u] = calculateVariance(runT, meanY2[u]);

            System.out.printf(Locale.US,
                    "%-4d | %+3d %+3d %+3d %+3d %+4d %+4d %+4d %+5d | %-6.2f %-6.2f %-6.2f | %-10.4f %-10.6f | %-10.4f %-10.4f\n",
                    u + 1,
                    xMatrix[u][0], xMatrix[u][1], xMatrix[u][2], xMatrix[u][3],
                    xMatrix[u][4], xMatrix[u][5], xMatrix[u][6], xMatrix[u][7],
                    arrVal, talkVal, retVal,
                    meanY1[u], varY1[u], meanY2[u], varY2[u]);
        }
        System.out.println("-------------------------------------------------------------------------------------------------------------------\n");

        analyzeRegressionModel("P_retry", xMatrix, meanY1, varY1);
        analyzeRegressionModel("T_total", xMatrix, meanY2, varY2);

        new ChartBuilder().buildFactorialChart(
                "factorial_profiles.png",
                "P_retry", meanY1, varY1,
                "T_total", meanY2, varY2
        );
    }

    private int[][] buildDesignMatrix(int experimentsCount) {
        int[][] xMatrix = new int[experimentsCount][experimentsCount];
        for (int i = 0; i < experimentsCount; i++) {
            int x1 = ((i & 1) == 0) ? -1 : 1;
            int x2 = ((i & 2) == 0) ? -1 : 1;
            int x3 = ((i & 4) == 0) ? -1 : 1;

            xMatrix[i][0] = 1;
            xMatrix[i][1] = x1;
            xMatrix[i][2] = x2;
            xMatrix[i][3] = x3;
            xMatrix[i][4] = x1 * x2;
            xMatrix[i][5] = x1 * x3;
            xMatrix[i][6] = x2 * x3;
            xMatrix[i][7] = x1 * x2 * x3;
        }
        return xMatrix;
    }

    private void analyzeRegressionModel(String responseName, int[][] xMatrix, double[] yMean, double[] yVar) {
        int nExp = 8;
        String[] termNames = {"b0 (x0)", "b1 (t_arr)", "b2 (T_talk)", "b3 (T_retry)", "b12", "b13", "b23", "b123"};

        System.out.println("=== Regression and statistical analysis for " + responseName + " ===");

        double sumVar = 0.0;
        double maxVar = 0.0;
        for (int u = 0; u < nExp; u++) {
            sumVar += yVar[u];
            if (yVar[u] > maxVar) {
                maxVar = yVar[u];
            }
        }
        double cochranG = maxVar / sumVar;
        int fCochran = runsPerPoint - 1;
        double cochranCrit = getCochranCritical005(nExp, fCochran);
        System.out.printf(Locale.US, "1. Cochran test: G_obs = %.4f, G_crit (alpha=0.05, N=%d, f=%d) = %.4f -> ",
                cochranG, nExp, fCochran, cochranCrit);
        if (cochranG < cochranCrit) {
            System.out.println("Homogeneous");
        } else {
            System.out.println("Heterogeneous");
        }

        double[] b = new double[nExp];
        for (int j = 0; j < nExp; j++) {
            double sum = 0.0;
            for (int u = 0; u < nExp; u++) {
                sum += xMatrix[u][j] * yMean[u];
            }
            b[j] = sum / nExp;
        }

        double s2Repr = sumVar / nExp;
        double sBeta = Math.sqrt(s2Repr / (nExp * runsPerPoint));
        double tCrit = 1.960;
        double deltaB = tCrit * sBeta;

        System.out.printf(Locale.US, "2. Student t-test: S^2_repr = %.6f, S_beta = %.6f, delta_b = %.6f\n", s2Repr, sBeta, deltaB);
        System.out.printf(Locale.US, "%-14s | %-12s | %-12s | %-15s\n", "Term", "Coefficient", "|b_j| / S_b", "Status");
        System.out.println("---------------------------------------------------------------");

        boolean[] significant = new boolean[nExp];
        int significantCount = 0;
        for (int j = 0; j < nExp; j++) {
            double tStat = Math.abs(b[j]) / sBeta;
            if (Math.abs(b[j]) > deltaB || j == 0) {
                significant[j] = true;
                significantCount++;
            }
            String status = significant[j] ? "Significant" : "Insignificant";
            System.out.printf(Locale.US, "%-14s | %+12.6f | %-12.4f | %-15s\n",
                    termNames[j], b[j], tStat, status);
        }
        System.out.println("---------------------------------------------------------------");

        double ssAdequacy = 0.0;
        for (int u = 0; u < nExp; u++) {
            double pred = 0.0;
            for (int j = 0; j < nExp; j++) {
                if (significant[j]) {
                    pred += b[j] * xMatrix[u][j];
                }
            }
            double diff = yMean[u] - pred;
            ssAdequacy += diff * diff;
        }

        int fAd = nExp - significantCount;
        if (fAd > 0) {
            double s2Ad = (runsPerPoint * ssAdequacy) / fAd;
            double fObs = s2Ad / s2Repr;
            double fCrit = getFisherCritical005(fAd);
            System.out.printf(Locale.US, "3. Fisher test: S^2_ad = %.6f, f_ad = %d, F_obs = %.4f, F_crit = %.4f -> ",
                    s2Ad, fAd, fObs, fCrit);
            if (fObs < fCrit) {
                System.out.println("Adequate");
            } else {
                System.out.println("Inadequate");
            }
        } else {
            System.out.println("3. Fisher test: All 8 terms are significant (f_ad = 0).");
        }

        StringBuilder eq = new StringBuilder();
        eq.append("Final regression equation: Y = ");
        boolean first = true;
        for (int j = 0; j < nExp; j++) {
            if (significant[j]) {
                if (!first) {
                    eq.append(b[j] >= 0 ? " + " : " - ");
                    eq.append(String.format(Locale.US, "%.4f", Math.abs(b[j])));
                } else {
                    eq.append(String.format(Locale.US, "%.4f", b[j]));
                    first = false;
                }
                if (j > 0) {
                    eq.append("*").append(termNames[j].split(" ")[0].substring(1));
                }
            }
        }
        System.out.println(eq.toString() + "\n");
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

    private double getCochranCritical005(int nExp, int f) {
        double z = 2.4977;
        double fQuantile = Math.exp(z * Math.sqrt((2.0 * nExp) / ((nExp - 1.0) * f)));
        return 1.0 / (1.0 + (nExp - 1.0) / fQuantile);
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