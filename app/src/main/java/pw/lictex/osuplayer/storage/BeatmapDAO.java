package pw.lictex.osuplayer.storage;

import androidx.lifecycle.*;
import androidx.room.*;
import androidx.sqlite.db.*;

import com.annimon.stream.*;

import java.util.*;

@Dao
public interface BeatmapDAO {

    static SupportSQLiteQuery constructQuery(String table, List<String> kw, String order) {
        StringBuilder query = new StringBuilder();
        List<Object> args = new ArrayList<>();
        query.append("SELECT * FROM ").append(table).append(" ");

        if (kw.size() > 0) {
            String[] f = {"title", "unicode_title", "artist", "unicode_artist", "version", "creator", "tags", "source"};
            query.append("WHERE ");
            for (int i = 0; i < kw.size(); i++) {
                query.append("( ");
                for (int x = 0; x < f.length; x++) {
                    if (x == 0) query.append(f[x]).append(" LIKE ");
                    else query.append("OR ").append(f[x]).append(" LIKE ");
                    args.add(kw.get(i));
                    query.append("'%'||?||'%' ");
                }
                query.append(") ");
                if (i != kw.size() - 1) query.append("AND ");
            }
        }

        query.append("ORDER BY ").append(order).append(" COLLATE NOCASE");
        return new SimpleSQLiteQuery(query.toString(), args.toArray());
    }

    @RawQuery(observedEntities = BeatmapEntity.class)
    LiveData<List<BeatmapEntity>> queryBeatmapEntity(SupportSQLiteQuery s);

    @RawQuery(observedEntities = CollectionBeatmapEntity.class)
    LiveData<List<BeatmapEntity>> queryCollectionBeatmapEntity(SupportSQLiteQuery s);

    @Query("DELETE FROM beatmapentity")
    void clear();

    default LiveData<List<BeatmapEntity>> query(String filter, boolean collection, String order) {
        List<String> kw = Stream.of(filter.split(" ")).filter(o -> !o.trim().isEmpty()).toList();
        if (collection) return queryCollectionBeatmapEntity(constructQuery("collectionbeatmapentity", kw, order));
        else return queryBeatmapEntity(constructQuery("beatmapentity", kw, order));

    }

    default LiveData<List<BeatmapEntity>> orderByTitle(String filter) {
        return query(filter, false, "title");
    }

    default LiveData<List<BeatmapEntity>> orderCollectionByTitle(String filter) {
        return query(filter, true, "title");
    }

    default LiveData<List<BeatmapEntity>> orderByArtist(String filter) {
        return query(filter, false, "artist");
    }

    default LiveData<List<BeatmapEntity>> orderCollectionByArtist(String filter) {
        return query(filter, true, "artist");
    }

    default LiveData<List<BeatmapEntity>> orderByCreator(String filter) {
        return query(filter, false, "creator");
    }

    default LiveData<List<BeatmapEntity>> orderCollectionByCreator(String filter) {
        return query(filter, true, "creator");
    }

    @Insert(entity = BeatmapEntity.class, onConflict = OnConflictStrategy.REPLACE)
    void insert(BeatmapEntity... b);

    @Insert(entity = CollectionBeatmapEntity.class, onConflict = OnConflictStrategy.REPLACE)
    void addCollection(BeatmapEntity... b);

    @Delete(entity = CollectionBeatmapEntity.class)
    void removeCollection(BeatmapEntity... b);

    @Update
    void update(BeatmapEntity... b);

}