package pw.lictex.osuplayer;

import android.app.*;
import android.content.*;
import android.os.*;

import pw.lictex.osuplayer.storage.*;

/**
 * Created by kpx on 2019/9/7.
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        BeatmapIndex.Build(getApplicationContext());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "Service";
            String channelName = "Service";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, PlayerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
        else startService(intent);
    }

    public void stop() {
        System.exit(0); //TODO
    }
}
