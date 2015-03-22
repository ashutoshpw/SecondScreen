/* Copyright 2015 Braden Farmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.farmerbb.secondscreen.fragment;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.farmerbb.secondscreen.R;
import com.farmerbb.secondscreen.activity.FragmentContainerActivity;
import com.farmerbb.secondscreen.activity.TaskerQuickActionsActivity;
import com.farmerbb.secondscreen.fragment.dialog.NewProfileDialogFragment;
import com.farmerbb.secondscreen.util.U;
import com.melnykov.fab.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;

// Fragment launched as part of MainActivity that shows a list of profiles to load.
// It gathers a listing of files in the app's private "files" directory as generated by
// ProfileEditFragment, then displays the contents of those files in a ListView - in this case, the
// files contain entries for profiles created by the ProfileEditFragment.  When an entry is either
// clicked or long-pressed, the ProfileViewFragment or ProfileEditFragment is launched respectively.
public final class ProfileListFragment extends Fragment {

    // This is used by other components to refresh the list of profiles
    private final class ListNotesReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            listProfiles();
        }
    }

    private final class ProfileListAdapter extends ArrayAdapter<String> {
        public ProfileListAdapter(Context context, ArrayList<String> notes) {
            super(context, R.layout.row_layout, notes);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            String profile = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if(convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_layout, parent, false);
            }
            // Lookup view for data population
            TextView profileTitle = (TextView) convertView.findViewById(R.id.profileTitle);
            // Populate the data into the template view using the data object
            profileTitle.setText(profile);

            // Return the completed view to render on screen
            return convertView;
        }
    }

    IntentFilter filter = new IntentFilter(U.LIST_PROFILES);
    ListNotesReceiver receiver = new ListNotesReceiver();

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event call backs. */
    public interface Listener {
        public void viewProfile(String filename);
        public void editProfile(String filename);
        public SharedPreferences getPrefCurrent();
        public SharedPreferences getPrefQuickActions();
        public TextView getHelperText();
    }

    // Use this instance of the interface to deliver action events
    Listener listener;

    // Override the Fragment.onAttach() method to instantiate the Listener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the Listener so we can send events to the host
            listener = (Listener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                                         + " must implement Listener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Set values
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        if(getId() == R.id.profileViewEdit) {
            // Change window title
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                getActivity().setTitle(getResources().getString(R.string.app_name));
            else
                getActivity().setTitle(" " + getResources().getString(R.string.app_name));

            // Don't show the Up button in the action bar, and disable the button
            getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);
            getActivity().getActionBar().setHomeButtonEnabled(false);
        }

        // Refresh list of profiles onResume (instead of onCreate) to reflect additions/deletions
        listProfiles();
    }

    @Override
    public void onStart() {
        super.onStart();

        getActivity().registerReceiver(receiver, filter);

        // Floating action button
        FloatingActionButton floatingActionButton = (FloatingActionButton) getActivity().findViewById(R.id.button_floating_action);
        floatingActionButton.hide(false);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getId() == R.id.profileViewEdit) {
            floatingActionButton.show();
            floatingActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogFragment newProfileFragment = new NewProfileDialogFragment();
                    newProfileFragment.show(getFragmentManager(), "new");
                }
            });
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        getActivity().unregisterReceiver(receiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate action bar menu
        if(getId() == R.id.profileViewEdit)
            inflater.inflate(R.menu.main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch(item.getItemId()) {
            // New button
            case R.id.action_new:
                DialogFragment newProfileFragment = new NewProfileDialogFragment();
                newProfileFragment.show(getFragmentManager(), "new");
                return true;

            // Quick Actions button
            case R.id.action_quick:
                Intent intentQuick = new Intent(getActivity(), TaskerQuickActionsActivity.class);
                intentQuick.putExtra("launched-from-app", true);
                startActivity(intentQuick);
                return true;

            // About button
            case R.id.action_settings:
                Intent intentSettings = new Intent(getActivity(), FragmentContainerActivity.class);
                intentSettings.putExtra("tag", "SettingsFragment");
                startActivity(intentSettings);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void listProfiles() {
        TextView helper = (TextView) getActivity().findViewById(R.id.textView1);
        final ListView listView = (ListView) getActivity().findViewById(R.id.listView1);

        // Get array of profiles
        final String[][] profileList = U.listProfiles(getActivity());

        // If there are no saved profiles, then display the empty view
        if(profileList == null) {
            TextView empty = (TextView) getActivity().findViewById(R.id.empty);
            empty.setText(getResources().getString(R.string.no_profiles_found));
            empty.setBackgroundColor(getResources().getColor(R.color.accent));

            listView.setEmptyView(empty);

            helper.setText(" ");
            helper.setBackgroundColor(Color.WHITE);
        } else {
            final int numOfFiles = profileList[1].length;

            // Create ArrayList and populate with list of profiles
            ArrayList<String> arrayList = new ArrayList<>(numOfFiles);
            arrayList.addAll(Arrays.asList(profileList[1]));

            // Create the custom adapter to bind the array to the ListView
            final ProfileListAdapter adapter = new ProfileListAdapter(getActivity(), arrayList);

            // Display the ListView
            listView.setAdapter(adapter);
            listView.setClickable(true);
            listView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                    listener.viewProfile(profileList[0][position]);

                    // Update status of indicated item
                    SharedPreferences prefCurrent = listener.getPrefCurrent();
                    if(prefCurrent.getString("filename", "0").equals("quick_actions")) {
                        SharedPreferences prefSaved = listener.getPrefQuickActions();
                        for(int i = 0; i < numOfFiles; i++) {
                            if(profileList[0][i].equals(prefSaved.getString("original_filename", "0")))
                                listView.setItemChecked(i, true);
                            else
                                listView.setItemChecked(i, false);
                        }
                    } else {
                        for(int i = 0; i < numOfFiles; i++) {
                            if(profileList[0][i].equals(prefCurrent.getString("filename", "0")))
                                listView.setItemChecked(i, true);
                            else
                                listView.setItemChecked(i, false);
                        }
                    }

                    // Set helper text based on whether or not a profile is active
                    TextView helper = listener.getHelperText();
                    if(prefCurrent.getString("filename", "0").equals("0")) {
                        helper.setText(R.string.profile_helper_text);
                        helper.setBackgroundColor(getResources().getColor(R.color.accent));
                    } else {
                        if(prefCurrent.getString("filename", "0").equals("quick_actions")) {
                            SharedPreferences prefSaved = listener.getPrefQuickActions();
                            if(prefSaved.getString("original_filename", "0").equals("0")) {
                                helper.setText(R.string.profile_helper_text);
                                helper.setBackgroundColor(getResources().getColor(R.color.accent));
                            } else {
                                helper.setText(R.string.profile_helper_text_alt);
                                helper.setBackgroundColor(getResources().getColor(R.color.accent));
                            }
                        } else {
                            helper.setText(R.string.profile_helper_text_alt);
                            helper.setBackgroundColor(getResources().getColor(R.color.accent));
                        }
                    }
                }
            });

            // Make ListView handle long presses
            listView.setLongClickable(true);
            listView.setOnItemLongClickListener(new OnItemLongClickListener() {

                @Override
                public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                    listener.editProfile(profileList[0][position]);

                    // Update status of indicated item
                    SharedPreferences prefCurrent = listener.getPrefCurrent();
                    if(prefCurrent.getString("filename", "0").equals("quick_actions")) {
                        SharedPreferences prefSaved = listener.getPrefQuickActions();
                        for(int i = 0; i < numOfFiles; i++) {
                            if(profileList[0][i].equals(prefSaved.getString("original_filename", "0")))
                                listView.setItemChecked(i, true);
                            else
                                listView.setItemChecked(i, false);
                        }
                    } else {
                        for(int i = 0; i < numOfFiles; i++) {
                            if(profileList[0][i].equals(prefCurrent.getString("filename", "0")))
                                listView.setItemChecked(i, true);
                            else
                                listView.setItemChecked(i, false);
                        }
                    }

                    // Set helper text based on whether or not a profile is active
                    TextView helper = listener.getHelperText();
                    if(prefCurrent.getString("filename", "0").equals("0")) {
                        helper.setText(R.string.profile_helper_text);
                        helper.setBackgroundColor(getResources().getColor(R.color.accent));
                    } else {
                        if(prefCurrent.getString("filename", "0").equals("quick_actions")) {
                            SharedPreferences prefSaved = listener.getPrefQuickActions();
                            if(prefSaved.getString("original_filename", "0").equals("0")) {
                                helper.setText(R.string.profile_helper_text);
                                helper.setBackgroundColor(getResources().getColor(R.color.accent));
                            } else {
                                helper.setText(R.string.profile_helper_text_alt);
                                helper.setBackgroundColor(getResources().getColor(R.color.accent));
                            }
                        } else {
                            helper.setText(R.string.profile_helper_text_alt);
                            helper.setBackgroundColor(getResources().getColor(R.color.accent));
                        }
                    }

                    return true;
                }

            });

            // Prepare the ListView for indicating the currently loaded profile
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

            SharedPreferences prefCurrent = listener.getPrefCurrent();
            if(prefCurrent.getString("filename", "0").equals("quick_actions")) {
                SharedPreferences prefSaved = listener.getPrefQuickActions();
                for(int i = 0; i < numOfFiles; i++) {
                    if(profileList[0][i].equals(prefSaved.getString("original_filename", "0")))
                        listView.setItemChecked(i, true);
                    else
                        listView.setItemChecked(i, false);
                }
            } else {
                for(int i = 0; i < numOfFiles; i++) {
                    if(profileList[0][i].equals(prefCurrent.getString("filename", "0")))
                        listView.setItemChecked(i, true);
                    else
                        listView.setItemChecked(i, false);
                }
            }

            // Attach floating action button to ListView
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getId() == R.id.profileViewEdit) {
                FloatingActionButton floatingActionButton = (FloatingActionButton) getActivity().findViewById(R.id.button_floating_action);
                floatingActionButton.attachToListView(listView);
            }

            // Set helper text based on whether or not a profile is active
            if(prefCurrent.getString("filename", "0").equals("0")) {
                helper.setText(R.string.profile_helper_text);
                helper.setBackgroundColor(getResources().getColor(R.color.accent));
            } else {
                if(prefCurrent.getString("filename", "0").equals("quick_actions")) {
                    SharedPreferences prefSaved = listener.getPrefQuickActions();
                    if(prefSaved.getString("original_filename", "0").equals("0")) {
                        helper.setText(R.string.profile_helper_text);
                        helper.setBackgroundColor(getResources().getColor(R.color.accent));
                    } else {
                        helper.setText(R.string.profile_helper_text_alt);
                        helper.setBackgroundColor(getResources().getColor(R.color.accent));
                    }
                } else {
                    helper.setText(R.string.profile_helper_text_alt);
                    helper.setBackgroundColor(getResources().getColor(R.color.accent));
                }
            }
        }
    }
}
