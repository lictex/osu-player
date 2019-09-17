package pw.lictex.libosu.beatmap;

import java.util.*;

import lombok.*;

@RequiredArgsConstructor @Getter @Setter
public abstract class HitObject {
    protected int x, y, time;
    protected int hitSounds;
    protected boolean newCombo;
    protected int skippedColors;
    protected SampleSet sampleSet = SampleSet.None;
    protected SampleSet additions = SampleSet.None;
    protected int customSampleSetIndex = 0;
    protected int volume = 0;
    protected String sampleFile = "";

    public static HitObject parse(String s) {
        HitObject object = null;
        var sp = s.split(",");
        var i = 0;

        //common fields
        int x = Integer.valueOf(sp[i++]);
        int y = Integer.valueOf(sp[i++]);
        int time = Integer.valueOf(sp[i++]);
        var typeRaw = Integer.valueOf(sp[i++]);
        var isCircle = (typeRaw & 0b00000001) > 0;
        var isSlider = (typeRaw & 0b00000010) > 0;
        var isSpinner = (typeRaw & 0b00001000) > 0;
        var isNewCombo = (typeRaw & 0b00000100) > 0;
        var isManiaHold = (typeRaw & 0b10000000) > 0;
        var skippedColors = (typeRaw & 0b01111111) >> 4;
        int hitsound = Integer.valueOf(sp[i++]);

        if (isCircle) {
            object = new HitObject.Circle(x, y, time, hitsound, isNewCombo, skippedColors);
        } else if (isSpinner) {
            int endTime = Integer.valueOf(sp[i++]);
            object = new HitObject.Spinner(x, y, time, hitsound, isNewCombo, skippedColors, endTime);
        } else if (isSlider) {
            var cpsArr = sp[i++].split("\\|");
            var cpsIdx = 0;
            Slider.Type type = Slider.Type.fromString(cpsArr[cpsIdx++]);
            List<int[]> cpList = new LinkedList<>();
            while (cpsIdx < cpsArr.length) {
                var it = cpsArr[cpsIdx++].split(":");
                int[] ia = {Integer.valueOf(it[0]), Integer.valueOf(it[1])};
                cpList.add(ia);
            }
            int repeat = Integer.valueOf(sp[i++]);
            double pixelLength = Double.valueOf(sp[i++]);
            ArrayList<Integer> edgeHitsounds = new ArrayList<>(repeat + 1);
            ArrayList<SampleSet> edgeSampleSet = new ArrayList<>(repeat + 1);
            ArrayList<SampleSet> edgeAdditionSet = new ArrayList<>(repeat + 1);
            if (i + 1 < sp.length) {
                var split = sp[i++].split("\\|");
                for (int idx = 0; idx < repeat + 1; idx++) edgeHitsounds.add(Integer.valueOf(split[idx]));
                split = sp[i++].split("\\|");
                for (int idx = 0; idx < repeat + 1; idx++) {
                    var e = split[idx].split(":");
                    edgeSampleSet.add(SampleSet.fromInt(Integer.valueOf(e[0])));
                    edgeAdditionSet.add(SampleSet.fromInt(Integer.valueOf(e[1])));
                }
            } else for (int idx = 0; idx < repeat + 1; idx++) {
                edgeHitsounds.add(0);
                edgeSampleSet.add(SampleSet.None);
                edgeAdditionSet.add(SampleSet.None);
            }
            if (i < sp.length) {
                var exa = sp[i].split(":");

            }

            object = new HitObject.Slider(x, y, time, hitsound, isNewCombo, skippedColors, type, cpList, repeat, pixelLength, edgeHitsounds, edgeSampleSet, edgeAdditionSet);
        } else if (isManiaHold) {
            throw new RuntimeException();  //TODO mania hold
        }

        //extras?
        if (i < sp.length) {
            var exArr = sp[i].split(":");
            object.setSampleSet(SampleSet.fromInt(Integer.valueOf(exArr[0])));
            object.setAdditions(SampleSet.fromInt(Integer.valueOf(exArr[1])));
            object.setCustomSampleSetIndex(Integer.valueOf(exArr[2]));
            object.setVolume(Integer.valueOf(exArr[3]));
            if (exArr.length > 4) object.setSampleFile(exArr[4]);
        }

        return object;
    }

    public static class Circle extends HitObject {
        public Circle(int x, int y, int time, int hitSounds, boolean newCombo, int skippedColors) {
            //super(x, y, time, hitSounds, newCombo, skippedColors);
            this.x = x;
            this.y = y;
            this.time = time;
            this.hitSounds = hitSounds;
            this.newCombo = newCombo;
            this.skippedColors = skippedColors;
        }

