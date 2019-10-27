package pw.lictex.osuplayer.storage;

import androidx.room.*;

@Database(entities = {BeatmapEntity.class, CollectionBeatmapEntity.class}, version = 1)
public abstract class BeatmapDatabase extends RoomDatabase {
    public abstract BeatmapDAO getDAO();
}
