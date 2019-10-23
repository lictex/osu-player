package pw.lictex.osuplayer.activity;

import android.*;
import android.animation.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.view.animation.Interpolator;
import android.view.animation.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.*;
import androidx.coordinatorlayout.widget.*;
import androidx.core.app.*;
import androidx.core.content.*;
import androidx.preference.*;

import com.google.android.material.bottomsheet.*;

import butterknife.*;
import eightbitlab.com.blurview.*;
import lombok.*;
import pw.lictex.osuplayer.R;
import pw.lictex.osuplayer.*;
import pw.lictex.osuplayer.audio.*;

public class MainActivity extends AppCompatActivity {
    private static final int animDuration = 250;
    private static final Interpolator animInterpolator = new AccelerateDecelerateInterpolator();
    private static final float blurRadius = 25f;

    @BindView(R.id.controllerBlur) BlurView controllerBlur;
    @BindView(R.id.contentLayout) View contentLayout;
    @BindView(R.id.content) CoordinatorLayout content;
    @BindView(R.id.infoLayout) LinearLayout info;
    @BindView(R.id.progressBar) SeekBar seekBar;
    @BindView(R.id.backgroundImage) ImageView bg;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.artist) TextView artist;
    @BindView(R.id.version) TextView version;
    @BindView(R.id.playlist_wrapper) View playlistWrapper;
    @BindView(R.id.audio_setting_wrapper) View audioSettingWrapper;
    @BindView(R.id.preference_wrapper) View preferenceWrapper;
    @BindView(R.id.buttonPlayPause) ImageButton buttonPlayPause;
    @BindView(R.id.buttonLoopMode) ImageButton buttonLoopMode;

    private Content current = Content.Playlist;
    private ServiceConnection playerServiceConnection;
    private Handler handler = new Handler();
    private AudioSettingFragment audioSettingFragment;
    private PlaylistFragment playlistFragment;
    private PreferenceFragment preferenceFragment;
    @Getter private PlayerService playerService;
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            seekBar.setProgress((int) (((double) playerService.getOsuAudioPlayer().getCurrentTime() / playerService.getOsuAudioPlayer().getAudioLength()) * seekBar.getMax()));
            handler.postDelayed(this, 1000);
        }
    };
    private BottomSheetBehavior bottomSheet;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        unbindService(playerServiceConnection);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        recreate();
    }

    @Override
    public void onBackPressed() {
        if (bottomSheet.getState() != BottomSheetBehavior.STATE_COLLAPSED) {
            bottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            if (getPlayerService().getOsuAudioPlayer().getEngine().getPlaybackStatus() != AudioEngine.PlaybackStatus.Playing) {
                unbindService(playerServiceConnection);
                stopService(new Intent(this, PlayerService.class));
                ((App) getApplication()).stop();
            }
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) savedInstanceState.remove("android:support:fragments");
        super.onCreate(savedInstanceState);
        setTheme(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("use_light_theme", false) ? R.style.LightTheme : R.style.DarkTheme);
        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);

        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(R.attr.acrylicColor, value, true);
        controllerBlur.setOverlayColor(value.data);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int color = Color.TRANSPARENT;
            Drawable bg = findViewById(R.id.llc).getBackground();
            if (bg instanceof ColorDrawable) color = ((ColorDrawable) bg).getColor();
            getWindow().setNavigationBarColor(color);
            if (color == 0xFFFFFFFF) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);

        controllerBlur.setupWith(content).setFrameClearDrawable(new ColorDrawable(0xFFFFFFFF))
                .setBlurAlgorithm(new RenderScriptBlur(this))
                .setBlurRadius(blurRadius)
                .setSaturation(1.25f);

        title.setText(getResources().getString(R.string.app_name));
        artist.setText(getResources().getString(R.string.version));
        version.setText(getResources().getString(R.string.slide_open_playlist));

        bottomSheet = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet));

        if (savedInstanceState != null) {
            getWindow().getDecorView().post(() -> setCurrentContent(Content.values()[savedInstanceState.getInt("content")]));
            bottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else getWindow().getDecorView().post(() -> setCurrentContent(Content.Playlist));

        bottomSheet.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override public void onStateChanged(@NonNull View view, int i) {
                if (i == BottomSheetBehavior.STATE_COLLAPSED) {
                    setCurrentContent(Content.Playlist);
                }
            }

            @Override public void onSlide(@NonNull View view, float v) {

            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b) getPlayerService().getOsuAudioPlayer().seekTo((long) ((float) i / seekBar.getMax() * getPlayerService().getOsuAudioPlayer().getAudioLength()));
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        playerServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                handler.removeCallbacks(runnable);
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                playerService = ((PlayerService.PlayerServiceBinder) service).getService();
                playerService.setOnUpdateCallback(() -> runOnUiThread(() -> updateStatus()));

                audioSettingFragment = new AudioSettingFragment();
                playlistFragment = new PlaylistFragment();
                preferenceFragment = new PreferenceFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.audio_setting_wrapper, audioSettingFragment, "audioSettingFragment")
                        .replace(R.id.playlist_wrapper, playlistFragment, "playlistFragment")
                        .replace(R.id.preference_wrapper, preferenceFragment, "preferenceFragment")
                        .commit();

                handler.post(() -> updateStatus());
                handler.postDelayed(runnable, 32);
            }
        };
        bindService(new Intent(this.getApplicationContext(), PlayerService.class), playerServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("content", current.ordinal());
    }

    @OnClick(R.id.buttonPlayPause) void onPauseClick() {
        if (playerService.getOsuAudioPlayer().getEngine().getPlaybackStatus() == AudioEngine.PlaybackStatus.Playing)
            getPlayerService().pause();
        else {
            getPlayerService().resume();
        }
        updateStatus();
    }

    @OnClick(R.id.buttonNext) void onNextClick() {
        getPlayerService().next();
    }

    @OnClick(R.id.buttonPrev) void onPrevClick() {
        getPlayerService().previous();
    }

    @OnClick(R.id.buttonLoopMode) void onLoopClick() {
        if (playerService.getLoopMode() == PlayerService.LoopMode.All)
            playerService.setLoopMode(PlayerService.LoopMode.Single);
        else if (playerService.getLoopMode() == PlayerService.LoopMode.Single)
            playerService.setLoopMode(PlayerService.LoopMode.Random);
        else if (playerService.getLoopMode() == PlayerService.LoopMode.Random)
            playerService.setLoopMode(PlayerService.LoopMode.All);
        updateStatus();
    }

    @OnClick(R.id.buttonAudioSetting) void onAudioSettingClick() {
        if (current != Content.AudioSetting) {
            setCurrentContent(Content.AudioSetting);
            bottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else {
            bottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    @OnClick(R.id.buttonSetting) void onSettingClick() {
        if (current != Content.Setting) {
            setCurrentContent(Content.Setting);
            bottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else {
            bottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    protected void updateStatus() {
        Bitmap background = playerService.getOsuAudioPlayer().getBackground();
        if (background != null) bg.setImageBitmap(background);
        else bg.setImageDrawable(getResources().getDrawable(R.drawable.defaultbg));

        var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String t, a;
        if (sharedPreferences.getBoolean("use_unicode_metadata", false)) {
            t = playerService.getOsuAudioPlayer().getTitle();
            a = playerService.getOsuAudioPlayer().getArtist();
        } else {
            t = playerService.getOsuAudioPlayer().getRomanisedTitle();
            a = playerService.getOsuAudioPlayer().getRomanisedArtist();
        }
        title.setText(t == null ? getResources().getString(R.string.app_name) : t);
        artist.setText(a == null ? getResources().getString(R.string.version) : a);

        if (playerService.getOsuAudioPlayer().getVersion() != null)
            version.setText(getString(R.string.version_by_mapper, playerService.getOsuAudioPlayer().getVersion(), playerService.getOsuAudioPlayer().getMapper()));
        else
            version.setText(getResources().getString(R.string.slide_open_playlist));

        OsuAudioPlayer player = getPlayerService().getOsuAudioPlayer();
        audioSettingFragment.update(player.getMusicVolume(), player.getSoundVolume(), player.getCurrentMod());

        playlistFragment.refreshList();

        buttonPlayPause.setImageDrawable(playerService.getOsuAudioPlayer().getEngine().getPlaybackStatus() == AudioEngine.PlaybackStatus.Playing ?
                getResources().getDrawable(R.drawable.ic_pause) : getResources().getDrawable(R.drawable.ic_play));

        buttonLoopMode.setImageDrawable(playerService.getLoopMode() == PlayerService.LoopMode.All ?
                getResources().getDrawable(R.drawable.ic_refresh) : playerService.getLoopMode() == PlayerService.LoopMode.Single ?
                getResources().getDrawable(R.drawable.ic_refresh_one) : getResources().getDrawable(R.drawable.ic_refresh_r));
    }

    private void setContentSize(float height, int duration) {
        setContentSize(height, duration, false);
    }

    private void setContentSize(float height, int duration, boolean hideInfo) {
        float infoAlpha = hideInfo ? 0 : 1;

        info.animate().alpha(infoAlpha).setInterpolator(animInterpolator).setDuration(duration).start();

        var heightAnim = ValueAnimator.ofInt(contentLayout.getMeasuredHeight(), Utils.dp2px(this, height));
        heightAnim.setInterpolator(animInterpolator);
        heightAnim.addUpdateListener(valueAnimator -> {
            int val = (Integer) valueAnimator.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = contentLayout.getLayoutParams();
            layoutParams.height = val;
            contentLayout.setLayoutParams(layoutParams);
        });
        heightAnim.setDuration(duration);
        heightAnim.start();
    }

    private void setCurrentContent(Content current) {
        boolean anim = true;
        if (bottomSheet.getState() == BottomSheetBehavior.STATE_COLLAPSED) anim = false;

        this.current = current;
        int statusBarHeight = 0;
        int resourceId = getApplicationContext().getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getApplicationContext().getResources().getDimensionPixelSize(resourceId);
        }
        switch (this.current) {
            case AudioSetting:
                setContentSize(200, anim ? animDuration : 0);
                audioSettingWrapper.animate().setDuration(anim ? animDuration : 0).alpha(1).withStartAction(() -> audioSettingWrapper.setVisibility(View.VISIBLE)).start();
                playlistWrapper.animate().setDuration(anim ? animDuration : 0).alpha(0).withEndAction(() -> playlistWrapper.setVisibility(View.GONE)).start();
                preferenceWrapper.animate().setDuration(anim ? animDuration : 0).alpha(0).withEndAction(() -> preferenceWrapper.setVisibility(View.GONE)).start();
                break;
            case Setting:
                setContentSize(Utils.px2dp(this, findViewById(R.id.content).getMeasuredHeight() - findViewById(R.id.llc).getMeasuredHeight() - findViewById(R.id.infoLayout).getMeasuredHeight() - statusBarHeight * 2) - 48, anim ? animDuration : 0);
                audioSettingWrapper.animate().setDuration(anim ? animDuration : 0).alpha(0).withEndAction(() -> audioSettingWrapper.setVisibility(View.GONE)).start();
                playlistWrapper.animate().setDuration(anim ? animDuration : 0).alpha(0).withEndAction(() -> playlistWrapper.setVisibility(View.GONE)).start();
                preferenceWrapper.animate().setDuration(anim ? animDuration : 0).alpha(1).withStartAction(() -> preferenceWrapper.setVisibility(View.VISIBLE)).start();
                break;
            case Playlist:
                setContentSize(Utils.px2dp(this, findViewById(R.id.content).getMeasuredHeight() - findViewById(R.id.llc).getMeasuredHeight() - findViewById(R.id.infoLayout).getMeasuredHeight() - statusBarHeight * 2) - 48, anim ? animDuration : 0);
                audioSettingWrapper.animate().setDuration(anim ? animDuration : 0).alpha(0).withEndAction(() -> audioSettingWrapper.setVisibility(View.GONE)).start();
                playlistWrapper.animate().setDuration(anim ? animDuration : 0).alpha(1).withStartAction(() -> playlistWrapper.setVisibility(View.VISIBLE)).start();
                preferenceWrapper.animate().setDuration(anim ? animDuration : 0).alpha(0).withEndAction(() -> preferenceWrapper.setVisibility(View.GONE)).start();
                break;
        }
    }

    private enum Content {
        Playlist, AudioSetting, Setting
    }

}
