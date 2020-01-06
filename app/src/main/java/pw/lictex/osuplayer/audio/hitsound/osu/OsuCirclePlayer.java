package pw.lictex.osuplayer.audio.hitsound.osu;

import pw.lictex.libosu.beatmap.*;
import pw.lictex.osuplayer.audio.hitsound.*;

public class OsuCirclePlayer extends HitsoundPlayer<HitObject.Circle> {
    public OsuCirclePlayer(HitsoundContext context, HitObject.Circle object) {
        super(context, object);
    }

    @Override public boolean play() {
        if (hsContext.getPreviousHitsoundTime() <= hitObject.getTime()) {
            SampleSet sampleSet = hsContext.sampleSetWithOverrideAt(hitObject.getTime(), hitObject);
            int customSet = hsContext.customSetWithOverrideAt(hitObject.getTime(), hitObject);
            float volume = hsContext.volumeWithOverrideAt(hitObject.getTime(), hitObject);
            SampleSet additionSet = (hitObject.getAdditions() != SampleSet.None) ? hitObject.getAdditions() : sampleSet;

            float pan = (hitObject.getX() / 512f - 0.5f) * 0.8f;
            int hitSounds = hitObject.getHitSounds();

            if (!hitObject.getSampleFile().isEmpty())
                hsContext.getSampleManager().getSample(hitObject.getSampleFile().substring(0, hitObject.getSampleFile().lastIndexOf("."))).play(volume, pan);
            else
                hsContext.getSampleManager().getSample(sampleSet, "hitnormal", customSet).play(volume, pan);

            if ((hitSounds & 1 << 1) > 0)
                hsContext.getSampleManager().getSample(additionSet, "hitwhistle", customSet).play(volume, pan);
            if ((hitSounds & 1 << 2) > 0)
                hsContext.getSampleManager().getSample(additionSet, "hitfinish", customSet).play(volume, pan);
            if ((hitSounds & 1 << 3) > 0)
                hsContext.getSampleManager().getSample(additionSet, "hitclap", customSet).play(volume, pan);

            hsContext.updateCurrentHitsoundTime((int) Math.ceil(hitObject.getTime()));
        }
        return true;
    }
}