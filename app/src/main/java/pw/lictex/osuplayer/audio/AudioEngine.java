package pw.lictex.osuplayer.audio;

import android.os.Process;
import android.os.*;
import android.util.*;

import java.io.*;
import java.nio.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import lombok.*;

import static com.un4seen.bass.BASS.*;
import static com.un4seen.bass.BASS_FX.*;

/**
 * Created by kpx on 2019/2/26.
 */
public class AudioEngine {
    private static final int SAMPLERATE = 44100;
    private static final int AUDIOFREQ = 250;

    private final AtomicLong currentTime = new AtomicLong();
    private final AtomicLong currentTPS = new AtomicLong();
    private final AtomicLong totalLength = new AtomicLong();
    private AudioThread audioThread;
    private Runnable tickEvent;
    private Runnable onTrackEndCallback;
    private int MainTrackChannel_BASS;

    @Getter private PlaybackStatus playbackStatus = PlaybackStatus.Stopped;

    AudioEngine() {
        audioThread = new AudioThread();
        audioThread.start();
    }

    void destroy() {
        try {
            audioThread.interrupt();
        } catch (Throwable ignored) {}
    }

    //TODO shouldn't be public
    public long getAudioClockFreq() {
        return currentTPS.get();
    }

    //TODO ???
    private void runOnAudioThread(Runnable r) {
        if (Thread.currentThread() == audioThread) {
            r.run();
            return;
        }
        audioThread.eventQueue.offer(r);
    }

    private void runOnAudioThreadSync(Runnable r) {
        if (Thread.currentThread() == audioThread) {
            r.run();
            return;
        }
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
        runOnAudioThreadSync(() -> {
            BASS_ChannelStop(MainTrackChannel_BASS);
            BASS_StreamFree(MainTrackChannel_BASS);
            MainTrackChannel_BASS = BASS_StreamCreateFile(file, 0, 0, BASS_STREAM_DECODE | BASS_STREAM_PRESCAN);
            MainTrackChannel_BASS = BASS_FX_TempoCreate(MainTrackChannel_BASS, BASS_FX_FREESOURCE);
            totalLength.set((long) (BASS_ChannelBytes2Seconds(MainTrackChannel_BASS, BASS_ChannelGetLength(MainTrackChannel_BASS, BASS_POS_BYTE)) * 1000));
            resume();
        });
    }

    void stopMainTrack() {
        runOnAudioThreadSync(() -> {
            BASS_ChannelStop(MainTrackChannel_BASS);
            playbackStatus = PlaybackStatus.Stopped;
        });
    }

    void resume() {
        runOnAudioThreadSync(() -> {
            BASS_ChannelPlay(MainTrackChannel_BASS, false);
            playbackStatus = PlaybackStatus.Playing;
        });
    }

    void pause() {
        runOnAudioThreadSync(() -> {
            BASS_ChannelPause(MainTrackChannel_BASS);
            playbackStatus = PlaybackStatus.Paused;
        });
    }

    void setMainTrackVolume(float d) {
        runOnAudioThreadSync(() -> BASS_ChannelSetAttribute(MainTrackChannel_BASS, BASS_ATTRIB_VOL, d));
    }

    void setTempo(float d) {
        runOnAudioThreadSync(() -> BASS_ChannelSetAttribute(MainTrackChannel_BASS, BASS_ATTRIB_TEMPO, (d - 1) * 100f));
    }

    void setPitch(float d) {
        runOnAudioThreadSync(() -> BASS_ChannelSetAttribute(MainTrackChannel_BASS, BASS_ATTRIB_TEMPO_PITCH, (d - 1) * 12f));
    }

    void setTime(long ms) {
        runOnAudioThreadSync(() -> {
            long pos = BASS_ChannelSeconds2Bytes(MainTrackChannel_BASS, ms / 1000d);
            BASS_ChannelSetPosition(MainTrackChannel_BASS, pos, BASS_POS_BYTE);
        });
    }

    long getMainTrackCurrentTime() {
        return currentTime.get();
    }

    long getMainTrackTotalTime() {
        return totalLength.get();
    }

    void setTickCallback(Runnable r) {
        runOnAudioThreadSync(() -> tickEvent = r);
    }

    void setOnTrackEndCallback(Runnable r) {
        runOnAudioThreadSync(() -> onTrackEndCallback = r);
    }

    Sample createSample(String name, InputStream file) {
        var sample = new Sample();
        sample.name = name;
        runOnAudioThreadSync(() -> {
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

    public enum PlaybackStatus {
        Playing, Paused, Stopped
    }

    class Sample {
        String name;
        int ptr;

        private volatile AtomicInteger channel;

        void play(float volume, float pan) {
            runOnAudioThreadSync(() -> {
                int handle = BASS_SampleGetChannel(ptr, false);
                BASS_ChannelSetAttribute(handle, BASS_ATTRIB_VOL, volume);
                BASS_ChannelSetAttribute(handle, BASS_ATTRIB_PAN, pan);
                BASS_ChannelFlags(handle, 0, BASS_SAMPLE_LOOP);
                BASS_ChannelPlay(handle, false);
            });
        }

        void loop(float volume, float pan, int sampleRate) {
            runOnAudioThreadSync(() -> {
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
            });
        }

        void loop(float volume) {
            loop(volume, 0, 0);
        }

        void loop(float volume, int sampleRate) {
            loop(volume, 0, sampleRate);
        }

        void endLoop() {
            runOnAudioThreadSync(() -> {
                if (channel != null) {
                    int handle = channel.get();
                    channel = null;
                    BASS_ChannelStop(handle);
                }
            });
        }
    }

    private class AudioThread extends Thread {

        private final ConcurrentLinkedQueue<Runnable> eventQueue = new ConcurrentLinkedQueue<>();

        private AudioThread() {
            setName("AudioEngineThread");
        }

        @Override
        public void run() {
            try {
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
                    if (BASS_ChannelIsActive(MainTrackChannel_BASS) == BASS_ACTIVE_STOPPED) {
                        if (getPlaybackStatus() == PlaybackStatus.Playing) {
                            playbackStatus = PlaybackStatus.Stopped;
                            onTrackEndCallback.run();
                        }
                    }
                    currentTime.set((long) (BASS_ChannelBytes2Seconds(MainTrackChannel_BASS, BASS_ChannelGetPosition(MainTrackChannel_BASS, BASS_POS_BYTE)) * 1000d));
                    if (tickEvent != null) tickEvent.run();
                    synchronized (eventQueue) {
                        Runnable r;
                        while ((r = eventQueue.poll()) != null) r.run();
                    }
                    //long sleepms = (long) Math.ceil(1f / AUDIOFREQ * 1000f - (SystemClock.elapsedRealtime() - s));
                    long sleepms = (long) Math.ceil(1f / AUDIOFREQ * 1000f);
                    if (sleepms > 0) SystemClock.sleep(sleepms);
                }
            } catch (Throwable e) {
                Log.e("AE", "Audio engine stopped.", e);
            }
            BASS_Free();
        }
    }
}