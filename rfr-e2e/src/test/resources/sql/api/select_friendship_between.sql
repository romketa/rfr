select BIN_TO_UUID(id) as id, BIN_TO_UUID(requester_id) as requester_id, BIN_TO_UUID(addressee_id) as addressee_id, status
from `friendship`
where requester_id = UUID_TO_BIN(?) and addressee_id = UUID_TO_BIN(?)
