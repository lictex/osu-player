package pw.lictex.osuplayer;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;

import java.io.*;
import java.util.*;

import lombok.*;
import pw.lictex.osuplayer.activity.*;
import pw.lictex.osuplayer.audio.*;

public class PlayerService extends Service {
    private final int ID = 141;
    private AudioManager audioManager;
    @Getter private ArrayList<String> playlist = new ArrayList<>();
    @Getter private int currentIndex = 0;
    @Getter private OsuAudioPlayer osuAudioPlayer;
    @Getter @Setter private Runnable onUpdateCallback;
    private final AudioManager.OnAudioFocusChangeListener focusChangeListener = focusChange -> {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                audioManager.unregisterMediaButtonEventReceiver(new ComponentName(PlayerService.this.getPackageName(), MediaBroadcastReceiver.class.getName()));
                audioManager.registerMediaButtonEventReceiver(new ComponentName(PlayerService.this.getPackageName(), MediaBroadcastReceiver.class.getName()));
                resume();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                audioManager.unregisterMediaButtonEventReceiver(new ComponentName(PlayerService.this.getPackageName(), MediaBroadcastReceiver.class.getName()));
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                pause();
                break;
        }
    };
    @Getter @Setter private LoopMode loopMode = LoopMode.All;

    public String getCurrentPath() {
        try {
            return playlist.get(currentIndex);
        } catch (Throwable e) {
            return "";
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new PlayerServiceBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        osuAudioPlayer = new OsuAudioPlayer(getApplicationContext());
        osuAudioPlayer.setOnBeatmapEndCallback(() -> {
            switch (loopMode) {
                case Single:
                    play(currentIndex);
                    break;
                case All:
                    play(currentIndex + 1);
                    break;
                case Random:
                    play(((int) (Math.random() * playlist.size())));
            }

        });
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        ensureAudioFocus();
        startForeground(ID, getNotification());
    }

    private void ensureAudioFocus() {
        audioManager.unregisterMediaButtonEventReceiver(new ComponentName(getPackageName(), MediaBroadcastReceiver.class.getName()));
        audioManager.abandonAudioFocus(focusChangeListener);
        audioManager.requestAudioFocus(focusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        audioManager.registerMediaButtonEventReceiver(new ComponentName(getPackageName(), MediaBroadcastReceiver.class.getName()));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        osuAudioPlayer.destroy();
        osuAudioPlayer = null;
        audioManager.unregisterMediaButtonEventReceiver(new ComponentName(getPackageName(), MediaBroadcastReceiver.class.getName()));
        audioManager.abandonAudioFocus(focusChangeListener);
    }

    public void next() {
        play(currentIndex + 1);
    }

    public void previous() {
        play(currentIndex - 1);
    }

    public void play(int index) {
        ensureAudioFocus();
        if (playlist.size() != 0) {
            index = (index < 0 || index >= playlist.size()) ? 0 : index;
            String s = playlist.get(index);
            osuAudioPlayer.openBeatmapSet(new File(s).getParent() + "/");
            osuAudioPlayer.playBeatmap(new File(s).getName());
            currentIndex = index;
            if (onUpdateCallback != null) onUpdateCallback.run();
        }
        refresh();
    }

    public void resume() {
        ensureAudioFocus();
        osuAudioPlayer.play();
        if (onUpdateCallback != null) onUpdateCallback.run();
    }

    public void pause() {
        osuAudioPlayer.pause();
        if (onUpdateCallback != null) onUpdateCallback.run();
    }

    public void refresh() {
        var notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(ID, getNotification());
    }

    private Notification getNotification() {
        var builder = new Notification.Builder(getApplication()).setAutoCancel(true).setSmallIcon(R.drawable.ic_osu_96)
                .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) builder.setChannelId("Service");

        if (playlist.size() == 0 || osuAudioPlayer.getTitle() == null) builder.setContentTitle("闲置中");
        else builder.setContentTitle(osuAudioPlayer.getTitle() + " - " + osuAudioPlayer.getArtist()).
                setContentText(osuAudioPlayer.getVersion() + " by " + osuAudioPlayer.getMapper());

        return builder.build();
    }

    public enum LoopMode {
        Single, All, Random
    }

    public class PlayerServiceBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }

}
