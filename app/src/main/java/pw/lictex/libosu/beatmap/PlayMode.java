package pw.lictex.libosu.beatmap;

public enum PlayMode {
    Osu(0),
    Taiko(1),
    CatchTheBeat(2),
    OsuMania(3);

    private int i;

    PlayMode(int i) {
        this.i = i;
    }

    public static PlayMode valueOf(int i) {
        for (PlayMode b : PlayMode.values())
            if (b.asInt() == i) return b;
        return null;
    }

    public int asInt() {
        return i;
    }
}
