package pw.lictex.libosu.beatmap;

import java.io.*;
import java.text.*;
import java.util.*;

import lombok.*;

public class OsuBeatmap {
    public static final String LS = "\r\n";
    public static final NumberFormat NF = NumberFormat.getInstance();

    @Getter private General generalSection = new General();
    @Getter private Metadata metadataSection = new Metadata();
    @Getter private Colours coloursSection = new Colours();
    @Getter private Events eventsSection = new Events();
    @Getter private HitObjects hitObjectsSection = new HitObjects();
    @Getter private TimingPoints timingPointsSection = new TimingPoints();
    @Getter private Editor editorSection = new Editor();
    @Getter private Difficulty difficultySection = new Difficulty();

    private OsuBeatmap() {
        NF.setMinimumFractionDigits(0);
        NF.setMaximumFractionDigits(Integer.MAX_VALUE);
        NF.setMaximumIntegerDigits(Integer.MAX_VALUE);
        NF.setGroupingUsed(false);
    }

    public static OsuBeatmap fromFile(String file) {
        try (var br = new BufferedReader(new FileReader(file))) {
            StringBuilder s = new StringBuilder();
            String t;
            while ((t = br.readLine()) != null) s.append(t).append("\n");
            return fromString(s.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static OsuBeatmap fromString(String s) {
        return new Reader(s).read().get();
    }

    @Override
    public String toString() {
        return "osu file format v14" + LS + LS +
                generalSection.toString() +
                editorSection.toString() +
                metadataSection.toString() +
                difficultySection.toString() +
                eventsSection.toString() +
                timingPointsSection.toString() +
                coloursSection.toString() +
                hitObjectsSection.toString();
    }

    public TimingPoint timingPoingAt(int ms) {
        List<TimingPoint> pointList = getTimingPointsSection().getTimingPoints();
        for (int i = 0; i < pointList.size(); i++) if (pointList.get(i).getOffset() > ms) return i > 0 ? pointList.get(i - 1) : pointList.get(0);
        return pointList.get(pointList.size() - 1);
    }

    public TimingPoint notInheritedTimingPoingAt(int ms) {
        List<TimingPoint> pointList = getTimingPointsSection().getTimingPoints();
        TimingPoint lp = null;
        for (int i = 0; i < pointList.size(); i++) {
            if (pointList.get(i).getBeatLength() >= 0) lp = pointList.get(i);
            if (pointList.get(i).getOffset() > ms) return lp;
        }
        return lp;
    }

    //region slider
    public double getSliderDuration(HitObject.Slider s) {
        TimingPoint p = notInheritedTimingPoingAt(s.getTime());
        TimingPoint rp = timingPoingAt(s.getTime());
        double b = p.getBeatLength();
        if (rp.getBeatLength() < 0) {
            b *= Math.abs(rp.getBeatLength()) / 100d;
        }
        return s.getPixelLength() / (100.0 * getDifficultySection().getSliderMultiplier()) * b;
    }

    public float[] getSliderPositionAt(HitObject.Slider s, int time) {
        float e = getCurveEnd(s);
        int sliderDuration = (int) getSliderDuration(s);
        int repeat = (time - s.getTime()) / sliderDuration;
        int dt = (time - s.getTime()) % sliderDuration;

        float t = dt / ((float) sliderDuration);
        float v = (repeat % 2 == 0 ? t : 1 - t) * e;
        return curvePointAt(s, v);
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

    private float getCurveEnd(HitObject.Slider s) {
        double sum = 0;
        float t = (float) (2 / s.getPixelLength());
        var lp = curvePointAt(s, 0);
        for (; t <= 1; t += (float) (2 / s.getPixelLength())) {
            var cp = curvePointAt(s, t);
            sum += Math.sqrt((cp[0] - lp[0]) * (cp[0] - lp[0]) + (cp[1] - lp[1]) * (cp[1] - lp[1]));
            if (sum >= s.getPixelLength()) {
                break;
            }
            lp = cp;
        }
        return t;
    }

    private float[] curvePointAt(HitObject.Slider s, float t) {
        var curvePoints = s.getCurvePoints();
        switch (s.getSliderType()) {
            case Bezier: {
                float startX = s.getX(), startY = s.getY();
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
                int[] a = new int[]{s.getX(), s.getY()};
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
                return new float[]{(s.getX() + curvePoints.get(0)[0]) / 2f, (s.getY() + curvePoints.get(0)[1]) / 2f};
            }
        }
    }
    //endregion

    private static class Reader {
        private BufferedReader in;
        private OsuBeatmap beatmap;

        private Reader(String s) {
            this.in = new BufferedReader(new StringReader(s));
        }

        public OsuBeatmap get() {
            return beatmap;
        }

        public Reader read() {
            this.beatmap = new OsuBeatmap();
            try {
                String currentLine = in.readLine().replace("\ufeff", ""); //fuck bom
                if (!currentLine.matches("osu file format v[0-9]*$")) throw new RuntimeException();
                //read sections
                while ((currentLine = in.readLine()) != null) {
                    if (currentLine.matches("^\\[.*]$")) {
                        var sectionName = currentLine.substring(1, currentLine.length() - 1);
                        switch (sectionName) {
                            case "General": {
                                while (!(currentLine = in.readLine()).isEmpty()) {
                                    var v = Value.parse(currentLine);
                                    switch (v.key) {
                                        case "AudioFilename":
                                            beatmap.getGeneralSection().setAudioFilename(v.asString());
                                            break;
                                        case "AudioLeadIn":
                                            beatmap.getGeneralSection().setAudioLeadIn(v.asInt());
                                            break;
                                        case "PreviewTime":
                                            beatmap.getGeneralSection().setPreviewTime(v.asInt());
                                            break;
                                        case "Countdown":
                                            beatmap.getGeneralSection().setCountdown(v.asInt());
                                            break;
                                        case "SampleSet":
                                            beatmap.getGeneralSection().setSampleSet(SampleSet.valueOf(v.asString()));
                                            break;
                                        case "StackLeniency":
                                            beatmap.getGeneralSection().setStackLeniency(v.asFloat());
                                            break;
                                        case "Mode":
                                            beatmap.getGeneralSection().setMode(PlayModes.valueOf(v.asInt()));
                                            break;
                                        case "LetterboxInBreaks":
                                            beatmap.getGeneralSection().setLetterboxInBreaks(v.asBoolean());
                                            break;
                                        case "EpilepsyWarning":
                                            beatmap.getGeneralSection().setEpilepsyWarning(v.asBoolean());
                                            break;
                                        case "WidescreenStoryboard":
                                            beatmap.getGeneralSection().setWidescreenStoryboard(v.asBoolean());
                                            break;
                                    }
                                }
                                break;
                            }
                            case "Editor": {
                                while (!(currentLine = in.readLine()).isEmpty()) {
                                    var v = Value.parse(currentLine);
                                    switch (v.key) {
                                        case "Bookmarks":
                                            beatmap.getEditorSection().setBookmarks(v.asString());
                                            break;
                                        case "DistanceSpacing":
                                            beatmap.getEditorSection().setDistanceSpacing(v.asDouble());
                                            break;
                                        case "BeatDivisor":
                                            beatmap.getEditorSection().setBeatDivisor(v.asInt());
                                            break;
                                        case "GridSize":
                                            beatmap.getEditorSection().setGridSize(v.asInt());
                                            break;
                                        case "TimelineZoom":
                                            beatmap.getEditorSection().setTimelineZoom(v.asFloat());
                                            break;
                                    }
                                }
                                break;
                            }
                            case "Metadata": {
                                while (!(currentLine = in.readLine()).isEmpty()) {
                                    var v = Value.parse(currentLine);
                                    switch (v.key) {
                                        case "Title":
                                            beatmap.getMetadataSection().setTitle(v.asString());
                                            break;
                                        case "TitleUnicode":
                                            beatmap.getMetadataSection().setTitleUnicode(v.asString());
                                            break;
                                        case "Artist":
                                            beatmap.getMetadataSection().setArtist(v.asString());
                                            break;
                                        case "ArtistUnicode":
                                            beatmap.getMetadataSection().setArtistUnicode(v.asString());
                                            break;
                                        case "Creator":
                                            beatmap.getMetadataSection().setCreator(v.asString());
                                            break;
                                        case "Version":
                                            beatmap.getMetadataSection().setVersion(v.asString());
                                            break;
                                        case "Source":
                                            beatmap.getMetadataSection().setSource(v.asString());
                                            break;
                                        case "Tags":
                                            beatmap.getMetadataSection().setTags(v.asString());
                                            break;
                                        case "BeatmapID":
                                            beatmap.getMetadataSection().setBeatmapID(v.asInt());
                                            break;
                                        case "BeatmapSetID":
                                            beatmap.getMetadataSection().setBeatmapSetID(v.asInt());
                                            break;
                                    }
                                }
                                break;
                            }
                            case "Difficulty": {
                                while (!(currentLine = in.readLine()).isEmpty()) {
                                    var v = Value.parse(currentLine);
                                    switch (v.key) {
                                        case "HPDrainRate":
                                            beatmap.getDifficultySection().setHpDrainRate(v.asFloat());
                                            break;
                                        case "CircleSize":
                                            beatmap.getDifficultySection().setCircleSize(v.asFloat());
                                            break;
                                        case "OverallDifficulty":
                                            beatmap.getDifficultySection().setOverallDifficulty(v.asFloat());
                                            break;
                                        case "ApproachRate":
                                            beatmap.getDifficultySection().setApproachRate(v.asFloat());
                                            break;
                                        case "SliderMultiplier":
                                            beatmap.getDifficultySection().setSliderMultiplier(v.asDouble());
                                            break;
                                        case "SliderTickRate":
                                            beatmap.getDifficultySection().setSliderTickRate(v.asDouble());
                                            break;
                                    }
                                }
                                break;
                            }
                            case "Events": {
                                StringBuilder sb = new StringBuilder();
                                while (!(currentLine = in.readLine()).isEmpty()) {
                                    sb.append(currentLine).append(LS);
                                }
                                beatmap.getEventsSection().setContent(sb.toString().trim());
                                break;
                            }
                            case "TimingPoints": {
                                while (!(currentLine = in.readLine()).isEmpty()) {
                                    beatmap.getTimingPointsSection().addTImingPoint(new TimingPoint(currentLine));
                                }
                                break;
                            }
                            case "Colours": {
                                while (!(currentLine = in.readLine()).isEmpty()) {
                                    beatmap.getColoursSection().addComboColour(new Color(currentLine));
                                }
                                break;
                            }
                            case "HitObjects": {
                                while ((currentLine = in.readLine()) != null && !currentLine.isEmpty()) {
                                    beatmap.getHitObjectsSection().addHitObject(HitObject.parse(currentLine));
                                }
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                this.beatmap = null;
            }
            return this;
        }

        @Getter
        private static class Value {
            private String key;
            private String value;

            private static Value parse(String s) {
                Value v = new Value();
                v.key = s.split(":", 2)[0].trim();
                v.value = s.split(":", 2)[1].trim();
                return v;
            }

            private int asInt() {
                return Integer.valueOf(value);
            }

            private long asLong() {
                return Long.valueOf(value);
            }

            private float asFloat() {
                return Float.valueOf(value);
            }

            private double asDouble() {
                return Double.valueOf(value);
            }

            private String asString() {
                return String.valueOf(value);
            }

            private boolean asBoolean() {
                return Boolean.valueOf(value);
            }
        }
    }

    //region Sections
    public class General {
        @Getter @Setter private String audioFilename;
        @Getter @Setter private int audioLeadIn;
        @Getter @Setter private int previewTime;
        @Getter @Setter private int countdown;
        @Getter @Setter private SampleSet sampleSet;
        @Getter @Setter private float stackLeniency;
        @Getter @Setter private PlayModes mode;
        @Getter @Setter private boolean letterboxInBreaks;
        @Getter @Setter private boolean epilepsyWarning;
        @Getter @Setter private boolean widescreenStoryboard;

        @Override
        public String toString() {
            return "[General]" + LS +
                    "AudioFilename: " + audioFilename + LS +
                    "AudioLeadIn: " + audioLeadIn + LS +
                    "PreviewTime: " + previewTime + LS +
                    "Countdown: " + countdown + LS +
                    "SampleSet: " + sampleSet + LS +
                    "StackLeniency: " + stackLeniency + LS +
                    "Mode: " + mode.asInt() + "\n" +
                    "LetterboxInBreaks: " + (letterboxInBreaks ? 1 : 0) + LS +
                    "WidescreenStoryboard: " + (widescreenStoryboard ? 1 : 0) + LS + LS;
        }
    }

    public class Editor {
        @Getter @Setter private double distanceSpacing;
        @Getter @Setter private int beatDivisor;
        @Getter @Setter private int gridSize;
        @Getter @Setter private float timelineZoom;
        private List<Integer> bookmarks = new ArrayList<>();

        public List<Integer> getBookmarks() {
            return new ArrayList<>(bookmarks);
        }

        public void setBookmarks(String s) {
            getBookmarks().clear();
            var sa = s.split(",");
            for (var i : sa) getBookmarks().add(Integer.valueOf(i));
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder().append("[Editor]").append(LS)
                    .append("Bookmarks: ");
            for (int i = 0; i < bookmarks.size(); i++) {
                s.append(bookmarks.get(i));
                if (i + 1 < bookmarks.size()) s.append(",");
            }
            s.append(LS)
                    .append("DistanceSpacing: ").append(NF.format(distanceSpacing)).append(LS)
                    .append("BeatDivisor: ").append(beatDivisor).append(LS)
                    .append("GridSize: ").append(gridSize).append(LS)
                    .append("TimelineZoom: ").append(timelineZoom).append(LS).append(LS);
            return s.toString();
        }
    }

    public class Metadata {
        @Getter @Setter private String title;
        @Getter @Setter private String titleUnicode;
        @Getter @Setter private String artist;
        @Getter @Setter private String artistUnicode;
        @Getter @Setter private String creator;
        @Getter @Setter private String version;
        @Getter @Setter private String source;
        @Getter @Setter private String tags;
        @Getter @Setter private int beatmapID;
        @Getter @Setter private int beatmapSetID;

        @Override
        public String toString() {
            return "[Metadata]" + LS +
                    "Title:" + title + LS +
                    "TitleUnicode:" + titleUnicode + LS +
                    "Artist:" + artist + LS +
                    "ArtistUnicode:" + artistUnicode + LS +
                    "Creator:" + creator + LS +
                    "Version:" + version + LS +
                    "Source:" + source + LS +
                    "Tags:" + tags + LS +
                    "BeatmapID:" + beatmapID + LS +
                    "BeatmapSetID:" + beatmapSetID + LS + LS;
        }
    }

    public class Difficulty {
        @Getter @Setter private float hpDrainRate;
        @Getter @Setter private float circleSize;
        @Getter @Setter private float overallDifficulty;
        @Getter @Setter private float approachRate;
        @Getter @Setter private double sliderMultiplier;
        @Getter @Setter private double sliderTickRate;

        @Override
        public String toString() {
            return "[Difficulty]" + LS +
                    "HPDrainRate:" + NF.format(hpDrainRate) + LS +
                    "CircleSize:" + NF.format(circleSize) + LS +
                    "OverallDifficulty:" + NF.format(overallDifficulty) + LS +
                    "ApproachRate:" + NF.format(approachRate) + LS +
                    "SliderMultiplier:" + NF.format(sliderMultiplier) + LS +
                    "SliderTickRate:" + NF.format(sliderTickRate) + LS + LS;
        }
    }

    public class Events { //TODO sb先鸽着
        @Getter private String content = "";
        @Getter private String backgroundImage = "";
        @Getter private String backgroundVideo = "";
        @Getter private List<Sample> samples = new ArrayList<>();
        @Getter private int videoOffset = 0;
        private List<int[]> breaks = new ArrayList<>();

        /**
         * @return the break times list. <br/><strong>use <code>setBreaks()</code> to save changes.</strong>
         */
        public List<int[]> getBreaks() {
            return new ArrayList<>(breaks);
        }

        public void setBreaks(List<int[]> breaks) {
            Collections.sort(breaks, new Comparator<int[]>() {
                @Override
                public int compare(int[] o1, int[] o2) {
                    return o1[0] - o2[0];
                }
            });
            content = content.replaceAll("\\r*\\n*2,.*", "");
            content = content.replaceAll("\\r*\\n*Break,.*", "");
            var sb = new StringBuilder();
            for (int[] p : breaks) sb.append("2,").append(p[0]).append(",").append(p[1]).append(LS);
            if (content.contains("//Break Periods"))
                content = content.replaceFirst("//Break Periods", "//Break Periods" + LS + sb.toString().trim());
            else
                content = ("//Break Periods" + LS + sb.toString().trim() + LS) + content;
            readBasicInfo();
        }

        public void setContent(String content) {
            this.content = content;
            readBasicInfo();
        }

        public void setBackgroundImage(String backgroundImage) {
            if (content.matches("0,.*") || content.matches("Background,.*")) {
                content = content.replaceAll("0,.*", "0,0,\"" + backgroundImage + "\",0,0");
                content = content.replaceAll("Background,.*", "0,0,\"" + backgroundImage + "\",0,0");
            } else {
                content = ("0,0,\"" + backgroundImage + "\",0,0") + LS + content;
            }
            readBasicInfo();
        }

        public void setBackgroundVideo(String backgroundVideo, int offset) {
            if (content.matches("1,.*") || content.matches("Video,.*")) {
                content = content.replaceAll("1,.*", "Video," + offset + ",\"" + backgroundVideo + "\",0,0");
                content = content.replaceAll("Video,.*", "Video," + offset + ",\"" + backgroundVideo + "\",0,0");
            } else {
                content = ("Video," + offset + ",\"" + backgroundVideo + "\",0,0") + LS + content;
            }
            readBasicInfo();
        }

        private void readBasicInfo() {
            breaks.clear();
            for (String s : content.split("\\n")) {
                if (s.trim().matches("0,.*") || s.trim().matches("Background,.*")) {
                    backgroundImage = s.split(",")[2].replace("\"", "");
                }
                if (s.trim().matches("1,.*") || s.trim().matches("Video,.*")) {
                    videoOffset = Integer.valueOf(s.split(",")[1]);
                    backgroundVideo = s.split(",")[2].replace("\"", "");
                }
                if (s.trim().matches("2,.*") || s.trim().matches("Break,.*")) {
                    var pt = s.trim().split(",");
                    breaks.add(new int[]{Integer.valueOf(pt[1]), Integer.valueOf(pt[2])});
                }
                if (s.trim().matches("Sample,.*")) {
                    var split = s.trim().split(",");
                    samples.add(new Sample(Integer.parseInt(split[1]), split[3].replace("\"", ""), Integer.parseInt(split[2]), split.length > 4 ? Integer.parseInt(split[4]) : 100));
                }
            }
        }

        @Override
        public String toString() {
            return "[Events]" + LS +
                    content + LS + LS;
        }

        @AllArgsConstructor @Getter @Setter
        public class Sample {
            private int time;
            private String file;
            private int layer;
            private int volume;
        }
    }

    public class TimingPoints {
        private List<TimingPoint> timingPoints = new ArrayList<>();

        public List<TimingPoint> getTimingPoints() {
            return new ArrayList<>(timingPoints);
        }

        public void setTimingPoints(List<TimingPoint> timingPoints) {
            this.timingPoints = timingPoints;
            Collections.sort(this.timingPoints, new Comparator<TimingPoint>() {
                @Override
                public int compare(TimingPoint o1, TimingPoint o2) {
                    return (int) (o1.getOffset() - o2.getOffset());
                }
            });
        }

        public void addTImingPoint(TimingPoint p) {
            timingPoints.add(p);
        }

        @Override
        public String toString() {
            var sb = new StringBuilder().append("[TimingPoints]").append(LS);
            for (TimingPoint point : timingPoints) {
                sb.append(point.toString()).append(LS);
            }
            sb.append(LS).append(LS);
            return sb.toString();
        }
    }

    public class Colours {
        private List<Color> comboColours = new ArrayList<>();

        public List<Color> getComboColours() {
            return new ArrayList<>(comboColours);
        }

        public void setComboColours(List<Color> comboColours) {
            this.comboColours = comboColours;
        }

        public void addComboColour(Color c) {
            comboColours.add(c);
        }

        @Override
        public String toString() {
            var sb = new StringBuilder().append("[Colours]").append(LS);
            for (int i = 0; i < comboColours.size(); i++) {
                sb.append("Combo").append(i + 1).append(" : ").append(comboColours.get(i).toString()).append(LS);
            }
            sb.append(LS);
            return sb.toString();
        }
    }

    public class HitObjects {
        private List<HitObject> hitObjects = new ArrayList<>();

        public List<HitObject> getHitObjects() {
            return new ArrayList<>(hitObjects);
        }

        public void setHitObjects(List<HitObject> hitObjects) {
            this.hitObjects = hitObjects;
        }

        public void addHitObject(HitObject o) {
            hitObjects.add(o);
        }

        @Override
        public String toString() {
            var sb = new StringBuilder().append("[HitObjects]").append(LS);
            for (HitObject object : hitObjects) {
                sb.append(object.toString()).append(LS);
            }
            sb.append(LS);
            return sb.toString();
        }
    }
    //endregion

}
