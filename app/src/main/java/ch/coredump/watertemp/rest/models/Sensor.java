package ch.coredump.watertemp.rest.models;

/**
 * Gson Sensor model.
 */
public class Sensor {
    private final String deviceName;
    private final String caption;
    private final Location location;

    public Sensor(String deviceName, String caption, Location location) {
        this.deviceName = deviceName;
        this.caption = caption;
        this.location = location;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getCaption() {
        return caption;
    }

    public Location getLocation() {
        return location;
    }
}
