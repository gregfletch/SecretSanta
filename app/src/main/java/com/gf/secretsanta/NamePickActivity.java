package com.gf.secretsanta;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.gf.secretsanta.model.Participant;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NamePickActivity extends AppCompatActivity {

    private static final int MAXIMUM_ATTEMPTS = 1000;
    private static final String EMAIL_TYPE_STRING = "message/rfc822";

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_name_pick);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if(toolbar != null) {
            toolbar.setTitle(R.string.name_pick_title);
            setSupportActionBar(toolbar);

            if(getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        final ArrayList<Participant> participantList = (ArrayList<Participant>) getIntent().
                getSerializableExtra(Participant.PARTICIPANT_INTENT_EXTRA_LABEL);

        TextView textArea = (TextView) findViewById(R.id.name_pick_result);
        final Map<String, String> pairings = pickNames(participantList);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        if(pairings != null && pairings.size() > 0) {
            String pairingText = "";
            for(Map.Entry<String, String> entry : pairings.entrySet()) {
                Participant giver = getParticipantFromId(entry.getKey(), participantList);
                Participant recipient = getParticipantFromId(entry.getValue(), participantList);
                pairingText += getString(R.string.pairing_message, giver.toString(),
                        recipient.toString()) + "\n\n";
            }

            final String emailText = pairingText.trim();
            textArea.setText(pairingText.trim());

            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    composeEmail(participantList, emailText);
                }
            });
        } else {
            // Display error and return to main activity, if we could not generate valid pairings
            // for the Secret Santa.
            new AlertDialog.Builder(this)
                    .setTitle(R.string.error_dialog_title)
                    .setMessage(R.string.pairing_error_message)
                    .setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            dialog.cancel();
                            finish();
                        }
                    })
                    .show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
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
        pickNamesItems.setEnabled(false);
        pickNamesItems.setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

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
                                    Intent licenseIntent = new Intent(NamePickActivity.this,
                                            LicenseActivity.class);
                                    startActivity(licenseIntent);
                                    dialog.dismiss();
                                    dialog.cancel();
                                }
                            })
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("unchecked")
    @Nullable private Map<String, String> pickNames(ArrayList<Participant> participantList) {
        Map<String, String> result = null;

        for(int i = 0; i < MAXIMUM_ATTEMPTS; i++) {
            ArrayList<Participant> recipients = (ArrayList<Participant>) participantList.clone();
            Collections.shuffle(recipients);
            result = new HashMap<>();

            for(int j = 0; j < participantList.size(); j++) {
                int recipientIndex = 0;

                // If the recipient ID matches the ID of the giver, assign the next ID to the giver
                // (if one exists). We will check that the assignments are valid later
                // (i.e. that no one is giving to themselves or someone on their exclusion list).
                if(participantList.get(j).getId().equals(recipients.get(0).getId()) &&
                        recipients.size() > 1) {
                    recipientIndex++;
                }
                result.put(participantList.get(j).getId(), recipients.get(recipientIndex).getId());
                recipients.remove(recipientIndex);
            }
            if(areValidPairings(result, participantList)) {
                break;
            }
        }

        return result;
    }

    private boolean areValidPairings(Map<String, String> pairings,
                                     ArrayList<Participant> participantList) {
        boolean isValid = true;
        for(Map.Entry<String, String> pairing : pairings.entrySet()) {
            if(pairing.getKey().equals(pairing.getValue())) {
                isValid = false;
                break;
            } else if(isRecipientInExclusionList(pairing.getKey(), pairing.getValue(),
                    participantList)) {
                isValid = false;
                break;
            }
        }

        return isValid;
    }

    private boolean isRecipientInExclusionList(String giverId, String recipientId,
                                               ArrayList<Participant> participantList) {
        boolean isInExclusionList = false;
        Participant participant = getParticipantFromId(giverId, participantList);
        if(participant != null && participant.getExclusionList().contains(recipientId)) {
            isInExclusionList = true;
        }

        return isInExclusionList;
    }

    private Participant getParticipantFromId(String id, ArrayList<Participant> participantList) {
        Participant participant = null;
        for(Participant p : participantList) {
            if(p.getId().equals(id)) {
                participant = p;
            }
        }

        return participant;
    }

    public void composeEmail(ArrayList<Participant> participantList, String emailText) {
        String[] addresses = new String[participantList.size()];
        for(int i = 0; i < addresses.length; i++) {
            addresses[i] = participantList.get(i).getEmailAddress();
        }

        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);

        ShareCompat.IntentBuilder.from(this)
                .setType(EMAIL_TYPE_STRING)
                .setEmailTo(addresses)
                .setSubject(getString(R.string.email_subject, year))
                .setText(getString(R.string.email_body, emailText))
                .setChooserTitle(R.string.email_chooser_title)
                .startChooser();
    }

}
