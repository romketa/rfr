delete from `photo_like`
where photo_id in (select id from `photo` where user_id = ?)
   or like_id in (select id from `like` where user_id = ?)
