package pw.lictex.osuplayer.storage;

import android.content.*;
import android.util.*;

import androidx.preference.*;

import java.io.*;
import java.util.*;

import lombok.*;
import pw.lictex.libosu.beatmap.*;

/**
 * Created by kpx on 2019/3/30.
 */
public class BeatmapIndex {
    private static String pathDef = "/storage/emulated/0/osu!droid/Songs/";
    @Getter private static BeatmapIndex instance = new BeatmapIndex(pathDef);

    private HashMap<String, Metadata> cache = new LinkedHashMap<>();

    private BeatmapIndex(String path) {
        if (path == null) path = pathDef;
        refresh(path);
        for (Map.Entry<String, Metadata> entry : cache.entrySet()) {
            Log.i("WTJB", entry.getValue().getTitle() + " - " + entry.getValue().getArtist() + " [" + entry.getValue().getVersion() + "]");
        }
    }

    public static void Build(Context c) {
        var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(c);
        instance = new BeatmapIndex(sharedPreferences.getString("storage_path", null));
    }

    private void refresh(String p) {
        cache.clear();
        LinkedList<String> pp = new LinkedList<>();
        searchOsuFiles(pp, new File(p));
        for (String s : pp) {
            try {
                var beatmap = OsuBeatmap.fromFile(s);
                var m = new Metadata();
                m.setArtist(beatmap.getMetadataSection().getArtistUnicode());
                m.setRomanisedArtist(beatmap.getMetadataSection().getArtist());
                m.setTitle(beatmap.getMetadataSection().getTitleUnicode());
                m.setRomanisedTitle(beatmap.getMetadataSection().getTitle());
                m.setVersion(beatmap.getMetadataSection().getVersion());
                m.setMapper(beatmap.getMetadataSection().getCreator());
                cache.put(s, m);
            } catch (Throwable t) {
                Log.w("B", "", t);
            }
        }
    }

    private void searchOsuFiles(List<String> out, File dir) {
        if (!dir.isDirectory()) return;
        var p = dir.listFiles();
        for (var file : p) {
            if (file.isDirectory()) {
                searchOsuFiles(out, file);
                continue;
            }
            if (file.getName().endsWith(".osu")) {
                try {
                    out.add(file.getCanonicalPath());
                } catch (IOException e) {
                    Log.w("B", "", e);
                }
            }
        }
    }

    public List<String> getAllBeatmaps() {
        return new ArrayList<>(cache.keySet());
    }

    public Metadata getMetadata(String path) {
        var n = cache.get(path);
        return n == null ? new Metadata() : n;
    }

    @Getter @Setter
    public class Metadata {
        private String title, romanisedTitle, artist, romanisedArtist, version, mapper;
    }
}
