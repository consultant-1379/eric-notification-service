CREATE TABLE "subscription_event_type" (
  "id" BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  "subscription_id" uuid not null,
  "event_type" VARCHAR(250) not null ,
  "filter_criteria" VARCHAR(250),
  "fields" VARCHAR(250),
  "hash" int not null
);

CREATE TABLE "subscription" (
  "id" uuid,
  "address" VARCHAR(255) not null,
  "tenant" VARCHAR(150),
   PRIMARY KEY (id)
);

ALTER TABLE "subscription_event_type" ADD FOREIGN KEY ("subscription_id") REFERENCES "subscription" ("id") ON DELETE CASCADE;


CREATE TABLE current_dispatch(
    event_id VARCHAR (255),
    address VARCHAR (255),
    hash int,
    payload  text,
    PRIMARY KEY (event_id, address, hash)
);



