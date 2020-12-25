package pw.lictex.osuplayer.activity;

import android.*;
import android.animation.*;
import android.content.*;
import android.content.pm.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.provider.*;
import android.util.*;
import android.view.*;
import android.view.animation.Interpolator;
import android.widget.*;

import androidx.annotation.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.*;
import androidx.core.app.*;
import androidx.core.content.*;
import androidx.core.content.res.*;
import androidx.interpolator.view.animation.*;
import androidx.preference.*;
import androidx.recyclerview.widget.*;

import com.google.android.material.bottomsheet.*;

import eightbitlab.com.blurview.*;
import lombok.*;
import pw.lictex.osuplayer.R;
import pw.lictex.osuplayer.*;
import pw.lictex.osuplayer.audio.*;
import pw.lictex.osuplayer.databinding.*;
import pw.lictex.osuplayer.storage.*;

public class MainActivity extends AppCompatActivity {
    private static final float blurRadius = 25f;

    private ActivityHomeBinding views;
    private final Handler handler = new Handler();
    private final Handler bottomSheetHandler = new Handler();
    private final Interpolator interpolator = new FastOutSlowInInterpolator();
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (!progressTouched) {
                progressAnimator = ObjectAnimator.ofInt(views.progressBar, "progress", views.progressBar.getProgress(), (int) (((double) playerService.getOsuAudioPlayer().getCurrentTime() / playerService.getOsuAudioPlayer().getAudioLength()) * views.progressBar.getMax()));
                progressAnimator.setInterpolator(new LinearOutSlowInInterpolator());
                progressAnimator.setDuration(750);
                progressAnimator.start();
            }
            handler.postDelayed(this, 1000);
        }
    };

    private Content current = Content.Playlist;
    private ServiceConnection playerServiceConnection;
    private AudioSettingFragment audioSettingFragment;
    private PlaylistFragment playlistFragment;
    private PreferenceFragment preferenceFragment;
    @Getter private PlayerService playerService;
    @Getter private int baseAnimationDuration;
    private ObjectAnimator progressAnimator;
    private boolean progressTouched;
    private BottomSheetBehavior<View> bottomSheet;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        bottomSheetHandler.removeCallbacksAndMessages(null);
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0x8086) onRequestPermissionsResult(requestCode, new String[0], new int[0]);
    }

    @Override
    public void onBackPressed() {
        if (views.audioSettingPanel.getVisibility() == View.VISIBLE) {
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

        views = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(views.getRoot());

        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(R.attr.acrylicColor, value, true);
        views.controllerBlur.setOverlayColor(value.data);
        views.audioSettingBlur.setOverlayColor(value.data);

        var controller = findViewById(R.id.llcbg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int color = Color.TRANSPARENT;
            Drawable bg = controller.getBackground();
            if (bg instanceof ColorDrawable) color = ((ColorDrawable) bg).getColor();
            getWindow().setNavigationBarColor(color);
            if (color == 0xFFFFFFFF) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager())
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION), 0x8086);

        views.controllerBlur.setupWith(views.content).setFrameClearDrawable(new ColorDrawable(0xFFFFFFFF))
                .setBlurAlgorithm(new RenderScriptBlur(this))
                .setBlurRadius(blurRadius)
                .setSaturation(1.25f).setContrast(0.8f);
        views.audioSettingBlur.setupWith(views.content).setFrameClearDrawable(new ColorDrawable(0xFFFFFFFF))
                .setBlurAlgorithm(new RenderScriptBlur(this))
                .setBlurRadius(blurRadius)
                .setSaturation(1.25f).setContrast(0.8f);

        views.title.setText(getResources().getString(R.string.app_name));
        views.artist.setText(getResources().getString(R.string.version));

        views.backgroundImage.setAnimationDuration(baseAnimationDuration);

        bottomSheet = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet));

        if (savedInstanceState != null) {
            getWindow().getDecorView().post(() -> setCurrentContent(Content.values()[savedInstanceState.getInt("content")]));
            bottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else getWindow().getDecorView().post(() -> setCurrentContent(Content.Playlist));

        getWindow().getDecorView().post(() -> {
            int statusBarHeight = 0;
            int resourceId = getApplicationContext().getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) statusBarHeight = getApplicationContext().getResources().getDimensionPixelSize(resourceId);

            ViewGroup.LayoutParams layoutParams = views.contentLayout.getLayoutParams();
            layoutParams.height = findViewById(R.id.content).getMeasuredHeight() - controller.getMeasuredHeight() - findViewById(R.id.infoLayout).getMeasuredHeight() - statusBarHeight - Utils.dp2px(getBaseContext(), 40);
            views.contentLayout.setLayoutParams(layoutParams);
        });

        bottomSheet.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override public void onStateChanged(@NonNull View view, int i) {
                if (i == BottomSheetBehavior.STATE_COLLAPSED) {
                    Utils.clearFocus(MainActivity.this);
                    bottomSheetHandler.postDelayed(() -> {
                        setCurrentContent(Content.Playlist);
                        playlistFragment.setPlaylist(playerService.isPlayCollectionList(), true);
                    }, 2000);
                } else bottomSheetHandler.removeCallbacksAndMessages(null);
            }

            @Override public void onSlide(@NonNull View view, float v) { }
        });

        views.buttonPlayPause.setOnClickListener(this::onPauseClick);
        views.buttonNext.setOnClickListener(this::onNextClick);
        views.buttonPrev.setOnClickListener(this::onPrevClick);
        views.buttonLoopMode.setOnClickListener(this::onLoopClick);
        views.buttonAudioSetting.setOnClickListener(this::onAudioSettingClick);
        views.buttonSetting.setOnClickListener(this::onSettingClick);
        views.backButton.setOnClickListener(this::onBackToPlaylistClick);
        views.audioSettingPanelBackground.setOnClickListener(this::onAudioSettingPanelBackgroundTouch);

        views.progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
                        playlistFragment.setPlaylist(savedInstanceState.getInt("playlist") == 1, false);
                        String search = savedInstanceState.getString("playlistSearch");
                        if (search != null && !search.isEmpty()) {
                            playlistFragment.openSearch(false);
                            playlistFragment.getViews().searchText.setText(search);
                        }
                        playlistFragment.refreshListToPosition(savedInstanceState.getInt("playlistIndex"), savedInstanceState.getInt("playlistOffset"));
                        preferenceFragment.getView().post(() -> ((ScrollView) preferenceFragment.getView()).smoothScrollTo(0, savedInstanceState.getInt("settingOffset")));
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
        outState.putString("playlistSearch", playlistFragment.getViews().searchText.getText().toString().trim());
        outState.putInt("playlistIndex", ((LinearLayoutManager) playlistFragment.getViews().recyclerView.getLayoutManager()).findFirstVisibleItemPosition());
        View v = playlistFragment.getViews().recyclerView.getChildAt(0);
        outState.putInt("playlistOffset", (v == null) ? 0 : (v.getTop() - playlistFragment.getViews().recyclerView.getPaddingTop()));
        outState.putInt("settingOffset", preferenceFragment.getView().getScrollY());
    }

    private void onPauseClick(View v) {
        if (playerService.getOsuAudioPlayer().getEngine().getPlaybackStatus() == AudioEngine.PlaybackStatus.Playing)
            getPlayerService().pause();
        else {
            getPlayerService().resume();
        }
        updateStatus();
    }

    private void onNextClick(View v) {
        Utils.runTask(() -> getPlayerService().next());
    }

    private void onPrevClick(View v) {
        Utils.runTask(() -> getPlayerService().previous());
    }

    private void onLoopClick(View v) {
        if (playerService.getLoopMode() == PlayerService.LoopMode.All)
            playerService.setLoopMode(PlayerService.LoopMode.Single);
        else if (playerService.getLoopMode() == PlayerService.LoopMode.Single)
            playerService.setLoopMode(PlayerService.LoopMode.Random);
        else if (playerService.getLoopMode() == PlayerService.LoopMode.Random)
            playerService.setLoopMode(PlayerService.LoopMode.All);
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("loop_mode", playerService.getLoopMode().ordinal()).apply();
        updateStatus();
    }

    private void onAudioSettingClick(View v) {
        if (views.audioSettingPanel.getAlpha() == 1) setAudioSettingVisibility(false);
        else if (views.audioSettingPanel.getAlpha() == 0) setAudioSettingVisibility(true);
    }

    private void onSettingClick(View v) {
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

    private void onBackToPlaylistClick(View v) {
        setCurrentContent(Content.Playlist);
    }

    private void onAudioSettingPanelBackgroundTouch(View v) {
        setAudioSettingVisibility(false);
    }

    protected void updateStatus() {
        Bitmap background = playerService.getOsuAudioPlayer().getBackground();
        if (background != null) views.backgroundImage.to(background);
        else views.backgroundImage.to(ResourcesCompat.getDrawable(getResources(), R.drawable.defaultbg, null));

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
            views.title.setText(getResources().getString(R.string.app_name));
            views.artist.setText(getResources().getString(R.string.version));
        } else {
            if (displaySimpleInfo) {
                views.title.setText(t);
                views.artist.setText(a);
            } else {
                var s = playerService.getOsuAudioPlayer().getSource();
                views.title.setText(getResources().getString(R.string.title_artist, t, s != null ? s + " (" + a + ")" : a));
                views.artist.setText(getResources().getString(R.string.version_by_mapper, playerService.getOsuAudioPlayer().getVersion(), playerService.getOsuAudioPlayer().getMapper()));
            }
        }

        OsuAudioPlayer player = getPlayerService().getOsuAudioPlayer();
        audioSettingFragment.update(player.getMusicVolume(), player.getSoundVolume(), player.getCurrentMod());

        playlistFragment.refreshList();

        views.buttonPlayPause.setImageDrawable(playerService.getOsuAudioPlayer().getEngine().getPlaybackStatus() == AudioEngine.PlaybackStatus.Playing ?
                ResourcesCompat.getDrawable(getResources(), R.drawable.ic_pause, null) : ResourcesCompat.getDrawable(getResources(), R.drawable.ic_play, null));

        views.buttonLoopMode.setImageDrawable(playerService.getLoopMode() == PlayerService.LoopMode.All ?
                ResourcesCompat.getDrawable(getResources(), R.drawable.ic_refresh, null) : playerService.getLoopMode() == PlayerService.LoopMode.Single ?
                ResourcesCompat.getDrawable(getResources(), R.drawable.ic_refresh_one, null) : ResourcesCompat.getDrawable(getResources(), R.drawable.ic_refresh_r, null));
    }

    private void setCurrentContent(Content current) {
        boolean anim = true;
        if (bottomSheet.getState() == BottomSheetBehavior.STATE_COLLAPSED) anim = false;
        this.current = current;
        var offsetPx = Utils.dp2px(this, 24);
        switch (this.current) {
            case Setting:
                setBackArrowVisibility(true);
                views.playlistWrapper.animate().setInterpolator(interpolator).setDuration(anim ? baseAnimationDuration / 2 : 0).alpha(0).translationX(offsetPx).withEndAction(() -> views.playlistWrapper.setVisibility(View.INVISIBLE)).start();
                views.preferenceWrapper.animate().setInterpolator(interpolator).setDuration(anim ? baseAnimationDuration : 0).alpha(1).translationX(0).withStartAction(() -> {
                    views.preferenceWrapper.setTranslationX(offsetPx); views.preferenceWrapper.setVisibility(View.VISIBLE);
                }).start();
                break;
            case Playlist:
                setBackArrowVisibility(false);
                views.playlistWrapper.animate().setInterpolator(interpolator).setDuration(anim ? baseAnimationDuration : 0).alpha(1).translationX(0).withStartAction(() -> {
                    views.playlistWrapper.setTranslationX(-offsetPx); views.playlistWrapper.setVisibility(View.VISIBLE);
                }).start();
                views.preferenceWrapper.animate().setInterpolator(interpolator).setDuration(anim ? baseAnimationDuration / 2 : 0).alpha(0).translationX(-offsetPx).withEndAction(() -> views.preferenceWrapper.setVisibility(View.INVISIBLE)).start();
                break;
        }
    }

    private void setBackArrowVisibility(boolean b) {
        if (b) {
            views.backButton.animate().setInterpolator(interpolator).setDuration(baseAnimationDuration).alpha(0.75f).withStartAction(() -> views.backButton.setVisibility(View.VISIBLE)).start();
            views.infoLayout.animate().setInterpolator(interpolator).setDuration(baseAnimationDuration).translationX(Utils.dp2px(this, 24)).start();
        } else {
            views.backButton.animate().setInterpolator(interpolator).setDuration(baseAnimationDuration / 2).alpha(0).withEndAction(() -> views.backButton.setVisibility(View.GONE)).start();
            views.infoLayout.animate().setInterpolator(interpolator).setDuration(baseAnimationDuration).translationX(0).start();
        }
    }

    private void setAudioSettingVisibility(boolean b) {
        if (b) {
            views.audioSettingPanel.animate().setInterpolator(new LinearOutSlowInInterpolator()).setDuration(baseAnimationDuration).withStartAction(() -> views.audioSettingPanel.setVisibility(View.VISIBLE)).alpha(1).translationY(0).start();
            views.audioSettingWrapper.setAlpha(0);
            views.audioSettingWrapper.animate().setInterpolator(new FastOutSlowInInterpolator()).setDuration(baseAnimationDuration).alpha(1).start();
        } else {
            views.audioSettingPanel.animate().setInterpolator(new FastOutLinearInInterpolator()).setDuration(baseAnimationDuration / 2).alpha(0).translationY(Utils.dp2px(this, 8)).withEndAction(() -> views.audioSettingPanel.setVisibility(View.INVISIBLE)).start();
        }
    }

    private enum Content {
        Playlist, Setting
    }
}
