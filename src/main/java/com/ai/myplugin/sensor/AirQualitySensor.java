/**
 * Created by User: veselin
 * On Date: 21/10/13
 */

package com.ai.myplugin.sensor;

import com.ai.api.*;
import com.ai.myplugin.util.Rest;
import com.ai.myplugin.util.SensorResultBuilder;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


// TODO transform to JavaScript
// @PluginImplementation
@PluginHeader (version = "1.0.1", author = "Veselin", category = "Environment", iconURL = "http://app.waylay.io/icons/air_quality.png")
public class AirQualitySensor implements SensorPlugin {

    private static final Logger log = LoggerFactory.getLogger(AirQualitySensor.class);

    public static final String LOCATION = "location";
    private static final String NAME = "AirQuality";
    String pathURL = "http://luchtkwaliteit.vmm.be/lijst.php";
    String detailInfoIRCURL = "http://deus.irceline.be/~celinair/actair/actair.php?lan=nl";
    private String location = null;
    private String[] states = {"Excellent","Good", "Normal", "Poor", "Bad"};

    @Override
    public Map<String, PropertyType> getRequiredProperties() {
        Map<String, PropertyType> map = new HashMap<>();
        map.put(LOCATION, new PropertyType(DataType.STRING, true, false));
        return map;
    }

    @Override
    public void setProperty(String s, Object o) {
        if(LOCATION.equals(s))
            location = o.toString();
        else
            throw new RuntimeException("Property "+ s + " not in the required settings");
    }

    @Override
    public Object getProperty(String s) {
        if(LOCATION.equals(s))
            return location;
        else
            throw new RuntimeException("Property "+ s + " not in the required settings");
    }

    @Override
    public String getDescription() {
        return "Air Quality";
    }

