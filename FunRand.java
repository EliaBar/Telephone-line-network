import java.util.Random;

public  class FunRand {
    private static final Random random = new Random();

    public static double exp(double timeMean, double minLimit, double maxLimit) {
        double val;
        do {
            double u = random.nextDouble();
            while (u == 0.0) {
                u = random.nextDouble();
            }
            val = -timeMean * Math.log(u);
        } while (val < minLimit || val > maxLimit);

        return val;
    }

    public static double exp(double timeMean) {
        return exp(timeMean, 0.0, Double.MAX_VALUE);
    }

    public static double norm(double timeMean, double timeDeviation, double minLimit, double maxLimit) {
        double val;
        do {
            val = timeMean + timeDeviation * random.nextGaussian();
        } while (val < minLimit || val > maxLimit);

        return val;
    }

    public static double norm(double timeMean, double timeDeviation) {
        return norm(timeMean, timeDeviation, 0.0, Double.MAX_VALUE);
    }
}