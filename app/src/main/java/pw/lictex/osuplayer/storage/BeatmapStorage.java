package pw.lictex.osuplayer.storage;

        import android.content.Context;
        import android.database.sqlite.SQLiteDatabase;

/**
 * Created by kpx on 2019/3/30.
 */
public class BeatmapStorage {
    public BeatmapStorage(Context context) {
        SQLiteDatabase.openOrCreateDatabase(context.getFilesDir().getPath() + "osuplayer.db", null);
    }
}
