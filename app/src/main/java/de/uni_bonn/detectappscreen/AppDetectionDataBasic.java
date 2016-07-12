package de.uni_bonn.detectappscreen;

/**
 * Interface that only provides basic information and modification options for AppDetectionData objects
 * TODO: Currently unused, probably best to delete
 */
public interface AppDetectionDataBasic {
    boolean isFinishedLoading();
    String getPackageName();
    void setPerformLayoutChecks(boolean value);
    boolean getPerformLayoutChecks();
    void setPerformOnClickChecks(boolean value);
    boolean getPerformOnClickChecks();
}
