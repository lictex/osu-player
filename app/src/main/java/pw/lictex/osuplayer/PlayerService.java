package pw.lictex.osuplayer;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;

import androidx.core.app.*;
import androidx.preference.*;

import java.io.*;
import java.util.*;

import lombok.*;
import pw.lictex.osuplayer.activity.*;
import pw.lictex.osuplayer.audio.*;
import pw.lictex.osuplayer.storage.*;

public class PlayerService extends Service {
    private final int ID = 141;
    private AudioManager audioManager;
    private RemoteControlClient remoteControlClient;
    @Getter private ArrayList<BeatmapEntity> allMapList = new ArrayList<>();
    @Getter private ArrayList<BeatmapEntity> collectionMapList = new ArrayList<>();
    @Getter @Setter private boolean playCollectionList = false;
    @Getter private BeatmapEntity currentMap = null;
    @Getter private OsuAudioPlayer osuAudioPlayer;
    @Getter @Setter private Runnable onUpdateCallback;

    private Utils.LimitedStack<Integer> randHistory = new Utils.LimitedStack<>(32);

    private boolean focusPlaying = false;
    private final AudioManager.OnAudioFocusChangeListener focusChangeListener = focusChange -> {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                audioManager.unregisterMediaButtonEventReceiver(new ComponentName(PlayerService.this.getPackageName(), MediaBroadcastReceiver.class.getName()));
                audioManager.registerMediaButtonEventReceiver(new ComponentName(PlayerService.this.getPackageName(), MediaBroadcastReceiver.class.getName()));
                if (focusPlaying) resume();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                audioManager.unregisterMediaButtonEventReceiver(new ComponentName(PlayerService.this.getPackageName(), MediaBroadcastReceiver.class.getName()));
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                focusPlaying = osuAudioPlayer.getEngine().getPlaybackStatus() == AudioEngine.PlaybackStatus.Playing;
                pause();
                break;
        }
    };
    @Getter private LoopMode loopMode = LoopMode.All;

    public void setLoopMode(LoopMode loopMode) {
        this.loopMode = loopMode;
        randHistory.clear();
        if (loopMode == LoopMode.Random) randHistory.push(getPlaylist().indexOf(currentMap));
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
                    play(getPlaylist().indexOf(currentMap));
                    break;
                case All:
                case Random:
                    next();
            }

        });

        var order = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt("loop_mode", PlayerService.LoopMode.All.ordinal());
        order = order < BeatmapIndex.Order.values().length ? order : 0;
        setLoopMode(PlayerService.LoopMode.values()[order]);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        ensureAudioFocus();
        startForeground(ID, generateNotification());
    }

    private static final String ACTION_RESUME = "pw.lictex.osuplayer.action_resume";
    private static final String ACTION_PAUSE = "pw.lictex.osuplayer.action_pause";
    private static final String ACTION_PREVIOUS = "pw.lictex.osuplayer.action_previous";
    private static final String ACTION_NEXT = "pw.lictex.osuplayer.action_next";
    private static final String ACTION_TERMINATE = "pw.lictex.osuplayer.action_stop";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            var action = intent.getAction();
            if (action != null) switch (action) {
                case ACTION_RESUME:
                    resume(); break;
                case ACTION_PAUSE:
                    pause(); break;
                case ACTION_PREVIOUS:
                    previous(); break;
                case ACTION_NEXT:
                    next(); break;
                case ACTION_TERMINATE:
                    stopSelf();
                    ((App) getApplication()).stop();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void ensureAudioFocus() {
        focusPlaying = false;
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
        if (loopMode == LoopMode.Random) play((int) (Math.random() * getPlaylist().size()));
        else play(getPlaylist().indexOf(currentMap) + 1);
    }

    public void previous() {
        if (loopMode == LoopMode.Random) play(randHistory.skip(1).pop().orElse((int) (Math.random() * getPlaylist().size())));
        else play(getPlaylist().indexOf(currentMap) - 1);
    }

    public synchronized void play(int index) {
        if (loopMode == LoopMode.Random) randHistory.push(index);
        ensureAudioFocus();
        if (getPlaylist().size() != 0) {
            if (index < 0) index = getPlaylist().size() - 1;
            else if (index >= getPlaylist().size()) index = 0;

            currentMap = getPlaylist().get(index);
            osuAudioPlayer.openBeatmapSet(new File(BeatmapIndex.getCurrentPath() + currentMap.path).getParent() + "/");
            osuAudioPlayer.playBeatmap(new File(BeatmapIndex.getCurrentPath() + currentMap.path).getName());
            if (onUpdateCallback != null) onUpdateCallback.run();
        }
        rebuildNotification();
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

    public void rebuildNotification() {
        var notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(ID, generateNotification());
    }

    public List<BeatmapEntity> getPlaylist() {
        return playCollectionList ? collectionMapList : allMapList;
    }

    private Notification generateNotification() {
        if (remoteControlClient == null) {
            var intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            intent.setComponent(new ComponentName(getPackageName(), MediaBroadcastReceiver.class.getName()));
            remoteControlClient = new RemoteControlClient(PendingIntent.getBroadcast(this, 0, intent, 0));
            audioManager.registerRemoteControlClient(remoteControlClient);
        }
        remoteControlClient.setPlaybackState(getOsuAudioPlayer().getEngine().getPlaybackStatus() == AudioEngine.PlaybackStatus.Playing ?
                RemoteControlClient.PLAYSTATE_PLAYING : RemoteControlClient.PLAYSTATE_PAUSED
        );
        remoteControlClient.setTransportControlFlags(
                RemoteControlClient.FLAG_KEY_MEDIA_PAUSE | RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS | RemoteControlClient.FLAG_KEY_MEDIA_NEXT | RemoteControlClient.FLAG_KEY_MEDIA_STOP
        );

        var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        var builder = new NotificationCompat.Builder(getApplication(), "Service")
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_osu_96)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
        var lockScreenEditor = remoteControlClient.editMetadata(true)
                .putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, osuAudioPlayer.getBackground());

        builder.addAction(R.drawable.ic_skip_previous, "Previous", PendingIntent.getService(getBaseContext(), 0, new Intent(this, PlayerService.class).setAction(ACTION_PREVIOUS), 0));
        if (getOsuAudioPlayer().getEngine().getPlaybackStatus() == AudioEngine.PlaybackStatus.Playing)
            builder.addAction(R.drawable.ic_pause, "Pause", PendingIntent.getService(getBaseContext(), 1, new Intent(this, PlayerService.class).setAction(ACTION_PAUSE), 0));
        else
            builder.addAction(R.drawable.ic_play, "Play", PendingIntent.getService(getBaseContext(), 2, new Intent(this, PlayerService.class).setAction(ACTION_RESUME), 0));
        builder.addAction(R.drawable.ic_skip_next, "Next", PendingIntent.getService(getBaseContext(), 3, new Intent(this, PlayerService.class).setAction(ACTION_NEXT), 0));
        builder.addAction(R.drawable.ic_close, "Terminate", PendingIntent.getService(getBaseContext(), 4, new Intent(this, PlayerService.class).setAction(ACTION_TERMINATE), 0));

        builder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1, 2, 3).setShowCancelButton(true).setCancelButtonIntent(PendingIntent.getService(getBaseContext(), 3, new Intent(this, PlayerService.class).setAction(ACTION_TERMINATE), 0)))
                .setLargeIcon(osuAudioPlayer.getBackground());

        if (allMapList.size() == 0 || osuAudioPlayer.getTitle() == null) {
            builder.setContentTitle("闲置中");
            lockScreenEditor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, "闲置中");
        } else {
            var title = sharedPreferences.getBoolean("use_unicode_metadata", false) ?
                    osuAudioPlayer.getTitle() + " - " + osuAudioPlayer.getArtist() :
                    osuAudioPlayer.getRomanisedTitle() + " - " + osuAudioPlayer.getRomanisedArtist();
            var mapper = getString(R.string.version_by_mapper, osuAudioPlayer.getVersion(), osuAudioPlayer.getMapper());
            builder.setContentTitle(title).setContentText(mapper);
            lockScreenEditor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, title).putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, mapper);
        }

        lockScreenEditor.apply();
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
