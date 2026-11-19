public abstract class Arc {
    private static int next = 0;
    private final int number;
    private final int numP;
    private final int numT;
    private final String nameP;
    private final String nameT;

    protected Arc(Place place, Transition transition) {
        if (place == null || transition == null) {
            throw new IllegalArgumentException("Place та Transition не можуть бути null");
        }
        this.number = next++;
        this.numP = place.getNumber();
        this.numT = transition.getNumber();
        this.nameP = place.getName();
        this.nameT = transition.getName();
    }

    public static void initNext() { 
        next = 0; 
    }

    public int getNumber() { 
        return number; 
    }

    public int getNumP() { 
        return numP; 
    }

    public int getNumT() { 
        return numT; 
    }

    public String getNameP() { 
        return nameP; 
    }

    public String getNameT() { 
        return nameT; 
    }
}