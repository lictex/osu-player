package pw.lictex.osuplayer.activity;

import android.os.*;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.*;
import androidx.preference.*;

import com.h6ah4i.android.widget.verticalseekbar.*;

import butterknife.*;
import lombok.*;
import pw.lictex.osuplayer.R;
import pw.lictex.osuplayer.audio.*;


public class AudioSettingFragment extends Fragment {
    @BindView(pw.lictex.osuplayer.R.id.checkboxSliderslide) protected CheckBox checkBoxSliderslide;
    @BindView(R.id.checkboxSpinnerspin) protected CheckBox checkBoxSpinnerspin;
    @BindView(R.id.checkboxSpinnerBonus) protected CheckBox checkBoxSpinnerBonus;
    @BindView(R.id.seekBarMusicVolume) VerticalSeekBar seekBarMusicVolume;
    @BindView(R.id.seekBarSoundVolume) VerticalSeekBar seekBarSoundVolume;
    @BindView(R.id.checkboxDT) CheckBox checkBoxDt;
    @BindView(R.id.checkboxNC) CheckBox checkBoxNc;
    @BindView(R.id.checkboxHT) CheckBox checkBoxHt;

    private void setModCheckBox(OsuAudioPlayer.Mod m) {
        checkBoxDt.setChecked(false);
        checkBoxDt.setChecked(false);
        checkBoxDt.setChecked(false);
        switch (m) {
            case DT:
                checkBoxDt.setChecked(true);
                break;
            case HT:
                checkBoxHt.setChecked(true);
                break;
            case NC:
                checkBoxNc.setChecked(true);
                break;
        }
    }

    public void update(int musicVol, int soundVol, OsuAudioPlayer.Mod mod, boolean sliderslide, boolean spinnerspin, boolean spinnerbonus) {
        seekBarMusicVolume.setProgress(musicVol);
        seekBarSoundVolume.setProgress(soundVol);
        checkBoxSliderslide.setChecked(sliderslide);
        checkBoxSpinnerspin.setChecked(spinnerspin);
        checkBoxSpinnerBonus.setChecked(spinnerbonus);

        setModCheckBox(mod);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_audiosetting, container, false);
        ButterKnife.bind(this, view);

        var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        CompoundButton.OnCheckedChangeListener listener = (compoundButton, b) -> {
            if (!checkBoxDt.isChecked() && !checkBoxNc.isChecked() && !checkBoxHt.isChecked()) {
                AudioSettingFragment.this.setModCheckBox(OsuAudioPlayer.Mod.None);
                ((MainActivity) getActivity()).getPlayerService().getOsuAudioPlayer().setMod(OsuAudioPlayer.Mod.None);
                return;
            }
            if (b) {
                if (compoundButton != checkBoxDt) checkBoxDt.setChecked(false);
                if (compoundButton != checkBoxNc) checkBoxNc.setChecked(false);
                if (compoundButton != checkBoxHt) checkBoxHt.setChecked(false);
                var current = compoundButton == checkBoxDt ? OsuAudioPlayer.Mod.DT :
                        compoundButton == checkBoxNc ? OsuAudioPlayer.Mod.NC :
                                compoundButton == checkBoxHt ? OsuAudioPlayer.Mod.HT : OsuAudioPlayer.Mod.None;
                AudioSettingFragment.this.setModCheckBox(current);
                ((MainActivity) getActivity()).getPlayerService().getOsuAudioPlayer().setMod(current);
            }
        };
        checkBoxDt.setOnCheckedChangeListener(listener);
        checkBoxNc.setOnCheckedChangeListener(listener);
        checkBoxHt.setOnCheckedChangeListener(listener);

        checkBoxSliderslide.setOnCheckedChangeListener((a, b) -> {
            sharedPreferences.edit().putBoolean("sliderslide_enabled", b).apply();
            ((MainActivity) getActivity()).getPlayerService().getOsuAudioPlayer().reloadSetting();
        });
        checkBoxSpinnerspin.setOnCheckedChangeListener((a, b) -> {
            sharedPreferences.edit().putBoolean("spinnerspin_enabled", b).apply();
            ((MainActivity) getActivity()).getPlayerService().getOsuAudioPlayer().reloadSetting();
        });
        checkBoxSpinnerBonus.setOnCheckedChangeListener((a, b) -> {
            sharedPreferences.edit().putBoolean("spinnerbonus_enabled", b).apply();
            ((MainActivity) getActivity()).getPlayerService().getOsuAudioPlayer().reloadSetting();
        });

        seekBarMusicVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sharedPreferences.edit().putInt("music_volume", i).apply();
                ((MainActivity) getActivity()).getPlayerService().getOsuAudioPlayer().reloadSetting();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        seekBarSoundVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
