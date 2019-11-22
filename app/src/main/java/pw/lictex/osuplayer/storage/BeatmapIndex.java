package pw.lictex.osuplayer.storage;

import android.content.*;
import android.util.*;

import androidx.lifecycle.*;
import androidx.preference.*;
import androidx.room.*;
import androidx.room.migration.*;
import androidx.sqlite.db.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import lombok.*;
import pw.lictex.libosu.beatmap.*;
import pw.lictex.osuplayer.*;

/**
 * Created by kpx on 2019/3/30.
 */
public class BeatmapIndex {
    private static String pathDef = "/storage/emulated/0/osu!droid/Songs/";
    @Getter private static BeatmapIndex instance;
    @Getter private static String currentPath = pathDef;
    private static SharedPreferences sharedPreferences;
    private static BeatmapDatabase beatmap;

    private Context context;

    private BeatmapIndex(Context c) {context = c; }

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE BeatmapEntity ADD COLUMN source TEXT");
            database.execSQL("CREATE TABLE t(path TEXT NOT NULL, PRIMARY KEY(path))");
            database.execSQL("INSERT INTO t(path) SELECT (path) FROM CollectionBeatmapEntity");
            database.execSQL("DROP TABLE CollectionBeatmapEntity");
            database.execSQL("ALTER TABLE t RENAME TO CollectionBeatmapEntity");
        }
    };

    public static void Initialize(Context c) {
        beatmap = Room.databaseBuilder(c, BeatmapDatabase.class, "beatmap").addMigrations(MIGRATION_1_2).build();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(c);
        updateCurrentPath();
        instance = new BeatmapIndex(c);
    }

    public void refresh() {
        Utils.runTask(() -> {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            updateCurrentPath();

            BeatmapDAO dao = beatmap.getDAO();
            dao.clear();
            LinkedList<String> pp = new LinkedList<>();
            searchOsuFiles(pp, new File(currentPath));
            for (String s : pp) {
                try {
                    var m = new BeatmapEntity();
                    var beatmap = OsuBeatmap.fromFile(s);

                    m.path = m(s);
                    m.unicode_artist = beatmap.getMetadataSection().getArtistUnicode();
                    m.artist = beatmap.getMetadataSection().getArtist();
                    m.unicode_title = beatmap.getMetadataSection().getTitleUnicode();
                    m.title = beatmap.getMetadataSection().getTitle();
                    m.version = beatmap.getMetadataSection().getVersion();
                    m.creator = beatmap.getMetadataSection().getCreator();
                    m.tags = beatmap.getMetadataSection().getTags();
                    m.source = beatmap.getMetadataSection().getSource();

                    dao.insert(m);
                } catch (Throwable t) {
                    Log.w("B", "", t);
                }
            }
        });
    }

    private static void updateCurrentPath() {
        currentPath = sharedPreferences.getString("storage_path", pathDef);
        if (!currentPath.endsWith("/")) currentPath += "/";
    }

    private void searchOsuFiles(List<String> out, File dir) {
        if (!dir.isDirectory()) return;
        var p = dir.listFiles();
        if (p == null) return;
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

    public LiveData<List<BeatmapEntity>> getAllBeatmaps(String filter) {
        return beatmap.getDAO().orderByTitle(filter);
    }

    public LiveData<List<BeatmapEntity>> getFavoriteBeatmaps(String filter) {
        return beatmap.getDAO().orderCollectionByTitle(filter);
    }

    public void addCollection(BeatmapEntity s) {
        Utils.runTask(() -> beatmap.getDAO().addCollection(s));
    }

    public void removeCollection(BeatmapEntity s) {
        Utils.runTask(() -> beatmap.getDAO().removeCollection(s));
    }

    private String m(String s) {
        return s.replaceFirst(Pattern.quote(currentPath), "").replaceAll("^/*", "");
    }

}
