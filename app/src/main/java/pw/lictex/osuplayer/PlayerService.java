package pw.lictex.osuplayer;

import android.app.*;
import android.content.*;
import android.os.*;

import java.io.*;
import java.util.*;

import lombok.*;
import pw.lictex.osuplayer.audio.*;
import pw.lictex.osuplayer.activity.MainActivity;

public class PlayerService extends Service {
    private final int ID = 141;

    @Getter private ArrayList<String> playlist = new ArrayList<>();
    @Getter private int currentIndex = 0;
    @Getter private OsuAudioPlayer osuAudioPlayer;
    @Getter @Setter private Runnable onNewTrackCallback;
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
        startForeground(ID, getNotification());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        osuAudioPlayer.destroy();
        osuAudioPlayer = null;
    }

    public void next() {
        play(currentIndex + 1);
    }

    public void previous() {
        play(currentIndex - 1);
    }

    public void play(int index) {
        if (playlist.size() != 0) {
            index = (index < 0 || index >= playlist.size()) ? 0 : index;
            String s = playlist.get(index);
            osuAudioPlayer.openBeatmapSet(new File(s).getParent() + "/");
            osuAudioPlayer.playBeatmap(new File(s).getName());
            currentIndex = index;

            if (onNewTrackCallback != null) onNewTrackCallback.run();
        }
        refresh();
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
