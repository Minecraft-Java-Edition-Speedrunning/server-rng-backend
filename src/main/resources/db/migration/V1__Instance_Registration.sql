create table users (
	id bigint primary key not null AUTO_INCREMENT,
	user_id char(36) not null,
	authentication int not null,
	unique (user_id, authentication)
);

create table registered_instances (
	id char(36) primary key not null,
	user_id bigint not null,
	refresh_token_key char(36) not null,
    invalidated boolean not null default false,
    refreshed_at datetime not null default CURRENT_TIMESTAMP,
    created_at datetime not null default CURRENT_TIMESTAMP,
    foreign key (user_id) references users(id),
	unique (refresh_token_key),
    index (user_id)
);

create table registered_access (
	id bigint primary key not null AUTO_INCREMENT,
	instance_id char(36) not null,
    access int not null,
    foreign key (instance_id) references registered_instances(id)
);