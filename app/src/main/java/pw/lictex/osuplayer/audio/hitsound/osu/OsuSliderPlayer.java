package pw.lictex.osuplayer.audio.hitsound.osu;

import lombok.*;
import pw.lictex.libosu.beatmap.*;
import pw.lictex.osuplayer.audio.hitsound.*;

public class OsuSliderPlayer extends HitsoundPlayer<HitObject.Slider> {
    public OsuSliderPlayer(HitsoundContext context, HitObject.Slider object) {
        super(context, object);
    }

    @Override public boolean play() {
        double sliderDuration = hsContext.getCurrentBeatmap().getSliderDuration(hitObject);
        boolean sliding = !(hitObject.getTime() + sliderDuration * hitObject.getRepeat() < hsContext.getPlayerTime());

        //set looping params
        if (hsContext.isSliderslideEnabled() && sliding) {
            SampleSet bodySampleSet = hitObject.getSampleSet() != SampleSet.None ? hitObject.getSampleSet() : hsContext.sampleSetAt(hsContext.getPlayerTime());
            SampleSet bodyAdditionSet = hitObject.getAdditions() != SampleSet.None ? hitObject.getAdditions() : bodySampleSet;
            int bodyCustomSet = hitObject.getCustomSampleSetIndex() != 0 ? hitObject.getCustomSampleSetIndex() : hsContext.customSetAt(hsContext.getPlayerTime());
            boolean bodyWhistling = (hitObject.getHitSounds() & 1 << 1) > 0;
            hsContext.attachLooper(new OsuSliderLooper(bodyWhistling, bodySampleSet, bodyAdditionSet, bodyCustomSet));
        }

        //slider hit
        for (int i = 0; i < hitObject.getRepeat() + 1; i++) {
            if (hitObject.getTime() + sliderDuration * i <= hsContext.getPlayerTime() && hitObject.getTime() + sliderDuration * i > hsContext.getPreviousHitsoundTime()) {
                SampleSet sliderHitSampleSet = hsContext.sampleSetAt((int) (hitObject.getTime() + sliderDuration * i));
                int sliderHitCustomSet = hsContext.customSetAt((int) (hitObject.getTime() + sliderDuration * i));
                float sliderHitVolume = hsContext.volumeAt((int) (hitObject.getTime() + sliderDuration * i));

                SampleSet sampleSet = (hitObject.getEdgeSampleSet().get(i) != SampleSet.None) ? hitObject.getEdgeSampleSet().get(i) : sliderHitSampleSet;
                SampleSet additionSet = (hitObject.getEdgeAdditionSet().get(i) != SampleSet.None) ? hitObject.getEdgeAdditionSet().get(i) : sampleSet;

                float pan = ((i % 2 == 0 ? hitObject.getX() : hitObject.getCurvePoints().get(hitObject.getCurvePoints().size() - 1)[0]) / 512f - 0.5f) * 0.8f;
                int hitSounds = hitObject.getEdgeHitsounds().get(i);

                //seems sliders are not affected by hitsounds override..?
                hsContext.getSampleManager().getSample(sampleSet, "hitnormal", sliderHitCustomSet).play(sliderHitVolume, pan);
                if ((hitSounds & 1 << 1) > 0)
                    hsContext.getSampleManager().getSample(additionSet, "hitwhistle", sliderHitCustomSet).play(sliderHitVolume, pan);
                if ((hitSounds & 1 << 2) > 0)
                    hsContext.getSampleManager().getSample(additionSet, "hitfinish", sliderHitCustomSet).play(sliderHitVolume, pan);
                if ((hitSounds & 1 << 3) > 0)
                    hsContext.getSampleManager().getSample(additionSet, "hitclap", sliderHitCustomSet).play(sliderHitVolume, pan);

                hsContext.updateCurrentHitsoundTime((int) Math.ceil(hitObject.getTime() + sliderDuration * i));
            }
        }

        //slider tick
        if (hsContext.isSlidertickEnabled()) {
            var tickLength = hsContext.getCurrentBeatmap().notInheritedTimingPointAt(hitObject.getTime()).getBeatLength() / hsContext.getCurrentBeatmap().getDifficultySection().getSliderTickRate();

            int repeat = 0;
            for (var i = hitObject.getTime() + tickLength; i <= hsContext.getPlayerTime(); i += tickLength) {
                var tl = i - hitObject.getTime();
                var r = ((int) Math.ceil(tl)) / ((int) sliderDuration);
                if (r != repeat) {
                    repeat = r;
                    i = hitObject.getTime() + repeat * sliderDuration;
                }
                if (i <= hsContext.getPreviousHitsoundTime() || i > hitObject.getTime() + sliderDuration * hitObject.getRepeat()) continue;

                if (Math.abs(tl - sliderDuration * repeat) > 16) {
                    SampleSet sampleSet = hsContext.sampleSetAt((int) i);
                    int customSampleSet = hsContext.customSetAt((int) i);
                    float volume = hsContext.volumeAt((int) i);

                    float[] position = hsContext.getCurrentBeatmap().getSliderPositionAt(hitObject, (int) i);
                    float pan = ((position[0]) / 512f - 0.5f) * 0.8f;
                    hsContext.getSampleManager().getSample(sampleSet, "slidertick", customSampleSet).play(volume, pan);
                    hsContext.updateCurrentHitsoundTime((int) Math.ceil(i));
                }
            }
        }

        return !sliding;
    }
}
