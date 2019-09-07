package pw.lictex.osuplayer.audio;

import android.os.Process;
import android.os.SystemClock;
import lombok.var;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.un4seen.bass.BASS.*;
import static com.un4seen.bass.BASS_FX.*;

/**
 * Created by kpx on 2019/2/26.
 */
public class AudioEngine {
    private static final int SAMPLERATE = 44100;
    private static final int AUDIOCLOCKFREQ = 250;

    private final AtomicLong currentTime = new AtomicLong();
    private final AtomicLong currentTPS = new AtomicLong();
    private final AtomicLong totalLength = new AtomicLong();
    private AudioThread audioThread;
    private Runnable tickEvent;
    private int MainTrackChannel_BASS;

    AudioEngine() {
        audioThread = new AudioThread();
        audioThread.start();
    }

    //shouldn't be public
    public long getAudioClockFreq() {
        return currentTPS.get();
    }

    void queueTask(Runnable r) {
        audioThread.eventQueue.offer(r);
    }

    void queueTaskSync(Runnable r) {
        try {
            var lock = new Object();
            synchronized (audioThread.eventQueue) {
                audioThread.eventQueue.offer(() -> {
                    r.run();
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                });
            }
            synchronized (lock) {
                lock.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void playMainTrack(String file) {
        BASS_ChannelStop(MainTrackChannel_BASS);
        BASS_StreamFree(MainTrackChannel_BASS);
        MainTrackChannel_BASS = BASS_StreamCreateFile(file, 0, 0, BASS_STREAM_DECODE | BASS_STREAM_PRESCAN);
        MainTrackChannel_BASS = BASS_FX_TempoCreate(MainTrackChannel_BASS, BASS_FX_FREESOURCE);
        totalLength.set((long) (BASS_ChannelBytes2Seconds(MainTrackChannel_BASS, BASS_ChannelGetLength(MainTrackChannel_BASS, BASS_POS_BYTE)) * 1000));
        resume();
    }

    void stopMainTrack() {
        BASS_ChannelStop(MainTrackChannel_BASS);
    }

    void resume() {
        BASS_ChannelPlay(MainTrackChannel_BASS, false);
    }

    void pause() {
        BASS_ChannelPause(MainTrackChannel_BASS);
    }

    void setMainTrackVolume(float d) {
        BASS_ChannelSetAttribute(MainTrackChannel_BASS, BASS_ATTRIB_VOL, d);
    }

    void setTempo(float d) {
        BASS_ChannelSetAttribute(MainTrackChannel_BASS, BASS_ATTRIB_TEMPO, (d - 1) * 100f);
    }

    void setPitch(float d) {
        BASS_ChannelSetAttribute(MainTrackChannel_BASS, BASS_ATTRIB_TEMPO_PITCH, (d - 1) * 12f);
    }

    void setTime(long ms) {
        long pos = BASS_ChannelSeconds2Bytes(MainTrackChannel_BASS, ms / 1000d);
        BASS_ChannelSetPosition(MainTrackChannel_BASS, pos, BASS_POS_BYTE);
    }

    long getMainTrackCurrentTime() {
        return currentTime.get();
    }

    long getMainTrackTotalTime() {
        return totalLength.get();
    }

    void setTickCallback(Runnable r) {
        queueTaskSync(() -> tickEvent = r);
    }

    Sample createSample(String name, InputStream file) {
        var sample = new Sample();
        sample.name = name;
        queueTaskSync(() -> {
            try {
                var bs = new ByteArrayOutputStream();
                var buff = new byte[128];
                int i;
                while ((i = file.read(buff, 0, 100)) > 0) {
                    bs.write(buff, 0, i);
                }
                byte[] b = bs.toByteArray();
                sample.ptr = BASS_SampleLoad(ByteBuffer.wrap(b), 0, b.length, 65535, BASS_SAMPLE_OVER_POS);
            } catch (IOException e) {
                e.printStackTrace();
                //TODO sth
            }
        });
        return sample;
    }

    class Sample {
        String name;
        int ptr;

        private volatile AtomicInteger channel;

        void play(float volume, float pan) {
            queueTask(() -> {
                int handle = BASS_SampleGetChannel(ptr, false);
                BASS_ChannelSetAttribute(handle, BASS_ATTRIB_VOL, volume);
                BASS_ChannelSetAttribute(handle, BASS_ATTRIB_PAN, pan);
                BASS_ChannelFlags(handle, 0, BASS_SAMPLE_LOOP);
                BASS_ChannelPlay(handle, false);
            });
        }

        void loop(float volume, float pan, int sampleRate) {
            if (channel == null) {
                channel = new AtomicInteger();
                channel.set(BASS_SampleGetChannel(ptr, false));

                BASS_ChannelSetAttribute(channel.get(), BASS_ATTRIB_VOL, volume);
                BASS_ChannelSetAttribute(channel.get(), BASS_ATTRIB_PAN, pan);
                BASS_ChannelSetAttribute(channel.get(), BASS_ATTRIB_FREQ, sampleRate);

                BASS_ChannelFlags(channel.get(), BASS_SAMPLE_LOOP, BASS_SAMPLE_LOOP);
                BASS_ChannelPlay(channel.get(), false);
            } else {
                BASS_ChannelSetAttribute(channel.get(), BASS_ATTRIB_VOL, volume);
                BASS_ChannelSetAttribute(channel.get(), BASS_ATTRIB_PAN, pan);
                BASS_ChannelSetAttribute(channel.get(), BASS_ATTRIB_FREQ, sampleRate);
            }
        }

        void loop(float volume) {
            loop(volume, 0, 0);
        }

        void loop(float volume, int sampleRate) {
            loop(volume, 0, sampleRate);
        }

        void endLoop() {
            if (channel != null) {
                int handle = channel.get();
                channel = null;
                BASS_ChannelStop(handle);
            }
        }
    }

    private class AudioThread extends Thread {

        private final ConcurrentLinkedQueue<Runnable> eventQueue = new ConcurrentLinkedQueue<>();

        private AudioThread() {
            setName("AudioThread");
        }

        @Override
        public void run() {
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
            BASS_Init(-1, SAMPLERATE, 0);

            //audio event loop
            long t = SystemClock.elapsedRealtime(), i = 0;
            while (!isInterrupted()) {
                i++;
                long s = SystemClock.elapsedRealtime();
                long l = SystemClock.elapsedRealtime() - t;
                if (l >= 1000) {
                    currentTPS.set(i);
                    i = 0;
                    t = SystemClock.elapsedRealtime();
                }
                currentTime.set((long) (BASS_ChannelBytes2Seconds(MainTrackChannel_BASS, BASS_ChannelGetPosition(MainTrackChannel_BASS, BASS_POS_BYTE)) * 1000d));
                if (tickEvent != null) tickEvent.run();
                synchronized (eventQueue) {
                    Runnable r;
                    while ((r = eventQueue.poll()) != null) r.run();
                }
                //long sleepms = (long) Math.ceil(1f / AUDIOCLOCKFREQ * 1000f - (SystemClock.elapsedRealtime() - s));
                long sleepms = (long) Math.ceil(1f / AUDIOCLOCKFREQ * 1000f);
                if (sleepms > 0) SystemClock.sleep(sleepms);
            }
            BASS_Free();
        }
    }
}