select exists(select 1
              from `friendship`
              where status = 'ACCEPTED'
                and (requester_id = UUID_TO_BIN(?) or addressee_id = UUID_TO_BIN(?)))
