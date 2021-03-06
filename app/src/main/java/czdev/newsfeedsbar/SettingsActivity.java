package czdev.newsfeedsbar;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import java.util.List;
import java.util.Locale;
import static czdev.newsfeedsbar.Constants.*;
import static czdev.newsfeedsbar.NewsFeedsBar.mPrefs;


/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {


    public static MultiSelectListPreference listPreference = null;
    public static Boolean previewEnabled = false;
    public static Context mContext = null;
    public static Activity settingsActivity = null;

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if(preference.getKey().equals("show_preview")) {
                if(Boolean.parseBoolean(stringValue))
                {
                    previewEnabled = true;
                    NewsFeedsBar.restartServiceNews(false);
                }else
                {
                    NewsFeedsBar.stopServiceNews();
                    previewEnabled = false;

                }
            }

            else if(preference.getKey().equals("news_bar_display_position") ||
                    preference.getKey().equals("news_bar_display_speed") ||
                    preference.getKey().equals("news_bar_text_style") ||
                    preference.getKey().equals("news_bar_display_text_size")
            ) {

                if (previewEnabled) {
                    NewsFeedsBar.restartServiceNews(false);
                }else
                {
                    NewsFeedsBar.stopServiceNews();
                }

            }

            else if(preference.getKey().equals("news_bar_lang")||
                    preference.getKey().equals("news_bar_resources") ||
                    preference.getKey().equals("news_day")
                    ) {

                    mPrefs.edit().putString("refresh_requested", "Yes").apply();

                if (previewEnabled) {
                        NewsFeedsBar.restartServiceNews(true);
                    }else
                    {
                        NewsFeedsBar.stopServiceNews();
                    }
                }

            else if(preference.getKey().equals("app_lang")){
            //TODO Change application language
                changeLanguageTo(stringValue);
            }


            return true;
        }
    };
            /**
             * Helper method to determine if the device has an extra-large screen. For
             * example, 10" tablets are extra-large.
             */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    private static void changeLanguageTo(String lang) {

        Log.d(TAG_LOG, "Change Language " + lang );

        Locale myLocale = new Locale(lang);
        Resources res =   mContext.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);

        Intent refresh = new Intent(mContext, SettingsActivity.class);
        mContext.startActivity(refresh);
        settingsActivity.finish();
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        mContext = this;
        settingsActivity = this;
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || DisplayPreferenceFragment.class.getName().equals(fragmentName)
                || AboutPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            listPreference = (MultiSelectListPreference) findPreference("news_bar_resources");
            bindPreferenceSummaryToValue(findPreference("show_preview"));
            bindPreferenceSummaryToValue(findPreference("app_lang"));
            bindPreferenceSummaryToValue(findPreference("news_bar_lang"));
            bindPreferenceSummaryToValue(findPreference("news_bar_resources"));
            bindPreferenceSummaryToValue(findPreference("news_bar_refresh_delay"));
            bindPreferenceSummaryToValue(listPreference);
            bindPreferenceSummaryToValue(findPreference("news_day"));

        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {

            int id = item.getItemId();

            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DisplayPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_display);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference("show_preview"));
            bindPreferenceSummaryToValue(findPreference("news_bar_display_position"));
            bindPreferenceSummaryToValue(findPreference("news_bar_display_speed"));
            bindPreferenceSummaryToValue(findPreference("news_bar_text_style"));
            bindPreferenceSummaryToValue(findPreference("news_bar_display_text_size"));

        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {

            int id = item.getItemId();
            Log.d(TAG_LOG, " GeneralPreferenceFragment onOptionsItemSelected    " + id  );

            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

     /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class AboutPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_about);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
