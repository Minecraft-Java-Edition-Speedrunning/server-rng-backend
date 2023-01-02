create table salt_block (
	id char(36) primary key not null,
    salt char(36) not null,
    created_at datetime not null default CURRENT_TIMESTAMP,
    expires_at datetime not null
);