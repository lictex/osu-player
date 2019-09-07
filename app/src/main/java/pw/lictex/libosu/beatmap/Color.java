package pw.lictex.libosu.beatmap;

import lombok.*;

@AllArgsConstructor
public class Color {
    private int r, g, b;

    public Color(String s) {
        var values = s.split(":")[1].trim().split(",");
        r = Integer.valueOf(values[0]);
        g = Integer.valueOf(values[1]);
        b = Integer.valueOf(values[2]);
    }

    @Override
    public String toString() {
        return r + "," + g + "," + b;
    }
}
