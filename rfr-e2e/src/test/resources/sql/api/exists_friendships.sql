select exists(select 1 from `friendship` where requester_id = UUID_TO_BIN(?) or addressee_id = UUID_TO_BIN(?))
