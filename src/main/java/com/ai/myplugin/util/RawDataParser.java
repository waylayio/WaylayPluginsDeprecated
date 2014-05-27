/**
 * Created by User: veselin
 * On Date: 04/11/13
 */

package com.ai.myplugin.util;

import com.ai.api.SessionContext;
import com.ai.api.SessionParams;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.ST;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class RawDataParser {
    private static final Logger log = LoggerFactory.getLogger(RawDataParser.class);

    public static String parseTemplateFromContext(String template, SessionContext testSessionContext){
        return parseTemplateFromRawMap(template, (Map) testSessionContext.getAttribute(SessionParams.RAW_DATA));
    }

    public static String parseTemplateFromRawMap(String template, Map sessionMap){
        log.debug("parseTemplateFromRawMap " + template);
        Set<String> set = parseKeyArgs(template);
        Map<String, String> map = new ConcurrentHashMap<String, String>();
        JSONObject jsonObject = new JSONObject(sessionMap);
        for(String key: set){
            Object obj = findObjForKey(key, jsonObject);
            if(obj != null)
                map.put(key, obj.toString());
        }
        //template keys can't have dots
        for(String key : map.keySet()) {
            template = template.replaceAll(key, key.replaceAll("\\.",""));
        }
        ST hello = new ST(template);
        for(String key : map.keySet()) {
            hello.add(key.replaceAll("\\.",""), map.get(key));
        }
        return hello.render();
    }

    public static Object findObjForKey(String key, JSONObject jsonObject) {
        return findObjForKey(key, jsonObject, null);
    }

    /**
     *
     * @param key example: node1.name.value , if the value is an array, it needs to continue like:
     *        node1.name.value.first.value2 OR node1.name.value.last.value2 OR node1.name.value.#number.value2
     *            if nothing given after array, it will return the first element
     * @param jsonObject    json object to be parsed
     * @param nodeSeparator separator between the node (node1) an the object (name.value), default is "."
     * @return object at the leave to which the key was pointing
     */
    private static Object findObjForKey(String key, JSONObject jsonObject, String nodeSeparator) {
        log.debug("findObjForKey "+key + " , "+jsonObject.toJSONString());
        if(nodeSeparator == null)
            nodeSeparator = ".";   //how the node is separated from the raw data . or -> for instance
        String delims = "."; //json notation for walking the graph
        String nodeKey;
        if(key.indexOf(nodeSeparator) > -1)   {
            nodeKey = key.substring(0, key.indexOf(nodeSeparator));
            key = key.substring(key.indexOf(nodeSeparator));
            JSONObject jso;
            if(jsonObject.get(nodeKey) != null){
                Object obj = null;
                //first node in the tree must be a json object, not an array
                jso = (JSONObject) jsonObject.get(nodeKey);
                StringTokenizer tokens = new StringTokenizer(key, delims);
                while (tokens.hasMoreTokens()){
                    obj = jso.get(tokens.nextElement());
                    if(obj instanceof JSONObject)
                        jso = (JSONObject) obj;
                    else if(obj instanceof JSONArray) {
                        if(!tokens.hasMoreTokens())  {
                            break;
                        }
                        else{
                            String nextT = tokens.nextToken();
                            if(nextT.equalsIgnoreCase("first"))
                                jso = (JSONObject) ((JSONArray) obj).get(0);
                            else if(nextT.equalsIgnoreCase("last"))
                                jso = (JSONObject) ((JSONArray) obj).get(((JSONArray) obj).size()-1);
                            else {
                                try {
                                    Double num = Utils.getDouble(nextT);
                                    jso = (JSONObject) ((JSONArray) obj).get(num.intValue());
                                } catch (Exception e){
                                    log.warn("failed to fetch the next number, returning first object instead");
                                    jso = (JSONObject) ((JSONArray) obj).get(0);
                                }
                            }
                        }
                    }
                    else {
                        try{
                            jso = (JSONObject) new JSONParser().parse(obj.toString());
                        } catch (Exception e){
                            break;
                        }
                    }
                }
                if(obj != null){
                    log.debug("Found for " + nodeKey + "[" +key + "] = "+obj.toString()) ;
                    return obj;
                }
            }
        }
        return null;
    }


    /**
     * find strings that are between < >
     * @param template
     * @return
     */
    public static Set<String> parseKeyArgs(String template){
        log.debug("parseKeyArgs "+template);
        String delims = ">";
        Set<String> set = new HashSet<String>();
        StringTokenizer tokens = new StringTokenizer(template, delims);
        while (tokens.hasMoreTokens()){
            String str = tokens.nextToken();
            if(str.indexOf("<") > -1)
                str = str.substring(str.indexOf("<") + 1);
            if(str.length() > 1 && (str.indexOf(" ") == -1 || str.indexOf(",") > -1))
                set.add(str);
        }
        log.debug("found keys "+Arrays.asList(set).toString());
        return set;
    };

    public static Set<String> getRuntimePropertiesFromTemplate(String template, String startString){
        if(template == null || startString == null)
            return new HashSet<>();
        Set<String> set = parseKeyArgs(template);
        Set<String> ret = new HashSet<String>();
        for(String key : set)
            if(key.contains(startString) ||  key.contains(startString.toLowerCase()) ||
                    key.contains(startString.toUpperCase()))
                ret.add(key);
        return ret;
    };


    public static String giveTargetNodeStateAsString(SessionContext testSessionContext) {
        String target = (String) testSessionContext.getAttribute(SessionParams.TARGET_NODE);
        String targetState = (String) testSessionContext.getAttribute(SessionParams.TARGET_STATE);
        String node = (String) testSessionContext.getAttribute(SessionParams.NODE_NAME);
        String nodeState = (String) testSessionContext.getAttribute(SessionParams.NODE_TRIGGERED_STATE);
        return "\n\nTarget "+target + " in the state: " + targetState + "\n" +
                "Node "+ node + " in the state: " + nodeState;
    }
}

