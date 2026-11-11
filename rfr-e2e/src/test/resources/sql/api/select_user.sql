select BIN_TO_UUID(u.id) as id, u.firstname, u.last_name, c.code
from `user` u
         join `country` c on c.id = u.country_id
where u.username = ?
