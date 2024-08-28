/*******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.common.service.ns.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility functions (static) used in the Notification Service code.
 */
public class Utils {

  private static final Logger LOGGER = LogManager.getLogger(Utils.class.getName());

  private static final String SIMPLE_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

  /**
   * Private constructor to avoid allocating this static class.
   */
  private Utils() {
  }

  /**
   * Create a time stamp with the current date and time.
   *
   * @return The "now" time stamp
   */
  public static Timestamp getCurrentTimestamp() {
    var tz = TimeZone.getTimeZone("GMT");
    var sdf = new SimpleDateFormat(SIMPLE_DATE_PATTERN);
    sdf.setTimeZone(tz);
    final String date = sdf.format(new Timestamp(System.currentTimeMillis()));
    return getStrToTimestamp(date);
  }

  /**
   * Convert a string to a time stamp.
   *
   * @param date The string with the date/time to be converted in format YYYY-MM-DD HH:MM:SS
   * @return The relevant timestamp, or null in case or wrong string
   */
  public static Timestamp getStrToTimestamp(final String date) {
    try {
      final var dateFormat = new SimpleDateFormat(SIMPLE_DATE_PATTERN);
      final var parsedDate = dateFormat.parse(date);
      return new Timestamp(parsedDate.getTime());
    } catch (final ParseException e) {
      return null;
    }
  }

  /**
   * Wait for a given period of time.
   *
   * @param msec The time to wait in milliseconds
   */
  public static void sleep(long msec) {
    try {
      TimeUnit.MILLISECONDS.sleep(msec);
    } catch (Exception ex) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * De-serialize an object of type T.
   *
   * @param <T> The class of the object to be deserialized
   * @param str The string with the JSON serialized content
   * @param type The expected class of the object
   * @return An instance of class T if deserialization is successful, otherwise null
   */
  public static <T> T deserialize(final String str, final Class<T> type) {
    // Use .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) to ignore unknown properties found in string
    try {
      return new ObjectMapper().readValue(str, type);
    } catch (IOException | NullPointerException | IllegalArgumentException ex) {
      LOGGER.info(ex.getMessage());
      return null;
    }
  }

  /**
   * Serialize in JSON a generic object.
   *
   * @param obj The object to be serialized
   * @return The string with the JSON (or null in case of failure)
   */
  public static String serialize(final Object obj) {
    try {
      return new ObjectMapper().writeValueAsString(obj);
    } catch (final IOException | NullPointerException e) {
      return null;
    }
  }

  /**
   * Convert a path expression representing one entry of the "fields" attribute of the SubscriptionFilter to a valid JsonPath
   * expression
   * @param fullPath The field entry (between commas in the "fields" attribute)
   * @return The relevant JSONPath expression
   */
  public static String convertPath(String fullPath) {
    var buffer = new StringBuilder();
    String[] pathParts = fullPath.contains(".") ? fullPath.trim().split("\\.") : new String[]{fullPath.trim()};
    for (String key : pathParts) {
      String actualKey = key.replace("[", "\\[");
      actualKey = actualKey.replace("]", "\\]");
      actualKey = actualKey.replace("*", "[0-9]*");
      buffer.append(actualKey);
      buffer.append(".");
    }
    buffer.deleteCharAt(buffer.length() - 1);
    return buffer.toString();
  }

  /**
   * Check if a given string is a valid JSON.
   *
   * @param data The data to be validated
   * @return True if data is valid, false otherwise
   */
  public static boolean isJsonDataValid(String data) {
    var ret = true;
    try {
      final var mapper = new ObjectMapper();
      mapper.readTree(data);
    } catch (IOException e) {
      ret = false;
    }
    return ret;
  }

  static final Set<String> protocols = new HashSet<>(Arrays.asList("http", "https"));

  public static boolean isURI(String str) {
    int colon = str.indexOf(':');
    if (colon < 3) {
      return false;
    }

    String proto = str.substring(0, colon).toLowerCase();
    if (!protocols.contains(proto)) {
      return false;
    }

    try {
      var uri = new URI(str);
      if (protocols.contains(proto)) {
        if (uri.getHost() == null) {
          return false;
        }

        String path = uri.getPath();
        if (path != null) {
          for (int i = path.length() - 1; i >= 0; i--) {
            if ("?<>:*|\"".indexOf(path.charAt(i)) > -1) {
              return false;
            }
          }
        }
      }
      return true;
    } catch (Exception ex) {
      return false;
    }
  }

}
