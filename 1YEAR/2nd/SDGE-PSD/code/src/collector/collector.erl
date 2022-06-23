%%%-------------------------------------------------------------------
%% @doc collector public
%% @end
%%%-------------------------------------------------------------------

-module(collector).

-export([start/0, start/1, start/3, start/5, stop/1, runMultipleCollectors/1, runMultipleCollectors/2]).

-define(DEBUG, 1).

%% start collector with 
%%  - device information = Map: #{ Username = String() => Password = String() }
%%  - device address = String()
%%  - device port = integer()
%%  - aggregator address = String()
%%  - aggregator port = integer() 
%%  - filename = String() : path to file with device information
start() ->
  spawn(fun() -> collector(#{}, {0, 0, 0, 0}, 8800, "localhost", 8002) end).

start(Filename) ->
  spawn(fun() -> collector(load(Filename), {0, 0, 0, 0}, 8800, "localhost", 8002) end).

start(Filename, DevicePort, AggregatorPort) ->
  spawn(fun() -> collector(load(Filename), {0, 0, 0, 0}, DevicePort, "localhost", AggregatorPort) end).

start(Filename, DeviceADD, DevicePort, AggregatorADD, AggregatorPort) ->
  {ok, AggADD} = inet:parse_address(AggregatorADD),
  {ok, DvcADD} = inet:parse_address(DeviceADD),
  spawn(fun() -> collector(load(Filename), DvcADD, DevicePort, AggADD, AggregatorPort) end).


%% @doc send stop message to collector in order to stop all processes
stop(ServerPid) ->
  ServerPid ! {stop, self()},
  receive
    stopped -> success
  after 1000 -> timeout
  end.

%% @doc creates map #{ Username = String() => Password = String() } with devices information where
%%  - Filename = String(): path to file  
load(Filename) ->
  {ok, DevicesInfoList} = file:consult(Filename),
  maps:from_list([{Username, Password} || {Username, Password, _} <- DevicesInfoList]).


%% @doc process collector that starts and stops other necessary processes
collector(DevicesInfo, DeviceADD, DevicePort, AggregatorADD, AggregatorPort) ->

  %application:start(chumak),
  {ok, AGSocket} = chumak:socket(push),
  {ok, _} = chumak:connect(AGSocket, tcp, AggregatorADD, AggregatorPort),
  debug("Aggregator OK~n"),

  {ok, LSock} = gen_tcp:listen(DevicePort, [binary, {ip, DeviceADD}, {active, true}, {packet, 0}]),
  debug("Device Socket OK~n"),

  spawn(fun() -> deviceAcceptor(LSock, AGSocket, DevicesInfo, self()) end),

  loopCollector(LSock, []).


%% @doc collector loop after initiating
%%  - LSock = socket(): devices listening socket
%%  - DevicesPids = List: list of device processes that are active, necessary to stop
loopCollector(LSock, DevicesPids) ->
  receive
    {register, Pid} ->
      debug("device registed~n"),
      loopCollector(LSock, [Pid | DevicesPids]);
    {remove, Pid} ->
      debug("device removed~n"),
      loopCollector(LSock, DevicesPids -- [Pid]);
    {stop, From} ->
      [Pid ! {stop, From} || Pid <- DevicesPids],
      application:stop(chumak),
      gen_tcp:close(LSock),
      From ! stopped,
      bye
  end.

%% @doc registers device process in collector
%%  - ColPid = pid(): Collector process pid
%%  - DVCPid = pid(): device process pid
registerDVC(ColPid, DVCPid) -> ColPid ! {register, DVCPid}.
%% @doc removes device process in collector
removeDVC(ColPid, DVCPid) -> ColPid ! {remove, DVCPid}.


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% @doc waits connection from device, and creates new unknown device when connection is accepted
%%  - LSocket = socket(): listening socket for devices
%%  - AGSocket: chumak PUSH socket for device processes to send events
%%  - DevicesInfo = Map: devices information #{ Username = String() => Password = String() }
%%  - ColPid = pid(): collector process pid
deviceAcceptor(LSocket, AGSocket, DevicesInfo, ColPid) ->
  case gen_tcp:accept(LSocket) of
    {ok, Socket} ->
      spawn(fun() -> deviceAcceptor(LSocket, AGSocket, DevicesInfo, ColPid) end),
      unknown_device(Socket, AGSocket, DevicesInfo, ColPid);
    _ ->
      stop
  end.


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% @doc unknown device needs to be autenticated
%%  - Socket = socket(): listening socket for one device
%%  - AGSocket: chumak PUSH socket
%%  - DeviceInfo = Map: devices information
%%  - ColPid = pid(): collector process pid
unknown_device(Socket, AGSocket, DevicesInfo, ColPid) ->
  receive
    {tcp, _, Data} ->
      case binary:split(Data, <<":">>, [global]) of
        [UsernameB, PasswordB, TypeB] ->
          UsernameL = binary:bin_to_list(UsernameB),
          PasswordL = binary:bin_to_list(PasswordB),

          case maps:get(UsernameL, DevicesInfo, badkey) of
            PasswordL -> % valid username and password
              debug("Device \"~s\" is active.~n", [UsernameL]),
              registerDVC(ColPid, self()), % registers device in collector process
              chumak:send(AGSocket, <<"ONLINE:", UsernameB/binary, ":", TypeB/binary>>), % send online event to aggregator
              active_device(Socket, AGSocket, ColPid, UsernameB, TypeB); % convert to active device
            _ -> % invalid username and password
              debug("Invalid username (\"~s\") or password (\"~s\").~n", [UsernameL, PasswordL]),
              gen_tcp:close(Socket)
          end;
        _ ->
          debug("Wrong formated message received: \"~s\".~n", [Data])
      end;
    {tcp_closed, _} ->
      debug("Socket closed ~n")
  after 60 * 1000 -> %% Timeout the unknown device if it's inactive
    gen_tcp:close(Socket)
  end.


%% @doc active device process, a device is active if it receives message in the interval of 3 secs
%%  - Socket = socket(): listening socket for device
%%  - AGSocket: chumak PUSH socket to agregator
%%  - ColPid = pid(): collector process pid
%%  - Username = String(): username of device connected to process through Socket
%%  - Type = String(): type of device 
active_device(Socket, AGSocket, ColPid, Username, Type) ->
  receive
    {tcp, _, Data} ->
      forward(AGSocket, Data),
      active_device(Socket, AGSocket, ColPid, Username, Type);
    {tcp_closed, _} ->
      lost(AGSocket, Username, Type, ColPid);
    {tcp_error, _, _} ->
      lost(AGSocket, Username, Type, ColPid)
  after 60 * 1000 -> %% Idle checker
    chumak:send(AGSocket, <<"IDLE:", Username/binary, ":", Type/binary>>),
    debug("device \"~s\": idle~n", [Username]),
    idle_device(Socket, AGSocket, ColPid, Username, Type)
  end.


%% @doc idle device process, a device is idle and returns to active when message is received
%%  - same arguments as active_device 
idle_device(Socket, AGSocket, ColPid, Username, Type) ->
  receive
    {tcp, _, Data} ->
      forward(AGSocket, Data),
      chumak:send(AGSocket, <<"ACTIVE:", Username/binary, ":", Type/binary>>),
      debug("device \"~s\": active~n", [Username]),
      active_device(Socket, AGSocket, ColPid, Username, Type);
    {tcp_closed, _} ->
      disconnected(AGSocket, Username, Type, ColPid);
    {tcp_error, _} ->
      disconnected(AGSocket, Username, Type, ColPid)
  end.

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% @doc processes event received from a device and sends it to agregator through AGSocket
%%  - AGSocket: chumak PUSH socket to send event
%%  - Data = string(): data received from a device
forward(AGSocket, Data) ->
  case binary:split(Data, <<":">>, [global]) of
    [Event] ->
      chumak:send(AGSocket, Event);
    [_, _, _, Event] ->
      chumak:send(AGSocket, Event);
    _ ->
      debug("Wrong formated message received from device: \"~s\".~n", [Data])
  end.

%% @doc device was lost, send LOST event to aggregator and remove device from process collector
%%  - AGSocket = socket(): chumak PUSH socket to aggregator
%%  - Username = String(): device username
%%  - Type = String(): device type
%%  - ColPid = pid(): collector process pid 
lost(AGSocket, Username, Type, ColPid) ->
  chumak:send(AGSocket, <<"LOST:", Username/binary, ":", Type/binary>>),
  removeDVC(ColPid, self()).

%% @doc same as lost except instead of LOST event sends DISCONNECTED event
disconnected(AGSocket, Username, Type, ColPid) ->
  chumak:send(AGSocket, <<"DISCONNECTED:", Username/binary, ":", Type/binary>>),
  removeDVC(ColPid, self()).

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% @doc for debugint, prints debug messages if constant ?DEBUG = 1
%%  - String = string(): string to be printed
%%  - Args = List: list of arguments to be used in io:format
debug(String) ->
  debug(String, []).

debug(String, Args) ->
  if
    ?DEBUG == 1 -> io:format(String, Args);
    true -> no_debug
  end.

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%% Testing %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% @doc runMultipleCollectors
%%  - N: number of collectors
runMultipleCollectors(N) ->
  application:start(chumak),
  [spawn(fun() -> collector(load("test/devices/Z" ++ integer_to_list(I) ++ ".txt"),
    {0, 0, 0, 0}, 8800 + I, "localhost", 8000 + I * 4 + 2) end) || I <- lists:seq(0, N - 1)].

runMultipleCollectors(N, InterfaceID) ->
  application:start(chumak),
  [spawn(fun() -> collector(load("test/devices/Z" ++ integer_to_list(I) ++ ".txt"),
    {0, 0, 0, 0}, 8800 + I, InterfaceID, 8000 + I * 4 + 2) end) || I <- lists:seq(0, N - 1)].