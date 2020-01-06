package pw.lictex.osuplayer.audio.hitsound;

import java.util.*;
import java.util.concurrent.atomic.*;

import lombok.*;
import pw.lictex.libosu.beatmap.*;
import pw.lictex.osuplayer.audio.*;
import pw.lictex.osuplayer.audio.hitsound.mania.*;
import pw.lictex.osuplayer.audio.hitsound.osu.*;

public class HitsoundContext {
    public static class Builder {
        private HitsoundContext _context = new HitsoundContext();

        public Builder setCurrentTime(int s) {_context.playerTime = s; return this;}

        public Builder setCurrentBeatmap(OsuBeatmap s) {_context.currentBeatmap = s; return this;}

        public Builder setSampleManager(SampleManager s) {_context.sampleManager = s; return this;}

        public Builder setPreviousHitsoundTime(int s) {_context.previousHitsoundTime = s; return this;}

        public Builder setSoundVolume(int s) {_context.soundVolume = s; return this;}

        public Builder setSpinnerspinEnabled(boolean s) {_context.spinnerspinEnabled = s; return this;}

        public Builder setPlaying(boolean s) {_context.playing = s; return this;}

        public Builder setLoopingSamples(Map<String, AudioEngine.Sample> s) {_context.loopingSamples = s; return this;}

        public Builder setActiveLooperList(Set<HitsoundLooper> s) {_context.activeLooperList = s; return this;}

        public Builder setSliderslideEnabled(boolean s) {_context.sliderslideEnabled = s; return this;}

        public Builder setSlidertickEnabled(boolean s) {_context.slidertickEnabled = s; return this;}

        public Builder setSpinnerBonusEnabled(boolean s) {_context.spinnerBonusEnabled = s; return this;}

        public HitsoundContext build() {_context.currentHitsoundTime = new AtomicInteger(_context.previousHitsoundTime); return _context;}
    }

    private HitsoundContext() {}

    @Getter private int playerTime;
    @Getter private OsuBeatmap currentBeatmap;
    @Getter private SampleManager sampleManager;
    @Getter private int previousHitsoundTime;
    @Getter private int soundVolume;
    @Getter private boolean slidertickEnabled;
    @Getter private boolean sliderslideEnabled;
    @Getter private boolean spinnerBonusEnabled;
    @Getter private boolean spinnerspinEnabled;
    @Getter private boolean playing;
    @Getter private Map<String, AudioEngine.Sample> loopingSamples;

    @Getter private AtomicInteger currentHitsoundTime;

    public void updateCurrentHitsoundTime(int currentHitsoundTime) {
        this.currentHitsoundTime.set(Math.max(this.currentHitsoundTime.get(), currentHitsoundTime));
    }

    private Set<HitsoundLooper> activeLooperList;
    private Set<Class> _keepActive = new HashSet<>();

    public void attachLooper(HitsoundLooper looper) {
        looper.setHsContext(this);
        activeLooperList.remove(looper);
        activeLooperList.add(looper);
        _keepActive.add(looper.getClass());
    }

    public void runLooper() {
        Iterator<HitsoundLooper> iterator = activeLooperList.iterator();
        while (iterator.hasNext()) {
            var value = iterator.next();
            if (_keepActive.contains(value.getClass()) && playing) {
                value.play();
            } else {
                value.stop();
                iterator.remove();
            }
        }
    }

    public HitsoundPlayer createPlayer(HitObject hitObject) {
        return createPlayer(hitObject, PlayMode.Osu);
    }

    public HitsoundPlayer createPlayer(HitObject hitObject, PlayMode mode) {
        HitsoundPlayer hitsoundPlayer = null;
        switch (mode) {
            case Taiko: //TODO
            case CatchTheBeat: //TODO
            case Osu:
            case OsuMania:
                if (hitObject instanceof HitObject.Circle) hitsoundPlayer = new OsuCirclePlayer(this, (HitObject.Circle) hitObject);
                if (hitObject instanceof HitObject.Slider) hitsoundPlayer = new OsuSliderPlayer(this, (HitObject.Slider) hitObject);
                if (hitObject instanceof HitObject.Spinner) hitsoundPlayer = new OsuSpinnerPlayer(this, (HitObject.Spinner) hitObject);
                if (hitObject instanceof HitObject.Hold) hitsoundPlayer = new ManiaHoldPlayer(this, (HitObject.Hold) hitObject);
                break;
        }
        return hitsoundPlayer;
    }

    public SampleSet sampleSetWithOverrideAt(int time, HitObject override) {
        return (override.getSampleSet() != SampleSet.None) ? override.getSampleSet() : sampleSetAt(time);
    }

    public SampleSet sampleSetAt(int time) {
        TimingPoint objectHSPoint = getCurrentBeatmap().timingPointAt(time + 4);
        return objectHSPoint != null ? objectHSPoint.getSampleSet() : SampleSet.Normal;
    }

    public int customSetWithOverrideAt(int time, HitObject override) {
        return (override.getCustomSampleSetIndex() != 0) ? override.getCustomSampleSetIndex() : customSetAt(time);
    }

    public int customSetAt(int time) {
        TimingPoint objectHSPoint = getCurrentBeatmap().timingPointAt(time + 4);
        return objectHSPoint != null ? objectHSPoint.getCustomSampleSet() : 0;
    }

    public float volumeWithOverrideAt(int time, HitObject override) {
        return (override.getVolume() != 0) ? override.getVolume() * getSoundVolume() / 100f / 100f : volumeAt(time);
    }

    public float volumeAt(int time) {
        TimingPoint objectHSPoint = getCurrentBeatmap().timingPointAt(time + 4);
        return (objectHSPoint != null ? objectHSPoint.getVolume() : 0) * getSoundVolume() / 100f / 100f;
    }
}
