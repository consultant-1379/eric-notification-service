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

import com.ericsson.oss.common.service.ns.util.Utils;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
class RsqlTest extends FilterBase {
  @Test
  void testFilter() {
    String json = getSimpleEvent();
    assertTrue(Utils.isJsonDataValid(json));
    EventPayload attrs = new EventPayload(json);
    assertFalse(RsqlFilter.isValid("ciofeca;;;"));
    try {
      RsqlFilter filter = new RsqlFilter("a==prova;b==a<<");
      assertTrue(false);
    } catch (Exception e) {
      assertTrue(true);
    }

    RsqlFilter filter = new RsqlFilter("z==1");
    assertFalse(filter.eval(attrs));
    filter = new RsqlFilter("z!=1");
    assertTrue(filter.eval(attrs));
    filter = new RsqlFilter("z>1");
    assertFalse(filter.eval(attrs));
    filter = new RsqlFilter("z=regex='[^-]-[a-z]*'");
    assertFalse(filter.eval(attrs));
    filter = new RsqlFilter("a==1");
    assertFalse(filter.eval(attrs));
    
    assertTrue(RsqlFilter.isValid("a==a-value;b==b-value;c==3,d.a==2.0;c<3,c<=3;d.a>1.5"));
    filter = new RsqlFilter("a==a-value;b==b-value;c==3,d.a==2.0;c<3,c<=3;d.a>1.5");
    assertTrue(filter.eval(attrs));
    filter = new RsqlFilter("a!=b-value;b=in=(a-value,b-value,c-value);c=out=(1,3,4,5)");
    assertTrue(filter.eval(attrs));
    filter = new RsqlFilter("(d.a>3.0 and e[1]=in=(b,c)) or d.a<=3.0");
    assertTrue(filter.eval(attrs));
    filter = new RsqlFilter("(d.a<3.0 or f==true) and (e[0]==a and (f!=true or d.a==2.0))");
    assertTrue(filter.eval(attrs));
    filter = new RsqlFilter("(c>1 and d.a>=2.0001) or (c>=3 or a<b-value or d.b[0]<=1 or d.a=in=(1,3))");
    assertTrue(filter.eval(attrs));
    filter = new RsqlFilter("(c==1;a==a-value);(a==b-value,b==a-value)");
    assertTrue(!filter.eval(attrs));
    filter = new RsqlFilter("(a==b-value,b==a-value);(c==1;a==a-value)");
    assertTrue(!filter.eval(attrs));
    filter = new RsqlFilter("a=regex='[^-]-[a-z]*';c<=3;d.a=in=(1.0,2.0, 3.0)");
    assertTrue(filter.eval(attrs));
  }

  @Test
  void testFilterWithArray() {
    String json = getSimpleEvent2();
    assertTrue(Utils.isJsonDataValid(json));
    EventPayload attrs = new EventPayload(json);
    RsqlFilter filter = new RsqlFilter("[*].a=='a-value'");
    assertTrue(filter.eval(attrs));
    filter = new RsqlFilter("[*].a=='a2-value'");
    assertTrue(filter.eval(attrs));
    filter = new RsqlFilter("[*].a!='c-value'");
    assertTrue(filter.eval(attrs));
    filter = new RsqlFilter("([*].d.a<3.0 or [*].f==true) and ([*].e[*]==a and ([*].f!=true or [*].d.a==2.0))");
    assertTrue(filter.eval(attrs));
    filter = new RsqlFilter("[*].d.b[*] > 12");
    assertFalse(filter.eval(attrs));
    filter = new RsqlFilter("[*].d.b[*] <= 2");
    assertTrue(filter.eval(attrs));
    filter = new RsqlFilter("[*].d.b[*] >= 4");
    assertTrue(filter.eval(attrs));
    filter = new RsqlFilter("[*].a=regex='[^-]-[a-z]*';[*].d.a=in=(1.0,2.0, 3.0)");
    assertTrue(filter.eval(attrs));
  }

  /**
   * Test a real notification TMF641 for some fields.
   */
  @Test
  void testGetJsonAttrValueReal() {
    String json = getServiceOrderCreateEvent();
    assertTrue(Utils.isJsonDataValid(json));
    EventPayload attrs = new EventPayload(json);
    RsqlFilter filter = new RsqlFilter("event.serviceOrder.state==acknowledged");
    assertTrue(filter.eval(attrs));
    filter = new RsqlFilter("event.serviceOrder.priority>0;event.serviceOrder.priority<=10;event.serviceOrder.serviceOrderItem[0].id<2");
    assertTrue(filter.eval(attrs));
    filter = new RsqlFilter("event.serviceOrder.id>=1");
    assertTrue(filter.eval(attrs));
    filter = new RsqlFilter("event.description==mianonna");
    assertFalse(filter.eval(attrs));
    filter = new RsqlFilter("event.serviceOrder.relatedParty[0].name=='Jean Pontus'");
    assertTrue(filter.eval(attrs));
    filter = new RsqlFilter("event.serviceOrder.relatedParty[*].name=='Jean Pontus'");
    assertTrue(filter.eval(attrs));
    filter = new RsqlFilter("event.serviceOrder.relatedParty[*].name!='Jean Leduc'");
    assertTrue(filter.eval(attrs));
    filter = new RsqlFilter("event.serviceOrder.serviceOrderItem[*].service.serviceCharacteristic[*].value=='ecm'");
    assertTrue(filter.eval(attrs));
    filter = new RsqlFilter("event.serviceOrder.serviceOrderItem[*].service.serviceSpecification.version<2");
    assertTrue(filter.eval(attrs));
    String jsonVal = attrs.doProjection(Arrays.asList("eventId"));
    assertEquals("{\"eventId\":\"00001\"}", jsonVal);
    jsonVal = attrs.doProjection(Arrays.asList("eventId", "event.serviceOrder.id"));
    assertEquals("{\"eventId\":\"00001\",\"event\":{\"serviceOrder\":{\"id\":\"42\"}}}", jsonVal);
  }

  
}
