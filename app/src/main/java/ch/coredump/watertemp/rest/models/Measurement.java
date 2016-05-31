package ch.coredump.watertemp.rest.models;

public class Measurement {
    private final int id;
    private final int sensorId;
    private final float temperature;

    public Measurement(int id, int sensorId, float temperature) {
        this.id = id;
        this.sensorId = sensorId;
        this.temperature = temperature;
    }

    public int getId() {
        return id;
    }

    public int getSensorId() {
        return sensorId;
    }

    public float getTemperature() {
        return temperature;
    }
}
