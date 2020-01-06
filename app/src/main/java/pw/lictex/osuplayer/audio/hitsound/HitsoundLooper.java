package pw.lictex.osuplayer.audio.hitsound;

import androidx.annotation.*;

import lombok.*;
import pw.lictex.osuplayer.audio.*;

public abstract class HitsoundLooper {
    @Setter protected HitsoundContext hsContext;

    public abstract void play();

    public abstract void stop();

    protected AudioEngine.Sample enableLoopingSample(String name, AudioEngine.Sample spinnerSample) {
        var loopingSample = hsContext.getLoopingSamples().get(name);
        if (loopingSample != spinnerSample) {
            if (loopingSample != null) loopingSample.endLoop();
            hsContext.getLoopingSamples().put(name, spinnerSample);
        }
        return spinnerSample;
    }

    protected void disableLoopingSample(String name) {
        AudioEngine.Sample spinnerspin = hsContext.getLoopingSamples().get(name);
        if (spinnerspin != null) {
            spinnerspin.endLoop();
            hsContext.getLoopingSamples().remove(name);
        }
    }

    @Override public boolean equals(@Nullable Object obj) {
        if (obj == null) return false;
        return getClass().equals(obj.getClass());
    }

    @Override public int hashCode() {
        return getClass().hashCode();
    }
}
