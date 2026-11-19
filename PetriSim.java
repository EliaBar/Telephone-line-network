import java.util.ArrayList;
import java.util.Comparator;

public class PetriSim {
    private final PetriNet net;
    private StateTime timeState;
    private String name;
    protected Transition eventMin;
    protected double timeMin;
    private static final Comparator<Transition> comparator = Comparator.comparingInt(Transition::getPriority).reversed();

    public PetriSim(PetriNet net) {
        this(net, new StateTime());
    }

    public PetriSim(PetriNet net, StateTime timeState) {
        this.net = net;
        this.timeState = timeState;
        this.name = net.getName();
        this.timeMin = Double.MAX_VALUE;
        this.eventMin = null;
    }

    public PetriNet getNet() {
        return net;
    }

    public String getName() {
        return name;
    }

    public double getCurrentTime() {
        return timeState.getCurrentTime();
    }

    public void setTimeCurr(double timeCurr) {
        timeState.setCurrentTime(timeCurr);
    }

    public double getSimulationTime() {
        return timeState.getSimulationTime();
    }

    public void setSimulationTime(double simulationTime) {
        timeState.setSimulationTime(simulationTime);
    }

    public StateTime getTimeState() {
        return timeState;
    }

    public double getTimeMin() {
        return timeMin;
    }

    public Transition getEventMin() {
        return eventMin;
    }

    public void eventMin() {
        Transition event = null;
        double min = Double.MAX_VALUE;
        for (Transition t : net.getListT()) {
            if (t.getMinTime() < min) {
                event = t;
                min = t.getMinTime();
            }
        }
        this.timeMin = min;
        this.eventMin = event;
    }

    public ArrayList<Transition> findActiveT() {
        ArrayList<Transition> active = new ArrayList<>();
        Place[] places = net.getListP();
        for (Transition t : net.getListT()) {
            if (t.condition(places)) {
                active.add(t);
            }
        }
        if (active.size() > 1) {
            active.sort(comparator);
        }
        return active;
    }

    public Transition doConflikt(ArrayList<Transition> transitions) {
        return transitions.get(0);
    }

    public boolean isBufferEmpty() {
        for (Transition t : net.getListT()) {
            if (t.getBuffer() > 0) {
                return false;
            }
        }
        return true;
    }

    public boolean isStop() {
        this.eventMin();
        return (eventMin == null || timeMin == Double.MAX_VALUE);
    }

    public void input() {
        ArrayList<Transition> activeT = this.findActiveT();
        Place[] places = net.getListP();
        while (!activeT.isEmpty()) {
            Transition tr = this.doConflikt(activeT);
            tr.actIn(places, this.getCurrentTime());
            activeT = this.findActiveT();
        }
        this.eventMin();
    }

    public void output() {
        Place[] places = net.getListP();
        for (Transition t : net.getListT()) {
            while (t.getBuffer() > 0 && t.getMinTime() == this.getCurrentTime()) {
                t.actOut(places);
            }
        }
    }

    public void step() {
        this.eventMin();
        if (this.eventMin == null || this.timeMin == Double.MAX_VALUE) {
            updateStatistics(getSimulationTime() - this.getCurrentTime());
            setTimeCurr(getSimulationTime());
            return;
        }

        if (this.timeMin > getSimulationTime()) {
            updateStatistics(getSimulationTime() - this.getCurrentTime());
            setTimeCurr(getSimulationTime());
            return;
        }

        double dt = this.timeMin - this.getCurrentTime();
        updateStatistics(dt);
        setTimeCurr(this.timeMin);
        this.output();
        this.input();
    }

    public void go() {
        setTimeCurr(0.0);
        this.input();

        while (this.getCurrentTime() < getSimulationTime() && !isStop()) {
            this.step();
        }
    }

    public void go(double time) {
        setSimulationTime(time);
        go();
    }

    private void updateStatistics(double dt) {
        if (dt > 0 && getSimulationTime() > 0) {
            double nextTime = this.getCurrentTime() + dt;
            double weight = dt / nextTime;
            for (Place p : net.getListP()) {
                p.changeMean(weight);
            }
            for (Transition t : net.getListT()) {
                t.changeMean(weight);
            }
        }
    }

    public void printMark() {
        net.printMark();
    }

    public void printBuffer() {
        net.printBuffer();
    }
}