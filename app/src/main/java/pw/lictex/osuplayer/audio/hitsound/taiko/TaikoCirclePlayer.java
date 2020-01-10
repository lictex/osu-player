package pw.lictex.osuplayer.audio.hitsound.taiko;

import pw.lictex.libosu.beatmap.*;
import pw.lictex.osuplayer.audio.hitsound.*;

public class TaikoCirclePlayer extends HitsoundPlayer<HitObject.Circle> {
    private static boolean rightHit = false;

    public TaikoCirclePlayer(HitsoundContext context, HitObject.Circle object) {
        super(context, object);
    }

    @Override public boolean play() {
        if (hsContext.getPreviousHitsoundTime() <= hitObject.getTime()) {
            SampleSet sampleSet = hsContext.sampleSetAt(hitObject.getTime());
            int customSet = hsContext.customSetAt(hitObject.getTime());
            float volume = hsContext.volumeAt(hitObject.getTime());

            float pan = rightHit ? 0.2f : -0.2f;
            rightHit = !rightHit;
            int hitSounds = hitObject.getHitSounds();

            boolean blue = (hitSounds & 1 << 3) > 0 || (hitSounds & 1 << 1) > 0;
            boolean finish = (hitSounds & 1 << 2) > 0;
            if (blue) {
                hsContext.getSampleManager().getTaikoSample(sampleSet, "hitclap", customSet).play(volume, pan);
                if (finish) {
                    hsContext.getSampleManager().getTaikoSample(sampleSet, "hitclap", customSet).play(volume, -pan);
                    hsContext.getSampleManager().getTaikoSample(sampleSet, "hitwhistle", customSet).play(volume, pan);
                }
            } else {
                hsContext.getSampleManager().getTaikoSample(sampleSet, "hitnormal", customSet).play(volume, pan);
                if (finish) {
                    hsContext.getSampleManager().getTaikoSample(sampleSet, "hitnormal", customSet).play(volume, -pan);
                    hsContext.getSampleManager().getTaikoSample(sampleSet, "hitfinish", customSet).play(volume, pan);
                }
            }
            hsContext.updateCurrentHitsoundTime((int) Math.ceil(hitObject.getTime()));
        }
        return true;
    }
}
