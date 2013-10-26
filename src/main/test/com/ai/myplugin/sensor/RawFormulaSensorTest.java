package com.ai.myplugin.sensor;

import com.ai.bayes.scenario.TestResult;
import com.ai.myplugin.util.Utils;
import com.ai.util.resource.NodeSessionParams;
import com.ai.util.resource.TestSessionContext;
import junit.framework.TestCase;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by User: veselin
 * On Date: 25/10/13
 */
public class RawFormulaSensorTest extends TestCase {

    public void testCalculationFormula() throws ParseException {
        RawFormulaSensor rawFormulaSensor = new RawFormulaSensor();
        String formula = "node1->value1 + node2->value2";
        rawFormulaSensor.setProperty("formula", formula);

        rawFormulaSensor.setProperty("threshold", "4");
        TestSessionContext testSessionContext = new TestSessionContext(1);
        Map<String, Object> mapTestResult = new HashMap<String, Object>();
        JSONObject jsonObject = new JSONObject();
        JSONObject jsonRaw = new JSONObject();
        jsonRaw.put("value1", 1);
        jsonRaw.put("value2", 3);
        jsonObject.put("rawData", jsonRaw.toJSONString());
        mapTestResult.put("node1", jsonObject);
        mapTestResult.put("node2", jsonObject);
        testSessionContext.setAttribute(NodeSessionParams.RAW_DATA, mapTestResult);
        TestResult testResult = rawFormulaSensor.execute(testSessionContext);
        double value = Utils.getDouble(((JSONObject) (new JSONParser().parse(testResult.getRawData()))).get("value"));
        System.out.println("formula = " + formula);
        System.out.println("value = " + value);
        assertEquals(value, 1+3.0);
    }
    public void testCalculationStates() throws ParseException {
        RawFormulaSensor rawFormulaSensor = new RawFormulaSensor();
        rawFormulaSensor.setProperty("formula", "node1->value1 + node2->value2");

        rawFormulaSensor.setProperty("threshold", "4");
        TestSessionContext testSessionContext = new TestSessionContext(1);
        Map<String, Object> mapTestResult = new HashMap<String, Object>();
        JSONObject jsonObject = new JSONObject();
        JSONObject jsonRaw = new JSONObject();
        jsonRaw.put("value1", 1);
        jsonRaw.put("value2", 3);
        jsonObject.put("rawData", jsonRaw.toJSONString());
        mapTestResult.put("node1", jsonObject);
        mapTestResult.put("node2", jsonObject);
        testSessionContext.setAttribute(NodeSessionParams.RAW_DATA, mapTestResult);
        TestResult testResult = rawFormulaSensor.execute(testSessionContext);
        System.out.println(testResult.getObserverState());
        System.out.println(testResult.getRawData());
        assertEquals("EQUAL", testResult.getObserverState());


        rawFormulaSensor.setProperty("threshold", "3");
        testResult = rawFormulaSensor.execute(testSessionContext);
        System.out.println(testResult.getObserverState());
        System.out.println(testResult.getRawData());
        assertEquals("ABOVE", testResult.getObserverState());

        rawFormulaSensor.setProperty("threshold", "5");
        testResult = rawFormulaSensor.execute(testSessionContext);
        System.out.println(testResult.getObserverState());
        System.out.println(testResult.getRawData());
        assertEquals("BELOW", testResult.getObserverState());

        rawFormulaSensor.setProperty("formula", "node1->value1 / node2->value2 + 3 * ( node1->value1 + node2->value2 )");
        testResult = rawFormulaSensor.execute(testSessionContext);
        System.out.println(testResult.getObserverState());
        System.out.println(testResult.getRawData());
        assertEquals("ABOVE", testResult.getObserverState());
        Double value1 = Utils.getDouble(((JSONObject) (new JSONParser().parse(testResult.getRawData()))).get("value"));

        rawFormulaSensor.setProperty("formula", "node1->value1 / node2->value2 + 3 * (node1->value1 + node2->value2)");
        testResult = rawFormulaSensor.execute(testSessionContext);
        System.out.println(testResult.getObserverState());
        System.out.println(testResult.getRawData());
        assertEquals("ABOVE", testResult.getObserverState());
        Double value2 = Utils.getDouble(((JSONObject) (new JSONParser().parse(testResult.getRawData()))).get("value"));
        assertEquals(value1, value2);
        assertEquals(value1, 12.33, 0.1);


        rawFormulaSensor.setProperty("formula", "node1->value1 - node1->value1");
        rawFormulaSensor.setProperty("threshold", 0);
        testResult = rawFormulaSensor.execute(testSessionContext);
        System.out.println(testResult.getObserverState());
        System.out.println(testResult.getRawData());
        assertEquals("EQUAL", testResult.getObserverState());
        value1 = Utils.getDouble(((JSONObject) (new JSONParser().parse(testResult.getRawData()))).get("value"));
        assertEquals(value1, 0.);
    }
}
