/**
 * User: pizuricv
 * Date: 6/4/13
 */

package com.ai.myplugin.sensor;

import com.ai.bayes.plugins.BNSensorPlugin;
import com.ai.bayes.scenario.TestResult;
import com.ai.myplugin.util.Rest;
import com.ai.myplugin.util.Utils;
import com.ai.util.resource.TestSessionContext;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class StockAbstractSensor implements BNSensorPlugin{

    public static final String STOCK = "stock";
    public static final String THRESHOLD = "threshold";
    static final String server = "http://finance.yahoo.com/d/quotes.csv?s=";
    Map<String, Object> propertiesMap = new ConcurrentHashMap<String, Object>();
    String [] states = {"Below", "Above"};
    private static final String FORMAT_QUERY = "&f=l1vhgm4p2d1t1";
    public static final String MOVING_AVERAGE = "MOVING_AVERAGE";
    public static final String PRICE = "PRICE";
    public static final String VOLUME = "VOLUME";
    public static final String PERCENT = "PERCENT";
    public static final String HIGH = "HIGH";
    public static final String LOW = "LOW";

    protected abstract String getTag();
    protected abstract String getSensorName();

    @Override
    public String[] getRequiredProperties() {
        return new String[]{STOCK, THRESHOLD};
    }

    @Override
    public void setProperty(String string, Object obj) {
        if(string.equalsIgnoreCase(STOCK) ) {
            propertiesMap.put(STOCK, obj.toString());
        } else if(string.equalsIgnoreCase(THRESHOLD)){
            propertiesMap.put(THRESHOLD, obj.toString()) ;
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
        return "Stock exchange sensor";
    }

    @Override
    public TestResult execute(TestSessionContext testSessionContext) {
        boolean testSuccess = true;
        final Double threshold = Utils.getDouble(getProperty(THRESHOLD));
        final String tag = getTag();
        System.out.println("Properties are " + getProperty(STOCK) + ", " + tag + ", "+threshold);
        String urlPath = server+ getProperty(STOCK) + FORMAT_QUERY;

        String stringToParse = null;
        try {
            stringToParse = Rest.httpGet(urlPath);
            System.out.println("Response for " + getProperty(STOCK) + " >>" + stringToParse);
        } catch (Exception e) {
            testSuccess = false;
        }

        final ConcurrentHashMap<String, Double> hashMap = new ConcurrentHashMap<String, Double>();

        if(testSuccess){
            StringTokenizer stringTokenizer = new StringTokenizer(stringToParse,",");
            parseOutput("PRICE", hashMap, stringTokenizer);
            parseOutput("VOLUME", hashMap, stringTokenizer);
            parseOutput("HIGH", hashMap, stringTokenizer);
            parseOutput("LOW", hashMap, stringTokenizer);
            parseOutput(MOVING_AVERAGE, hashMap, stringTokenizer);
            parseOutput("PERCENT", hashMap, stringTokenizer);

            //date:time
            SimpleDateFormat format =
                    new SimpleDateFormat("\"MM/dd/yyyy\" \"HH:mma\"");
            String dateString = stringTokenizer.nextToken() + " " + stringTokenizer.nextToken();
            try {
                Date parsed = format.parse(dateString);
                System.out.println("Date is " + parsed.toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        final boolean finalTestSuccess = testSuccess;
        return new TestResult() {
            @Override
            public boolean isSuccess() {
                return finalTestSuccess;
            }

            @Override
            public String getName() {
                return "Stock result";
            }

            @Override
            public String getObserverState() {
                if("PRICE".equalsIgnoreCase(tag))  {
                    if(hashMap.get("PRICE") < threshold)
                        return "Below";
                    return "Above";
                } else if("HIGH".equalsIgnoreCase(tag))  {
                    if(hashMap.get("HIGH") < threshold)
                        return "Below";
                    return "Above";
                }  else if("LOW".equalsIgnoreCase(tag))  {
                    if(hashMap.get("LOW") < threshold)
                        return "Below";
                    return "Above";
                }  else if("VOLUME".equalsIgnoreCase(tag))  {
                    if(hashMap.get("VOLUME") < threshold)
                        return "Below";
                    return "Above";
                }  else if("MOVING_AVERAGE".equalsIgnoreCase(tag))  {
                    if(hashMap.get("MOVING_AVERAGE") < threshold)
                        return "Below";
                    return "Above";
                } else if("PERCENT".equalsIgnoreCase(tag))  {
                    if(hashMap.get("PERCENT") < threshold)
                        return "Below";
                    return "Above";
                } else {
                    throw new RuntimeException("Error getting Stock result");
                }
            }

            @Override
            public List<Map<String, Number>> getObserverStates() {
                return null;
            }

            @Override
            public String getRawData() {
                String res = "";
                String sep = ",\r\n";
                int i = 0;
                int len = hashMap.size();
                for(Map.Entry<String, Double> key : hashMap.entrySet()){
                    if(i == len-1){
                        sep = "\r\n";
                    }
                    i++;
                    res += "\""+ key.getKey().toLowerCase() + "\" : " + key.getValue() + sep;
                }
                return "{" +
                        res +
                        "}";
            }
        } ;
    }

    private void parseOutput(String tag, Map<String, Double> parsing, StringTokenizer stringTokenizer) {
        try{
            String string = stringTokenizer.nextToken();
            Double value = Double.parseDouble(string.replaceAll("%", "").replaceAll("\"", ""));
            System.out.println(tag + " = " + value);
            parsing.put(tag, value);
        } catch (Exception e){
            System.err.println("Error parsing [" + tag + "] " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return getSensorName();
    }

    @Override
    public String[] getSupportedStates() {
        return states;
    }

    public static void main(String[] args){
        StockAbstractSensor stockSensor = new StockAbstractSensor() {
            @Override
            protected String getTag() {
                return "PRICE";
            }

            @Override
            protected String getSensorName() {
                return "Price sensor";
            }
        };
        stockSensor.setProperty(STOCK, "MSFT");
        stockSensor.setProperty(THRESHOLD, 36);
        System.out.println(Arrays.toString(stockSensor.getSupportedStates()));
        System.out.println(stockSensor.execute(null).getObserverState());


        stockSensor.setProperty(STOCK, "GOOG");
        stockSensor.setProperty(THRESHOLD, "800.0");
        System.out.println(stockSensor.execute(null).getObserverState());

        stockSensor.setProperty(STOCK, "BAR.BR");
        stockSensor.setProperty(THRESHOLD, "-1.0");
        System.out.println(stockSensor.execute(null).getObserverState());
        System.out.println(stockSensor.execute(null).getRawData());
    }
}