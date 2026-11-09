select BIN_TO_UUID(id) as id, BIN_TO_UUID(requester_id) as requester_id, BIN_TO_UUID(addressee_id) as addressee_id, status
from `friendship`
where id = UUID_TO_BIN(?)
