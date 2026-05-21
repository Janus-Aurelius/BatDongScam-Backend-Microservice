package com.se.bds.core.rtm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RtmParser {

    public static class RtmInfo {
        public String reqId;
        public String reqDesc;
        public String tcDesc;
        public String category; // Derived from Req ID prefix or similar if needed

        public RtmInfo(String reqId, String reqDesc, String tcDesc) {
            this.reqId = reqId;
            this.reqDesc = reqDesc;
            this.tcDesc = tcDesc;
            this.category = "Functional"; // Default
        }
    }

    /**
     * Parses the RTM CSV and returns a map of TC ID -> RtmInfo.
     */
    public static Map<String, RtmInfo> parseRtm(String filePath) {
        Map<String, RtmInfo> rtmMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // Skip 3 header rows
            br.readLine();
            br.readLine();
            br.readLine();

            String line;
            while ((line = br.readLine()) != null) {
                List<String> columns = parseCsvLine(line);
                if (columns.size() < 5) continue;

                String reqId = columns.get(1);
                String reqDesc = columns.get(2);
                String tcIdsRaw = columns.get(3);
                String tcDesc = columns.get(4);

                RtmInfo info = new RtmInfo(reqId, reqDesc, tcDesc);
                
                // Clean TC IDs and map each one
                String[] ids = tcIdsRaw.replace("\"", "").split(",");
                for (String id : ids) {
                    rtmMap.put(id.trim(), info);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse RTM CSV: " + e.getMessage());
        }
        return rtmMap;
    }

    /**
     * Simple CSV parser to handle quoted strings containing commas.
     */
    private static List<String> parseCsvLine(String line) {
        List<String> columns = new ArrayList<>();
        Matcher m = Pattern.compile("(?:^|,)(?:\"([^\"]*(?:\"\"[^\"]*)*)\"|([^,]*))").matcher(line);
        while (m.find()) {
            String col = m.group(1) != null ? m.group(1).replace("\"\"", "\"") : m.group(2);
            columns.add(col != null ? col : "");
        }
        return columns;
    }
}
