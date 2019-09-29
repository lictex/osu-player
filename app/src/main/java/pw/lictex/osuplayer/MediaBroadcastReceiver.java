package pw.lictex.osuplayer;

import android.content.*;
import android.view.*;

import pw.lictex.osuplayer.audio.*;

/**
 * Created by kpx on 2019/9/18.
 */
public class MediaBroadcastReceiver extends BroadcastReceiver {
    public MediaBroadcastReceiver() {
        super();
    }

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
                            playerService.resume();
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PAUSE:
                            playerService.pause();
                            break;
                        case KeyEvent.KEYCODE_HEADSETHOOK:
                        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                            if (playerService.getOsuAudioPlayer().getEngine().getPlaybackStatus() == AudioEngine.PlaybackStatus.Playing)
                                playerService.pause();
                            else
                                playerService.resume();
                            break;
                        case KeyEvent.KEYCODE_MEDIA_NEXT:
                            playerService.next();
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                            playerService.previous();
                            break;
                    }
                abortBroadcast();
            }
        }
    }
}
