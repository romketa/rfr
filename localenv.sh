#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"

# compose.yaml declares the volume as external, so it must exist before the first start
docker volume create mysql-volume > /dev/null

# A container left by the former docker-run version of this script holds the name; the data stays in the volume
if [ -n "$(docker ps -aq --filter 'name=^rfr-mysql$')" ] \
  && [ -z "$(docker inspect rfr-mysql --format '{{index .Config.Labels "com.docker.compose.project"}}')" ]; then
  echo '### Remove standalone rfr-mysql container ###'
  docker rm -f rfr-mysql > /dev/null
fi

echo '### Run mysql db ###'
docker compose up -d --wait --wait-timeout 120 mysql
