/*******************************************************************************
 * COPYRIGHT Ericsson 2021
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

package com.ericsson.oss.common.service.ns.repository;

import com.ericsson.oss.common.service.ns.PostgreSqlContainerBase;
import com.ericsson.oss.common.service.ns.model.dispatch.Dispatch;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@JdbcTest
@AutoConfigureTestDatabase(replace = NONE)
@Import({DispatchRepositoryImpl.class})
@ActiveProfiles("test")
class DispatchRepositoryImplTest extends PostgreSqlContainerBase {

  @Autowired
  private DispatchRepository repository;

  @Test
  @Sql("/db/add-dispatches.sql")
  void shouldFindAll() {
    //given
    //when
    List<Dispatch> all = repository.findAll("8c878e6f-ee13-4a37-a208-7510c2638941");

    //then
    assertNotNull(all);
    assertEquals(3, all.size());
  }

  @Test
  @Sql("/db/add-dispatches.sql")
  void shouldDelete() {
    //given
    //when
    int deleted = repository.delete("8c878e6f-ee13-4a37-a208-7510c2638941", "http://client.us.com", 1);

    //then
    assertEquals(1, deleted);
    List<Dispatch> all = repository.findAll("8c878e6f-ee13-4a37-a208-7510c2638941");
    assertNotNull(all);
    assertEquals(2, all.size());
  }

  @Test
  void shouldSave() {
    //given
    Dispatch dispatchRecord = new Dispatch("8c878e6f-ee13-4a37-a208-7510c2638941", "http://client.us.com", 1, "payload1", "49508a2e-cf9f-11ec-9d64-0242ac120002", "application/json", null);
    //when
    repository.save(dispatchRecord);

    //then
    List<Dispatch> all = repository.findAll("8c878e6f-ee13-4a37-a208-7510c2638941");
    assertNotNull(all);
    assertEquals(1, all.size());
    Dispatch dispatch = all.get(0);
    assertEquals("8c878e6f-ee13-4a37-a208-7510c2638941", dispatch.getEventId());
    assertEquals("http://client.us.com", dispatch.getAddress());
    assertEquals(1, dispatch.getHash());
    assertEquals("payload1", dispatch.getPayload());
    assertEquals("49508a2e-cf9f-11ec-9d64-0242ac120002", dispatch.getOauthRegistrationId());
    assertEquals("application/json", dispatch.getAuthHeaders());
  }

  @Test
  void shouldSaveAll() {
    //given
    List<Dispatch> records = new ArrayList<>();
    records.add(new Dispatch("8c878e6f-ee13-4a37-a208-7510c2638941", "http://client.us.com", 1, "payload1", "00feba16-cf9f-11ec-9d64-0242ac120002", "application/json", null));
    records.add(new Dispatch("8c878e6f-ee13-4a37-a208-7510c2638941", "http://client.us.com", 2, "payload2", null, "application/json", null));
    records.add(new Dispatch("8c878e6f-ee13-4a37-a208-7510c2638942", "http://client.uk.com", 3, "payload3", null, "application/json", null));
    records.add(new Dispatch("8c878e6f-ee13-4a37-a208-7510c2638941", "http://client.it.com", 0, null, "ec5caf78-cf9e-11ec-9d64-0242ac120002", "application/json", null));
    //when
    repository.save(records);

    //then
    List<Dispatch> all = repository.findAll("8c878e6f-ee13-4a37-a208-7510c2638941");
    assertNotNull(all);
    assertEquals(3, all.size());
  }
}
