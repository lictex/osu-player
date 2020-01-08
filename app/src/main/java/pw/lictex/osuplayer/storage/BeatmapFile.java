package pw.lictex.osuplayer.storage;

import java.io.*;

public interface BeatmapFile {
    String getName() throws IOException;

    String getType();

    String getNativePath() throws IOException;

    InputStream openStream() throws IOException;
}
