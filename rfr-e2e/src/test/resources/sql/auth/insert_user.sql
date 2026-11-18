insert into `user` (id, username, password, enabled, account_non_expired, account_non_locked, credentials_non_expired)
values (UUID_TO_BIN(?), ?, ?, true, true, true, true)
