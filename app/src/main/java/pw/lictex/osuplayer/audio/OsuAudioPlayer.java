package pw.lictex.osuplayer.audio;

import android.content.*;
import android.graphics.*;

import androidx.preference.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import lombok.*;
import pw.lictex.libosu.beatmap.*;
import pw.lictex.osuplayer.R;

import static com.un4seen.bass.BASS.*;

/**
 * Created by kpx on 2019/2/26.
 */
public class OsuAudioPlayer {
    @Getter private final AudioEngine engine;
    private final AtomicInteger lastHitsoundTime = new AtomicInteger();
    private final AtomicInteger lastNightcoreBeat = new AtomicInteger();

    @Getter @Setter private boolean nightcoreUseSoundVolume = false;
    @Getter @Setter private boolean storyboardUseSoundVolume = false;
    @Getter @Setter private boolean sliderslideEnabled = true;
    @Getter @Setter private boolean slidertickEnabled = true;
    @Getter @Setter private boolean spinnerspinEnabled = true;
    @Getter @Setter private boolean spinnerbonusEnabled = true;
    @Getter @Setter private int sampleOffset = 25;
    @Getter @Setter private int musicVolume = 80;
    @Getter @Setter private int soundVolume = 80;

    private volatile AudioEngine.Sample currentSpinnerSpinSound = null;
    private volatile AudioEngine.Sample currentSliderSlideSound = null;
    private volatile AudioEngine.Sample currentSliderWhistleSound = null;
    private Context context;
    private SampleManager sampleManager;
    private String currentBeatmapSetPath;
    private OsuBeatmap currentBeatmap = null;
    private List<HitObject> hitObjectsRemains = new ArrayList<>();
    private List<OsuBeatmap.Events.Sample> storyboardSampleRemains = new ArrayList<>();
    @Getter private Mod currentMod = Mod.None;

    public OsuAudioPlayer(Context context) {
        this.context = context;
        engine = new AudioEngine();
        sampleManager = new SampleManager();
        reloadSetting();
        engine.setTickCallback(this::tick);
    }

    public void reloadSetting() {
        var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        int i = 0;
        try {
            i = Integer.parseInt(Objects.requireNonNull(sharedPreferences.getString("audio_latency", null)));
        } catch (Throwable e) {
            sharedPreferences.edit().putString("audio_latency", "0").apply();
        }
        setSampleOffset(i);

        setNightcoreUseSoundVolume(sharedPreferences.getBoolean("nightcore_sound_volume", false));
        setStoryboardUseSoundVolume(sharedPreferences.getBoolean("storyboard_sound_volume", false));

        setMusicVolume(sharedPreferences.getInt("music_volume", 80));
        setSoundVolume(sharedPreferences.getInt("sound_volume", 80));

        setSliderslideEnabled(sharedPreferences.getBoolean("sliderslide_enabled", false));
        setSlidertickEnabled(sharedPreferences.getBoolean("slidertick_enabled", false));
        setSpinnerspinEnabled(sharedPreferences.getBoolean("spinnerspin_enabled", false));
        setSpinnerbonusEnabled(sharedPreferences.getBoolean("spinnerbonus_enabled", false));

    }

    public void setOnBeatmapEndCallback(Runnable onBeatmapEndCallback) {
        engine.setOnTrackEndCallback(onBeatmapEndCallback);
    }

    public String getRomanisedTitle() {
        try {
            return currentBeatmap.getMetadataSection().getTitle();
        } catch (Throwable ignored) {}
        return null;
    }

    public String getTitle() {
        try {
            return currentBeatmap.getMetadataSection().getTitleUnicode();
        } catch (Throwable ignored) {}
        return null;
    }

    public String getArtist() {
        try {
            return currentBeatmap.getMetadataSection().getArtistUnicode();
        } catch (Throwable ignored) {}
        return null;
    }

    public Bitmap getBackground() {
        try {
            String backgroundImage = currentBeatmap.getEventsSection().getBackgroundImage();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeFile(currentBeatmapSetPath + backgroundImage, options);
        } catch (Throwable ignored) {}
        return null;
    }

