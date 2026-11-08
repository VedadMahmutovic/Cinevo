package com.vedad.cinevo.ui.settings;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.vedad.cinevo.R;
import com.vedad.cinevo.data.repository.SavedMovieRepository;
import com.vedad.cinevo.databinding.DialogAboutBinding;
import com.vedad.cinevo.util.AppSettings;

import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.preference.PreferenceGroup;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        tintIcons(getPreferenceScreen(),
                ContextCompat.getColor(requireContext(), R.color.cinevo_icon_accent));

        Preference theme = findPreference(AppSettings.KEY_THEME);
        if (theme != null) {
            theme.setOnPreferenceChangeListener((preference, newValue) -> {
                AppCompatDelegate.setDefaultNightMode(AppSettings.themeModeFor((String) newValue));
                return true;
            });
        }

        Preference clear = findPreference("pref_clear_saved");
        if (clear != null) {
            clear.setOnPreferenceClickListener(preference -> {
                confirmClear();
                return true;
            });
        }

        Preference about = findPreference("pref_about");
        if (about != null) {
            about.setOnPreferenceClickListener(preference -> {
                showAboutDialog();
                return true;
            });
        }
    }

    private void confirmClear() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clear_dialog_title)
                .setMessage(R.string.clear_dialog_message)
                        
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    new SavedMovieRepository(requireContext()).clearAll();
                    Toast.makeText(requireContext(),
                            getString(R.string.cleared_toast), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showAboutDialog() {
        DialogAboutBinding dialogBinding = DialogAboutBinding.inflate(getLayoutInflater());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.pref_about_title)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private void tintIcons(PreferenceGroup group, int color) {
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference preference = group.getPreference(i);
            if (preference instanceof PreferenceGroup) {
                tintIcons((PreferenceGroup) preference, color);
            }
            Drawable icon = preference.getIcon();
            if (icon != null) {
                icon = DrawableCompat.wrap(icon.mutate());
                DrawableCompat.setTint(icon, color);
                preference.setIcon(icon);
            }
        }
    }
}