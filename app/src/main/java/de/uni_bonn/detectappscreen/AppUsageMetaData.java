package de.uni_bonn.detectappscreen;

import java.util.LinkedList;
import java.util.List;

/**
 * Meta data for app usage data entries, containing additional information needed
 * for formatting, todo: among other things
 */
public class AppUsageMetaData {
    private List<AppUsageDataEntry> dataEntries;
    private int level;
    public AppUsageMetaData() {
        this.dataEntries = new LinkedList<>();
        this.level = 0;
    }
    public AppUsageMetaData(List<AppUsageDataEntry> entries) {
        this.dataEntries = entries;
        this.level = 0;
    }
    public AppUsageMetaData(List<AppUsageDataEntry> entries, int level) {
        this.dataEntries = entries;
        this.level = level;
    }

    public List<AppUsageDataEntry> getDataEntries() {
        return dataEntries;
    }

    public int getLevel() {
        return level;
    }
}