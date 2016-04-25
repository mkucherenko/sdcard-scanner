package com.mkucherenko.sdcard_scanner.model;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Zim on 4/23/2016.
 */
public class ScanResults {

    private double mAvgFileSize;
    private List<Map.Entry<String, Integer>> mMostRecentExt;
    private List<File> mLargestFiles;

    public ScanResults(double avgFileSize, List<File> largestFiles, List<Map.Entry<String, Integer>> mostRecentExt) {
        mAvgFileSize = avgFileSize;
        mLargestFiles = largestFiles;
        mMostRecentExt = mostRecentExt;
    }

    public double getAvgFileSize() {
        return mAvgFileSize;
    }

    public List<Map.Entry<String, Integer>> getMostRecentExt() {
        return mMostRecentExt;
    }

    public List<File> getLargestFiles() {
        return mLargestFiles;
    }
}
