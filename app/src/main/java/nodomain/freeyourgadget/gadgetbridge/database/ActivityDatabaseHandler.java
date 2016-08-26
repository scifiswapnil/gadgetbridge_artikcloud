package nodomain.freeyourgadget.gadgetbridge.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.schema.ActivityDBCreationScript;
import nodomain.freeyourgadget.gadgetbridge.database.schema.SchemaMigration;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoMaster;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.DATABASE_NAME;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_CUSTOM_SHORT;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_INTENSITY;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_PROVIDER;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_STEPS;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_TIMESTAMP;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_TYPE;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.TABLE_GBACTIVITYSAMPLES;

// TODO: can be removed entirely
public class ActivityDatabaseHandler extends SQLiteOpenHelper implements DBHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ActivityDatabaseHandler.class);

    private static final int DATABASE_VERSION = 7;
    private static final String UPDATER_CLASS_NAME_PREFIX = "ActivityDBUpdate_";

    public ActivityDatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            ActivityDBCreationScript script = new ActivityDBCreationScript();
            script.createSchema(db);
        } catch (RuntimeException ex) {
            GB.toast("Error creating database.", Toast.LENGTH_SHORT, GB.ERROR, ex);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        new SchemaMigration(UPDATER_CLASS_NAME_PREFIX).onUpgrade(db, oldVersion, newVersion);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        new SchemaMigration(UPDATER_CLASS_NAME_PREFIX).onDowngrade(db, oldVersion, newVersion);
    }

    @Override
    public SQLiteDatabase getDatabase() {
        return super.getWritableDatabase();
    }

    public void addGBActivitySample(ActivitySample sample) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(KEY_TIMESTAMP, sample.getTimestamp());
            values.put(KEY_PROVIDER, sample.getProvider().getID());
            values.put(KEY_INTENSITY, sample.getRawIntensity());
            values.put(KEY_STEPS, sample.getSteps());
