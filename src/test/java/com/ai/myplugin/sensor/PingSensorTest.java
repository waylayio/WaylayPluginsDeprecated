/**
 * Created by User: veselin
 * On Date: 27/02/14
 */
package com.ai.myplugin.sensor;

import com.ai.api.DataType;
import com.ai.api.RawDataType;
import com.ai.api.SensorResult;
import com.ai.api.SessionContext;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;


public class PingSensorTest{

    @Test
    public void testExecute() throws Exception {
        PingSensor pingSensor = new PingSensor();
        pingSensor.setProperty("address", "www.waylay.io");
        SensorResult testResult = pingSensor.execute(new SessionContext(1));
        System.out.println(testResult.getRawData());
        assertEquals("Alive", testResult.getObserverState());
    }

    @Test
    public void testExecuteUppercaseProperty() throws Exception {
        PingSensor pingSensor = new PingSensor();
        pingSensor.setProperty("ADDRESS", "www.waylay.io");
        SensorResult testResult = pingSensor.execute(new SessionContext(1));
        System.out.println(testResult.getRawData());
        assertEquals("Alive", testResult.getObserverState());
    }

    @Test
    public void testRawDataMetadata() throws Exception {
        PingSensor pingSensor = new PingSensor();
        pingSensor.setProperty("ADDRESS", "www.waylay.io");
        SensorResult testResult = pingSensor.execute(new SessionContext(1));
        List<RawDataType> list = pingSensor.getRawDataTypes();
        JSONObject obj = (JSONObject) new JSONParser().parse(testResult.getRawData());
        for(RawDataType rawDataType : list){
            if(rawDataType.getName().equals("time")){
                assertEquals(DataType.DOUBLE, rawDataType.getDataType());
                try{
                    Double.parseDouble(obj.get("time").toString());
                } catch (Exception e){
                    fail("result should be double");
                }
            } else if(rawDataType.getName().equals("result")){
                assertEquals(DataType.STRING, rawDataType.getDataType());
                try{
                    Double.parseDouble(obj.get("result").toString());
                    fail("result should not be double");
                } catch (Exception e){

                }
            }
        }
    }


    @Test
    public void testDown() throws Exception {
        PingSensor pingSensor = new PingSensor();
        pingSensor.setProperty("address", "www.waylaay.io");
        SensorResult testResult = pingSensor.execute(new SessionContext(1));
        System.out.println(testResult.getRawData());
        assertEquals("Not Alive", testResult.getObserverState());
    }
}