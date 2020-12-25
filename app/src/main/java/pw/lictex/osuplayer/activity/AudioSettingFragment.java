package pw.lictex.osuplayer.activity;

import android.os.*;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.*;
import androidx.preference.*;

import lombok.*;
import pw.lictex.osuplayer.R;
import pw.lictex.osuplayer.audio.*;
import pw.lictex.osuplayer.databinding.*;

public class AudioSettingFragment extends Fragment {
    private FragmentAudiosettingBinding views;

    private void setModCheckBox(OsuAudioPlayer.Mod m) {
        views.checkboxDT.setChecked(false);
        views.checkboxHT.setChecked(false);
        views.checkboxNC.setChecked(false);
        switch (m) {
            case DT:
                views.checkboxDT.setChecked(true);
                break;
            case HT:
                views.checkboxHT.setChecked(true);
                break;
            case NC:
                views.checkboxNC.setChecked(true);
                break;
        }
    }

    public void update(int musicVol, int soundVol, OsuAudioPlayer.Mod mod) {
        views.seekBarMusicVolume.setProgress(musicVol);
        views.seekBarSoundVolume.setProgress(soundVol);

        setModCheckBox(mod);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_audiosetting, container, false);
        views = FragmentAudiosettingBinding.bind(view);

        var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        CompoundButton.OnCheckedChangeListener listener = (compoundButton, b) -> {
            if (!views.checkboxDT.isChecked() && !views.checkboxNC.isChecked() && !views.checkboxHT.isChecked()) {
                AudioSettingFragment.this.setModCheckBox(OsuAudioPlayer.Mod.None);
                ((MainActivity) getActivity()).getPlayerService().getOsuAudioPlayer().setMod(OsuAudioPlayer.Mod.None);
                return;
            }
            if (b) {
                if (compoundButton != views.checkboxDT) views.checkboxDT.setChecked(false);
                if (compoundButton != views.checkboxNC) views.checkboxNC.setChecked(false);
                if (compoundButton != views.checkboxHT) views.checkboxHT.setChecked(false);
                var current = compoundButton == views.checkboxDT ? OsuAudioPlayer.Mod.DT :
                        compoundButton == views.checkboxNC ? OsuAudioPlayer.Mod.NC :
                                compoundButton == views.checkboxHT ? OsuAudioPlayer.Mod.HT : OsuAudioPlayer.Mod.None;
                AudioSettingFragment.this.setModCheckBox(current);
                ((MainActivity) getActivity()).getPlayerService().getOsuAudioPlayer().setMod(current);
            }
        };
        views.checkboxDT.setOnCheckedChangeListener(listener);
        views.checkboxNC.setOnCheckedChangeListener(listener);
        views.checkboxHT.setOnCheckedChangeListener(listener);

        views.seekBarMusicVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sharedPreferences.edit().putInt("music_volume", i).apply();
                ((MainActivity) getActivity()).getPlayerService().getOsuAudioPlayer().reloadSetting();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        views.seekBarSoundVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sharedPreferences.edit().putInt("sound_volume", i).apply();
                ((MainActivity) getActivity()).getPlayerService().getOsuAudioPlayer().reloadSetting();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        return view;
    }
}
