/**
 * Created by User: veselin
 * On Date: 07/11/13
 */
package com.ai.myplugin.action;

import com.ai.api.*;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO clean up ugly
 */
@PluginImplementation
@PluginHeader(version = "1.0.1", author = "Veselin", category = "Java Script", iconURL = "http://app.waylay.io/icons/hook.png")
public class WebHookAction implements ActuatorPlugin{

    private static final Logger log = LoggerFactory.getLogger(WebHookAction.class);

    private static String HOOK_URL = "callback_URL";
    private URL hook;

    @Override
    public Map<String,PropertyType> getRequiredProperties() {
        Map<String,PropertyType> map = new HashMap<>();
        map.put(HOOK_URL, new PropertyType(DataType.STRING, true, true));
        return map;
    }

    @Override
    public void setProperty(String string, Object o) {
        if(string.equalsIgnoreCase(HOOK_URL)) {
            try {
                hook = new URL(o.toString());
            } catch (MalformedURLException e) {
                log.error(e.getLocalizedMessage(), e);
            }} else {
            throw new RuntimeException("Property "+ string + " not in the required settings");
        }
    }

    @Override
    public Object getProperty(String s) {
        if(s.equalsIgnoreCase(HOOK_URL))
            return hook;
        return null;
    }

    @Override
    public String getDescription() {
        return "Web Hook Action";
    }

    @Override
    public ActuatorResult action(SessionContext testSessionContext) {
        if(hook == null){
            throw new RuntimeException("URL post hook not defined");
        }
        Map map = (Map) testSessionContext.getAttribute(SessionParams.RAW_DATA);
        Long id = testSessionContext.getId();
        String actionNode = (String) testSessionContext.getAttribute(SessionParams.ACTION_NODE);
        String targetState = (String) testSessionContext.getAttribute(SessionParams.TARGET_STATE);
        String target = (String) testSessionContext.getAttribute(SessionParams.TARGET_NODE);
        String node = (String) testSessionContext.getAttribute(SessionParams.NODE_NAME);
        String nodeState = (String) testSessionContext.getAttribute(SessionParams.NODE_TRIGGERED_STATE);
        String resource = (String) testSessionContext.getAttribute(SessionParams.RESOURCE);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("rawData", map);
        jsonObject.put("actionNode", actionNode);
        jsonObject.put("targetState", targetState);
        jsonObject.put("target", target);
        jsonObject.put("node", node);
        jsonObject.put("nodeState", nodeState);
        jsonObject.put("resource", resource);
        jsonObject.put("id", id);

        // FIXME use the Rest class instead of this connection mess

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) hook.openConnection();
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
            return new ActuatorFailedResult(e.getMessage());
        }
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setInstanceFollowRedirects(false);
        try {
            connection.setRequestMethod("POST");
        } catch (ProtocolException e) {
            log.error(e.getLocalizedMessage(), e);
            return new ActuatorFailedResult(e.getMessage());
        }

        // FIXME this is wrong, putting json in form-urlencoded
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("charset", "utf-8");
        connection.setRequestProperty("Content-Length", "" + Integer.toString(jsonObject.toJSONString().getBytes().length));
        connection.setUseCaches (false);

        DataOutputStream wr = null;
        try {
            wr = new DataOutputStream(connection.getOutputStream ());
            wr.writeBytes(jsonObject.toJSONString());
            wr.flush();
            wr.close();
            connection.disconnect();
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
            return new ActuatorFailedResult(e.getMessage());
        }
        return ActuatorSuccessResult.INSTANCE;
    }

    @Override
    public String getName() {
        return "WebHookAction";
    }
}
