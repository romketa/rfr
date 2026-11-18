insert into `user` (id, username, firstname, last_name, country_id)
select UUID_TO_BIN(?), ?, nullif(?, ''), nullif(?, ''), id from `country` where code = ?
