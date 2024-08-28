delete from subscription_event_type;
delete from subscription;

-- All fields
insert into subscription("id", address, tenant) values ('{8c878e6f-ee13-4a37-a208-7510c2638941}', 'http://client.us.com', 'master');
insert into subscription_event_type(subscription_id, event_type, hash) values ('{8c878e6f-ee13-4a37-a208-7510c2638941}', 'OrderCreated', 1293028901);
insert into subscription_event_type(subscription_id, event_type, filter_criteria, fields, hash)
values ('{8c878e6f-ee13-4a37-a208-7510c2638941}', 'OrderDeleted', 'event.priority==4','event.eventType,event.priority', -1120972243);

insert into subscription("id", address, tenant) values ('{8c878e6f-ee13-4a37-a208-7510c2638942}', 'http://client.ie.com', 'tenant');
insert into subscription_event_type(subscription_id, event_type, filter_criteria, hash)
values ('{8c878e6f-ee13-4a37-a208-7510c2638942}', 'OrderUpdated', 'event.priority==4', -656551867);

-- Only required fields
insert into subscription("id", address, tenant) values ('{8c878e6f-ee13-4a37-a208-7510c2638943}', 'http://client.uk.com', '');
insert into subscription_event_type(subscription_id, event_type, hash)
values ('{8c878e6f-ee13-4a37-a208-7510c2638943}', 'OrderCanceled', 627050669);