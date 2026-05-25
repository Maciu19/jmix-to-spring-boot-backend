CREATE TABLE user_
(
    id                  uuid         NOT NULL PRIMARY KEY,
    version             integer,
    created_by          varchar(255),
    created_date        timestamp with time zone,
    last_modified_by    varchar(255),
    last_modified_date  timestamp with time zone,
    deleted             boolean,
    deleted_date        timestamp with time zone
);