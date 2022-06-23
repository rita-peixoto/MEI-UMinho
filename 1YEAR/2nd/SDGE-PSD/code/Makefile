all:
	./rebar3 compile
	erlc src/device/device.erl
	javac -cp src/aggregator:lib/jeromq-0.5.2.jar src/aggregator/Aggregator.java
	javac -cp src/client:lib/jeromq-0.5.2.jar src/client/Client.java

clean:
	find . -name \*.class -type f -delete
	rm -rf out *.beam *.dump