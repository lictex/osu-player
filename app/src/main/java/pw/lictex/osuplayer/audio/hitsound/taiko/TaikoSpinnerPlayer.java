package pw.lictex.osuplayer.audio.hitsound.taiko;

import pw.lictex.libosu.beatmap.*;
import pw.lictex.osuplayer.audio.hitsound.*;

public class TaikoSpinnerPlayer extends HitsoundPlayer<HitObject.Spinner> {
    private static int nextHit = 0;

    public TaikoSpinnerPlayer(HitsoundContext context, HitObject.Spinner object) {
        super(context, object);
    }

    @Override public boolean play() {
        SampleSet sampleSet = hsContext.sampleSetAt(hsContext.getPlayerTime());
        int customSet = hsContext.customSetAt(hsContext.getPlayerTime());
        float volume = hsContext.volumeAt(hsContext.getPlayerTime());

        int interval;
        double tickRate = hsContext.getCurrentBeatmap().getDifficultySection().getSliderTickRate();
        if (tickRate == 1.5 || tickRate == 3 || tickRate == 6)
            interval = (int) Math.ceil(hsContext.getCurrentBeatmap().notInheritedTimingPointAt(hitObject.getTime()).getBeatLength() / 6);
        else
            interval = (int) Math.ceil(hsContext.getCurrentBeatmap().notInheritedTimingPointAt(hitObject.getTime()).getBeatLength() / 8);
        while (interval < 60) interval *= 2;
        while (interval > 120) interval /= 2;

        hsContext.updateCurrentHitsoundTime((int) Math.ceil(hitObject.getTime()));
        if (hsContext.getPreviousHitsoundTime() <= hitObject.getEndTime()) {
            while (hsContext.getPlayerTime() - hsContext.getCurrentHitsoundTime().get() > interval) {
                switch (nextHit) {
                    case 0:
                        hsContext.getSampleManager().getTaikoSample(sampleSet, "hitnormal", customSet).play(volume, -0.2f); break;
                    case 1:
                        hsContext.getSampleManager().getTaikoSample(sampleSet, "hitclap", customSet).play(volume, -0.2f); break;
                    case 2:
                        hsContext.getSampleManager().getTaikoSample(sampleSet, "hitnormal", customSet).play(volume, 0.2f); break;
                    case 3:
                        hsContext.getSampleManager().getTaikoSample(sampleSet, "hitclap", customSet).play(volume, 0.2f); break;
                }
                nextHit = nextHit < 3 ? nextHit + 1 : 0;
                hsContext.getCurrentHitsoundTime().addAndGet(interval);
            }
            return false;
        } else return true;
    }
}
