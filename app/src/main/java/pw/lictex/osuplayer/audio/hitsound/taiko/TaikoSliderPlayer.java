package pw.lictex.osuplayer.audio.hitsound.taiko;

import pw.lictex.libosu.beatmap.*;
import pw.lictex.osuplayer.audio.hitsound.*;

public class TaikoSliderPlayer extends HitsoundPlayer<HitObject.Slider> {
    private static boolean rightHit = false;

    public TaikoSliderPlayer(HitsoundContext context, HitObject.Slider object) {
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

        int sliderDuration = ((int) (hsContext.getCurrentBeatmap().getSliderDuration(hitObject) * hitObject.getRepeat()));
        hsContext.updateCurrentHitsoundTime((int) Math.ceil(hitObject.getTime()));
        if (hsContext.getPreviousHitsoundTime() <= hitObject.getTime() + sliderDuration) {
            while (hsContext.getPlayerTime() - hsContext.getCurrentHitsoundTime().get() > interval) {
                float pan = rightHit ? 0.2f : -0.2f;
                hsContext.getSampleManager().getTaikoSample(sampleSet, "hitnormal", customSet).play(volume, pan);
                rightHit = !rightHit;
                hsContext.getCurrentHitsoundTime().addAndGet(interval);
            }
            return false;
        } else return true;
    }
}
