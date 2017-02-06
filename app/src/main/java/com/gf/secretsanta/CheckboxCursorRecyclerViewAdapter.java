package com.gf.secretsanta;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.gf.secretsanta.model.Participant;
import com.gf.secretsanta.model.ParticipantContract;
import com.gf.secretsanta.model.ParticipantContract.ParticipantEntry;
import com.gf.secretsanta.model.ParticipantDbHelper;

import java.util.List;

class CheckboxCursorRecyclerViewAdapter
        extends RecyclerView.Adapter<CheckboxCursorRecyclerViewAdapter.ViewHolder>
        implements CheckBox.OnCheckedChangeListener {
    private boolean m_dataValid;
    private Cursor m_cursor;
    private Context m_context;
    private int m_rowIDColumn;
    private OnCheckedListener m_onCheckedListener;
    private ChangeObserver m_changeObserver;
    private DataSetObserver m_dataSetObserver;
    private List<String> m_excludeList;
    private String m_id;

    private static final String TAG = "CursorAdapter";

    /**
     * Recommended constructor.
     *
     * @param c The cursor from which to get the data.
     * @param context The context
     */
    CheckboxCursorRecyclerViewAdapter(final Context context, final Cursor c,
                                      final List<String> excludeList, final String id) {
        init(context, c, excludeList, id);
    }

    private void init(final Context context, final Cursor c, final List<String> excludeList,
                      final String id) {
        final boolean cursorPresent = c != null;
        m_cursor = c;
        m_dataValid = cursorPresent;
        m_context = context;
        m_rowIDColumn = cursorPresent ? c.getColumnIndexOrThrow(
                ParticipantContract.ParticipantEntry.COLUMN_NAME_PARTICIPANT_ID): -1;
        m_changeObserver = new ChangeObserver();
        m_dataSetObserver = new MyDataSetObserver();
        m_excludeList = excludeList;
        m_id = id;

        if(cursorPresent) {
            c.registerContentObserver(m_changeObserver);
            c.registerDataSetObserver(m_dataSetObserver);
        }
    }

    void setOnCheckedChangedListener(OnCheckedListener onCheckedChangedListener) {
        m_onCheckedListener = onCheckedChangedListener;
    }

    /**
     * Returns the cursor.
     * @return the cursor.
     */
    public Cursor getCursor() {
        return m_cursor;
    }

    @Override
    public int getItemCount() {
        if(m_dataValid && m_cursor != null) {
            return m_cursor.getCount();
        } else {
            return 0;
        }
    }

    /**
     * @see android.support.v7.widget.RecyclerView.Adapter
     */
    Participant getItem(final int position) {
        if(m_dataValid && m_cursor != null) {
            m_cursor.moveToPosition(position);
            return getParticipant(m_cursor);
        } else {
            return null;
        }
    }

    /**
     * @see android.support.v7.widget.RecyclerView.Adapter#getItemId(int)
     */
    @Override
    public long getItemId(final int position) {
        if(m_dataValid && m_cursor != null) {
            if(m_cursor.moveToPosition(position)) {
                return m_cursor.getLong(m_rowIDColumn);
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        View v = LayoutInflater.from(m_context).inflate(R.layout.participant_exclude_item, parent,
                false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        if(!m_dataValid) {
            Log.e(TAG, "Attempt to bind to invalid cursor.");
            throw new IllegalStateException("This should only be called when the cursor is valid");
        }
        if(!m_cursor.moveToPosition(position)) {
            Log.e(TAG, "Unable to move to position " + position + ".");
            throw new IllegalStateException("Couldn't move cursor to position " + position);
        }
        bindViewHolder(holder, m_cursor);
    }

    /**
     * Bind an existing view to the data pointed to by cursor
     * @param holder Existing view, returned earlier by newView
     * @param cursor The cursor from which to get the data. The cursor is already
     * moved to the correct position.
     */
    private void bindViewHolder(ViewHolder holder, Cursor cursor) {
        Participant participant = getParticipant(cursor);
        holder.nameText.setText(participant.getName() + " (" + participant.getEmailAddress() + ")");

        if(m_excludeList != null && m_excludeList.size() > 0 &&
                m_excludeList.contains(participant.getId())) {
            holder.checkbox.setChecked(true);
        }

        holder.checkbox.setOnCheckedChangeListener(this);
        holder.checkbox.setTag(participant);

        holder.itemView.setTag(participant);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        if(m_onCheckedListener != null) {
            m_onCheckedListener.onCheckedChanged(isChecked, (Participant) compoundButton.getTag());
        }
    }

    private Participant getParticipant(Cursor cursor) {
        Participant participant = new Participant();

        participant.setId(cursor.getString(cursor.getColumnIndexOrThrow(
                ParticipantContract.ParticipantEntry.COLUMN_NAME_PARTICIPANT_ID)));
        participant.setName(cursor.getString(cursor.getColumnIndexOrThrow(
                ParticipantContract.ParticipantEntry.COLUMN_NAME_PARTICIPANT_NAME)));
        participant.setEmailAddress(cursor.getString(cursor.getColumnIndexOrThrow(
                ParticipantContract.ParticipantEntry.COLUMN_NAME_PARTICIPANT_EMAIL_ADDRESS)));

        return participant;
    }

    public void filter(SQLiteDatabase db, String table, String[] projection, String where,
                       String[] whereArgs, String sort) {
        m_cursor = db.query(
                table,
                projection,
                where,
                whereArgs,
                null,
                null,
                sort + " COLLATE NOCASE"
        );
    }

    /**
     * Change the underlying cursor to a new cursor. If there is an existing cursor it will be
     * closed.
     *
     * @param cursor The new cursor to be used
     */
    private void changeCursor(final Cursor cursor) {
        final Cursor old = swapCursor(cursor);
        if(old != null) {
            old.close();
        }
    }
    /**
     * Swap in a new Cursor, returning the old Cursor.  Unlike
     * {@link #changeCursor(Cursor)}, the returned old Cursor is <em>not</em>
     * closed.
     *
     * @param newCursor The new cursor to be used.
     * @return Returns the previously set Cursor, or null if there wasa not one.
     * If the given new Cursor is the same instance is the previously set
     * Cursor, null is also returned.
     */
    @Nullable
    private Cursor swapCursor(@NonNull final Cursor newCursor) {
        if(newCursor == m_cursor) {
            return null;
        }

        final Cursor oldCursor = m_cursor;
        if(oldCursor != null) {
            if(m_changeObserver != null) {
                oldCursor.unregisterContentObserver(m_changeObserver);
            }
            if(m_dataSetObserver != null) {
                oldCursor.unregisterDataSetObserver(m_dataSetObserver);
            }
        }

        m_cursor = newCursor;
        if(m_changeObserver != null) {
            newCursor.registerContentObserver(m_changeObserver);
        }
        if(m_dataSetObserver != null) {
            newCursor.registerDataSetObserver(m_dataSetObserver);
        }

        m_rowIDColumn = newCursor.getColumnIndexOrThrow(ParticipantContract.ParticipantEntry.COLUMN_NAME_PARTICIPANT_ID);
        m_dataValid = true;

        // notify the observers about the new cursor
        notifyDataSetChanged();

        return oldCursor;
    }

    /**
     * Converts the cursor into a CharSequence. Returns an
     * empty String for null values or the default String representation of
     * the value.
     *
     * @param cursor the cursor to convert to a CharSequence
     * @return a CharSequence representing the value
     */
    public CharSequence convertToString(final Cursor cursor) {
        return cursor == null ? "" : cursor.toString();
    }

    /**
     * Called when the {@link ContentObserver} on the cursor receives a change notification.
     * The default implementation provides the auto-requery logic, but may be overridden by
     * sub classes.
     *
     * @see ContentObserver#onChange(boolean)
     */
    private void onContentChanged() {
        UpdateCursorTask updateCursor = new UpdateCursorTask();
        updateCursor.execute();
    }

    private class ChangeObserver extends ContentObserver {
        ChangeObserver() {
            super(new Handler());
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(final boolean selfChange) {
            onContentChanged();
        }
    }

    private class MyDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            m_dataValid = true;
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            m_dataValid = false;
            notifyDataSetChanged();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        CheckBox checkbox;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = (TextView) itemView.findViewById(R.id.participant_name);
            checkbox = (CheckBox) itemView.findViewById(R.id.is_excluded);
        }
    }

    interface OnCheckedListener {
        void onCheckedChanged(boolean isChecked, Participant participant);
    }

    private class UpdateCursorTask extends AsyncTask<Void, Void, Cursor> {

        @Override
        protected Cursor doInBackground (Void...params){
            SQLiteDatabase db = new ParticipantDbHelper(m_context).getReadableDatabase();

            String[] projection = {
                    ParticipantEntry._ID,
                    ParticipantEntry.COLUMN_NAME_PARTICIPANT_ID,
                    ParticipantEntry.COLUMN_NAME_PARTICIPANT_NAME,
                    ParticipantEntry.COLUMN_NAME_PARTICIPANT_EMAIL_ADDRESS
            };

            if(m_id != null) {
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
                String[] selectArgs = new String[] { m_id };

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
        protected void onPostExecute (Cursor result) {
            changeCursor(result);
        }
    }

    private static final int ITEM_CLICK_DELAY = 70;
}
