public class Main {
    public static void main(String[] args) {
        //PhoneNetworkModel.generateEventsProtocol("events_protocol.txt", 50.0);
        //PhoneNetworkModel.doVerification();

        double observationDuration = 4000.0;
        int sampleRuns = 1000;

        //TransientAnalyze.analyze();
        //int requiredRuns = Statistics.analyze(sampleRuns, observationDuration);
        //FactorialRegression.run(1000, observationDuration);
        DiscreteAnova.run(208, observationDuration);
    }
}