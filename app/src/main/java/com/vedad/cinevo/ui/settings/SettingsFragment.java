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

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

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
                .setTitle("Obriši spremljene filmove?")
                .setMessage("Svi favoriti i filmovi s liste za gledanje bit će obrisani. "
                        + "Ovo se ne može poništiti.")
                .setPositiveButton("Obriši", (dialog, which) -> {
                    new SavedMovieRepository(requireContext()).clearAll();
                    Toast.makeText(requireContext(),
                            "Spremljeni filmovi su obrisani", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Odustani", null)
                .show();
    }

    private void showAboutDialog() {
        DialogAboutBinding dialogBinding = DialogAboutBinding.inflate(getLayoutInflater());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("O aplikaciji")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("U redu", null)
                .show();
    }
}