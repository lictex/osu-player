package pw.lictex.osuplayer.storage;

import androidx.lifecycle.*;
import androidx.room.*;

import java.util.*;

@Dao
public interface BeatmapDAO {
    @Query("DELETE FROM beatmapentity")
    void clear();

    @Query("SELECT * FROM beatmapentity " +
            "WHERE title LIKE :s " +
            "OR unicode_title LIKE :s " +
            "OR artist LIKE :s " +
            "OR unicode_artist LIKE :s " +
            "OR version LIKE :s " +
            "OR creator LIKE :s " +
            "OR tags LIKE :s " +
            "ORDER BY title COLLATE NOCASE")
    LiveData<List<BeatmapEntity>> orderByTitle(String s);

    @Query("SELECT * FROM beatmapentity " +
            "ORDER BY title COLLATE NOCASE")
    LiveData<List<BeatmapEntity>> orderByTitle();

    @Query("SELECT * FROM collectionbeatmapentity " +
            "WHERE title LIKE :s " +
            "OR unicode_title LIKE :s " +
            "OR artist LIKE :s " +
            "OR unicode_artist LIKE :s " +
            "OR version LIKE :s " +
            "OR creator LIKE :s " +
            "OR tags LIKE :s " +
            "ORDER BY title COLLATE NOCASE")
    LiveData<List<BeatmapEntity>> orderCollectionByTitle(String s);

    @Query("SELECT * FROM collectionbeatmapentity " +
            "ORDER BY title COLLATE NOCASE")
    LiveData<List<BeatmapEntity>> orderCollectionByTitle();

    @Query("SELECT * FROM beatmapentity " +
            "WHERE title LIKE :s " +
            "OR unicode_title LIKE :s " +
            "OR artist LIKE :s " +
            "OR unicode_artist LIKE :s " +
            "OR version LIKE :s " +
            "OR creator LIKE :s " +
            "OR tags LIKE :s " +
            "ORDER BY artist COLLATE NOCASE")
    LiveData<List<BeatmapEntity>> orderByArtist(String s);

    @Query("SELECT * FROM beatmapentity " +
            "ORDER BY artist COLLATE NOCASE")
    LiveData<List<BeatmapEntity>> orderByArtist();

    @Query("SELECT * FROM collectionbeatmapentity " +
            "WHERE title LIKE :s " +
            "OR unicode_title LIKE :s " +
            "OR artist LIKE :s " +
            "OR unicode_artist LIKE :s " +
            "OR version LIKE :s " +
            "OR creator LIKE :s " +
            "OR tags LIKE :s " +
            "ORDER BY artist COLLATE NOCASE")
    LiveData<List<BeatmapEntity>> orderCollectionByArtist(String s);

    @Query("SELECT * FROM collectionbeatmapentity " +
            "ORDER BY artist COLLATE NOCASE")
    LiveData<List<BeatmapEntity>> orderCollectionByArtist();

    @Query("SELECT * FROM beatmapentity " +
            "WHERE title LIKE :s " +
            "OR unicode_title LIKE :s " +
            "OR artist LIKE :s " +
            "OR unicode_artist LIKE :s " +
            "OR version LIKE :s " +
            "OR creator LIKE :s " +
            "OR tags LIKE :s " +
            "ORDER BY creator COLLATE NOCASE")
    LiveData<List<BeatmapEntity>> orderByCreator(String s);

    @Query("SELECT * FROM beatmapentity " +
            "ORDER BY creator COLLATE NOCASE")
    LiveData<List<BeatmapEntity>> orderByCreator();

    @Query("SELECT * FROM collectionbeatmapentity " +
            "WHERE title LIKE :s " +
            "OR unicode_title LIKE :s " +
            "OR artist LIKE :s " +
            "OR unicode_artist LIKE :s " +
            "OR version LIKE :s " +
            "OR creator LIKE :s " +
            "OR tags LIKE :s " +
            "ORDER BY creator COLLATE NOCASE")
    LiveData<List<BeatmapEntity>> orderCollectionByCreator(String s);

    @Query("SELECT * FROM collectionbeatmapentity " +
            "ORDER BY creator COLLATE NOCASE")
    LiveData<List<BeatmapEntity>> orderCollectionByCreator();

    @Insert(entity = BeatmapEntity.class, onConflict = OnConflictStrategy.REPLACE)
    void insert(BeatmapEntity... b);

    @Insert(entity = CollectionBeatmapEntity.class, onConflict = OnConflictStrategy.REPLACE)
    void addCollection(BeatmapEntity... b);

    @Delete(entity = CollectionBeatmapEntity.class)
    void removeCollection(BeatmapEntity... b);

    @Update
    void update(BeatmapEntity... b);
}
