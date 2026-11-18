select exists(select 1 from `user` where id = UUID_TO_BIN(?))
