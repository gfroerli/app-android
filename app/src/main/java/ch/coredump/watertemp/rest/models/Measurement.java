package ch.coredump.watertemp.rest.models;

import org.threeten.bp.ZonedDateTime;

public class Measurement {
    private final int id;
    private final int sensorId;
    private final float temperature;
    private final ZonedDateTime createdAt;
    private final ZonedDateTime updatedAt;

    public Measurement(int id, int sensorId, float temperature, ZonedDateTime createdAt, ZonedDateTime updatedAt) {
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

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }
}
