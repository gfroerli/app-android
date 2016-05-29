package ch.coredump.watertemp.rest.models;

/**
 * Gson Location model.
 */
public class Location {
    private final float latitude;
    private final float longitude;

    public Location(float latitude, float longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    @Override
    public String toString() {
        return latitude + ", " + longitude;
    }
}
