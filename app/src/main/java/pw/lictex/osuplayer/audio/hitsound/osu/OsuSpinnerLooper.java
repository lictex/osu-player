package pw.lictex.osuplayer.audio.hitsound.osu;

import pw.lictex.osuplayer.audio.hitsound.*;

public class OsuSpinnerLooper extends HitsoundLooper {

    private float spinnerCompletion;

    public OsuSpinnerLooper(float spinnerCompletion) {
        this.spinnerCompletion = spinnerCompletion;
    }

    @Override public void play() {
        enableLoopingSample("spinnerspin", hsContext.getSampleManager().getSample("spinnerspin"))
                .loop(hsContext.volumeAt(hsContext.getPlayerTime()), Math.min(100000, 20000 + (int) (40000 * spinnerCompletion)));
    }

    @Override public void stop() {
        disableLoopingSample("spinnerspin");
    }
}
