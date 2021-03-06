package com.pranveraapp.common.extensibility.context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
/**
 * Created by elion on 25/01/16.
 */
public class StandardConfigLocations {

    private static final Log LOG = LogFactory.getLog(StandardConfigLocations.class);
    public static final String EXTRACONFIGLOCATIONSKEY = "extra.config.locations";

    public static final int ALLCONTEXTTYPE = 0;
    public static final int WEBCONTEXTTYPE = 1;
    public static final int SERVICECONTEXTTYPE = 2;
    public static final int TESTCONTEXTTYPE = 3;
    public static final int APPCONTEXTTYPE = 4;

    public static String[] retrieveAll(int contextType) throws IOException {
        String[] response;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(StandardConfigLocations.class.getResourceAsStream("StandardConfigLocations.txt")));
            ArrayList<String> items = new ArrayList<String>();
            boolean eof = false;
            while (!eof) {
                String temp = reader.readLine();
                if (temp == null) {
                    eof = true;
                } else {
                    addContextFile(contextType, items, temp);
                }
            }

            String extraConfigFiles = System.getProperty(EXTRACONFIGLOCATIONSKEY);
            if (extraConfigFiles != null) {
                String[] files = extraConfigFiles.split(" ");
                for (String file : files) {
                    addContextFile(contextType, items, file);
                }
            }

            response = new String[]{};
            response = items.toArray(response);
        } finally {
            if (reader != null) {
                try{ reader.close(); } catch (Throwable e) {
                    LOG.error("Unable to merge source and patch locations", e);
                }
            }
        }

        return response;
    }

    private static void addContextFile(int contextType, ArrayList<String> items, String temp) {
        if (!temp.startsWith("#") && temp.trim().length() > 0 && StandardConfigLocations.class.getClassLoader().getResource(temp.trim()) != null) {
            if (
                    contextType == ALLCONTEXTTYPE  ||
                            ((contextType == WEBCONTEXTTYPE || contextType == APPCONTEXTTYPE) && temp.indexOf("-web-") >= 0) ||
                            ((contextType == SERVICECONTEXTTYPE || contextType == TESTCONTEXTTYPE || contextType == APPCONTEXTTYPE) && temp.indexOf("-web-") < 0 && temp.indexOf("-test") < 0 && temp.indexOf("-admin-") < 0) ||
                            ((contextType == SERVICECONTEXTTYPE || contextType == TESTCONTEXTTYPE || contextType == APPCONTEXTTYPE) && temp.indexOf("-admin-applicationContext-persistence") >= 0) ||
                            (contextType == TESTCONTEXTTYPE && (temp.indexOf("-test") >= 0 || temp.indexOf("-web-") >= 0))
                    ){
                items.add(temp.trim());
            }
        }
    }
}
