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

import static org.junit.Assert.assertTrue;

import com.ericsson.oss.common.service.ns.util.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
class PayloadTest extends FilterBase {

  /**
   * Test a simple JSON string for different types of attributes.
   */
  @Test
  void testGetJsonAttrValue() {
    String json = getSimpleEvent();
    assertTrue(Utils.isJsonDataValid(json));
    EventPayload attrs = new EventPayload(json);

    assertEquals("a-value", (String) attrs.getJsonAttrValue("a"));
    assertEquals("b-value", (String) attrs.getJsonAttrValue("b"));
    assertEquals(2, (int) attrs.getJsonAttrValue("c"));
    assertEquals(2.0, (double) attrs.getJsonAttrValue("d.a"), 0.0001);
    assertEquals(Arrays.asList(1, 2, 3), (List<Integer>) attrs.getJsonAttrValue("d.b"));
    assertEquals(Arrays.asList("a", "b"), (List<String>) attrs.getJsonAttrValue("e"));
  }

  /**
   * Test a real notification TMF641 for some fields.
   */
  @Test
  void testGetJsonAttrValueReal() {
    String json = getServiceOrderCreateEvent();
    assertTrue(Utils.isJsonDataValid(json));
    EventPayload attrs = new EventPayload(json);
    assertEquals("ServiceOrderCreateEvent", (String) attrs.getJsonAttrValue("eventType"));
    assertEquals("acknowledged", (String) attrs.getJsonAttrValue("event.serviceOrder.state"));
  }
  
  @Test
  void testProjection() {
    String json = getServiceOrderCreateEvent();
    assertTrue(Utils.isJsonDataValid(json));
    EventPayload attrs = new EventPayload(json);
    String jsonVal = attrs.doProjection(Arrays.asList("eventId"));
    assertEquals("{\"eventId\":\"00001\"}", jsonVal);
    jsonVal = attrs.doProjection(Arrays.asList("eventId", "event.serviceOrder.id"));
    assertEquals("{\"eventId\":\"00001\",\"event\":{\"serviceOrder\":{\"id\":\"42\"}}}", jsonVal);
    jsonVal = attrs.doProjection(Arrays.asList("ciccio"));
    assertEquals("{}", jsonVal);
    List<String> actualFields = new ArrayList<>();
    Arrays.asList("eventId", "event.serviceOrder.serviceOrderItem[*].id").forEach(field -> actualFields.add(Utils.convertPath(field)));
    jsonVal = attrs.doProjection(actualFields);
    assertEquals("{\"eventId\":\"00001\",\"event\":{\"serviceOrder\":{\"serviceOrderItem\":[{\"id\":\"1\"},{\"id\":\"2\"}]}}}", jsonVal);
    actualFields.clear();
    Arrays.asList("eventId", "event.serviceOrder.serviceOrderItem[1].id").forEach(field -> actualFields.add(Utils.convertPath(field)));
    jsonVal = attrs.doProjection(actualFields);
    assertEquals("{\"eventId\":\"00001\",\"event\":{\"serviceOrder\":{\"serviceOrderItem\":[{\"id\":\"2\"}]}}}", jsonVal);
    jsonVal = attrs.doProjection(Arrays.asList("eventId", "event.serviceOrder.id", "event.serviceOrder.notPresent"));
    assertEquals("{\"eventId\":\"00001\",\"event\":{\"serviceOrder\":{\"id\":\"42\"}}}", jsonVal);    
  }
}
