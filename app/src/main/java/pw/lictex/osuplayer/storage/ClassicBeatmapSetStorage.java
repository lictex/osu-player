package pw.lictex.osuplayer.storage;

import androidx.annotation.*;

import com.annimon.stream.function.*;

import java.io.*;
import java.util.*;

public class ClassicBeatmapSetStorage implements BeatmapSetStorage {
    private final File root;

    public ClassicBeatmapSetStorage(String path) {
        root = new File(path);
    }

    @Override public List<BeatmapFile> searchFiles(Function<BeatmapFile, Boolean> filter) {
        ArrayList<BeatmapFile> result = new ArrayList<>();
        _search(root, filter, result);
        return result;
    }

    private void _search(File parent, Function<BeatmapFile, Boolean> filter, List<BeatmapFile> res) {
        File[] files = parent.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                _search(file, filter, res);
                continue;
            }
            ClassicBeatmapFile osuFile = new ClassicBeatmapFile(root, file);
            if (filter.apply(osuFile)) res.add(osuFile);
        }
    }

    @Override public BeatmapFile getFile(String name) throws IOException {
        File file = new File(root, name);
        if (!file.isFile()) throw new IOException();
        return new ClassicBeatmapFile(root, file);
    }

    @Override public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof ClassicBeatmapSetStorage)) return false;
        try {
            return root.getCanonicalPath().equals(((ClassicBeatmapSetStorage) obj).root.getCanonicalPath());
        } catch (IOException ignored) { }
        return false;
    }
}
