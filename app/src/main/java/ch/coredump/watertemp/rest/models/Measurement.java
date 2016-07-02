package ch.coredump.watertemp.rest.models;

import java.util.Date;

public class Measurement {
    private final int id;
    private final int sensorId;
    private final float temperature;
    private final Date createdAt;
    private final Date updatedAt;

    public Measurement(int id, int sensorId, float temperature, Date createdAt, Date updatedAt) {
        this.id = id;
        this.sensorId = sensorId;
        this.temperature = temperature;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }
}
