create table salt_block (
	id char(36) primary key not null,
    salt char(48) not null,
    created_at datetime not null default CURRENT_TIMESTAMP,
    active_at datetime not null,
    expires_at datetime not null
);