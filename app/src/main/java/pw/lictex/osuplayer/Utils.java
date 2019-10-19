package pw.lictex.osuplayer;

import android.content.*;
import android.os.*;

/**
 * Created by kpx on 2019/9/12.
 */
public class Utils {
    public static int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static float px2dp(Context context, int pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return pxValue / scale + 0.5f;
    }

    public static void runTask(Runnable r) {
        new Task(r).execute();
    }

    private static class Task extends AsyncTask<Void, Void, Void> {
        private Runnable r;

        public Task(Runnable r) {
            this.r = r;
        }

        @Override protected Void doInBackground(Void... voids) {
            r.run();
            return null;
        }
    }
}
