package pw.lictex.osuplayer.audio;

import android.content.*;

import java.io.*;
import java.util.*;

import lombok.*;
import pw.lictex.libosu.beatmap.*;
import pw.lictex.osuplayer.*;

public class SampleManager {
    private AudioEngine engine;

    private Map<String, AudioEngine.Sample> defaultSet = new HashMap<>();
    private Map<String, AudioEngine.Sample> list = new HashMap<>();

    public SampleManager(Context context, AudioEngine engine) {
        this.engine = engine;

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

        addSample(defaultSet, "taiko-normal-hitnormal", context.getResources().openRawResource(R.raw.taiko_normal_hitnormal));
        addSample(defaultSet, "taiko-normal-hitclap", context.getResources().openRawResource(R.raw.taiko_normal_hitclap));
        addSample(defaultSet, "taiko-normal-hitfinish", context.getResources().openRawResource(R.raw.taiko_normal_hitfinish));
        addSample(defaultSet, "taiko-normal-hitwhistle", context.getResources().openRawResource(R.raw.taiko_normal_hitwhistle));
        addSample(defaultSet, "taiko-soft-hitnormal", context.getResources().openRawResource(R.raw.taiko_soft_hitnormal));
        addSample(defaultSet, "taiko-soft-hitclap", context.getResources().openRawResource(R.raw.taiko_soft_hitclap));
        addSample(defaultSet, "taiko-soft-hitfinish", context.getResources().openRawResource(R.raw.taiko_soft_hitfinish));
        addSample(defaultSet, "taiko-soft-hitwhistle", context.getResources().openRawResource(R.raw.taiko_soft_hitwhistle));
    }

    public void addSample(Map<String, AudioEngine.Sample> m, String name, InputStream stream) {
        m.put(name, engine.createSample(name, stream));
    }

    public void reset() {
        for (Map.Entry<String, AudioEngine.Sample> s : list.entrySet()) {
            s.getValue().close();
        }
        list.clear();
    }

    public void setDirectory(String p) {
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

    public AudioEngine.Sample getSample(String name) {
        var sample = list.get(name);
        return (sample == null) ? getDefaultSample(name) : sample;
    }

    public AudioEngine.Sample getDefaultSample(String name) {
        var sample = defaultSet.get(name.replaceAll("[0-9]+$", ""));
        return (sample == null) ? defaultSet.get("soft-hitnormal") : sample;
    }

    public AudioEngine.Sample getSample(SampleSet sampleSet, String name, int custom) {
        if (custom == 0)
            return getDefaultSample(sampleSet.toString().toLowerCase() + "-" + name.toLowerCase());
        return getSample(sampleSet.toString().toLowerCase() + "-" + name.toLowerCase() + (custom > 1 ? custom : ""));
    }

    public AudioEngine.Sample getTaikoSample(SampleSet sampleSet, String name, int custom) {
        if (custom == 0) {
            if (sampleSet == SampleSet.Drum) {
                if (name.equalsIgnoreCase("hitnormal"))
                    return getDefaultSample(sampleSet.toString().toLowerCase() + "-hitfinish");
                if (name.equalsIgnoreCase("hitfinish"))
                    return getDefaultSample(sampleSet.toString().toLowerCase() + "-hitnormal");
                return getDefaultSample(sampleSet.toString().toLowerCase() + "-" + name.toLowerCase());
            } else return getDefaultSample("taiko-" + sampleSet.toString().toLowerCase() + "-" + name.toLowerCase());
        } else {
            String s = sampleSet.toString().toLowerCase() + "-" + name.toLowerCase() + (custom > 1 ? custom : "");
            var sample = list.get("taiko-" + s);
            if (sample != null) return sample;
            if (sampleSet == SampleSet.Drum) {
                if (name.equalsIgnoreCase("hitnormal"))
                    return getDefaultSample(sampleSet.toString().toLowerCase() + "-hitfinish");
                if (name.equalsIgnoreCase("hitfinish"))
                    return getDefaultSample(sampleSet.toString().toLowerCase() + "-hitnormal");
                return getDefaultSample(s);
            } else return getDefaultSample("taiko-" + s);
        }
    }
}