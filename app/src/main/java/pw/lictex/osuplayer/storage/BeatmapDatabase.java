package pw.lictex.osuplayer.storage;

import androidx.room.*;

@Database(entities = {BeatmapEntity.class, CollectionBeatmapEntity.class}, version = 2)
public abstract class BeatmapDatabase extends RoomDatabase {
    public abstract BeatmapDAO getDAO();
}
