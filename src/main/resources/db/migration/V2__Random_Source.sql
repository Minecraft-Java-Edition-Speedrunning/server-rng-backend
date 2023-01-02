create table random_source (
	id bigint primary key not null AUTO_INCREMENT,
    seed char(36) not null,
    created_at datetime not null default CURRENT_TIMESTAMP,
    expires_at datetime not null
);