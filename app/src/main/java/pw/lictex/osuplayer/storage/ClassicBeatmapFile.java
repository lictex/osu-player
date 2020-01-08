package pw.lictex.osuplayer.storage;

import java.io.*;
import java.util.regex.*;

public class ClassicBeatmapFile implements BeatmapFile {
    private File beatmapSet;
    private File file;

    public ClassicBeatmapFile(File beatmapSet, File file) {
        this.beatmapSet = beatmapSet;
        this.file = file;
    }

    @Override public String getType() {
        String[] s = file.getName().split("\\.");
        if (s.length > 1) return s[s.length - 1];
        return "";
    }

    @Override public String getName() throws IOException {
        String s = file.getCanonicalPath().replaceFirst(Pattern.quote(beatmapSet.getCanonicalPath()), "").replace('\\', '/');
        while (s.startsWith("/")) s = s.substring(1);
        if (s.contains(".")) s = s.substring(0, s.lastIndexOf('.'));

        return s;
    }

    @Override public InputStream openStream() throws IOException {
        return new FileInputStream(file);
    }

    @Override public String getNativePath() throws IOException {
        return file.getCanonicalPath().replace('\\', '/');
    }
}
