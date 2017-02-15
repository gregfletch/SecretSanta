package com.gf.secretsanta;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.gf.secretsanta.model.Participant;
import com.gf.secretsanta.model.ParticipantContract.ParticipantEntry;
import com.gf.secretsanta.model.ParticipantDbHelper;

import java.util.ArrayList;
import java.util.List;

public class ParticipantActivity extends AppCompatActivity
        implements CheckboxCursorRecyclerViewAdapter.OnCheckedListener {

    private EditText m_name;
    private EditText m_emailAddress;
    private Participant m_participant;
    private Cursor m_cursor;
    private CheckboxCursorRecyclerViewAdapter m_adapter;
    private boolean m_isNew = false;

    private static final int MAXIMUM_EXCLUSIONS = 2;

    private static final String TAG = "ParticipantActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_participant);

        m_name = (EditText) findViewById(R.id.name_edittext);
        m_emailAddress = (EditText) findViewById(R.id.email_edittext);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        m_participant = (Participant) getIntent().getSerializableExtra(
                Participant.PARTICIPANT_INTENT_EXTRA_LABEL);

        if(toolbar != null) {
            if(m_participant == null) {
                toolbar.setTitle(R.string.add_participant_title);
                m_participant = new Participant();
                m_isNew = true;
            } else {
                toolbar.setTitle(R.string.edit_participant_title);
            }
        }

        ReadParticipantsTask readParticipants = new ReadParticipantsTask(this);
        readParticipants.execute();

        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if(!m_isNew) {
            m_name.setText(m_participant.getName());
            m_emailAddress.setText(m_participant.getEmailAddress());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        m_cursor.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_participants, menu);

        MenuItem deleteItem = menu.findItem(R.id.action_delete);
        deleteItem.setEnabled(m_isNew);
        deleteItem.setVisible(m_isNew);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.action_about)
                    .setMessage(R.string.about_text)
                    .setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            dialog.cancel();
                        }
                    })
                    .setNegativeButton(R.string.action_licenses,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent licenseIntent = new Intent(ParticipantActivity.this,
                                            LicenseActivity.class);
                                    startActivity(licenseIntent);
                                    dialog.dismiss();
                                    dialog.cancel();
                                }
                            })
                    .show();
            return true;
        } else if(id == R.id.action_done) {
            save();
            return true;
        } else if(id == R.id.action_delete) {
            RemoveParticipantTask removeParticipant = new RemoveParticipantTask(this);
            removeParticipant.execute();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCheckedChanged(boolean isChecked, Participant participant) {
        if(isChecked) {
            m_participant.addToExclusionList(participant.getId());
        } else {
            m_participant.removeFromExclusionList(participant.getId());
        }
    }

    private void save() {
        View content = findViewById(R.id.content_participant);

        String name = m_name.getText().toString().trim();
        if(name.equals("")) {
            Snackbar.make(content, R.string.participant_name_required, Snackbar.LENGTH_LONG)
                    .show();
            return;
        }

        String emailAddress = m_emailAddress.getText().toString().trim();
        if(emailAddress.equals("")) {
            Snackbar.make(content, R.string.participant_email_required, Snackbar.LENGTH_LONG)
                    .show();
            return;
        } else if(!Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
            Snackbar.make(content, R.string.participant_email_invalid, Snackbar.LENGTH_LONG)
                    .show();
            return;
        }

        if(m_participant.getExclusionList().size() > MAXIMUM_EXCLUSIONS) {
            Snackbar.make(content, getString(R.string.exclude_list_too_large, MAXIMUM_EXCLUSIONS),
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        m_participant.setName(name);
        m_participant.setEmailAddress(emailAddress);

        if(m_isNew) {
            AddParticipantTask addParticipant = new AddParticipantTask(this);
            addParticipant.execute();
        } else {
            UpdateParticipantTask updateParticipant = new UpdateParticipantTask(this);
            updateParticipant.execute();
        }
    }

    private void setupRecyclerView() {
        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.participant_list);

        if(recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL,
                    false));
            m_adapter.setOnCheckedChangedListener(this);
            recyclerView.setAdapter(m_adapter);
        }
    }

    private class AddParticipantTask extends AsyncTask<Void, Void, String> {
        private Context m_context;

        AddParticipantTask(Context context) {
            m_context = context;
        }

        @Override
        protected String doInBackground(Void... params) {
            SQLiteDatabase db = new ParticipantDbHelper(m_context).getWritableDatabase();

            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(ParticipantEntry.COLUMN_NAME_PARTICIPANT_ID, m_participant.getId());
            values.put(ParticipantEntry.COLUMN_NAME_PARTICIPANT_NAME, m_participant.getName());
            values.put(ParticipantEntry.COLUMN_NAME_PARTICIPANT_EMAIL_ADDRESS,
                    m_participant.getEmailAddress());

            String exclusionList = "";
            if(m_participant.getExclusionList() != null &&
                    m_participant.getExclusionList().size() > 0) {
                for (String s : m_participant.getExclusionList()) {
                    exclusionList += s + ",";
                }
                // Remove trailing comma.
                exclusionList = exclusionList.substring(0, exclusionList.length() - 1);
            }

            values.put(ParticipantEntry.COLUMN_NAME_PARTICIPANT_EXCLUSION_LIST, exclusionList);

            // Insert the new row, returning the primary key value of the new row
            long row = db.insert(ParticipantEntry.TABLE_NAME, null, values);
            return row >= 0 ? m_participant.getName() : null;
        }

        @Override
        protected void onPostExecute(String result) {
            if(result == null) {
                Log.w(TAG, "Failed to add new participant.");
                View content = findViewById(R.id.content_participant);

                if(content != null) {
                    Snackbar.make(content, getString(R.string.participant_added_failed),
                            Snackbar.LENGTH_LONG).show();
                }
            } else {
                Log.i(TAG, "Successfully added new participant with name " + result + ".");

                Intent intent = new Intent();
                intent.putExtra(Participant.PARTICIPANT_INTENT_EXTRA_LABEL, result);
                setResult(RESULT_OK, intent);
                finish();
            }
        }
    }

    private class UpdateParticipantTask extends AsyncTask<Void, Void, String> {
        private Context m_context;

        UpdateParticipantTask(Context context) {
            m_context = context;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(Void... params) {
            SQLiteDatabase db = new ParticipantDbHelper(m_context).getWritableDatabase();

            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(ParticipantEntry.COLUMN_NAME_PARTICIPANT_ID, m_participant.getId());
            values.put(ParticipantEntry.COLUMN_NAME_PARTICIPANT_NAME, m_participant.getName());
            values.put(ParticipantEntry.COLUMN_NAME_PARTICIPANT_EMAIL_ADDRESS,
                    m_participant.getEmailAddress());

            String exclusionList = "";
            if(m_participant.getExclusionList() != null &&
                    m_participant.getExclusionList().size() > 0) {
                for (String s : m_participant.getExclusionList()) {
                    exclusionList += s + ",";
                }
                // Remove trailing comma.
                exclusionList = exclusionList.substring(0, exclusionList.length() - 1);
            }

            values.put(ParticipantEntry.COLUMN_NAME_PARTICIPANT_EXCLUSION_LIST, exclusionList);

            String whereClause = ParticipantEntry.COLUMN_NAME_PARTICIPANT_ID+ " LIKE ?";
            String[] args = {m_participant.getId()};

            // Update the existing participant, returning the number of rows affected by the update
            int numAffected = db.update(ParticipantEntry.TABLE_NAME, values, whereClause, args);
            return numAffected > 0 ? m_participant.getName() : null;
        }

        @Override
        protected void onPostExecute(String result) {
            if(result == null) {
                Log.w(TAG, "Error updating participant with name " + m_participant.getName() + ".");
                View content = findViewById(R.id.content_participant);

                if(content != null) {
                    Snackbar.make(content,
                            getString(R.string.participant_updated_failed, m_participant.getName()),
                            Snackbar.LENGTH_LONG).show();
                }
            } else {
                Log.i(TAG, "Successfully updated participant with name " + result + ".");

                Intent intent = new Intent();
                intent.putExtra(Participant.PARTICIPANT_INTENT_EXTRA_LABEL, result);
                setResult(RESULT_OK, intent);
                finish();
            }
        }
    }

    private class RemoveParticipantTask extends AsyncTask<Void, Void, String> {
        private Context m_context;

        RemoveParticipantTask(Context context) {
            m_context = context;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(Void... params) {
            SQLiteDatabase db = new ParticipantDbHelper(m_context).getWritableDatabase();

            String whereClause = ParticipantEntry.COLUMN_NAME_PARTICIPANT_ID + " LIKE ?";
            String[] args = {m_participant.getId()};

            int numAffected = db.delete(ParticipantEntry.TABLE_NAME, whereClause, args);
            return numAffected > 0 ? m_participant.getName() : null;
        }

        @Override
        protected void onPostExecute(String result) {
            if(result == null) {
                Log.w(TAG, "Failed to remove participant.");
            } else {
                Log.i(TAG, "Successfully removed participant with name " + result + ".");
            }

            Intent intent = new Intent();
            intent.putExtra(Participant.PARTICIPANT_INTENT_EXTRA_LABEL, result);
            setResult(MainActivity.RESULT_DELETED, intent);
            finish();
        }
    }

    private class ReadParticipantsTask extends AsyncTask<Void, Void, Cursor> {
        private Context m_context;

        ReadParticipantsTask(Context context) {
            m_context = context;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Cursor doInBackground(Void... params) {
            SQLiteDatabase db = new ParticipantDbHelper(m_context).getReadableDatabase();

            String[] projection = {
                    ParticipantEntry._ID,
                    ParticipantEntry.COLUMN_NAME_PARTICIPANT_ID,
                    ParticipantEntry.COLUMN_NAME_PARTICIPANT_NAME,
                    ParticipantEntry.COLUMN_NAME_PARTICIPANT_EMAIL_ADDRESS
            };

            if(m_isNew) {
                return db.query(
                        ParticipantEntry.TABLE_NAME,
                        projection,
                        null,
                        null,
                        null,
                        null,
                        ParticipantEntry.COLUMN_NAME_PARTICIPANT_NAME
                );
            } else {
                String select = ParticipantEntry.COLUMN_NAME_PARTICIPANT_ID + " != ?";
                String[] selectArgs = new String[] { m_participant.getId() };

                return db.query(
                        ParticipantEntry.TABLE_NAME,
                        projection,
                        select,
                        selectArgs,
                        null,
                        null,
                        ParticipantEntry.COLUMN_NAME_PARTICIPANT_NAME
                );
            }
        }

        @Override
        protected void onPostExecute(Cursor result) {
            m_cursor = result;
            m_adapter = new CheckboxCursorRecyclerViewAdapter(ParticipantActivity.this, m_cursor,
                    m_participant != null ? m_participant.getExclusionList() : null,
                    m_participant != null ? m_participant.getId() : null);
            setupRecyclerView();
        }
    }

}
