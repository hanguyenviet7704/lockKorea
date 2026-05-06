package com.lockerkorea.ui.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Data provider for reading test data from CSV files
 */
public class CSVDataProvider {

    /**
     * Read all data from CSV file
     * @param resourcePath path to CSV file in resources (e.g., "test-data/users.csv")
     * @return List of maps, each map is a row with column headers as keys
     */
    public static List<Map<String, String>> readAll(String resourcePath) throws IOException {
        List<Map<String, String>> data = new ArrayList<>();
        List<String> lines = readLines(resourcePath);

        if (lines.isEmpty()) {
            return data;
        }

        // First line is header
        String[] headers = splitCSVLine(lines.get(0));

        for (int i = 1; i < lines.size(); i++) {
            String[] values = splitCSVLine(lines.get(i));
            Map<String, String> row = new LinkedHashMap<>();
            for (int j = 0; j < headers.length && j < values.length; j++) {
                row.put(headers[j].trim(), values[j].trim());
            }
            data.add(row);
        }

        return data;
    }

    /**
     * Read CSV as array of arrays (for JUnit parameterized tests)
     * @param resourcePath path to CSV file
     * @return List of Object arrays
     */
    public static List<Object[]> readAsObjects(String resourcePath) throws IOException {
        List<Object[]> data = new ArrayList<>();
        List<Map<String, String>> rows = readAll(resourcePath);

        for (Map<String, String> row : rows) {
            data.add(row.values().toArray());
        }

        return data;
    }

    /**
     * Read specific columns from CSV
     */
    public static List<Map<String, String>> readSelectedColumns(String resourcePath, String... columns) throws IOException {
        List<Map<String, String>> allData = readAll(resourcePath);
        List<Map<String, String>> filtered = new ArrayList<>();

        for (Map<String, String> row : allData) {
            Map<String, String> filteredRow = new LinkedHashMap<>();
            for (String col : columns) {
                if (row.containsKey(col)) {
                    filteredRow.put(col, row.get(col));
                }
            }
            filtered.add(filteredRow);
        }

        return filtered;
    }

    /**
     * Get a single row by index (0-based, after header)
     */
    public static Map<String, String> getRow(String resourcePath, int index) throws IOException {
        List<Map<String, String>> data = readAll(resourcePath);
        if (index < 0 || index >= data.size()) {
            throw new IndexOutOfBoundsException("Row index " + index + " out of bounds for CSV file: " + resourcePath);
        }
        return data.get(index);
    }

    /**
     * Get value by row index and column name
     */
    public static String getValue(String resourcePath, int rowIndex, String columnName) throws IOException {
        Map<String, String> row = getRow(resourcePath, rowIndex);
        return row.get(columnName);
    }

    /**
     * Find rows matching a condition
     */
    public static List<Map<String, String>> findRows(String resourcePath, String column, String value) throws IOException {
        List<Map<String, String>> allData = readAll(resourcePath);
        List<Map<String, String>> matches = new ArrayList<>();

        for (Map<String, String> row : allData) {
            if (value.equals(row.get(column))) {
                matches.add(row);
            }
        }

        return matches;
    }

    /**
     * Read lines from resource file
     */
    private static List<String> readLines(String resourcePath) throws IOException {
        InputStream input = CSVDataProvider.class.getClassLoader().getResourceAsStream(resourcePath);
        if (input == null) {
            throw new FileNotFoundException("CSV file not found: " + resourcePath);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip empty lines and comments
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    lines.add(line);
                }
            }
            return lines;
        }
    }

    /**
     * Simple CSV splitter (handles basic comma-separated values)
     * For complex CSV with quotes, consider using Apache Commons CSV
     */
    private static String[] splitCSVLine(String line) {
        // Basic split on commas - does not handle quoted commas
        return line.split(",");
    }

    /**
     * Write data to CSV file (for generating test reports or data)
     */
    public static void writeAll(String resourcePath, List<Map<String, String>> data, String... headers) throws IOException {
        // Not typically used in tests, but could be for result export
        throw new UnsupportedOperationException("Write not implemented yet");
    }
}
