select version, description, checksum from `flyway_schema_history` where success = 1 and type = 'SQL' order by installed_rank
