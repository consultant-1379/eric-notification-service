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
package com.ericsson.oss.common.service.ns.filter;

import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import com.jayway.jsonpath.DocumentContext;
import java.util.ArrayList;
import java.util.List;
import com.github.wnameless.json.flattener.JsonFlattener;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;

/**
 * PayloadParser analyzes and keeps the values of JSON string. These attribute/values can be queried and checked eventually.
 */
public class EventPayload {

  /**
   * The JSON payload to be processed.
   */
  private final String jsonPayload;
  /**
   * The JsonPath context used to perform operations
   */
  private final DocumentContext ctx;

  /**
   * The constructor of the parser. Parses the payload string and stores all the attributes/values in a map to be used for value
   * retrieval.
   *
   * @param str The jsonPayload string
   */
  public EventPayload(String str) {
    jsonPayload = str;
    Configuration config = Configuration.builder().options(Option.DEFAULT_PATH_LEAF_TO_NULL).build();
    ctx = JsonPath.using(config).parse(str);
  }

  /**
   * Retrieve, as a generic Object, the value associated to a given attribute. The attribute name can have "points" to represent
   * nesting in the JSON structure. E.g. the name a.b.c represents a field "c" of the subfield "b" of the field "a".
   *
   * @param attr The attribute name
   * @return The associated value (or null if the field cannot be fount)
   */
  public Object getJsonAttrValue(String attr) {
    return ctx.read(attr);
  }

  /**
   * Create the projection of the current jsonPayload including just the mentioned fields (using dotted notation).
   *
   * @param includes The list of fields to be included
   * @return The JSON string representing the projected jsonPayload
   */
  public String doProjection(List<String> includes) {
    // Configuration for the JsonPath library
    Configuration config = Configuration.builder().options(Option.DEFAULT_PATH_LEAF_TO_NULL).build();
    // Parse the jsonPayload into JsonPath. Need original jsonPayload string. Cannot use ctx, in order to prevent errors 
    // when doProjection is called multiple times (ctx would be overwritten).
    DocumentContext context = JsonPath.using(config).parse(jsonPayload);
    // Read all the json paths in the payload (using json flattener)
    List<String> excludes = getPaths(jsonPayload);
    // Remove from them the list of paths to be kept (as in fields attribute of the subscription)
    List<String> todelete = new ArrayList<>();
    includes.forEach(include -> 
            excludes.stream().filter(exclude -> exclude.matches(include)).forEachOrdered(todelete::add)); // was startsWith(include)
    excludes.removeAll(todelete);
    // Remove the remaining paths from the context
    excludes.forEach(context::delete);
    // Clean up the context removing all the empty structures and arrays
    clean(context);
    // Return the reduced json string
    return context.jsonString();
  }

  /**
   * Return all the paths (items in dotted notation) of the provided json string.
   *
   * @param json The Json string to be analyzed
   * @return The list of flattened attributes included in the json string
   */
  private List<String> getPaths(String json) {
    var flat = new JsonFlattener(json);
    Map<String, Object> flatMap = flat.flattenAsMap();
    List<String> ret = new ArrayList<>();
    ret.addAll(flatMap.keySet());
    return ret;
  }

  /**
   * Clean the document contexts, removing all the empty arrays and structures. Used by projection to provide json including just
   * the wanted fields
   *
   * @param ctx The context to be cleaned
   */
  private void clean(DocumentContext ctx) {
    boolean deleted;
    do {
      List<String> paths = getPaths(ctx.jsonString());
      deleted = false;
      for (String path : paths) {
        Object obj = ctx.read(path);
        if (obj == null
                || ((obj instanceof Map) && ((Map<String, Object>) obj).isEmpty())
                || ((obj instanceof List) && ((List) obj).isEmpty())) {
          ctx.delete(path);
          deleted = true;
          break;
        }
      }
    } while (deleted);
  }
}