    @Override
    public SensorResult execute(SessionContext testSessionContext) {
        log.info("execute "+ getName() + ", sensor type:" +this.getClass().getName());
        for(String property : getRequiredProperties().keySet()){
            if(getProperty(property) == null)
                throw new RuntimeException("Required property "+property + " not defined");
        }

        int value = -1;
        String stringToParse = "";
        double O3 = -1;
        double NO2 = -1;
        double CO = -1;
        double SO2 = -1;
        double PM10 = -1;
        double PM25 = -1;
        double C6H6 = -1;

        try{
            stringToParse = Rest.httpGet(pathURL).body();
            Document doc = Jsoup.parse(stringToParse);
            for (Element table : doc.select("#stations")) {
                for (Element row : table.select("tr")) {
                    Elements tds = row.select("td");
                    if (tds.size() >4 && !"".equals(tds.get(4).text())) {
                        String location = tds.get(1).text();
                        if(location.equalsIgnoreCase((String) getProperty(LOCATION))) {
                            value = Integer.parseInt(tds.get(4).text().trim().replaceAll("      ",""));
                            break;
                        }
                        log.info("Found entry " + tds.get(0).text() + ":" + tds.get(1).text() + ":" +
                                tds.get(2).text() + ":" + tds.get(3).text() + ":" + tds.get(4).text());
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            return SensorResultBuilder.failure(e.getMessage()).build();
        }
        if(value == -1){
            log.error("location not found");
            return SensorResultBuilder.failure("location not found").build();
        }
        //try to get more detail information, don't fail if there is nothing
        try{
            stringToParse = Rest.httpGet(detailInfoIRCURL).body();
            Document doc = Jsoup.parse(stringToParse);
            for (Element table : doc.select("table")) {
                for (Element row : table.select("tr")) {
                    Elements tds = row.select("td");
                    //index, location, O3, NO2, CO, SO2, PM10. PM2.5, C6H6
                    if (tds.size() >8) {
                        String location = tds.get(1).text();
                        if(location.equalsIgnoreCase((String) getProperty(LOCATION))) {
                            O3 = getDouble(tds.get(2).text().trim().replace(",", "."));
                            NO2 = getDouble(tds.get(3).text().trim().replace(",", "."));
                            CO = getDouble(tds.get(4).text().trim().replace(",", "."));
                            SO2 = getDouble(tds.get(5).text().trim().replace(",", "."));
                            PM10 = getDouble(tds.get(6).text().trim().replace(",", "."));
                            PM25 = getDouble(tds.get(7).text().trim().replace(",", "."));
                            C6H6 = getDouble(tds.get(8).text().trim().replace(",", "."));
                            log.info("Found detailed entry " + tds.get(0).text() + ":" + tds.get(1).text() + ":" +
                                    tds.get(2).text() + ":" + tds.get(3).text() + ":" + tds.get(4).text()+
                                    tds.get(5).text() + ":" + tds.get(6).text() + ":" +
                                    tds.get(7).text() + ":" + tds.get(8).text());
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            return SensorResultBuilder.failure(e.getMessage()).build();
        }

        final int finalValue = value;
        final double finalO3 = O3;
        final double finalCO = CO;
        final double finalNO = NO2;
        final double finalPM1 = PM10;
        final double finalSO = SO2;
        final double finalPM2 = PM25;
        final double finalC6H = C6H6;
        return new SensorResult() {
            @Override
            public boolean isSuccess() {
                return true;
            }

            @Override
            public String getObserverState() {
                return mapValue(finalValue);
            }

            @Override
            public List<Map<String, Number>> getObserverStates() {
                return null;
            }

            @Override
            public String getRawData() {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("airQuality", finalValue);

                if(finalO3 != -1)
                    jsonObject.put("O3", finalO3);
                if(finalCO != -1)
                    jsonObject.put("CO", finalCO);
                if(finalNO != -1)
                    jsonObject.put("NO2", finalNO);
                if(finalPM1 != -1)
                    jsonObject.put("PM10", finalPM1);
                if(finalSO != -1)
                    jsonObject.put("SO2", finalSO);
                if(finalPM2 != -1)
                    jsonObject.put("PM25", finalPM2);
                if(finalC6H != -1)
                    jsonObject.put("C6H6", finalC6H);
                return jsonObject.toJSONString();
            }
        };
    }

    @Override
    public Map<String, RawDataType> getProducedRawData() {
        Map<String, RawDataType> map = new ConcurrentHashMap<>();
        map.put("airQuality", new RawDataType("value", DataType.DOUBLE, true, CollectedType.INSTANT));
        map.put("O3", new RawDataType("value", DataType.DOUBLE, true, CollectedType.INSTANT));
        map.put("CO", new RawDataType("value", DataType.DOUBLE, true, CollectedType.INSTANT));
        map.put("NO2", new RawDataType("value", DataType.DOUBLE, true, CollectedType.INSTANT));
        map.put("PM10", new RawDataType("value", DataType.DOUBLE, true, CollectedType.INSTANT));
        map.put("SO2", new RawDataType("value", DataType.DOUBLE, true, CollectedType.INSTANT));
        map.put("PM25", new RawDataType("value", DataType.DOUBLE, true, CollectedType.INSTANT));
        map.put("C6H6", new RawDataType("value", DataType.DOUBLE, true, CollectedType.INSTANT));
        return map;
    }

    private double getDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e){
            return -1.;
        }
    }

    @Override
    public void setup(SessionContext testSessionContext) {
        log.debug("Setup : " + getName() + ", sensor : "+this.getClass().getName());
    }

    @Override
    public void shutdown(SessionContext testSessionContext) {
        log.debug("Shutdown : " + getName() + ", sensor : "+this.getClass().getName());
    }

    private String mapValue(int finalValue) {
        if(finalValue < 3)
            return "Excellent";
        if(finalValue < 5)
            return "Good";
        if(finalValue < 7)
            return "Normal";
        if(finalValue < 9)
            return "Poor";
        return "Bad";
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Set<String> getSupportedStates() {
        return new HashSet(Arrays.asList(states));
    }

    public static void main(String []args){
        AirQualitySensor airQualitySensor = new AirQualitySensor();
        airQualitySensor.setProperty(LOCATION, "Gent");
        SensorResult testResult = airQualitySensor.execute(null);
        log.info(testResult.getObserverState());
        log.info(testResult.getRawData());

        airQualitySensor.setProperty(LOCATION, "Antwerpen");
        testResult = airQualitySensor.execute(null);
        log.info(testResult.getObserverState());
        log.info(testResult.getRawData());
    }
}


