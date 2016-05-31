package ch.coredump.watertemp.rest.models;

/**
 * Gson Sensor model.
 */
public class Sensor {
    private final int id;
    private final String deviceName;
    private final String caption;
    private final Location location;

    public Sensor(int id, String deviceName, String caption, Location location) {
        this.id = id;
        this.deviceName = deviceName;
        this.caption = caption;
        this.location = location;
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

    public Location getLocation() {
        return location;
    }
}
