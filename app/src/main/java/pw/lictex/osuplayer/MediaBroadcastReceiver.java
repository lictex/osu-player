package pw.lictex.osuplayer;

import android.content.*;
import android.os.*;
import android.view.*;

import java.util.concurrent.atomic.*;

import pw.lictex.osuplayer.audio.*;

/**
 * Created by kpx on 2019/9/18.
 */
public class MediaBroadcastReceiver extends BroadcastReceiver {
    public MediaBroadcastReceiver() {
        super();
    }

    private static volatile boolean active = false;
    private static final AtomicInteger count = new AtomicInteger();

    @Override
    public void onReceive(Context c, Intent i) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(i.getAction())) {
            PlayerService.PlayerServiceBinder binder = (PlayerService.PlayerServiceBinder) peekService(c, new Intent(c, PlayerService.class));
            if (binder != null) {
                PlayerService playerService = binder.getService();
                KeyEvent keyEvent = i.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN)
                    switch (keyEvent.getKeyCode()) {
                        case KeyEvent.KEYCODE_MEDIA_PLAY:
                            Utils.runTask(playerService::resume);
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PAUSE:
                            Utils.runTask(playerService::pause);
                            break;
                        case KeyEvent.KEYCODE_HEADSETHOOK:
                        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                            count.getAndIncrement();
                            if (!active) {
                                active = true;
                                new Handler().postDelayed(() -> {
                                    if (count.get() == 1) {
                                        if (playerService.getOsuAudioPlayer().getEngine().getPlaybackStatus() == AudioEngine.PlaybackStatus.Playing)
                                            Utils.runTask(playerService::pause);
                                        else Utils.runTask(playerService::resume);
                                    } else if (count.get() == 2) Utils.runTask(playerService::next);
                                    else if (count.get() == 3) Utils.runTask(playerService::previous);
                                    active = false;
                                    count.set(0);
                                }, 500);
                            }
                            break;
                        case KeyEvent.KEYCODE_MEDIA_NEXT:
                            Utils.runTask(playerService::next);
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                            Utils.runTask(playerService::previous);
                            break;
                    }
            }
        }
    }
}
