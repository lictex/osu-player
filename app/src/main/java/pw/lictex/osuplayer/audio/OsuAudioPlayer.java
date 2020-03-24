package pw.lictex.osuplayer.audio;

import android.content.*;
import android.graphics.*;
import android.os.*;

import androidx.preference.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import lombok.*;
import pw.lictex.libosu.beatmap.*;
import pw.lictex.osuplayer.*;
import pw.lictex.osuplayer.audio.hitsound.*;

/**
 * Created by kpx on 2019/2/26.
 */
public class OsuAudioPlayer {
    @Getter private final AudioEngine engine;
    private final AtomicInteger previousHitsoundTime = new AtomicInteger();
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

    private Map<String, AudioEngine.Sample> loopingSamples = new ConcurrentHashMap<>();
    private Set<HitsoundLooper> activeLooperList = Collections.synchronizedSet(new HashSet<>());

    private Context context;
    private SampleManager sampleManager;
    private String currentBeatmapSetPath;
    private OsuBeatmap currentBeatmap = null;
    private List<HitObject> hitObjectsRemains = new ArrayList<>();
    private List<OsuBeatmap.Events.Sample> storyboardSampleRemains = new ArrayList<>();
    @Getter private Mod currentMod = Mod.None;

    public OsuAudioPlayer(Context context) {
        this.context = context;
        engine = new AudioEngine(Utils.getPreferenceStringAsInt(PreferenceManager.getDefaultSharedPreferences(context), "audio_buffer_size", 24));
        sampleManager = new SampleManager(context, engine);
        reloadSetting();
        engine.setTickCallback(this::tick);
    }

    public void reloadSetting() {
        var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        setSampleOffset(Utils.getPreferenceStringAsInt(sharedPreferences, "audio_latency", 0));

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
        engine.setOnTrackEndCallback(() -> new Handler(Looper.getMainLooper()).post(onBeatmapEndCallback));
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

            HitsoundContext hitsoundContext = new HitsoundContext.Builder()
                    .setSampleManager(sampleManager)
                    .setCurrentBeatmap(currentBeatmap)
                    .setPlaying(getEngine().getPlaybackStatus() == AudioEngine.PlaybackStatus.Playing)
                    .setCurrentTime((int) (engine.getMainTrackCurrentTime() + sampleOffset))
                    .setPreviousHitsoundTime(previousHitsoundTime.get())
                    .setSliderslideEnabled(sliderslideEnabled)
                    .setSlidertickEnabled(slidertickEnabled)
                    .setSpinnerspinEnabled(spinnerspinEnabled)
                    .setSpinnerBonusEnabled(spinnerbonusEnabled)
                    .setSoundVolume(soundVolume)
                    .setLoopingSamples(loopingSamples)
                    .setActiveLooperList(activeLooperList)
                    .build();

            //play storyboard samples
            Iterator<OsuBeatmap.Events.Sample> sampleIterator = storyboardSampleRemains.iterator();
            while (sampleIterator.hasNext()) {
                var sample = sampleIterator.next();
                if (sample.getTime() > hitsoundContext.getPlayerTime()) break;

                if (previousHitsoundTime.get() <= sample.getTime()) {
                    //sampleManager.getSample(sample.getFile().replaceAll("\\..+$", "")).play(sample.getVolume() * (storyboardUseSoundVolume ? soundVolume : musicVolume) / 100f / 100f, 0);

                    //seems replaceAll has performance issue so use substring instead..?
                    String file = sample.getFile();
                    sampleManager.getSample(file.substring(0, file.length() - 4)).play(sample.getVolume() * (storyboardUseSoundVolume ? soundVolume : musicVolume) / 100f / 100f, 0);
                    hitsoundContext.getCurrentHitsoundTime().set(Math.max(hitsoundContext.getCurrentHitsoundTime().get(), (int) Math.ceil(sample.getTime())));
                }
                sampleIterator.remove();
            }

            //play hitsounds
            Iterator<HitObject> iterator = hitObjectsRemains.iterator();
            while (iterator.hasNext()) {
                HitObject hitObject = iterator.next();
                if (hitObject.getTime() > hitsoundContext.getPlayerTime()) break;
                HitsoundPlayer player = hitsoundContext.createPlayer(hitObject, hitsoundContext.getCurrentBeatmap().getGeneralSection().getMode());
                if (player != null) if (player.play()) iterator.remove();
            }

            hitsoundContext.runLooper();
            previousHitsoundTime.set(hitsoundContext.getCurrentHitsoundTime().get());

            //nightcore
            if (currentMod == Mod.NC) {
                float nightcoreVolume = (nightcoreUseSoundVolume ? soundVolume : musicVolume) / 100f;
                var np = currentBeatmap.notInheritedTimingPointAt(hitsoundContext.getPlayerTime());
                int beat = (int) ((hitsoundContext.getPlayerTime() - np.getOffset()) * 2 / np.getBeatLength());
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
        previousHitsoundTime.set((int) ms);
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
}