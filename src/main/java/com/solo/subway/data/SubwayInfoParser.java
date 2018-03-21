package com.solo.subway.data;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.solo.subway.util.HttpUtil;
import com.solo.subway.util.Station;
import com.solo.subway.util.SubwayLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SubwayInfoParser {
    private static String url = "http://map.amap.com/service/subway?_1469083453978&srhdata=1100_drw_beijing.json";
    private static SubwayInfoParser instance = new SubwayInfoParser();
    private static Logger logger = LoggerFactory.getLogger(SubwayInfoParser.class);

    private Map<String, SubwayLine> lineName = new HashMap<>();
    private Map<String, Station> stations = new HashMap<>();
    private SubwayInfoParser(){
    }

    public static SubwayInfoParser getInstace() {
        return instance;
    }

    public void parse() throws IOException {
        String result = HttpUtil.httpGet(url);
        logger.info("http info " + result);
        Map<String, Object> json = (Map<String, Object>) JSON.parse(result);

        JSONArray lines = (JSONArray) json.get("l");
        Iterator iterator = lines.iterator();
        while (iterator.hasNext()) {
            Map<String, Object> line = (Map<String, Object>) iterator.next();
            logger.info("handle line " + line);
            SubwayLine subwayLine = new SubwayLine();
            subwayLine.setId(line.get("ls").toString());
            subwayLine.setName(line.get("ln").toString());
            if (line.get("lo").equals("1")) {
                subwayLine.setCircle(true);
            } else {
                subwayLine.setCircle(false);
            }
            lineName.put(line.get("ls").toString(), subwayLine);

            JSONArray lineStations = (JSONArray) line.get("st");
            parseStation(lineStations, subwayLine.isCircle());
        }

        for (Station station : stations.values()) {
            logger.info(station.getName());
            for (String line : station.getLines()) {
                logger.info(station.getName() + " in " + lineName.get(line).getName());
            }
            for (String next : station.getNextStations()) {
                logger.info(station.getName() + " next to " + stations.get(next).getName());
            }
        }
    }

    private void parseStation(JSONArray lineStations, boolean isCircleLine) {
        Iterator iterator = lineStations.iterator();
        Station head = null;
        Station previous = null;
        while (iterator.hasNext()) {
            Map<String, String> station = (Map<String, String>) iterator.next();
            logger.info("handle station " + station);
            Station station1 = stations.get(station.get("poiid"));

            if (station1 == null) {
                station1 = new Station();
            }
            station1.setId(station.get("poiid"));
            station1.setName(station.get("n"));
            station1.setPinyin(station.get("sp"));
            station1.setPosition(station.get("sl"));
            String[] staionLines = station.get("r").split("\\|");
            for (String l : staionLines) {
                station1.addLine(l);
            }
            if (previous != null) {
                previous.addStation(station1.getId());
                station1.addStation(previous.getId());
            }

            stations.put(station1.getId(), station1);

            if (head == null) {
                head = station1;
            }

            previous = station1;

        }
        if (head != null && previous != null) {
            previous.addStation(head.getId());
            head.addStation(previous.getId());
        }
    }

    public static void main(String args[]) throws IOException {
        SubwayInfoParser.getInstace().parse();
    }
}
