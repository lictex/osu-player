package pw.lictex.libosu.beatmap;

import lombok.*;

@Getter @Setter
public class TimingPoint {
    private double offset;
    private double beatLength;
    private int timeSignature;
    private SampleSet sampleSet;
    private int customSampleSet;
    private int volume;
    private boolean timingChange;
    private int effects;

    public TimingPoint(String s) {
        var arr = s.split(",");
        offset = Double.valueOf(arr[0]);
        beatLength = Double.valueOf(arr[1]);
        timeSignature = Integer.valueOf(arr[2]);
        sampleSet = SampleSet.fromInt(Integer.valueOf(arr[3]));
        customSampleSet = Integer.valueOf(arr[4]);
        volume = Integer.valueOf(arr[5]);
        timingChange = Integer.valueOf(arr[6]) == 1;
        effects = Integer.valueOf(arr[7]);
    }

    @Override
    public String toString() {
        return OsuBeatmap.NF.format(offset) + "," +
                OsuBeatmap.NF.format(beatLength) + "," +
                timeSignature + "," +
                sampleSet.asInt() + "," +
                customSampleSet + "," +
                volume + "," +
                (timingChange ? 1 : 0) + "," +
                effects;
    }

    public boolean isEffectEnabled(Effect e) {
        return (getEffects() & e.asInt()) != 0;
    }

    public enum Effect {
        None(0),
        Kiai(1),
        OmitFirstBarLine(8);

        private int i;

        Effect(int i) {
            this.i = i;
        }

        private int asInt() {return i;}
    }
}
