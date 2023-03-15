CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE TABLE student
(
    id                uuid not null,
    email             text not null,
    name              text not null,
    phone             text not null,
    payment_intent_id text
);
