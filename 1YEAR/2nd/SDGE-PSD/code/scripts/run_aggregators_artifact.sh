#! /bin/bash

# need to use kill to terminate processes
# netstat -tulpn to see processes with sockets open
N=${1:-5} #default = 5 aggregators

mkdir test || true
mkdir test/tmp || true
mkdir test/devices || true
mkdir test/logs || true

for ((i = 0; i < $N; i++)); do
  let port=8000+$i*4
  java -jar out/artifacts/aggregator_jar/aggregator.jar "*:$port" >"test/logs/agg$port.txt" &
  echo -n $! >"test/tmp/process$port.pid"
  sleep 1
done
