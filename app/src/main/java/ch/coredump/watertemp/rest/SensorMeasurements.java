package ch.coredump.watertemp.rest;

import java.util.ArrayList;
import java.util.List;

import ch.coredump.watertemp.rest.models.Measurement;
import ch.coredump.watertemp.rest.models.Sensor;

/**
 * A class that bundles a sensor together with its measurements.
 */
public class SensorMeasurements {

    private final Sensor sensor;
    private final List<Measurement> measurements;

    public SensorMeasurements(Sensor sensor) {
        this.sensor = sensor;
        this.measurements = new ArrayList<>();
    }

    public SensorMeasurements(Sensor sensor, List<Measurement> measurements) {
        this.sensor = sensor;
        this.measurements = measurements;
    }

    public Sensor getSensor() {
        return sensor;
    }

    public List<Measurement> getMeasurements() {
        return measurements;
    }

    public void addMeasurement(Measurement measurement) {
        this.measurements.add(measurement);
    }
}