    public String getRomanisedArtist() {
        try {
            return currentBeatmap.getMetadataSection().getArtist();
        } catch (Throwable ignored) {}
        return null;
    }

    public String getMapper() {
        try {
            return currentBeatmap.getMetadataSection().getCreator();
        } catch (Throwable ignored) {}
        return null;
    }

    public String getVersion() {
        try {
            return currentBeatmap.getMetadataSection().getVersion();
        } catch (Throwable ignored) {}
        return null;
    }

    public String getSource() {
        try {
            var source = currentBeatmap.getMetadataSection().getSource();
            return source.trim().isEmpty() ? null : source;
        } catch (Throwable ignored) {}
        return null;
    }

    public void destroy() {
        engine.destroy();
    }

    private void tick() {
        if (currentBeatmap == null) return;
        synchronized (this) {
            if (currentBeatmap == null) return; //double check is needed

            long currentTime = engine.getMainTrackCurrentTime() + sampleOffset;
            TimingPoint timingPoint = currentBeatmap.timingPointAt((int) currentTime);

            SampleSet timingSampleSet = timingPoint != null ? timingPoint.getSampleSet() : SampleSet.Normal;
            int timingCustomSampleSet = timingPoint != null ? timingPoint.getCustomSampleSet() : 0;
            float timingVolume = (timingPoint != null ? timingPoint.getVolume() : 0) * soundVolume / 100f / 100f;

            boolean sliderSliding = false;
            boolean sliderWhistling = false;
            boolean spinnerSpinning = false;
            float spinnerCompletion = 0;
            SampleSet sliderSampleSet = SampleSet.Normal;
            SampleSet sliderAdditionSet = SampleSet.Normal;
            int sliderCustomSampleSet = 0;

            int lastHs = lastHitsoundTime.get();

            Iterator<OsuBeatmap.Events.Sample> sampleIterator = storyboardSampleRemains.iterator();
            while (sampleIterator.hasNext()) {
                var sample = sampleIterator.next();
                if (sample.getTime() > currentTime) break;

                if (lastHitsoundTime.get() <= sample.getTime()) {
                    //sampleManager.getSample(sample.getFile().replaceAll("\\..+$", "")).play(sample.getVolume() * (storyboardUseSoundVolume ? soundVolume : musicVolume) / 100f / 100f, 0);

                    //seems replaceAll has performance issue so use substring instead..?
                    String file = sample.getFile();
                    sampleManager.getSample(file.substring(0, file.length() - 4)).play(sample.getVolume() * (storyboardUseSoundVolume ? soundVolume : musicVolume) / 100f / 100f, 0);
                    lastHs = Math.max(lastHs, (int) Math.ceil(sample.getTime()));
                }
                sampleIterator.remove();
            }

            Iterator<HitObject> iterator = hitObjectsRemains.iterator();
            while (iterator.hasNext()) {
                HitObject hitObject = iterator.next();
                if (hitObject.getTime() > currentTime) break;

                int objectCustomSampleSet = (hitObject.getCustomSampleSetIndex() != 0) ? hitObject.getCustomSampleSetIndex() : timingCustomSampleSet;
                float objectVolume = (hitObject.getVolume() != 0) ? hitObject.getVolume() * soundVolume / 100f / 100f : timingVolume;

                if (hitObject instanceof HitObject.Slider) {
                    HitObject.Slider slider = (HitObject.Slider) hitObject;

                    sliderWhistling = (slider.getHitSounds() & 1 << 1) > 0;
                    sliderSampleSet = (hitObject.getSampleSet() != SampleSet.None) ? hitObject.getSampleSet() : timingSampleSet;
                    sliderAdditionSet = (hitObject.getAdditions() != SampleSet.None) ? hitObject.getAdditions() : sliderSampleSet;
                    sliderCustomSampleSet = (hitObject.getCustomSampleSetIndex() != 0) ? hitObject.getCustomSampleSetIndex() : timingCustomSampleSet;

                    //region playSliderSample();
                    double sliderDuration = currentBeatmap.getSliderDuration(slider);
                    for (int i = 0; i < slider.getRepeat() + 1; i++) {
                        if (slider.getTime() + sliderDuration * i <= currentTime && slider.getTime() + sliderDuration * i > lastHitsoundTime.get()) {
                            SampleSet sampleSet = (slider.getEdgeSampleSet().get(i) != SampleSet.None) ? slider.getEdgeSampleSet().get(i) : timingSampleSet;
                            SampleSet additionSet = (slider.getEdgeAdditionSet().get(i) != SampleSet.None) ? slider.getEdgeAdditionSet().get(i) : sampleSet;

                            float pan = ((i % 2 == 0 ? slider.getX() : slider.getCurvePoints().get(slider.getCurvePoints().size() - 1)[0]) / 512f - 0.5f) * 0.8f;
                            int hitSounds = slider.getEdgeHitsounds().get(i);

                            //seems sliders are not affected by hitsounds override..?
                            sampleManager.getSample(sampleSet, "hitnormal", timingCustomSampleSet).play(timingVolume, pan);
                            if ((hitSounds & 1 << 1) > 0)
                                sampleManager.getSample(additionSet, "hitwhistle", timingCustomSampleSet).play(timingVolume, pan);
                            if ((hitSounds & 1 << 2) > 0)
                                sampleManager.getSample(additionSet, "hitfinish", timingCustomSampleSet).play(timingVolume, pan);
                            if ((hitSounds & 1 << 3) > 0)
                                sampleManager.getSample(additionSet, "hitclap", timingCustomSampleSet).play(timingVolume, pan);

                            lastHs = Math.max(lastHs, (int) Math.ceil(slider.getTime() + sliderDuration * i));
                        }
                    }
                    if (slidertickEnabled) {
                        var tickLength = currentBeatmap.notInheritedTimingPointAt(slider.getTime()).getBeatLength() / currentBeatmap.getDifficultySection().getSliderTickRate();
                        SampleSet sampleSet = (slider.getSampleSet() != SampleSet.None) ? slider.getSampleSet() : timingSampleSet;
                        int repeat = 0;
                        for (var i = slider.getTime() + tickLength; i <= currentTime; i += tickLength) {
                            var tl = i - slider.getTime();
                            var r = ((int) Math.ceil(tl)) / ((int) sliderDuration);
                            if (r != repeat) {
                                repeat = r;
                                i = slider.getTime() + repeat * sliderDuration;
                            }
                            if (i <= lastHitsoundTime.get() || i > slider.getTime() + sliderDuration * slider.getRepeat()) continue;

                            if (Math.abs(tl - sliderDuration * repeat) > 16) {
                                float[] position = currentBeatmap.getSliderPositionAt(slider, (int) i);
                                float pan = ((position[0]) / 512f - 0.5f) * 0.8f;
                                sampleManager.getSample(sampleSet, "slidertick", objectCustomSampleSet).play(objectVolume, pan);
                                lastHs = Math.max(lastHs, (int) Math.ceil(i));
                            }
                        }
                    }
                    //endregion
                    if (slider.getTime() + sliderDuration * slider.getRepeat() < currentTime) iterator.remove();
                    else sliderSliding = true;
                }

                if (hitObject instanceof HitObject.Circle) {
                    //region playCircleSample();
                    if (lastHitsoundTime.get() <= hitObject.getTime()) {
                        SampleSet sampleSet = (hitObject.getSampleSet() != SampleSet.None) ? hitObject.getSampleSet() : timingSampleSet;
                        SampleSet additionSet = (hitObject.getAdditions() != SampleSet.None) ? hitObject.getAdditions() : sampleSet;

                        float pan = (hitObject.getX() / 512f - 0.5f) * 0.8f;
                        int hitSounds = hitObject.getHitSounds();

                        if (!hitObject.getSampleFile().isEmpty())
                            sampleManager.getSample(hitObject.getSampleFile().substring(0, hitObject.getSampleFile().lastIndexOf("."))).play(objectVolume, pan);
                        else
                            sampleManager.getSample(sampleSet, "hitnormal", objectCustomSampleSet).play(objectVolume, pan);

                        if ((hitSounds & 1 << 1) > 0)
                            sampleManager.getSample(additionSet, "hitwhistle", objectCustomSampleSet).play(objectVolume, pan);
                        if ((hitSounds & 1 << 2) > 0)
                            sampleManager.getSample(additionSet, "hitfinish", objectCustomSampleSet).play(objectVolume, pan);
                        if ((hitSounds & 1 << 3) > 0)
                            sampleManager.getSample(additionSet, "hitclap", objectCustomSampleSet).play(objectVolume, pan);
                        lastHs = Math.max(lastHs, (int) Math.ceil(hitObject.getTime()));
                    }

                    //endregion
                    iterator.remove();
                }

                if (hitObject instanceof HitObject.Spinner) {
                    HitObject.Spinner spinner = (HitObject.Spinner) hitObject;
                    if (spinner.getEndTime() < currentTime) {
                        SampleSet sampleSet = (spinner.getSampleSet() != SampleSet.None) ? spinner.getSampleSet() : timingSampleSet;
                        SampleSet additionSet = (spinner.getAdditions() != SampleSet.None) ? spinner.getAdditions() : sampleSet;

                        int hitSounds = spinner.getHitSounds();

                        if (!spinner.getSampleFile().isEmpty())
                            sampleManager.getSample(spinner.getSampleFile().substring(0, spinner.getSampleFile().lastIndexOf("."))).play(objectVolume, 0);
                        else
                            sampleManager.getSample(sampleSet, "hitnormal", objectCustomSampleSet).play(objectVolume, 0);

                        if ((hitSounds & 1 << 1) > 0)
                            sampleManager.getSample(additionSet, "hitwhistle", objectCustomSampleSet).play(objectVolume, 0);
                        if ((hitSounds & 1 << 2) > 0)
                            sampleManager.getSample(additionSet, "hitfinish", objectCustomSampleSet).play(objectVolume, 0);
                        if ((hitSounds & 1 << 3) > 0)
                            sampleManager.getSample(additionSet, "hitclap", objectCustomSampleSet).play(objectVolume, 0);
                        lastHs = Math.max(lastHs, (int) Math.ceil(spinner.getTime()));

                        iterator.remove();
                    } else {
                        spinnerSpinning = true;
                        spinnerCompletion = (float) (currentTime - spinner.getTime()) / (spinner.getEndTime() - spinner.getTime()) * 2;
                        if (spinnerCompletion >= 1) {
                            int x = (spinner.getEndTime() - spinner.getTime()) / 2;
                            lastHs = Math.max(lastHs, spinner.getTime() + x);
                            int t = 200;
                            while (currentTime - lastHs > t) {
                                if (spinnerbonusEnabled) {
                                    sampleManager.getSample("spinnerbonus").play(timingVolume, 0);
                                }
                                lastHs += t;
                            }
                        }
                    }
                }

                if (hitObject instanceof HitObject.Hold) {
                    if (lastHitsoundTime.get() <= hitObject.getTime()) {
                        SampleSet sampleSet = (hitObject.getSampleSet() != SampleSet.None) ? hitObject.getSampleSet() : timingSampleSet;
                        SampleSet additionSet = (hitObject.getAdditions() != SampleSet.None) ? hitObject.getAdditions() : sampleSet;

                        float pan = (hitObject.getX() / 512f - 0.5f) * 0.8f; //TODO maybe unnecessary..?
                        int hitSounds = hitObject.getHitSounds();

                        if (!hitObject.getSampleFile().isEmpty())
                            sampleManager.getSample(hitObject.getSampleFile().substring(0, hitObject.getSampleFile().lastIndexOf("."))).play(objectVolume, pan);
                        else
                            sampleManager.getSample(sampleSet, "hitnormal", objectCustomSampleSet).play(objectVolume, pan);

                        if ((hitSounds & 1 << 1) > 0)
                            sampleManager.getSample(additionSet, "hitwhistle", objectCustomSampleSet).play(objectVolume, pan);
                        if ((hitSounds & 1 << 2) > 0)
                            sampleManager.getSample(additionSet, "hitfinish", objectCustomSampleSet).play(objectVolume, pan);
                        if ((hitSounds & 1 << 3) > 0)
                            sampleManager.getSample(additionSet, "hitclap", objectCustomSampleSet).play(objectVolume, pan);
                        lastHs = Math.max(lastHs, (int) Math.ceil(hitObject.getTime()));
                    }
                    iterator.remove();
                }
            }

            lastHitsoundTime.set(lastHs);

            if (sliderslideEnabled && sliderSliding && getEngine().getPlaybackStatus() == AudioEngine.PlaybackStatus.Playing) {
                var slideSample = sampleManager.getSample(sliderSampleSet, "sliderslide", sliderCustomSampleSet);
                if (currentSliderSlideSound != slideSample) {
                    if (currentSliderSlideSound != null) currentSliderSlideSound.endLoop();
                    currentSliderSlideSound = slideSample;
                }
                slideSample.loop(timingVolume);

                if (sliderWhistling) {
                    var whistleSample = sampleManager.getSample(sliderAdditionSet, "sliderwhistle", sliderCustomSampleSet);
                    if (currentSliderWhistleSound != whistleSample) {
                        if (currentSliderWhistleSound != null) currentSliderWhistleSound.endLoop();
                        currentSliderWhistleSound = whistleSample;
                    }
                    whistleSample.loop(timingVolume);
                }
            } else {
                if (currentSliderSlideSound != null) {
                    currentSliderSlideSound.endLoop();
                    currentSliderSlideSound = null;
                }
                if (currentSliderWhistleSound != null) {
                    currentSliderWhistleSound.endLoop();
                    currentSliderWhistleSound = null;
                }
            }

            if (spinnerspinEnabled && spinnerSpinning && getEngine().getPlaybackStatus() == AudioEngine.PlaybackStatus.Playing) {
                var spinnerSample = sampleManager.getSample("spinnerspin");
                if (currentSpinnerSpinSound != spinnerSample) {
                    if (currentSpinnerSpinSound != null) currentSpinnerSpinSound.endLoop();
                    currentSpinnerSpinSound = spinnerSample;
                }
                spinnerSample.loop(timingVolume, Math.min(100000, 20000 + (int) (40000 * spinnerCompletion)));
            } else {
                if (currentSpinnerSpinSound != null) {
                    currentSpinnerSpinSound.endLoop();
                    currentSpinnerSpinSound = null;
                }
            }

            if (currentMod == Mod.NC) {
                float nightcoreVolume = (nightcoreUseSoundVolume ? soundVolume : musicVolume) / 100f;
                var np = currentBeatmap.notInheritedTimingPointAt((int) currentTime);
                int beat = (int) ((currentTime - np.getOffset()) * 2 / np.getBeatLength());
                int bar = beat % np.getTimeSignature();
                if (bar == lastNightcoreBeat.get()) return;
                lastNightcoreBeat.set(bar);

                if (beat % (8 * np.getTimeSignature()) == 0) {
                    sampleManager.getDefaultSample("nightcore-kick").play(nightcoreVolume, 0);
                    if (!np.isEffectEnabled(TimingPoint.Effect.OmitFirstBarLine) || bar > 0)
                        sampleManager.getDefaultSample("nightcore-finish").play(nightcoreVolume, 0);
                } else if (bar % 4 == 0) {
                    sampleManager.getDefaultSample("nightcore-kick").play(nightcoreVolume, 0);
                } else if (bar % 4 == 2) {
                    sampleManager.getDefaultSample("nightcore-clap").play(nightcoreVolume, 0);
                } else if (currentBeatmap.getDifficultySection().getSliderTickRate() % 2 == 0) {
                    sampleManager.getDefaultSample("nightcore-hat").play(nightcoreVolume, 0);
                }
            }
        }
    }

