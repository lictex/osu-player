package pw.lictex.libosu.beatmap;

import android.text.*;

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

        String extras = null;
        if (isCircle) {
            object = new HitObject.Circle(x, y, time, hitsound, isNewCombo, skippedColors);
            if (i < sp.length) extras = sp[i];
        } else if (isSpinner) {
            int endTime = Integer.valueOf(sp[i++]);
            object = new HitObject.Spinner(x, y, time, hitsound, isNewCombo, skippedColors, endTime);
            if (i < sp.length) extras = sp[i];
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
            object = new HitObject.Slider(x, y, time, hitsound, isNewCombo, skippedColors, type, cpList, repeat, pixelLength, edgeHitsounds, edgeSampleSet, edgeAdditionSet);
            if (i < sp.length) extras = sp[i];
        } else if (isManiaHold) {
            var c = sp[i++];
            var split = c.split(":");
            int endTime = Integer.valueOf(split[0]);
            object = new HitObject.Hold(x, y, time, hitsound, endTime);
            if (split.length > 1) extras = TextUtils.join(":", Arrays.copyOfRange(split, 1, split.length));
        }

        //extras?
        if (extras != null) {
            var exArr = extras.split(":");
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
        private Type sliderType;
        private List<int[]> curvePoints;
        private int repeat;
        private double pixelLength;
        private List<Integer> edgeHitsounds;
        private List<SampleSet> edgeSampleSet;
        private List<SampleSet> edgeAdditionSet;

        private float curveEnd;

        public Slider(int x, int y, int time, int hitSounds, boolean newCombo, int skippedColors, Type sliderType, List<int[]> curvePoints, int repeat, double pixelLength, List<Integer> edgeHitsounds, List<SampleSet> edgeSampleSet, List<SampleSet> edgeAdditionSet) {
            //super(x, y, time, hitSounds, newCombo, skippedColors);
            this.x = x;
            this.y = y;
            this.time = time;
            this.hitSounds = hitSounds;
            this.newCombo = newCombo;
            this.skippedColors = skippedColors;

            this.sliderType = curvePoints.size() <= 1 ? Type.Linear : sliderType; //1 point bezier = linear..?
            this.curvePoints = curvePoints;
            this.repeat = repeat;
            this.pixelLength = pixelLength;
            this.edgeHitsounds = edgeHitsounds;
            this.edgeSampleSet = edgeSampleSet;
            this.edgeAdditionSet = edgeAdditionSet;

            curveEnd = calcCurveEnd();
        }

        private static float getAngle(float[] centerPt, int[] targetPt) {
            float a = (float) Math.atan2((float) targetPt[1] - centerPt[1], (float) targetPt[0] - centerPt[0]);
            a += (float) Math.PI;
            if (a < 0) a += (float) Math.PI * 2f;
            return a;
        }

        private static float[] getCircleCenter(int[] p1, int[] p2, int[] p3) {
            float[] center = new float[2];
            float ax = ((float) p1[0] + p2[0]) / 2f;
            float ay = ((float) p1[1] + p2[1]) / 2f;
            float ux = ((float) p1[1] - p2[1]);
            float uy = ((float) p2[0] - p1[0]);
            float bx = ((float) p2[0] + p3[0]) / 2f;
            float by = ((float) p2[1] + p3[1]) / 2f;
            float vx = ((float) p2[1] - p3[1]);
            float vy = ((float) p3[0] - p2[0]);
            float dx = ax - bx;
            float dy = ay - by;
            float vu = vx * uy - vy * ux;
            if (vu == 0) return null;
            float g = (dx * uy - dy * ux) / vu;
            center[0] = bx + g * vx;
            center[1] = by + g * vy;
            return center;
        }

        private static float getArcDirection(int[] a, int[] b, int[] c) {
            return ((float) b[0] - a[0]) * ((float) c[1] - b[1]) - ((float) b[1] - a[1]) * ((float) c[0] - b[0]);
        }

        private static float fact(int n) {
            float fact = 1;
            for (int i = 1; i <= n; i++) fact *= i;
            return fact;
        }

        private float calcCurveEnd() {
            double sum = 0;
            float step = (float) (8 / getPixelLength());
            float t = step;
            var lp = curvePointAt(0);
            for (; t <= 1; t += step) {
                var cp = curvePointAt(t);
                sum += Math.sqrt((cp[0] - lp[0]) * (cp[0] - lp[0]) + (cp[1] - lp[1]) * (cp[1] - lp[1]));
                if (sum >= getPixelLength()) {
                    break;
                }
                lp = cp;
            }
            return t;
        }

        public float[] curvePointAt(float t) {
            t = t > 1 ? 1 : t;
            var curvePoints = getCurvePoints();
            switch (getSliderType()) {
                case Bezier: {
                    float startX = getX(), startY = getY();
                    int length = curvePoints.size() + 1;
                    float[] bp = new float[length];
                    for (int i = 0; i < length; i++)
                        bp[i] = (float) (fact(length) / (fact(i + 1) * fact(length - (i + 1))) * Math.pow(1 - t, length - (i + 1)) * Math.pow(t, i + 1));
                    float x = 0, y = 0;
                    for (int i = 1; i < curvePoints.size(); i++) {
                        x += bp[i] * (curvePoints.get(i - 1)[0] - startX);
                        y += bp[i] * (curvePoints.get(i - 1)[1] - startY);
                    }

                    return new float[]{x + startX, y + startY};
                }
                case Catmull: {
                    return new float[]{256, 192}; //TODO ...
                }
                case Perfect: {
                    int[] a = new int[]{getX(), getY()};
                    int[] b = curvePoints.get(0);
                    int[] c = curvePoints.get(1);

                    float[] center;
                    if ((center = getCircleCenter(a, b, c)) != null) {
                        var d = getArcDirection(a, b, c);
                        var ao = (float) Math.abs(Math.sqrt((a[0] - center[0]) * (a[0] - center[0]) + (a[1] - center[1]) * (a[1] - center[1])));

                        var aox = getAngle(center, a);
                        var cox = getAngle(center, c);
                        var all = cox - aox;
                        if (d < 0) all = (float) Math.PI * 2f - all;
                        var an = aox + ((d > 0 ? 1 : -1) * all * t);
                        var jjj = new float[]{center[0] + ao * (float) Math.cos(an - Math.PI), center[1] + ao * (float) Math.sin(an - Math.PI)};
                        return new float[]{jjj[0], jjj[1]};
                    }
                    //else goto Linear:
                }
                default:
                case Linear: {
                    return new float[]{(getX() + curvePoints.get(0)[0]) / 2f, (getY() + curvePoints.get(0)[1]) / 2f};
                }
            }
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

    @Getter
    public static class Hold extends HitObject {
        private int endTime;

        public Hold(int x, int y, int time, int hitSounds, int endTime) {
            //super(x, y, time, hitSounds, newCombo, skippedColors);
            this.x = x;
            this.y = y;
            this.time = time;
            this.hitSounds = hitSounds;
        }

        @Override
        public String toString() {
            return x + "," +
                    y + "," +
                    time + "," +
                    (0b10000000) + "," +
                    hitSounds + "," +
                    endTime + ":" +
                    sampleSet.asInt() + ":" +
                    additions.asInt() + ":" +
                    customSampleSetIndex + ":" +
                    volume + ":" +
                    sampleFile;
        }
    }
}
