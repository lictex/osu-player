package pw.lictex.osuplayer.activity;

import android.animation.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.view.animation.Interpolator;
import android.view.animation.*;
import android.widget.*;

import androidx.appcompat.app.*;
import androidx.cardview.widget.*;

import butterknife.*;
import eightbitlab.com.blurview.*;
import lombok.*;
import pw.lictex.osuplayer.R;
import pw.lictex.osuplayer.*;
import pw.lictex.osuplayer.audio.*;

public class MainActivity extends AppCompatActivity {
    private static final int animDuration = 250;
    private static final Interpolator animInterpolator = new AccelerateDecelerateInterpolator();
    private static final float blurRadius = 24f;

    @BindView(pw.lictex.osuplayer.R.id.contentBlur) BlurView contentBlur;
    @BindView(R.id.controllerBlur) BlurView controllerBlur;
    @BindView(R.id.contentLayout) CardView window;
    @BindView(R.id.infoLayout) LinearLayout info;
    @BindView(R.id.fullscreenDim) View dim;
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

    private Content current = Content.None;
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        if (getPlayerService().getOsuAudioPlayer().getEngine().getPlaybackStatus() != AudioEngine.PlaybackStatus.Playing) {
            unbindService(playerServiceConnection);
            stopService(new Intent(this, PlayerService.class));
            ((App) getApplication()).stop();
        }
        unbindService(playerServiceConnection);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);

        ViewGroup rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        contentBlur.setupWith(rootView).setFrameClearDrawable(new ColorDrawable(0xFF000000))
                .setBlurAlgorithm(new RenderScriptBlur(this))
                .setBlurRadius(blurRadius);
        controllerBlur.setupWith(rootView).setFrameClearDrawable(new ColorDrawable(0xFF000000))
                .setBlurAlgorithm(new RenderScriptBlur(this))
                .setBlurRadius(blurRadius);
        setContentSize(0, 0);

        title.setText(null);
        artist.setText(null);
        version.setText(null);

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
        if (current != Content.AudioSetting)
            setCurrentContent(Content.AudioSetting);
        else setCurrentContent(Content.None);
    }

    @OnClick(R.id.buttonPlaylist) void onPlaylistClick() {
        if (current != Content.Playlist)
            setCurrentContent(Content.Playlist);
        else setCurrentContent(Content.None);
    }

    @OnClick(R.id.buttonSetting) void onSettingClick() {
        if (current != Content.Setting)
            setCurrentContent(Content.Setting);
        else setCurrentContent(Content.None);
    }

    private void updateStatus() {
        Bitmap background = playerService.getOsuAudioPlayer().getBackground();
        if (background != null) bg.setImageBitmap(background);
        else bg.setImageDrawable(getResources().getDrawable(R.drawable.defaultbg));

        title.setText(playerService.getOsuAudioPlayer().getTitle());
        artist.setText(playerService.getOsuAudioPlayer().getArtist());
        if (playerService.getOsuAudioPlayer().getVersion() != null)
            version.setText(playerService.getOsuAudioPlayer().getVersion() + " by " + playerService.getOsuAudioPlayer().getMapper());
        else
            version.setText(null);

        OsuAudioPlayer player = getPlayerService().getOsuAudioPlayer();
        audioSettingFragment.update(player.getMusicVolume(), player.getSoundVolume(), player.getCurrentMod(), player.isSliderslideEnabled(), player.isSpinnerspinEnabled(), player.isSpinnerbonusEnabled());

        playlistFragment.refreshList();

        buttonPlayPause.setImageDrawable(playerService.getOsuAudioPlayer().getEngine().getPlaybackStatus() == AudioEngine.PlaybackStatus.Playing ?
                getResources().getDrawable(R.drawable.ic_pause) : getResources().getDrawable(R.drawable.ic_play));

        buttonLoopMode.setImageDrawable(playerService.getLoopMode() == PlayerService.LoopMode.All ?
                getResources().getDrawable(R.drawable.ic_refresh) : playerService.getLoopMode() == PlayerService.LoopMode.Single ?
                getResources().getDrawable(R.drawable.ic_refresh_one) : getResources().getDrawable(R.drawable.ic_refresh_r));
    }

    private void setContentSize(int height, int duration) {
        setContentSize(height, duration, false);
    }

    private void setContentSize(int height, int duration, boolean hideInfo) {
        float infoAlpha = hideInfo ? 0 : 1;

        int targetHeight;
        float targetAlpha;
        boolean targetDim;
        float targetOffset;
        if (height == 0) {
            targetHeight = 0;
            targetAlpha = 0;
            targetDim = false;
            targetOffset = 16;
        } else {
            targetHeight = height;
            targetAlpha = 1;
            targetDim = true;
            targetOffset = 0;
        }

        info.animate().alpha(infoAlpha).setInterpolator(animInterpolator).setDuration(duration).start();
        dim.animate().alpha(targetDim ? 1 : 0).setInterpolator(animInterpolator).setDuration(duration).start();

        window.animate().alpha(targetAlpha).setInterpolator(animInterpolator).setDuration(duration).start();
        info.animate().translationY(Utils.dp2px(this, targetOffset)).setInterpolator(animInterpolator).setDuration(duration).start();

        var heightAnim = ValueAnimator.ofInt(window.getMeasuredHeight(), Utils.dp2px(this, targetHeight));
        heightAnim.setInterpolator(animInterpolator);
        heightAnim.addUpdateListener(valueAnimator -> {
            int val = (Integer) valueAnimator.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = window.getLayoutParams();
            layoutParams.height = val;
            window.setLayoutParams(layoutParams);
        });
        heightAnim.setDuration(duration);
        heightAnim.start();
    }

    private void setCurrentContent(Content current) {
        this.current = current;
        int statusBarHeight = 0;
        int resourceId = getApplicationContext().getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getApplicationContext().getResources().getDimensionPixelSize(resourceId);
        }
        switch (this.current) {
            case None:
                setContentSize(0, animDuration);
                audioSettingWrapper.animate().alpha(0).withEndAction(() -> audioSettingWrapper.setVisibility(View.GONE)).start();
                playlistWrapper.animate().alpha(0).withEndAction(() -> playlistWrapper.setVisibility(View.GONE)).start();
                preferenceWrapper.animate().alpha(0).withEndAction(() -> preferenceWrapper.setVisibility(View.GONE)).start();
                break;
            case Setting:
                setContentSize(Utils.px2dp(this, findViewById(R.id.content).getMeasuredHeight() - findViewById(R.id.controllerBlur).getMeasuredHeight() - statusBarHeight) - 32, animDuration, true);
                audioSettingWrapper.animate().alpha(0).withEndAction(() -> audioSettingWrapper.setVisibility(View.GONE)).start();
                playlistWrapper.animate().alpha(0).withEndAction(() -> playlistWrapper.setVisibility(View.GONE)).start();
                preferenceWrapper.animate().alpha(1).withStartAction(() -> preferenceWrapper.setVisibility(View.VISIBLE)).start();
                break;
            case AudioSetting:
                setContentSize(192, animDuration);
                audioSettingWrapper.animate().alpha(1).withStartAction(() -> audioSettingWrapper.setVisibility(View.VISIBLE)).start();
                playlistWrapper.animate().alpha(0).withEndAction(() -> playlistWrapper.setVisibility(View.GONE)).start();
                preferenceWrapper.animate().alpha(0).withEndAction(() -> preferenceWrapper.setVisibility(View.GONE)).start();
                break;
            case Playlist:
                setContentSize(Utils.px2dp(this, findViewById(R.id.content).getMeasuredHeight() - findViewById(R.id.controllerBlur).getMeasuredHeight() - findViewById(R.id.infoLayout).getMeasuredHeight() - statusBarHeight) - 48, animDuration);
                audioSettingWrapper.animate().alpha(0).withEndAction(() -> audioSettingWrapper.setVisibility(View.GONE)).start();
                playlistWrapper.animate().alpha(1).withStartAction(() -> playlistWrapper.setVisibility(View.VISIBLE)).start();
                preferenceWrapper.animate().alpha(0).withEndAction(() -> preferenceWrapper.setVisibility(View.GONE)).start();
                break;
        }
    }

    private enum Content {
        None, Playlist, AudioSetting, Setting
    }

}
