package com.gf.secretsanta;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.gf.secretsanta.model.Participant;
import com.gf.secretsanta.model.ParticipantContract.ParticipantEntry;
import com.gf.secretsanta.model.ParticipantDbHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements CursorRecyclerViewAdapter.OnItemClickListener {

    private Cursor m_cursor;
    private CursorRecyclerViewAdapter m_adapter;

    private static final int MINIMUM_NUMBER_OF_PARTICIPANTS = 3;

    public static final int ADD_PARTICIPANT_REQUEST_CODE = 1;
    public static final int UPDATE_PARTICIPANT_REQUEST_CODE = 2;
    public static final int RESULT_DELETED = -10;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent participantIntent = new Intent(MainActivity.this,
                        ParticipantActivity.class);
                startActivityForResult(participantIntent, ADD_PARTICIPANT_REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        ReadParticipantsTask readParticipants = new ReadParticipantsTask(this);
        readParticipants.execute();
    }

    @Override
    protected void onPause() {
        super.onPause();
        m_cursor.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem pickNamesItems = menu.findItem(R.id.action_pick_names);
        pickNamesItems.setEnabled(m_cursor.getCount() >= MINIMUM_NUMBER_OF_PARTICIPANTS);
        pickNamesItems.setVisible(m_cursor.getCount() >= MINIMUM_NUMBER_OF_PARTICIPANTS);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if(id == R.id.action_about) {
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
                            Intent licenseIntent = new Intent(MainActivity.this,
                                    LicenseActivity.class);
                            startActivity(licenseIntent);
                            dialog.dismiss();
                            dialog.cancel();
                        }
                    })
                    .show();
            return true;
        } else if(id == R.id.action_pick_names) {
            ArrayList<Participant> participantList = getParticipantList();
            Intent intent = new Intent(MainActivity.this, NamePickActivity.class);
            intent.putExtra(Participant.PARTICIPANT_INTENT_EXTRA_LABEL, participantList);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        View content = findViewById(R.id.content_main);

        if(resultCode == RESULT_OK) {
            if(requestCode == ADD_PARTICIPANT_REQUEST_CODE) {
                Snackbar.make(content, getString(R.string.participant_added_success,
                        data.getStringExtra(Participant.PARTICIPANT_INTENT_EXTRA_LABEL)),
                        Snackbar.LENGTH_LONG).show();
            } else if(requestCode == UPDATE_PARTICIPANT_REQUEST_CODE) {
                Snackbar.make(content, getString(R.string.participant_updated_success,
                        data.getStringExtra(Participant.PARTICIPANT_INTENT_EXTRA_LABEL)),
                        Snackbar.LENGTH_LONG).show();

            }
        } else if(resultCode == RESULT_DELETED) {
            Snackbar.make(content, getString(R.string.participant_removed_success,
                    data.getStringExtra(Participant.PARTICIPANT_INTENT_EXTRA_LABEL)),
                    Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedState) {
        super.onRestoreInstanceState(savedState);

        ReadParticipantsTask readParticipants = new ReadParticipantsTask(this);
        readParticipants.execute();
    }

    @Override
    public void onItemClick(View view, Participant participant) {
        Intent intent = new Intent(this, ParticipantActivity.class);
        intent.putExtra(Participant.PARTICIPANT_INTENT_EXTRA_LABEL, participant);
        startActivityForResult(intent, UPDATE_PARTICIPANT_REQUEST_CODE);
    }

    private void setupRecyclerView() {
        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.participant_list);

        if(recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL,
                    false));
            m_adapter.setOnItemClickListener(this);
            recyclerView.setAdapter(m_adapter);
        }

        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.START | ItemTouchHelper.END) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                RemoveParticipantTask removeParticipant =
                        new RemoveParticipantTask(MainActivity.this, position);
                removeParticipant.execute();
            }
        });

        touchHelper.attachToRecyclerView(recyclerView);
    }

    private ArrayList<Participant> getParticipantList() {
        ArrayList<Participant> participantList = new ArrayList<>();

        Cursor c = m_cursor;

        if(c.moveToFirst()) {
            while(!c.isAfterLast()) {
                String id = c.getString(c.getColumnIndex(
                        ParticipantEntry.COLUMN_NAME_PARTICIPANT_ID));
                String name = c.getString(c.getColumnIndex(
                        ParticipantEntry.COLUMN_NAME_PARTICIPANT_NAME));
                String emailAddress = c.getString(c.getColumnIndex(
                        ParticipantEntry.COLUMN_NAME_PARTICIPANT_EMAIL_ADDRESS));
                String exclusionString = c.getString(c.getColumnIndex(
                        ParticipantEntry.COLUMN_NAME_PARTICIPANT_EXCLUSION_LIST));

                Participant participant = new Participant();
                participant.setId(id);
                participant.setName(name);
                participant.setEmailAddress(emailAddress);

                List<String> exclusionList = new ArrayList<>();
                Collections.addAll(exclusionList, exclusionString.split(","));
                participant.setExclusionList(exclusionList);

                participantList.add(participant);

                c.moveToNext();
            }
        }

        return participantList;
    }

    private class ReadParticipantsTask extends AsyncTask<Void, Void, Void> {
        private Context m_context;

        ReadParticipantsTask(Context context) {
            m_context = context;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... params) {
            SQLiteDatabase db = new ParticipantDbHelper(m_context).getReadableDatabase();

            String[] projection = {
                    ParticipantEntry._ID,
                    ParticipantEntry.COLUMN_NAME_PARTICIPANT_ID,
                    ParticipantEntry.COLUMN_NAME_PARTICIPANT_NAME,
                    ParticipantEntry.COLUMN_NAME_PARTICIPANT_EMAIL_ADDRESS,
                    ParticipantEntry.COLUMN_NAME_PARTICIPANT_EXCLUSION_LIST
            };

            m_cursor = db.query(
                    ParticipantEntry.TABLE_NAME,
                    projection,
                    null,
                    null,
                    null,
                    null,
                    ParticipantEntry.COLUMN_NAME_PARTICIPANT_NAME
            );

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            invalidateOptionsMenu();
            m_adapter = new CursorRecyclerViewAdapter(MainActivity.this, m_cursor);
            setupRecyclerView();
        }
    }

    private class RemoveParticipantTask extends AsyncTask<Void, Void, String> {
        private Context m_context;
        private int m_position;

        RemoveParticipantTask(Context context, int position) {
            m_context = context;
            m_position = position;
        }

        @Override
        protected String doInBackground(Void... params) {
            SQLiteDatabase db = new ParticipantDbHelper(m_context).getWritableDatabase();
            Participant participant = m_adapter.getItem(m_position);

            String whereClause = ParticipantEntry.COLUMN_NAME_PARTICIPANT_ID + " LIKE ?";
            String[] args = {participant.getId()};

            int numAffected = db.delete(ParticipantEntry.TABLE_NAME, whereClause, args);
            return numAffected > 0 ? participant.getName() : null;
        }

        @Override
        protected void onPostExecute(String result) {
            if(result == null) {
                Log.w(TAG, "Failed to remove participant.");
            } else {
                Log.i(TAG, "Successfully removed participant with name " + result + ".");
                m_adapter.notifyItemRemoved(m_position);

                View content = findViewById(R.id.content_main);
                Snackbar.make(content, getString(R.string.participant_removed_success, result),
                        Snackbar.LENGTH_LONG).show();

                // Re-read the data to update the Cursor.
                ReadParticipantsTask readParticipants = new ReadParticipantsTask(m_context);
                readParticipants.execute();
            }
        }
    }

}