    public void play() {
        if (currentBeatmap != null) engine.resume();
    }

    public void pause() {
        engine.pause();
    }

    public void setMusicVolume(int vol) {
        this.musicVolume = vol;
        engine.setMainTrackVolume(this.musicVolume / 100f);
    }

    public void setMod(Mod m) {
        currentMod = m;
        switch (m) {
            case DT:
                engine.setTempo(1.5f);
                engine.setPitch(1f);
                break;
            case HT:
                engine.setTempo(0.75f);
                engine.setPitch(1f);
                break;
            case NC:
                engine.setTempo(1.5f);
                engine.setPitch(1.5f);
                break;
            case None:
                engine.setTempo(1f);
                engine.setPitch(1f);
                break;
        }
    }

    public void seekTo(long ms) {
        synchronized (this) {
            hitObjectsRemains.clear();
            storyboardSampleRemains.clear();
        }
        engine.setTime(ms);
        lastHitsoundTime.set((int) ms);
        synchronized (this) {
            hitObjectsRemains.clear();
            storyboardSampleRemains.clear();
            if (currentBeatmap != null) {
                hitObjectsRemains.addAll(currentBeatmap.getHitObjectsSection().getHitObjects());
                storyboardSampleRemains.addAll(currentBeatmap.getEventsSection().getSamples());
            }
        }
    }

