package pw.lictex.osuplayer.storage;

import androidx.annotation.*;
import androidx.room.*;

@Entity
public class BeatmapEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "path")
    public String path;

    @ColumnInfo(name = "unicode_artist")
    public String unicode_artist;

    @ColumnInfo(name = "artist")
    public String artist;

    @ColumnInfo(name = "unicode_title")
    public String unicode_title;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "version")
    public String version;

    @ColumnInfo(name = "creator")
    public String creator;

    @ColumnInfo(name = "tags")
    public String tags;

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj != null) return path.equals(((BeatmapEntity) obj).path);
        else return false;
    }
}
