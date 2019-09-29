package pw.lictex.osuplayer.activity;

import android.content.*;
import android.os.*;
import android.widget.*;

import androidx.annotation.*;
import androidx.preference.*;
import androidx.recyclerview.widget.*;

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
            case "nightcore_sound_volume":
                activity.getPlayerService().getOsuAudioPlayer().reloadSetting();
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
