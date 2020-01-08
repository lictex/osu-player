package pw.lictex.osuplayer.storage;

import com.annimon.stream.function.*;

import java.io.*;
import java.util.*;

public interface BeatmapSetStorage {
    BeatmapFile getFile(String name) throws IOException;

    List<BeatmapFile> searchFiles(Function<BeatmapFile, Boolean> filter);
}
