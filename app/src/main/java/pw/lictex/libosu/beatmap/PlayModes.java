package pw.lictex.libosu.beatmap;

public enum PlayModes {
    Osu(0),
    Taiko(1),
    CatchTheBeat(2),
    OsuMania(3);

    private int i;

    PlayModes(int i) {
        this.i = i;
    }

    public static PlayModes valueOf(int i) {
        for (PlayModes b : PlayModes.values())
            if (b.asInt() == i) return b;
        return null;
    }

    public int asInt() {
        return i;
    }
}