    public void openBeatmapSet(String dir) {
        if (dir.equals(currentBeatmapSetPath)) return;
        synchronized (this) {
            currentBeatmap = null;
        }
        engine.stopMainTrack();
        sampleManager.setDirectory(dir);
        currentBeatmapSetPath = dir;
    }

    public void playBeatmap(String filename) {
        engine.stopMainTrack();
        hitObjectsRemains.clear();
        storyboardSampleRemains.clear();

        OsuBeatmap beatmap = OsuBeatmap.fromFile(currentBeatmapSetPath + filename);
        if (beatmap == null) return; //TODO invalid beatmap

        engine.playMainTrack(currentBeatmapSetPath + beatmap.getGeneralSection().getAudioFilename());
        engine.setMainTrackVolume(this.musicVolume / 100f);
        setMod(currentMod);
        synchronized (this) {
            this.currentBeatmap = beatmap;
        }
        seekTo(0);
    }

    public long getAudioLength() {
        return engine.getMainTrackTotalTime();
    }

    public long getCurrentTime() {
        return engine.getMainTrackCurrentTime();
    }

    public enum Mod {
        None, DT, NC, HT
    }

    private class SampleManager {
        private Map<String, AudioEngine.Sample> defaultSet = new HashMap<>();
        private Map<String, AudioEngine.Sample> list = new HashMap<>();

