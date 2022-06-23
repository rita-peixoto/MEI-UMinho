#! /bin/bash

# need to use kill to terminate processes
# netstat -tulpn to see processes with sockets open
N=${1:-5} #default = 5 aggregators

for ((i = 0 ; i < $N ; i++)); do
    let port=8000+$i*4
    java -cp src/aggregator:lib/jeromq-0.5.2.jar src/aggregator/Aggregator.java "*:$port" > "test/logs/agg$port.txt" &
    echo -n $! > "test/tmp/process$port.pid"
    sleep 1
done

