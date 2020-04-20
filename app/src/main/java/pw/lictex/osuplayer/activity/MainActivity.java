package pw.lictex.osuplayer.activity;

import android.*;
import android.animation.*;
import android.content.*;
import android.content.pm.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.view.animation.Interpolator;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.*;
import androidx.coordinatorlayout.widget.*;
import androidx.core.app.*;
import androidx.core.content.*;
import androidx.interpolator.view.animation.*;
import androidx.preference.*;
import androidx.recyclerview.widget.*;

import com.google.android.material.bottomsheet.*;

import butterknife.*;
import eightbitlab.com.blurview.*;
import lombok.*;
import pw.lictex.osuplayer.R;
import pw.lictex.osuplayer.*;
import pw.lictex.osuplayer.audio.*;
import pw.lictex.osuplayer.storage.*;

public class MainActivity extends AppCompatActivity {
    private static final float blurRadius = 25f;

    @BindView(R.id.controllerBlur) BlurView controllerBlur;
    @BindView(R.id.audioSettingBlur) BlurView audioSettingBlur;
    @BindView(R.id.audioSettingPanel) View audioSettingPanel;
    @BindView(R.id.contentLayout) View contentLayout;
    @BindView(R.id.content) CoordinatorLayout content;
    @BindView(R.id.backButton) ImageButton backBtn;
    @BindView(R.id.infoLayout) LinearLayout info;
    @BindView(R.id.progressBar) SeekBar seekBar;
    @BindView(R.id.backgroundImage) ImageSwitcher bg;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.artist) TextView artist;
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
    @Getter private int baseAnimationDuration;
    private ObjectAnimator progressAnimator;
    private boolean progressTouched;
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (!progressTouched) {
                progressAnimator = ObjectAnimator.ofInt(seekBar, "progress", seekBar.getProgress(), (int) (((double) playerService.getOsuAudioPlayer().getCurrentTime() / playerService.getOsuAudioPlayer().getAudioLength()) * seekBar.getMax()));
                progressAnimator.setInterpolator(new LinearOutSlowInInterpolator());
                progressAnimator.setDuration(750);
                progressAnimator.start();
            }
            handler.postDelayed(this, 1000);
        }
    };

    private Handler bottomSheetHandler = new Handler();
    private BottomSheetBehavior bottomSheet;
    private final Interpolator interpolator = new FastOutSlowInInterpolator();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        detachService();
    }

    public void detachService() {
        if (playerServiceConnection == null) return;
        unbindService(playerServiceConnection);
        playerServiceConnection = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        BeatmapIndex.getInstance().refresh();
        recreate();
    }

    @Override
    public void onBackPressed() {
        if (audioSettingPanel.getVisibility() == View.VISIBLE) {
            setAudioSettingVisibility(false);
        } else if (bottomSheet.getState() != BottomSheetBehavior.STATE_COLLAPSED) {
            if (current != Content.Playlist) setCurrentContent(Content.Playlist);
            else bottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) savedInstanceState.remove("android:support:fragments");
        super.onCreate(savedInstanceState);

        String theme = PreferenceManager.getDefaultSharedPreferences(this).getString("theme", "dark");
        switch (theme) {
            case "default":
                switch (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
                    case Configuration.UI_MODE_NIGHT_YES:
                        setTheme(R.style.DarkTheme);
                        break;
                    case Configuration.UI_MODE_NIGHT_UNDEFINED:
                    case Configuration.UI_MODE_NIGHT_NO:
                        setTheme(R.style.LightTheme);
                        break;
                }
                break;
            case "light":
                setTheme(R.style.LightTheme);
                break;
            default:
            case "dark":
                setTheme(R.style.DarkTheme);
                break;
        }

        var fastAnimation = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("fast_animation", false);
        if (fastAnimation) baseAnimationDuration = 160;
        else baseAnimationDuration = 240;

        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);

        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(R.attr.acrylicColor, value, true);
        controllerBlur.setOverlayColor(value.data);
        audioSettingBlur.setOverlayColor(value.data);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int color = Color.TRANSPARENT;
            Drawable bg = findViewById(R.id.llcbg).getBackground();
            if (bg instanceof ColorDrawable) color = ((ColorDrawable) bg).getColor();
            getWindow().setNavigationBarColor(color);
            if (color == 0xFFFFFFFF) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);

        controllerBlur.setupWith(content).setFrameClearDrawable(new ColorDrawable(0xFFFFFFFF))
                .setBlurAlgorithm(new RenderScriptBlur(this))
                .setBlurRadius(blurRadius)
                .setSaturation(1.25f).setContrast(0.8f);
        audioSettingBlur.setupWith(content).setFrameClearDrawable(new ColorDrawable(0xFFFFFFFF))
                .setBlurAlgorithm(new RenderScriptBlur(this))
                .setBlurRadius(blurRadius)
                .setSaturation(1.25f).setContrast(0.8f);

        title.setText(getResources().getString(R.string.app_name));
        artist.setText(getResources().getString(R.string.version));

        bg.setAnimationDuration(baseAnimationDuration);

        bottomSheet = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet));

        if (savedInstanceState != null) {
            getWindow().getDecorView().post(() -> setCurrentContent(Content.values()[savedInstanceState.getInt("content")]));
            bottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else getWindow().getDecorView().post(() -> setCurrentContent(Content.Playlist));

        getWindow().getDecorView().post(() -> {
            int statusBarHeight = 0;
            int resourceId = getApplicationContext().getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) statusBarHeight = getApplicationContext().getResources().getDimensionPixelSize(resourceId);

            ViewGroup.LayoutParams layoutParams = contentLayout.getLayoutParams();
            layoutParams.height = findViewById(R.id.content).getMeasuredHeight() - findViewById(R.id.llcbg).getMeasuredHeight() - findViewById(R.id.infoLayout).getMeasuredHeight() - statusBarHeight - Utils.dp2px(getBaseContext(), 40);
            contentLayout.setLayoutParams(layoutParams);
        });

        bottomSheet.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override public void onStateChanged(@NonNull View view, int i) {
                if (i == BottomSheetBehavior.STATE_COLLAPSED) {
                    Utils.clearFocus(MainActivity.this);
                    bottomSheetHandler.postDelayed(() -> {
                        setCurrentContent(Content.Playlist);
                        playlistFragment.refreshListToCurrent();
                    }, 2000);
                } else bottomSheetHandler.removeCallbacksAndMessages(null);
            }

            @Override public void onSlide(@NonNull View view, float v) { }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b) getPlayerService().getOsuAudioPlayer().seekTo((long) ((float) i / seekBar.getMax() * getPlayerService().getOsuAudioPlayer().getAudioLength()));
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {progressAnimator.cancel(); progressTouched = true; }

            @Override public void onStopTrackingTouch(SeekBar seekBar) {progressTouched = false; }
        });

        playerServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                handler.removeCallbacks(runnable);
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                playerService = ((PlayerService.PlayerServiceBinder) service).getService();
                playerService.setOnUpdateCallback(() -> { getPlayerService().rebuildNotification(); runOnUiThread(() -> updateStatus());});

                audioSettingFragment = new AudioSettingFragment();
                playlistFragment = new PlaylistFragment();
                preferenceFragment = new PreferenceFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.audio_setting_wrapper, audioSettingFragment, "audioSettingFragment")
                        .replace(R.id.playlist_wrapper, playlistFragment, "playlistFragment")
                        .replace(R.id.preference_wrapper, preferenceFragment, "preferenceFragment")
                        .commit();
                handler.post(() -> {
                    if (savedInstanceState != null) {
                        if (savedInstanceState.getInt("playlist") == 1) playlistFragment.setPlaylist(true, false);
                        String search = savedInstanceState.getString("playlistSearch");
                        if (search != null && !search.isEmpty()) {
                            playlistFragment.openSearch(false);
                            playlistFragment.searchText.setText(search);
                        }
                        playlistFragment.refreshListToPosition(savedInstanceState.getInt("playlistIndex"), savedInstanceState.getInt("playlistOffset"));
                    }
                });
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
        outState.putInt("playlist", playlistFragment.isShowCollectionList() ? 1 : 0);
        outState.putString("playlistSearch", playlistFragment.searchText.getText().toString().trim());
        outState.putInt("playlistIndex", ((LinearLayoutManager) playlistFragment.mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition());
        View v = playlistFragment.mRecyclerView.getChildAt(0);
        outState.putInt("playlistOffset", (v == null) ? 0 : (v.getTop() - playlistFragment.mRecyclerView.getPaddingTop()));
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
        Utils.runTask(() -> getPlayerService().next());
    }

    @OnClick(R.id.buttonPrev) void onPrevClick() {
        Utils.runTask(() -> getPlayerService().previous());
    }

    @OnClick(R.id.buttonLoopMode) void onLoopClick() {
        if (playerService.getLoopMode() == PlayerService.LoopMode.All)
            playerService.setLoopMode(PlayerService.LoopMode.Single);
        else if (playerService.getLoopMode() == PlayerService.LoopMode.Single)
            playerService.setLoopMode(PlayerService.LoopMode.Random);
        else if (playerService.getLoopMode() == PlayerService.LoopMode.Random)
            playerService.setLoopMode(PlayerService.LoopMode.All);
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("loop_mode", playerService.getLoopMode().ordinal()).apply();
        updateStatus();
    }

    @OnClick(R.id.buttonAudioSetting) void onAudioSettingClick() {
        if (audioSettingPanel.getAlpha() == 1) setAudioSettingVisibility(false);
        else if (audioSettingPanel.getAlpha() == 0) setAudioSettingVisibility(true);
    }

    @OnClick(R.id.buttonSetting) void onSettingClick() {
        if (current != Content.Setting) {
            setCurrentContent(Content.Setting);
            bottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else {
            if (bottomSheet.getState() == BottomSheetBehavior.STATE_COLLAPSED)
                bottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
            else setCurrentContent(Content.Playlist);
        }
        setAudioSettingVisibility(false);
    }

    @OnClick(R.id.backButton) void onBackToPlaylistClick() {
        setCurrentContent(Content.Playlist);
    }

    @OnClick(R.id.audioSettingPanelBackground) void onAudioSettingPanelBackgroundTouch() {
        setAudioSettingVisibility(false);
    }

    protected void updateStatus() {
        Bitmap background = playerService.getOsuAudioPlayer().getBackground();
        if (background != null) bg.to(background);
        else bg.to(getResources().getDrawable(R.drawable.defaultbg));

        var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String t, a;
        if (sharedPreferences.getBoolean("use_unicode_metadata", false)) {
            t = playerService.getOsuAudioPlayer().getTitle();
            a = playerService.getOsuAudioPlayer().getArtist();
        } else {
            t = playerService.getOsuAudioPlayer().getRomanisedTitle();
            a = playerService.getOsuAudioPlayer().getRomanisedArtist();
        }
        boolean displaySimpleInfo = sharedPreferences.getBoolean("display_simple_info", true);
        if (t == null || a == null) {
            title.setText(getResources().getString(R.string.app_name));
            artist.setText(getResources().getString(R.string.version));
        } else {
            if (displaySimpleInfo) {
                title.setText(t);
                artist.setText(a);
            } else {
                var s = playerService.getOsuAudioPlayer().getSource();
                title.setText(getResources().getString(R.string.title_artist, t, s != null ? s + " (" + a + ")" : a));
                artist.setText(getResources().getString(R.string.version_by_mapper, playerService.getOsuAudioPlayer().getVersion(), playerService.getOsuAudioPlayer().getMapper()));
            }
        }

        OsuAudioPlayer player = getPlayerService().getOsuAudioPlayer();
        audioSettingFragment.update(player.getMusicVolume(), player.getSoundVolume(), player.getCurrentMod());

        playlistFragment.refreshList();

        buttonPlayPause.setImageDrawable(playerService.getOsuAudioPlayer().getEngine().getPlaybackStatus() == AudioEngine.PlaybackStatus.Playing ?
                getResources().getDrawable(R.drawable.ic_pause) : getResources().getDrawable(R.drawable.ic_play));

        buttonLoopMode.setImageDrawable(playerService.getLoopMode() == PlayerService.LoopMode.All ?
                getResources().getDrawable(R.drawable.ic_refresh) : playerService.getLoopMode() == PlayerService.LoopMode.Single ?
                getResources().getDrawable(R.drawable.ic_refresh_one) : getResources().getDrawable(R.drawable.ic_refresh_r));
    }

    private void setCurrentContent(Content current) {
        boolean anim = true;
        if (bottomSheet.getState() == BottomSheetBehavior.STATE_COLLAPSED) anim = false;
        this.current = current;
        var offsetPx = Utils.dp2px(this, 24);
        switch (this.current) {
            case Setting:
                setBackArrowVisibility(true);
                playlistWrapper.animate().setInterpolator(interpolator).setDuration(anim ? baseAnimationDuration / 2 : 0).alpha(0).translationX(offsetPx).withEndAction(() -> playlistWrapper.setVisibility(View.INVISIBLE)).start();
                preferenceWrapper.animate().setInterpolator(interpolator).setDuration(anim ? baseAnimationDuration : 0).alpha(1).translationX(0).withStartAction(() -> {
                    preferenceWrapper.setTranslationX(offsetPx); preferenceWrapper.setVisibility(View.VISIBLE);
                }).start();
                break;
            case Playlist:
                setBackArrowVisibility(false);
                playlistWrapper.animate().setInterpolator(interpolator).setDuration(anim ? baseAnimationDuration : 0).alpha(1).translationX(0).withStartAction(() -> {
                    playlistWrapper.setTranslationX(-offsetPx); playlistWrapper.setVisibility(View.VISIBLE);
                }).start();
                preferenceWrapper.animate().setInterpolator(interpolator).setDuration(anim ? baseAnimationDuration / 2 : 0).alpha(0).translationX(-offsetPx).withEndAction(() -> preferenceWrapper.setVisibility(View.INVISIBLE)).start();
                break;
        }
    }

    private void setBackArrowVisibility(boolean b) {
        if (b) {
            backBtn.animate().setInterpolator(interpolator).setDuration(baseAnimationDuration).alpha(0.75f).withStartAction(() -> backBtn.setVisibility(View.VISIBLE)).start();
            info.animate().setInterpolator(interpolator).setDuration(baseAnimationDuration).translationX(Utils.dp2px(this, 24)).start();
        } else {
            backBtn.animate().setInterpolator(interpolator).setDuration(baseAnimationDuration / 2).alpha(0).withEndAction(() -> backBtn.setVisibility(View.GONE)).start();
            info.animate().setInterpolator(interpolator).setDuration(baseAnimationDuration).translationX(0).start();
        }
    }

    private void setAudioSettingVisibility(boolean b) {
        if (b) {
            audioSettingPanel.animate().setInterpolator(new LinearOutSlowInInterpolator()).setDuration(baseAnimationDuration).withStartAction(() -> audioSettingPanel.setVisibility(View.VISIBLE)).alpha(1).translationY(0).start();
            audioSettingWrapper.setAlpha(0);
            audioSettingWrapper.animate().setInterpolator(new FastOutSlowInInterpolator()).setDuration(baseAnimationDuration).alpha(1).start();
        } else {
            audioSettingPanel.animate().setInterpolator(new FastOutLinearInInterpolator()).setDuration(baseAnimationDuration / 2).alpha(0).translationY(Utils.dp2px(this, 8)).withEndAction(() -> audioSettingPanel.setVisibility(View.INVISIBLE)).start();
        }
    }

    private enum Content {
        Playlist, Setting
    }

}
