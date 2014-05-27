/**
 * Created by User: veselin
 * On Date: 18/03/14
 */

package com.ai.myplugin.action;

import com.ai.api.*;
import com.ai.myplugin.util.NodeConfig;
import com.ai.myplugin.util.RawDataParser;
import com.ai.myplugin.util.Utils;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import twitter4j.internal.org.json.JSONObject;
import org.stringtemplate.v4.ST;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@PluginImplementation
public class NodeJSAction implements ActuatorPlugin{
    private static final Log log = LogFactory.getLog(NodeJSAction.class);
    private static final int WAIT_FOR_RESULT = 5;
    private static final String JAVA_SCRIPT = "javaScript";
    private String javaScriptCommand;
    private String nodePath = NodeConfig.getNodePath();
    private String workingDir = NodeConfig.getNodeDir();
    private int exitVal = -1;
    private String result = "";
    private AtomicBoolean done = new AtomicBoolean(false);

    @Override
    public void action(SessionContext testSessionContext) {
        log.info("execute "+ getName() + ", action type:" +this.getClass().getName());

        if(testSessionContext != null && testSessionContext.getAttribute(SessionParams.RAW_DATA) != null){
            Map sessionMap = (Map) testSessionContext.getAttribute(SessionParams.RAW_DATA);
            JSONObject jsonObject = new JSONObject(sessionMap);
            javaScriptCommand = "RAW_STRING = '"+jsonObject.toString() + "';\n" + javaScriptCommand;
        }

        File file;
        File dir = new File(workingDir);
        String javascriptFile = "";

        try {
            try {
                javascriptFile =  Long.toString(System.nanoTime()) + "runs.js";
                file = new File(dir+ File.separator + javascriptFile);
                BufferedWriter output = new BufferedWriter(new FileWriter(file));
                output.write(javaScriptCommand);
                output.close();
            } catch ( IOException e ) {
                e.printStackTrace();
                log.error(e.getMessage());
                throw new RuntimeException(e);
            }

            ProcessBuilder pb = new ProcessBuilder(nodePath, javascriptFile);
            pb.directory(new File(workingDir));
            Process process = pb.start();

            StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), StdType.ERROR);
            StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), StdType.OUTPUT);

            errorGobbler.start();
            outputGobbler.start();

            exitVal = process.waitFor();

            log.debug(getName() + " ExitValue: " + exitVal);
            file.delete();

            (new Runnable() {
                //waitForResult is not a timeout for the javaScriptCommand itself, but how long you wait before the stream of
                //output data is processed, should be really fast.
                private int waitForResult = WAIT_FOR_RESULT;
                @Override
                public void run() {
                    while(!done.get() && waitForResult > 0)
                        try {
                            Thread.sleep(1000);
                            System.out.print(".");
                            System.out.print(result);
                            waitForResult --;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                }
            } ).run();

        } catch (Throwable t) {
            log.error(t.getLocalizedMessage());
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @Override
    public String getName() {
        return "NodeJSAction";
    }

    @Override
    public Map<String,PropertyType> getRequiredProperties() {
        Map<String,PropertyType> map = new HashMap<>();
        if(getProperty(JAVA_SCRIPT) == null) {
            map.put(JAVA_SCRIPT, new PropertyType(DataType.STRING, true, true));
            return map;
        }
        Set<String> set = RawDataParser.parseKeyArgs((String) getProperty(JAVA_SCRIPT));
        Set<String> set2 = RawDataParser.getRuntimePropertiesFromTemplate((String) getProperty(JAVA_SCRIPT), "runtime_");
        set.removeAll(set2);
        set.add("javaScript");
        for(int i=0 ; i< set.size(); i++)
            map.put((String) set.toArray()[i], new PropertyType(DataType.STRING, true, true));
        return map;
    }

    @Override
    public void setProperty(String s, Object o) {
        if("javaScript".equals(s)){
            javaScriptCommand = o.toString();
        } else if ("nodePath".equals(s)){
            nodePath = o.toString();
        } else {
            Set<String> set = RawDataParser.parseKeyArgs((String) getProperty(JAVA_SCRIPT));
            if(set.contains(s)){
                String template = (String) getProperty(JAVA_SCRIPT);
                ST hello = new ST(template);
                try{
                    Utils.getDouble(o);
                } catch (Exception e){
                    o = "'" +o.toString() + "'";
                }
                hello.add(s, o);
                setProperty(JAVA_SCRIPT, hello.render());
            }
        }
    }

    @Override
    public Object getProperty(String s) {
        if("javaScript".endsWith(s)){
            return javaScriptCommand;
        }
        else{
            throw new RuntimeException("Property " + s + " not recognised by " + getName());
        }
    }

    @Override
    public String getDescription() {
        return "webscript action";
    }

    enum StdType {
        ERROR, OUTPUT
    }

    private class StreamGobbler extends Thread {
        InputStream is;
        private StdType stdType;

        StreamGobbler(InputStream is, StdType type) {
            this.is = is;
            this.stdType = type;
        }

        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;
                while ((line = br.readLine()) != null)
                    logLine(line, stdType);
                done.set(true);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        private void logLine(String line, StdType type) {
            if(type.equals(StdType.ERROR)){
                log.error("Error executing the script >" + line);
            } else{
                result += line;
                log.info(line);
            }
        }
    }

    public static void main(String [] args) {
        NodeJSAction nodeJSAction = new NodeJSAction();
        nodeJSAction.getRequiredProperties();
        String javaScript =  "a = { observedState:\"world\"};\n" +
                "console.log(a)" ;
        nodeJSAction.setProperty("javaScript", javaScript);
        nodeJSAction.action(null);
    }

}