package pw.lictex.osuplayer.audio.hitsound.osu;

import pw.lictex.libosu.beatmap.*;
import pw.lictex.osuplayer.audio.hitsound.*;

public class OsuSliderLooper extends HitsoundLooper {

    private final boolean sliderWhistling;
    private final SampleSet sampleSet;
    private final SampleSet additionSet;
    private final int customSet;

    public OsuSliderLooper(boolean sliderWhistling, SampleSet sampleSet, SampleSet additionSet, int customSet) {
        this.sliderWhistling = sliderWhistling;
        this.sampleSet = sampleSet;
        this.additionSet = additionSet;
        this.customSet = customSet;
    }

    @Override public void play() {
        enableLoopingSample("sliderslide", hsContext.getSampleManager().getSample(sampleSet, "sliderslide", customSet))
                .loop(hsContext.volumeAt(hsContext.getPlayerTime()));

        if (sliderWhistling) {
            enableLoopingSample("sliderwhistle", hsContext.getSampleManager().getSample(additionSet, "sliderwhistle", customSet))
                    .loop(hsContext.volumeAt(hsContext.getPlayerTime()));
        }
    }

    @Override public void stop() {
        disableLoopingSample("sliderslide");
        disableLoopingSample("sliderwhistle");
    }
}
