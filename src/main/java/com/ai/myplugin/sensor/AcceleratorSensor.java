/**
 * Created by User: veselin
 * On Date: 12/01/14
 */

package com.ai.myplugin.sensor;

import com.ai.api.*;
import com.ai.myplugin.util.*;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by User: veselin
 * On Date: 26/12/13
 */
@PluginImplementation
@PluginHeader (version = "1.0.1", author = "Veselin", category = "IOT", iconURL = "http://app.waylay.io/icons/acceleration.png")
public class AcceleratorSensor implements SensorPlugin {

    private static final Logger log = LoggerFactory.getLogger(AcceleratorSensor.class);

    static final String ACCELERATION_THRESHOLD = "acceleration_threshold";
    static final String RUNTIME_ACCELERATION = "acceleration";

    Map<String, Object> propertiesMap = new ConcurrentHashMap<String, Object>();

    String [] states = {"Below", "Above"};
    private static final String NAME = "Acceleration";

    @Override
    public Map<String, PropertyType> getRequiredProperties() {
        Map<String, PropertyType> map = new HashMap<>();
        map.put(ACCELERATION_THRESHOLD, new PropertyType(DataType.DOUBLE, true, false));
        return map;
    }

    @Override
    public Map<String, RawDataType> getRequiredRawData() {
        Map<String, RawDataType> map = new HashMap<>();
        map.put(RUNTIME_ACCELERATION, new RawDataType("m/s^2", DataType.DOUBLE));
        return map;
    }

    @Override
    public void setProperty(String string, Object obj) {
        if(getRequiredProperties().keySet().contains(string)) {
            propertiesMap.put(string, obj);
        } else {
            throw new RuntimeException("Property "+ string + " not in the required settings");
        }
    }

    @Override
    public Object getProperty(String string) {
        return propertiesMap.get(string);
    }

    @Override
    public String getDescription() {
        return "Accelerator sensor, check if the measurement is above the threshold";
    }

    @Override
    public SensorResult execute(SessionContext testSessionContext) {
        log.info("execute "+ getName() + ", sensor type:" +this.getClass().getName());
        if(getProperty(ACCELERATION_THRESHOLD) == null)
            throw new RuntimeException("acceleration threshold not set");

        Object rt1 = testSessionContext.getAttribute(RUNTIME_ACCELERATION);
        if(rt1 == null){
            log.warn("no runtime acceleration provided");
            return SensorResultBuilder.failure("no runtime " + RUNTIME_ACCELERATION + " provided").build();
        }
        Double runtime_force = Utils.getDouble(rt1);
        log.info("Current force: " + runtime_force);

        JSONObject jsonObject = new JSONObject();

        Double configuredThreshold = Utils.getDouble(getProperty(ACCELERATION_THRESHOLD));


        jsonObject.put(RUNTIME_ACCELERATION, runtime_force);
        jsonObject.put(ACCELERATION_THRESHOLD, configuredThreshold);

        final String state;
        if(configuredThreshold  > runtime_force)
            state = states[0];
        else
            state = states[1];

        final JSONObject finalJsonObject = jsonObject;
        return new SensorResult() {
            @Override
            public boolean isSuccess() {
                return true;
            }

            @Override
            public String getObserverState() {
                return state;
            }

            @Override
            public List<Map<String, Number>> getObserverStates() {
                return null;
            }

            @Override
            public String getRawData() {
                return finalJsonObject.toJSONString();
            }
        };

    }

    @Override
    public Map<String, RawDataType> getProducedRawData() {
        Map<String, RawDataType> map = new ConcurrentHashMap<>();
        map.put(RUNTIME_ACCELERATION, new RawDataType("value", DataType.DOUBLE, true, CollectedType.INSTANT));
        map.put(ACCELERATION_THRESHOLD, new RawDataType("value", DataType.DOUBLE, true, CollectedType.INSTANT));
        return map;
    }

    @Override
    public void setup(SessionContext testSessionContext) {
        log.debug("Setup : " + getName() + ", sensor : "+this.getClass().getName());
    }

    @Override
    public void shutdown(SessionContext testSessionContext) {
        log.debug("Shutdown : " + getName() + ", sensor : "+this.getClass().getName());
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Set<String> getSupportedStates() {
        return new HashSet(Arrays.asList(states));
    }

//    public static void main(String []args) throws ParseException {
//        AcceleratorSensor acceleratorSensor = new AcceleratorSensor();
//        acceleratorSensor.setProperty(ACCELERATION_THRESHOLD, 5);
//        SessionContext testSessionContext = new SessionContext(1);
//        testSessionContext.setAttribute(RUNTIME_ACCELERATION, 19.851858);
//        SensorResult testResult = acceleratorSensor.execute(testSessionContext);
//        System.out.println(testResult.getObserverState());
//        System.out.println(testResult.getRawData());
//        testSessionContext.setAttribute(RUNTIME_ACCELERATION, 1);
//        testResult = acceleratorSensor.execute(testSessionContext);
//        System.out.println(testResult.getObserverState());
//        System.out.println(testResult.getRawData());
//    }
}
