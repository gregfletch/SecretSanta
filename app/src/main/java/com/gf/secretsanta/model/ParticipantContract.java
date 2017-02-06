package com.gf.secretsanta.model;

import android.provider.BaseColumns;

public final class ParticipantContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public ParticipantContract() {}

    /* Inner class that defines the table contents */
    public static abstract class ParticipantEntry implements BaseColumns {
        public static final String TABLE_NAME = "participants";
        public static final String COLUMN_NAME_PARTICIPANT_ID = "pid";
        public static final String COLUMN_NAME_PARTICIPANT_NAME = "name";
        public static final String COLUMN_NAME_PARTICIPANT_EMAIL_ADDRESS = "email";
        public static final String COLUMN_NAME_PARTICIPANT_EXCLUSION_LIST = "exclusions";
    }
}
