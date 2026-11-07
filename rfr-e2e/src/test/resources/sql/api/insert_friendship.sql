insert into `friendship` (id, requester_id, addressee_id, status, created_at, responded_at)
values (UUID_TO_BIN(?), UUID_TO_BIN(?), UUID_TO_BIN(?), 'ACCEPTED', now(6), now(6))
