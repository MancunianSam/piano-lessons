CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE TABLE student
(
    id                uuid not null,
    email             text not null,
    name              text not null,
    student           text,
    level             text,
    phone             text not null,
    notes             text default null,
    payment_intent_id text,
    total_cost        numeric,
    payment_confirmed boolean,
    constraint student_pk PRIMARY KEY (id)
);

CREATE TABLE times
(
    id                uuid      not null,
    number_of_lessons int       not null,
    length_of_lessons int       not null,
    start_date        timestamp not null,
    end_date          timestamp not null,
    student_id        uuid      null default null,
    constraint times_pk primary key (id),
    constraint times_student foreign key (student_id) references student (id)
);

CREATE TABLE news
(
    id    uuid         not null,
    date  date         not null default now(),
    title varchar(255) not null,
    body  text         not null,
    constraint news_pk primary key (id)
);
