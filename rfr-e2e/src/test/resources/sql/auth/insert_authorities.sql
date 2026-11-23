insert into `authority` (user_id, authority)
values (UUID_TO_BIN(?), 'read'), (UUID_TO_BIN(?), 'write')
