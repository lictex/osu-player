package pw.lictex.libosu.beatmap;

public enum SampleSet {
    All(-1),
    None(0),
    Normal(1),
    Soft(2),
    Drum(3);

    private int i;

    SampleSet(int i) {
        this.i = i;
    }

    public static SampleSet fromInt(int i) {
        for (SampleSet b : SampleSet.values())
            if (b.asInt() == i) return b;
        return null;
    }

    public int asInt() {
        return i;
    }
}