//            values.put(KEY_CUSTOM_SHORT, sample.getCustomValue());
            values.put(KEY_TYPE, sample.getRawKind());

            db.insert(TABLE_GBACTIVITYSAMPLES, null, values);
        }
    }

    /**
     * Adds the a new sample to the database
     *
     * @param timestamp        the timestamp of the same, second-based!
     * @param provider         the SampleProvider ID
     * @param intensity        the sample's raw intensity value
     * @param steps            the sample's steps value
     * @param kind             the raw activity kind of the sample
     * @param customShortValue
     */
    public void addGBActivitySample(AbstractActivitySample sample) {
        float intensity = sample.getIntensity();
        int steps = sample.getSteps();
        int kind = sample.getRawKind();
        int timestamp = sample.getTimestamp();
        int customShortValue = 0;

        if (intensity < 0) {
            LOG.error("negative intensity received, ignoring");
            intensity = 0;
        }
        if (steps < 0) {
            LOG.error("negative steps received, ignoring");
            steps = 0;
        }

        if (customShortValue < 0) {
            LOG.error("negative short value received, ignoring");
            customShortValue = 0;
        }

        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(KEY_TIMESTAMP, timestamp);
//            values.put(KEY_PROVIDER, provider);
            values.put(KEY_INTENSITY, intensity);
            values.put(KEY_STEPS, steps);
            values.put(KEY_TYPE, kind);
            values.put(KEY_CUSTOM_SHORT, customShortValue);

            db.insert(TABLE_GBACTIVITYSAMPLES, null, values);
        }
    }

    public void addGBActivitySamples(AbstractActivitySample[] activitySamples) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {

            String sql = "INSERT INTO " + TABLE_GBACTIVITYSAMPLES + " (" + KEY_TIMESTAMP + "," +
                    KEY_PROVIDER + "," + KEY_INTENSITY + "," + KEY_STEPS + "," + KEY_TYPE + "," + KEY_CUSTOM_SHORT + ")" +
                    " VALUES (?,?,?,?,?,?);";
            SQLiteStatement statement = db.compileStatement(sql);
            db.beginTransaction();

            for (ActivitySample activitySample : activitySamples) {
                statement.clearBindings();
                statement.bindLong(1, activitySample.getTimestamp());
                statement.bindLong(2, activitySample.getProvider().getID());
                statement.bindLong(3, activitySample.getRawIntensity());
                statement.bindLong(4, activitySample.getSteps());
                statement.bindLong(5, activitySample.getRawKind());
//                statement.bindLong(6, activitySample.getCustomValue());
                statement.execute();
            }
            db.setTransactionSuccessful();
            db.endTransaction();
        }
    }

    public ArrayList<ActivitySample> getSleepSamples(int timestamp_from, int timestamp_to, SampleProvider provider) {
        return getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_SLEEP, provider);
    }

    public ArrayList<ActivitySample> getActivitySamples(int timestamp_from, int timestamp_to, SampleProvider provider) {
        return getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_ACTIVITY, provider);
    }

    @Override
    public void closeDb() {
    }

    @Override
    public void openDb() {
    }

    @Override
    public SQLiteOpenHelper getHelper() {
        return this;
    }

    public ArrayList<ActivitySample> getAllActivitySamples(int timestamp_from, int timestamp_to, SampleProvider provider) {
        return getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_ALL, provider);
    }

    public ArrayList<ActivitySample> getAllActivitySamples() {
        return getActivitySamples(null, "timestamp", null);
    }

    /**
     * Returns all available activity samples from between the two timestamps (inclusive), of the given
     * provided and type(s).
     *
     * @param timestamp_from
     * @param timestamp_to
     * @param activityTypes  ORed combination of #TYPE_DEEP_SLEEP, #TYPE_LIGHT_SLEEP, #TYPE_ACTIVITY
     * @param provider       the producer of the samples to be sought
     * @return
     */
    private ArrayList<ActivitySample> getGBActivitySamples(int timestamp_from, int timestamp_to, int activityTypes, SampleProvider provider) {
        if (timestamp_to < 0) {
            throw new IllegalArgumentException("negative timestamp_to");
        }
        if (timestamp_from < 0) {
            throw new IllegalArgumentException("negative timestamp_from");
        }
        final String where = "(provider=" + provider.getID() + " and timestamp>=" + timestamp_from + " and timestamp<=" + timestamp_to + getWhereClauseFor(activityTypes, provider) + ")";
        LOG.info("Activity query where: " + where);
        final String order = "timestamp";

        ArrayList<ActivitySample> samples = getActivitySamples(where, order, null);

        return samples;
    }

    private ArrayList<ActivitySample> getActivitySamples(String where, String order, SampleProvider provider) {
        ArrayList<ActivitySample> samples = new ArrayList<>();
        try (SQLiteDatabase db = this.getReadableDatabase()) {
            try (Cursor cursor = db.query(TABLE_GBACTIVITYSAMPLES, null, where, null, null, null, order)) {
                LOG.info("Activity query result: " + cursor.getCount() + " samples");
                int colTimeStamp = cursor.getColumnIndex(KEY_TIMESTAMP);
                int colIntensity = cursor.getColumnIndex(KEY_INTENSITY);
                int colSteps = cursor.getColumnIndex(KEY_STEPS);
                int colType = cursor.getColumnIndex(KEY_TYPE);
                int colCustomShort = cursor.getColumnIndex(KEY_CUSTOM_SHORT);
                while (cursor.moveToNext()) {
                    GBActivitySample sample = new GBActivitySample(
                            provider,
                            cursor.getInt(colTimeStamp),
                            cursor.getInt(colIntensity),
                            cursor.getInt(colSteps),
                            cursor.getInt(colType),
                            cursor.getInt(colCustomShort));
                    samples.add(sample);
                }
            }
        }
        return samples;
    }

    private String getWhereClauseFor(int activityTypes, SampleProvider provider) {
        if (activityTypes == ActivityKind.TYPE_ALL) {
            return ""; // no further restriction
        }

        StringBuilder builder = new StringBuilder(" and (");
        int[] dbActivityTypes = ActivityKind.mapToDBActivityTypes(activityTypes, provider);
        for (int i = 0; i < dbActivityTypes.length; i++) {
            builder.append(" type=").append(dbActivityTypes[i]);
            if (i + 1 < dbActivityTypes.length) {
                builder.append(" or ");
            }
        }
        builder.append(')');
        return builder.toString();
    }

    public void changeStoredSamplesType(int timestampFrom, int timestampTo, int kind, SampleProvider provider) {
        try (SQLiteDatabase db = this.getReadableDatabase()) {
            String sql = "UPDATE " + TABLE_GBACTIVITYSAMPLES + " SET " + KEY_TYPE + "= ? WHERE "
                    + KEY_PROVIDER + " = ? AND "
                    + KEY_TIMESTAMP + " >= ? AND " + KEY_TIMESTAMP + " < ? ;"; //do not use BETWEEN because the range is inclusive in that case!

            SQLiteStatement statement = db.compileStatement(sql);
            statement.bindLong(1, kind);
            statement.bindLong(2, provider.getID());
            statement.bindLong(3, timestampFrom);
            statement.bindLong(4, timestampTo);
            statement.execute();
        }
    }

    public void changeStoredSamplesType(int timestampFrom, int timestampTo, int fromKind, int toKind, SampleProvider provider) {
        try (SQLiteDatabase db = this.getReadableDatabase()) {
            String sql = "UPDATE " + TABLE_GBACTIVITYSAMPLES + " SET " + KEY_TYPE + "= ? WHERE "
                    + KEY_TYPE + " = ? AND "
                    + KEY_PROVIDER + " = ? AND "
                    + KEY_TIMESTAMP + " >= ? AND " + KEY_TIMESTAMP + " < ? ;"; //do not use BETWEEN because the range is inclusive in that case!

            SQLiteStatement statement = db.compileStatement(sql);
            statement.bindLong(1, toKind);
            statement.bindLong(2, fromKind);
            statement.bindLong(3, provider.getID());
            statement.bindLong(4, timestampFrom);
            statement.bindLong(5, timestampTo);
            statement.execute();
        }
    }

    public int fetchLatestTimestamp(SampleProvider provider) {
        try (SQLiteDatabase db = this.getReadableDatabase()) {
            try (Cursor cursor = db.query(TABLE_GBACTIVITYSAMPLES, new String[]{KEY_TIMESTAMP}, KEY_PROVIDER + "=" + String.valueOf(provider.getID()), null, null, null, KEY_TIMESTAMP + " DESC", "1")) {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(0);
                }
            }
        }
        return -1;
    }

    public boolean hasContent() {
        try {
            try (SQLiteDatabase db = this.getReadableDatabase()) {
                try (Cursor cursor = db.query(TABLE_GBACTIVITYSAMPLES, new String[]{KEY_TIMESTAMP}, null, null, null, null, null, "1")) {
                    return cursor.moveToFirst();
                }
            }
        } catch (Exception ex) {
            // can't expect anything
            GB.log("Error looking for old activity data: " + ex.getMessage(), GB.ERROR, ex);
            return false;
        }
    }

    @Override
    public DaoSession getDaoSession() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DaoMaster getDaoMaster() {
        throw new UnsupportedOperationException();
    }
}