        SampleManager() {
            addSample(defaultSet, "soft-hitnormal", context.getResources().openRawResource(R.raw.soft_hitnormal));
            addSample(defaultSet, "soft-hitclap", context.getResources().openRawResource(R.raw.soft_hitclap));
            addSample(defaultSet, "soft-hitfinish", context.getResources().openRawResource(R.raw.soft_hitfinish));
            addSample(defaultSet, "soft-hitwhistle", context.getResources().openRawResource(R.raw.soft_hitwhistle));
            addSample(defaultSet, "soft-sliderslide", context.getResources().openRawResource(R.raw.soft_sliderslide));
            addSample(defaultSet, "soft-slidertick", context.getResources().openRawResource(R.raw.soft_slidertick));
            addSample(defaultSet, "soft-sliderwhistle", context.getResources().openRawResource(R.raw.soft_sliderwhistle));

            addSample(defaultSet, "normal-hitnormal", context.getResources().openRawResource(R.raw.normal_hitnormal));
            addSample(defaultSet, "normal-hitclap", context.getResources().openRawResource(R.raw.normal_hitclap));
            addSample(defaultSet, "normal-hitfinish", context.getResources().openRawResource(R.raw.normal_hitfinish));
            addSample(defaultSet, "normal-hitwhistle", context.getResources().openRawResource(R.raw.normal_hitwhistle));
            addSample(defaultSet, "normal-sliderslide", context.getResources().openRawResource(R.raw.normal_sliderslide));
            addSample(defaultSet, "normal-slidertick", context.getResources().openRawResource(R.raw.normal_slidertick));
            addSample(defaultSet, "normal-sliderwhistle", context.getResources().openRawResource(R.raw.normal_sliderwhistle));

            addSample(defaultSet, "drum-hitnormal", context.getResources().openRawResource(R.raw.drum_hitnormal));
            addSample(defaultSet, "drum-hitclap", context.getResources().openRawResource(R.raw.drum_hitclap));
            addSample(defaultSet, "drum-hitfinish", context.getResources().openRawResource(R.raw.drum_hitfinish));
            addSample(defaultSet, "drum-hitwhistle", context.getResources().openRawResource(R.raw.drum_hitwhistle));
            addSample(defaultSet, "drum-sliderslide", context.getResources().openRawResource(R.raw.drum_sliderslide));
            addSample(defaultSet, "drum-slidertick", context.getResources().openRawResource(R.raw.drum_slidertick));
            addSample(defaultSet, "drum-sliderwhistle", context.getResources().openRawResource(R.raw.drum_sliderwhistle));

            addSample(defaultSet, "spinnerbonus", context.getResources().openRawResource(R.raw.spinnerbonus));
            addSample(defaultSet, "spinnerspin", context.getResources().openRawResource(R.raw.spinnerspin));

            addSample(defaultSet, "nightcore-clap", context.getResources().openRawResource(R.raw.nightcore_clap));
            addSample(defaultSet, "nightcore-hat", context.getResources().openRawResource(R.raw.nightcore_hat));
            addSample(defaultSet, "nightcore-finish", context.getResources().openRawResource(R.raw.nightcore_finish));
            addSample(defaultSet, "nightcore-kick", context.getResources().openRawResource(R.raw.nightcore_kick));
        }

