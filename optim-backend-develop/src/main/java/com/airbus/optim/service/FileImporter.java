package com.airbus.optim.service;

import org.apache.poi.ss.usermodel.Row;

import java.io.InputStream;

public interface FileImporter {
    void readFile();
    void processFileInParallel(InputStream inputStream);
    void processRow(Row row);
    void shutdown();
}
