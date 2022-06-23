-module(device).
-export([randomDevice/3, randomDevice/5, 
          manualDevice/3, manualDevice/5, 
          runMultipleDevices/1, runMultipleDevices/3, 
          runMultipleZonesDevices/1,
          event/2, 
          close/1]).

-define(EVENTS, ["alarme", "avaria", "travagem"]).

%% @doc create single device process that sends random events to collector
%%  - Username = String(): device username
%%  - Password = String(): device password
%%  - Type = String(): device type
%%  - ColAddr = collector address
%%  - ColPort = collector port 
randomDevice(Username, Password, Type) ->
  randomDevice(Username, Password, Type, {127, 0, 0, 1}, 8800).

randomDevice(Username, Password, Type, ColAddr, ColPort) ->
  spawn(fun() ->  {ok, LSock} = gen_tcp:connect(ColAddr, ColPort, [binary, {packet, 0}]),
                  authenticate(Username, Password, Type, LSock), 
                  sendEventsLoop(LSock) 
        end).

%% @doc equals randomDevice except events are sent by the user when using event/2
manualDevice(Username, Password, Type) ->
  manualDevice(Username, Password, Type, {127, 0, 0, 1}, 8800).

manualDevice(Username, Password, Type, ColAddr, ColPort) ->
  spawn(fun() ->  {ok, LSock} = gen_tcp:connect(ColAddr, ColPort, [binary, {packet, 0}]),
                  authenticate(Username, Password, Type, LSock), 
                  sendEventsLoopManual(LSock) 
        end).

%% @doc authenticate device by sending username, password and type
authenticate(Username, Password, Type, Sock) ->
  case gen_tcp:send(Sock, erlang:list_to_binary([Username, ":", Password, ":", Type])) of
    ok ->
      ok;
    {error, _} ->
      io:format("~s: connection failed ~n", [Username])
  end.

%% @doc loop that sends events randomly to collector
sendEventsLoop(Socket) ->
  timer:sleep(rand:uniform(2000)), % sleep between 1 to 2000 ms
  case sendEvent(Socket, erlang:list_to_binary(lists:nth(rand:uniform(length(?EVENTS)), ?EVENTS))) of
    ok ->
      sendEventsLoop(Socket);
    {error, _} ->
      io:format("~w: connection failed ~n", [self()]);
    error ->
      error
  end.

%% @doc loop that sends events to collector when event msg is received
sendEventsLoopManual(Socket) ->
  receive 
    {event, Event} ->
      sendEvent(Socket, Event),
      sendEventsLoopManual(Socket);
    {error, _} ->
      io:format("~w: connection failed ~n", [self()]);
    exit ->
      exit
  end.

%% @doc send event to device process
event(Pid, Event) -> Pid ! {event, Event}.
%% @doc send exit to device process
close(Pid) -> Pid ! exit.

%% @doc sends single event to collector
sendEvent(Socket, Event) ->
  case gen_tcp:send(Socket, Event) of
    ok ->
      ok;
    {error, _} ->
      io:format("connection failed ~n"),
      error
  end.

%% @doc run multiple devices that are in file
%%  - Filename = String(): path to file with devices
%%  - ColAddr: collector address
%%  - ColPort: collector port
runMultipleDevices(Filename) ->
  runMultipleDevices(Filename, {127, 0, 0, 1}, 8800).

runMultipleDevices(Filename, ColAddr, ColPort) ->
  {ok, DevicesInfoList} = file:consult(Filename),
  [randomDevice(Username, Password, Type, ColAddr, ColPort) || {Username, Password, Type} <- DevicesInfoList].

%% @doc run multiple collectores in multiple zones
%%  N: number of zones
runMultipleZonesDevices(N) ->
  [runMultipleDevices("test/devices/Z"++integer_to_list(I)++".txt", {127, 0, 0, 1}, 8800 + I) || I <- lists:seq(0,N-1)].


