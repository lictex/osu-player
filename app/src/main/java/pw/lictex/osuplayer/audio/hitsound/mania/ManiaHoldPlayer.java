package pw.lictex.osuplayer.audio.hitsound.mania;

import pw.lictex.libosu.beatmap.*;
import pw.lictex.osuplayer.audio.hitsound.*;

public class ManiaHoldPlayer extends HitsoundPlayer<HitObject.Hold> {
    public ManiaHoldPlayer(HitsoundContext context, HitObject.Hold object) {
        super(context, object);
    }

    @Override public boolean play() {
        if (hsContext.getPreviousHitsoundTime() <= hitObject.getTime()) {
            SampleSet sampleSet = hsContext.sampleSetWithOverrideAt(hitObject.getTime(), hitObject);
            int customSet = hsContext.customSetWithOverrideAt(hitObject.getTime(), hitObject);
            float volume = hsContext.volumeWithOverrideAt(hitObject.getTime(), hitObject);
            SampleSet additionSet = (hitObject.getAdditions() != SampleSet.None) ? hitObject.getAdditions() : sampleSet;

            float pan = (hitObject.getX() / 512f - 0.5f) * 0.8f; //TODO maybe unnecessary..?
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
            hsContext.getCurrentHitsoundTime().set(Math.max(hsContext.getCurrentHitsoundTime().get(), (int) Math.ceil(hitObject.getTime())));
        }
        return true;
    }
}