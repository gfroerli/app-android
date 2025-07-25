package ch.coredump.watertemp.rest;

import java.util.ArrayList;
import java.util.List;

import ch.coredump.watertemp.rest.models.ApiMeasurement;
import ch.coredump.watertemp.rest.models.ApiSensor;

/**
 * A class that bundles a sensor together with its measurements.
 */
public class SensorWithMeasurements {

    private final ApiSensor apiSensor;
    private final List<ApiMeasurement> apiMeasurements;

    public SensorWithMeasurements(ApiSensor apiSensor) {
        this.apiSensor = apiSensor;
        this.apiMeasurements = new ArrayList<>();
    }

    public SensorWithMeasurements(ApiSensor apiSensor, List<ApiMeasurement> apiMeasurements) {
        this.apiSensor = apiSensor;
        this.apiMeasurements = apiMeasurements;
    }

    public ApiSensor getSensor() {
        return apiSensor;
    }

    public List<ApiMeasurement> getMeasurements() {
        return apiMeasurements;
    }

    public void addMeasurement(ApiMeasurement apiMeasurement) {
        this.apiMeasurements.add(apiMeasurement);
    }
}
