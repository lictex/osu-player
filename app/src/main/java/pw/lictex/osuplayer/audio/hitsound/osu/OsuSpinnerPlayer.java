package pw.lictex.osuplayer.audio.hitsound.osu;

import pw.lictex.libosu.beatmap.*;
import pw.lictex.osuplayer.audio.hitsound.*;

public class OsuSpinnerPlayer extends HitsoundPlayer<HitObject.Spinner> {
    public OsuSpinnerPlayer(HitsoundContext context, HitObject.Spinner object) {
        super(context, object);
    }

    @Override public boolean play() {
        if (hitObject.getEndTime() < hsContext.getPlayerTime()) {
            if (hsContext.getPreviousHitsoundTime() <= hitObject.getEndTime()) {
                SampleSet sampleSet = hsContext.sampleSetWithOverrideAt(hitObject.getEndTime(), hitObject);
                int customSet = hsContext.customSetWithOverrideAt(hitObject.getEndTime(), hitObject);
                float volume = hsContext.volumeWithOverrideAt(hitObject.getEndTime(), hitObject);
                SampleSet additionSet = (hitObject.getAdditions() != SampleSet.None) ? hitObject.getAdditions() : sampleSet;

                int hitSounds = hitObject.getHitSounds();

                if (!hitObject.getSampleFile().isEmpty())
                    hsContext.getSampleManager().getSample(hitObject.getSampleFile().substring(0, hitObject.getSampleFile().lastIndexOf("."))).play(volume, 0);
                else
                    hsContext.getSampleManager().getSample(sampleSet, "hitnormal", customSet).play(volume, 0);

                if ((hitSounds & 1 << 1) > 0)
                    hsContext.getSampleManager().getSample(additionSet, "hitwhistle", customSet).play(volume, 0);
                if ((hitSounds & 1 << 2) > 0)
                    hsContext.getSampleManager().getSample(additionSet, "hitfinish", customSet).play(volume, 0);
                if ((hitSounds & 1 << 3) > 0)
                    hsContext.getSampleManager().getSample(additionSet, "hitclap", customSet).play(volume, 0);

                hsContext.updateCurrentHitsoundTime((int) Math.ceil(hitObject.getTime()));
            }
            return true;
        } else {
            float spinnerCompletion = (float) (hsContext.getPlayerTime() - hitObject.getTime()) / (hitObject.getEndTime() - hitObject.getTime()) * 2;

            if (hsContext.isSpinnerspinEnabled())
                hsContext.attachLooper(new OsuSpinnerLooper(spinnerCompletion));

            //spinner bonus
            if (spinnerCompletion >= 1) {
                int x = (hitObject.getEndTime() - hitObject.getTime()) / 2;
                hsContext.updateCurrentHitsoundTime(hitObject.getTime() + x);
                int t = 200;
                while (hsContext.getPlayerTime() - hsContext.getCurrentHitsoundTime().get() > t) {
                    if (hsContext.isSpinnerBonusEnabled()) {
                        hsContext.getSampleManager().getSample("spinnerbonus").play(hsContext.volumeAt(hsContext.getPlayerTime()), 0);
                    }
                    hsContext.getCurrentHitsoundTime().addAndGet(t);
                }
            }
        }
        return false;
    }
}
