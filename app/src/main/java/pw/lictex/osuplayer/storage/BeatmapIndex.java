package pw.lictex.osuplayer.storage;

import android.content.*;
import android.util.*;

import androidx.preference.*;

import com.annimon.stream.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import lombok.*;
import pw.lictex.libosu.beatmap.*;

/**
 * Created by kpx on 2019/3/30.
 */
public class BeatmapIndex {
    private static String pathDef = "/storage/emulated/0/osu!droid/Songs/";
    @Getter private static BeatmapIndex instance;
    @Getter private static String currentPath = pathDef;
    private static SharedPreferences sharedPreferences;
    private HashMap<String, Metadata> cache = new LinkedHashMap<>();
    private Set<String> collection = new LinkedHashSet<>();

    private BeatmapIndex(String path) {
        if (path == null) path = pathDef;
        collection.clear();
        collection.addAll(sharedPreferences.getStringSet("collection", collection));
        refresh(path);
    }

    public static void Build(Context c) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(c);
        currentPath = sharedPreferences.getString("storage_path", pathDef);
        if (!currentPath.endsWith("/")) currentPath += "/";
        instance = new BeatmapIndex(currentPath);
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

    public List<String> getFavoriteBeatmaps() {
        return Collections.unmodifiableList(Stream.of(collection).map(s -> currentPath + s).toList());
    }

    public void addCollection(String s) {
        collection.add(m(s));
        sharedPreferences.edit().putStringSet("collection", collection).apply();
    }

    public boolean isInCollection(String s) {
        return collection.contains(m(s));
    }

    public void removeCollection(String s) {
        collection.remove(m(s));
        sharedPreferences.edit().putStringSet("collection", collection).apply();
    }

    private String m(String s) {
        return s.replaceFirst(Pattern.quote(currentPath), "").replaceAll("^/*", "");
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
