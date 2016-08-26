package nodomain.freeyourgadget.gadgetbridge.model;

import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;

public interface ActivitySample extends TimeStamped {

    int NOT_MEASURED = -1;

    /**
     * Returns the provider of the data.
     *
     * @return who created the sample data
     */
    SampleProvider getProvider();

    /**
     * Returns the raw activity kind value as recorded by the SampleProvider
     */
    int getRawKind();

    /**
     * Returns the activity kind value as recorded by the SampleProvider
     *
     * @see ActivityKind
     */
    int getKind();

    /**
     * Returns the raw intensity value as recorded by the SampleProvider
     */
    int getRawIntensity();

    /**
     * Returns the normalized intensity value between 0 and 1
     */
    float getIntensity();

    /**
     * Returns the number of steps performed during the period of this sample
     */
    int getSteps();

    /**
     * Returns the heart rate measured at the corresponding timestamp.
     * The value is returned in heart beats per minute, in the range from
     * 0-255, where 255 is an illegal value (e.g. due to a bad measurement)
     *
     * @return the heart rate value in beats per minute, or -1 if none
     */
    int getHeartRate();

    /**
     * Sets the heart rate value of this sample. Typically only used in
     * generic db migration.
     *
     * @param value the value in bpm
     */
    void setHeartRate(int value);
}
