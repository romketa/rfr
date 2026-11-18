select exists(select 1 from `friendship` where id = UUID_TO_BIN(?))
