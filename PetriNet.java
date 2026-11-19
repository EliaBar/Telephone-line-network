import java.util.ArrayList;
import java.util.StringJoiner;

public class PetriNet{
    private final String name;
    private final Place[] listP;
    private final Transition[] listT;
    private final ArcIn[] listIn;
    private final ArcOut[] listOut;

    public PetriNet(String name, Place[] places, Transition[] transitions, ArcIn[] inArcs, ArcOut[] outArcs) {
        this.name = name;
        this.listP = places;
        this.listT = transitions;
        this.listIn = inArcs;
        this.listOut = outArcs;

        for (Transition transition : listT) {
            transition.createInP(listIn);
            transition.createOutP(listOut);
        }
        validateStructure();
    }

    public PetriNet(String name, ArrayList<Place> places, ArrayList<Transition> transitions, 
                    ArrayList<ArcIn> inArcs, ArrayList<ArcOut> outArcs) {
        this(name, places.toArray(new Place[0]), transitions.toArray(new Transition[0]),
            inArcs.toArray(new ArcIn[0]), outArcs.toArray(new ArcOut[0]));
    }

    public ArcIn[] getListIn() {
        return listIn;
    }

    public ArcOut[] getListOut() {
        return listOut;
    }

    public String getName() {
        return name;
    }

    public void validateStructure() {
        for (Transition transition : listT) {
            if (!transition.hasConsumingInput()) {
                throw new IllegalStateException("Transition " + transition.getName() + " has no input places.");
            }
        }
    }

    public Place[] getListP() {
        return listP;
    }

    public Transition[] getListT() {
        return listT;
    }

    public int getCurrentMark(String placeName) {
        int index = strToNumP(placeName);
        if (index == -1) {
            throw new IllegalArgumentException("Place with name '" + placeName + "' not found in net " + name);
        }
        return listP[index].getMark();
    }

    public double getMeanMark(String placeName) {
        int index = strToNumP(placeName);
        if (index == -1) {
            throw new IllegalArgumentException("Place with name '" + placeName + "' not found in net " + name);
        }
        return listP[index].getMean();
    }

    public int getCurrentBuffer(String transitionName) {
        int index = strToNumT(transitionName);
        if (index == -1) {
            throw new IllegalArgumentException("Transition with name '" + transitionName + "' not found in net " + name);
        }
        return listT[index].getBuffer();
    }

    public double getMeanBuffer(String transitionName) {
        int index = strToNumT(transitionName);
        if (index == -1) {
            throw new IllegalArgumentException("Transition with name '" + transitionName + "' not found in net " + name);
        }
        return listT[index].getMean();
    }

    public int strToNumP(String name) {
        for (int i = 0; i < listP.length; i++) {
            if (listP[i].getName().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    public int strToNumT(String name) {
        for (int i = 0; i < listT.length; i++) {
            if (listT[i].getName().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    public void printArcs() {
        System.out.println("Petri net " + name + " arcs: " + listIn.length + " input arcs and " + listOut.length + " output arcs");

        System.out.println("Input arcs (Place -> Transition):");
        for (ArcIn arc : listIn) {
            System.out.println("  Arc #" + arc.getNumber() + ": " + arc.getNameP() + " -> " + arc.getNameT());
        }

        System.out.println("Output arcs (Transition -> Place):");
        for (ArcOut arc : listOut) {
            System.out.println("  Arc #" + arc.getNumber() + ": " + arc.getNameT() + " -> " + arc.getNameP());
        }
    }

    public void printMark() {
        StringJoiner sj = new StringJoiner(", ", "Marking in Net " + name + ": [", "]");
        for (Place p : listP) {
            sj.add(p.getName() + "=" + p.getMark());
        }
        System.out.println(sj.toString());
    }

    public void printBuffer() {
        StringJoiner sj = new StringJoiner(", ", "Buffer in Net " + name + ": [", "]");
        for (Transition t : listT) {
            sj.add(t.getName() + "=" + t.getBuffer());
        }
        System.out.println(sj.toString());
    }
}