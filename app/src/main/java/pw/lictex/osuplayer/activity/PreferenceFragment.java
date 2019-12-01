package pw.lictex.osuplayer.activity;

import android.content.*;
import android.os.*;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.*;
import androidx.preference.*;

import lombok.*;
import pw.lictex.osuplayer.R;
import pw.lictex.osuplayer.*;
import pw.lictex.osuplayer.storage.*;


public class PreferenceFragment extends PreferenceFragmentCompat {

    private SharedPreferences sharedPreference;
    private SharedPreferences.OnSharedPreferenceChangeListener listener = (sp, k) -> {
        MainActivity activity = (MainActivity) getActivity();
        switch (k) {
            case "storage_path":
                BeatmapIndex.getInstance().refresh();
                break;
            case "audio_latency":
            case "storyboard_sound_volume":
            case "nightcore_sound_volume":
            case "sliderslide_enabled":
            case "slidertick_enabled":
            case "spinnerspin_enabled":
            case "spinnerbonus_enabled":
                activity.getPlayerService().getOsuAudioPlayer().reloadSetting();
                break;
            case "use_unicode_metadata":
            case "display_simple_info":
                activity.getPlayerService().rebuildNotification();
                activity.updateStatus();
                break;
            case "theme":
            case "fast_animation":
                activity.recreate();
                break;
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        var scrollView = new ScrollView(getActivity());
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        scrollView.addView(super.onCreateView(inflater, container, savedInstanceState));
        scrollView.setClipToPadding(false);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setPadding(0, 0, 0, Utils.dp2px(getContext(), 16));

        findPreference("rebuild_database").setOnPreferenceClickListener(preference -> {
            BeatmapIndex.getInstance().refresh(); return true;
        });
        return scrollView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
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
