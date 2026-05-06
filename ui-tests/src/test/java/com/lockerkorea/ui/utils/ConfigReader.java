package com.lockerkorea.ui.utils;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Utility class for reading configuration properties
 */
public class ConfigReader {
    private static final String CONFIG_FILE = "config/test-config.properties";
    private static Properties properties;

    static {
        loadProperties();
    }

    private static void loadProperties() {
        properties = new Properties();
        try (InputStream input = ConfigReader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new FileNotFoundException("Configuration file not found: " + CONFIG_FILE);
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration: " + CONFIG_FILE, e);
        }
    }

    /**
     * Get property value as string
     */
    public static String getString(String key) {
        return properties.getProperty(key);
    }

    /**
     * Get property value as string with default
     */
    public static String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Get property value as int
     */
    public static int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get property value as boolean
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    /**
     * Get all properties
     */
    public static Properties getAll() {
        return properties;
    }

    /**
     * Reload properties (useful for testNG @BeforeSuite if needed)
     */
    public static void reload() {
        loadProperties();
    }
}