        void addSample(Map<String, AudioEngine.Sample> m, String name, InputStream stream) {
            m.put(name, engine.createSample(name, stream));
        }

        void reset() {
            for (Map.Entry<String, AudioEngine.Sample> s : list.entrySet()) {
                BASS_SampleFree(s.getValue().ptr);
            }
            list.clear();
        }

        void setDirectory(String p) {
            reset();
            File f = new File(p);
            if (f.isDirectory()) {
                var files = f.listFiles(file -> {
                    if (!file.isFile()) return false;
                    if (file.getName().toLowerCase().endsWith(".wav")) return true;
                    if (file.getName().toLowerCase().endsWith(".ogg")) return true;

                    if (file.getName().toLowerCase().endsWith(".mp3")) return true;
                    return false;
                });
                for (var file : files) {
                    try {
                        addSample(list, file.getName().substring(0, file.getName().lastIndexOf(".")), new FileInputStream(file));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        AudioEngine.Sample getSample(String name) {
            var sample = list.get(name);
            return (sample == null) ? getDefaultSample(name) : sample;
        }

        AudioEngine.Sample getDefaultSample(String name) {
            var sample = defaultSet.get(name.replaceAll("[0-9]+$", ""));
            return (sample == null) ? defaultSet.get("soft-hitnormal") : sample;
        }

        AudioEngine.Sample getSample(SampleSet sampleSet, String name, int custom) {
            if (custom == 0)
                return sampleManager.getDefaultSample(sampleSet.toString().toLowerCase() + "-" + name.toLowerCase());
            return sampleManager.getSample(sampleSet.toString().toLowerCase() + "-" + name.toLowerCase() + (custom > 1 ? custom : ""));
        }
    }
}