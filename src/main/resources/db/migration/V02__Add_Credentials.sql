CREATE TABLE "credentials" (
  "id" uuid,
  "api_key" VARCHAR(255) not null,
  "client_secret" VARCHAR(255),
  "client_id" VARCHAR(255),
  "grant_type" VARCHAR(255),
  "auth_type" VARCHAR(255) not null,
  "token_url" VARCHAR(255),
  "auth_headers" VARCHAR(255),
   PRIMARY KEY (id)
);

ALTER TABLE "subscription" ADD COLUMN "credentials_id" uuid;

ALTER TABLE "subscription" ALTER COLUMN "tenant" TYPE VARCHAR(255);
ALTER TABLE "subscription" ALTER COLUMN "tenant" SET NOT NULL;

ALTER TABLE "subscription" ADD FOREIGN KEY ("credentials_id") REFERENCES "credentials" ("id") ON DELETE CASCADE;

CREATE INDEX "subscription_event_type_hash" ON "subscription_event_type" ("hash");