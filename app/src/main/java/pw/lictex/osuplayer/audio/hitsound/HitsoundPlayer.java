package pw.lictex.osuplayer.audio.hitsound;

import pw.lictex.libosu.beatmap.*;

public abstract class HitsoundPlayer<H extends HitObject> {
    protected HitsoundContext hsContext;
    protected H hitObject;

    public HitsoundPlayer(HitsoundContext context, H object) {
        hsContext = context;
        hitObject = object;
    }

    /**
     * @return true if this hit object has ended
     */
    public abstract boolean play();
}