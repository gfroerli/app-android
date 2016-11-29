package ch.coredump.watertemp.rest.models;

/**
 * Gson Sensor model.
 */
public class Sensor {
    private final int id;
    private final String deviceName;
    private final String caption;
    private final double latitude;
    private final double longitude;

    public Sensor(int id, String deviceName, String caption, double latitude, float longitude) {
        this.id = id;
        this.deviceName = deviceName;
        this.caption = caption;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public int getId() {
        return id;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getCaption() {
        return caption;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
