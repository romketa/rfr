delete from `photo_like`
where photo_id in (select id from `photo` where user_id = UUID_TO_BIN(?))
   or like_id in (select id from `like` where user_id = UUID_TO_BIN(?))
