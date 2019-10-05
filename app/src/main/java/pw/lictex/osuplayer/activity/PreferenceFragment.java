package pw.lictex.osuplayer.activity;

import android.content.*;
import android.os.*;

import androidx.annotation.*;
import androidx.preference.*;

import pw.lictex.osuplayer.R;
import pw.lictex.osuplayer.storage.*;


public class PreferenceFragment extends PreferenceFragmentCompat {

    private SharedPreferences sharedPreference;
    private SharedPreferences.OnSharedPreferenceChangeListener listener = (sp, k) -> {
        MainActivity activity = (MainActivity) getActivity();
        switch (k) {
            case "storage_path":
                BeatmapIndex.Build(getContext());
                ((PlaylistFragment) activity.getSupportFragmentManager().findFragmentByTag("playlistFragment")).rebuildList();
                break;
            case "audio_latency":
            case "storyboard_sound_volume":
            case "nightcore_sound_volume":
            case "sliderslide_enabled":
            case "spinnerspin_enabled":
            case "spinnerbonus_enabled":
                activity.getPlayerService().getOsuAudioPlayer().reloadSetting();
                break;
            case "use_unicode_metadata":
                activity.getPlayerService().rebuildNotification();
                activity.updateStatus();
                break;
        }
    };

    @Override public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setDivider(null);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference, rootKey);
        sharedPreference = getPreferenceScreen().getSharedPreferences();
        sharedPreference.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        sharedPreference.unregisterOnSharedPreferenceChangeListener(listener);
    }
}
