/*
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
 */ 
 
delete from current_dispatch;

insert into current_dispatch(event_id, address, hash, payload) 
  values ('8c878e6f-ee13-4a37-a208-7510c2638941', 'http://client.us.com', 1, 'payload1');

insert into current_dispatch(event_id, address, hash, payload) 
  values ('8c878e6f-ee13-4a37-a208-7510c2638941', 'http://client.us.com', 2, 'payload2');

insert into current_dispatch(event_id, address, hash, payload) 
  values ('8c878e6f-ee13-4a37-a208-7510c2638942', 'http://client.uk.com', 3, 'payload3');

insert into current_dispatch(event_id, address, hash) 
  values ('8c878e6f-ee13-4a37-a208-7510c2638941', 'http://client.it.com', 0);