        @Override
        public String toString() {
            return x + "," +
                    y + "," +
                    time + "," +
                    (0b00000001 + (newCombo ? 0b00000100 : 0) + (skippedColors << 4)) + "," +
                    hitSounds + "," +
                    sampleSet.asInt() + ":" +
                    additions.asInt() + ":" +
                    customSampleSetIndex + ":" +
                    volume + ":" +
                    sampleFile;
        }
    }

    @Getter
    public static class Slider extends HitObject {
        Type sliderType;
        List<int[]> curvePoints;
        int repeat;
        double pixelLength;
        List<Integer> edgeHitsounds;
        List<SampleSet> edgeSampleSet;
        List<SampleSet> edgeAdditionSet;

        public Slider(int x, int y, int time, int hitSounds, boolean newCombo, int skippedColors, Type sliderType, List<int[]> curvePoints, int repeat, double pixelLength, List<Integer> edgeHitsounds, List<SampleSet> edgeSampleSet, List<SampleSet> edgeAdditionSet) {
            //super(x, y, time, hitSounds, newCombo, skippedColors);
            this.x = x;
            this.y = y;
            this.time = time;
            this.hitSounds = hitSounds;
            this.newCombo = newCombo;
            this.skippedColors = skippedColors;

            this.sliderType = sliderType;
            this.curvePoints = curvePoints;
            this.repeat = repeat;
            this.pixelLength = pixelLength;
            this.edgeHitsounds = edgeHitsounds;
            this.edgeSampleSet = edgeSampleSet;
            this.edgeAdditionSet = edgeAdditionSet;
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder()
                    .append(x).append(",")
                    .append(y).append(",")
                    .append(time).append(",")
                    .append(0b00000010 + (newCombo ? 0b00000100 : 0) + (skippedColors << 4)).append(",")
                    .append(hitSounds).append(",")
                    .append(sliderType.asString());
            for (int[] curvePoint : curvePoints) {
                s.append("|").append(curvePoint[0]).append(":").append(curvePoint[1]);
            }
            s.append(",")
                    .append(repeat).append(",")
                    .append(OsuBeatmap.NF.format(pixelLength));

            boolean con = false;
            for (var edgeHitsound : edgeHitsounds) if (edgeHitsound != 0) con = true;
            if (!con) for (var sampleset : edgeSampleSet) if (sampleset != SampleSet.None) con = true;
            if (!con) for (var sampleset : edgeAdditionSet) if (sampleset != SampleSet.None) con = true;
            if (!con) if (customSampleSetIndex != 0) con = true;
            if (!con) if (volume != 0) con = true;
            if (!con) if (!sampleFile.isEmpty()) con = true;

            if (con) {
                s.append(",");
                for (int i = 0; i < edgeHitsounds.size(); i++) {
                    s.append(edgeHitsounds.get(i));
                    if (i < edgeHitsounds.size() - 1) s.append("|");
                }
                s.append(",");
                for (int i = 0; i < edgeHitsounds.size(); i++) {
                    s.append(edgeSampleSet.get(i).asInt()).append(":").append(edgeAdditionSet.get(i).asInt());
                    if (i < edgeHitsounds.size() - 1) s.append("|");
                }
                s.append(",")
                        .append(sampleSet.asInt()).append(":")
                        .append(additions.asInt()).append(":")
                        .append(customSampleSetIndex).append(":")
                        .append(volume).append(":")
                        .append(sampleFile);
            }
            return s.toString();
        }

        public enum Type {
            Linear("L"),
            Perfect("P"),
            Bezier("B"),
            Catmull("C");

            private String i;

            Type(String i) {
                this.i = i;
            }

            public static Type fromString(String i) {
                for (Type b : Type.values())
                    if (b.asString().equals(i)) return b;
                return null;
            }

            public String asString() {
                return i;
            }
        }
    }

    @Getter
    public static class Spinner extends HitObject {
        private int endTime;

        public Spinner(int x, int y, int time, int hitSounds, boolean newCombo, int skippedColors, int endTime) {
            //super(x, y, time, hitSounds, newCombo, skippedColors);
            this.x = x;
            this.y = y;
            this.time = time;
            this.hitSounds = hitSounds;
            this.newCombo = newCombo;
            this.skippedColors = skippedColors;

            this.endTime = endTime;
        }

        @Override
        public String toString() {
            return x + "," +
                    y + "," +
                    time + "," +
                    (0b00001000 + (newCombo ? 0b00000100 : 0) + (skippedColors << 4)) + "," +
                    hitSounds + "," +
                    endTime + "," +
                    sampleSet.asInt() + ":" +
                    additions.asInt() + ":" +
                    customSampleSetIndex + ":" +
                    volume + ":" +
                    sampleFile;
        }
    }
}
