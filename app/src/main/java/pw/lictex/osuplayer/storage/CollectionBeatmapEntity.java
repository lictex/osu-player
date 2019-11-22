package pw.lictex.osuplayer.storage;

import androidx.annotation.*;
import androidx.room.*;

@Entity()
public class CollectionBeatmapEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "path")
    public String path;
}
