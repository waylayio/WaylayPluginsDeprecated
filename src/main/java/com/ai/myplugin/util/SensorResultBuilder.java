package com.ai.myplugin.util;

import com.ai.api.SensorResult;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SensorResultBuilder {

    private SensorResultBuilder(){
        throw new UnsupportedOperationException("Utility class");
    }

    public static SensorResultBuild success(){
        return new SensorResultBuildImpl(true);
    }

    public static SensorResultBuild failure(String message){
        return new SensorResultBuildImpl(false).withErrorMessage(message);
    }

    public static interface SensorResultBuild{
        SensorResultBuild withRawData(final String rawData);

        SensorResultBuild withRawData(final JSONObject rawData);

        SensorResultBuild withRawData(final JsonElement rawData);

        SensorResultBuild withObserverState(final String observerState);

        SensorResultBuild addObserverState(final Map<String, Number> state);

        SensorResult build();
    }

    private static class SensorResultBuildImpl implements SensorResultBuild{

        private final boolean success;
        private String errorMessage;
        private String observerState = null;
        private List<Map<String, Number>> observerStates = new ArrayList<>();
        private String rawData = null;

        private SensorResultBuildImpl(final boolean success){
            this.success = success;
        }

        public SensorResultBuild withErrorMessage(final String message){
            this.errorMessage = message;
            return this;
        }

        public SensorResultBuild withRawData(final String rawData){
            this.rawData = rawData;
            return this;
        }

        public SensorResultBuild withRawData(final JSONObject rawData){
            this.rawData = rawData.toJSONString();
            return this;
        }

        public SensorResultBuild withRawData(final JsonElement rawData){
            this.rawData = new Gson().toJson(rawData);
            // TODO we probably want to set a payload content type?
            return this;
        }

        public SensorResultBuild withObserverState(final String observerState){
            this.observerState = observerState;
            return this;
        }

        public SensorResultBuild addObserverState(Map<String, Number> state){
            this.observerStates.add(Collections.unmodifiableMap(state));
            return this;
        }

        public SensorResult build(){
            return new ImmutableSensorResult(success, observerState, Collections.unmodifiableList(observerStates), rawData, errorMessage);
        }
    }

    private static class ImmutableSensorResult implements SensorResult{

        private final boolean success;
        private final String errorMessage;
        private final String observerState;
        private final List<Map<String, Number>> observerStates;
        private final String rawData;

        private ImmutableSensorResult(
                final boolean success,
                final String observerState,
                List<Map<String, Number>> observerStates,
                String rawData,
                final String errorMessage) {
            this.success = success;
            this.observerState = observerState;
            this.observerStates = observerStates;
            this.rawData = rawData;
            this.errorMessage = errorMessage;
        }

        @Override
        public boolean isSuccess() {
            return success;
        }

        public String errorMessage() {
            return errorMessage;
        }

        @Override
        public String getObserverState() {
            if(!success){
                throw new UnsupportedOperationException("Observed state undefined as the sensor failed: " + errorMessage);
            }
            return observerState;
        }

        @Override
        public List<Map<String, Number>> getObserverStates() {
            if(!success){
                throw new UnsupportedOperationException("Observed states undefined as the sensor failed: " + errorMessage);
            }
            return observerStates;
        }

        @Override
        public String getRawData() {
            if(!success){
                throw new UnsupportedOperationException("Raw data undefined as the sensor failed: " + errorMessage);
            }
            return rawData;
        }
    }
}
