insert into `friendship` (id, requester_id, addressee_id, status, created_at)
values (UUID_TO_BIN(?), UUID_TO_BIN(?), UUID_TO_BIN(?), 'PENDING', now(6))
