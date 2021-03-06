-module(starter).
-export([start/1]).

-define(NAME, "station").
-define(LOGFILE, lists:flatten(io_lib:format("log/~p.log", [?NAME]))).
-define(DEBUG, false).
	
start(ArgsList) ->
	%Anforderungs-Nr.: 4.3; 7.1 
	{InterfaceName, MulticastAddr, ReceivePort, StationClass, StationNumber, UtcOffsetMs} = readArgs(ArgsList),

	SenderPID = spawn(sender, start, [InterfaceName, MulticastAddr, ReceivePort, StationClass, StationNumber]),
	debug("sender spawned", ?DEBUG),
	ReceiverPID = spawn(receiver, start, [InterfaceName, MulticastAddr, ReceivePort, SenderPID, StationClass, UtcOffsetMs]),
	debug("receiver spawned", ?DEBUG),
	Name = list_to_atom(lists:append(?NAME, atom_to_list(StationNumber))),
	registerAtLocalNameservice(Name),
	debug("waiting for kill-command", ?DEBUG),
	receive
		kill ->
			SenderPID ! kill,
			ReceiverPID ! kill
	end,
	unregisterAtLocalNameservice(Name).
	
readArgs(ArgsList) when length(ArgsList) == 5 ->
	InterfaceName = lists:nth(1, ArgsList),
	MulticastAddr = list_to_tuple([list_to_integer(X) || X <- string:tokens(atom_to_list(lists:nth(2, ArgsList)), [$.])]), 
	ReceivePort = lists:nth(3, ArgsList), 
	StationClass = atom_to_list(lists:nth(4, ArgsList)), 
	StationNumber = lists:nth(5, ArgsList),
	{InterfaceName, MulticastAddr, ReceivePort, StationClass, StationNumber, 0};
readArgs(ArgsList) when length(ArgsList) == 6 ->
	InterfaceName = lists:nth(1, ArgsList),
	MulticastAddr = list_to_tuple([list_to_integer(X) || X <- string:tokens(atom_to_list(lists:nth(2, ArgsList)), [$.])]), 
	ReceivePort = lists:nth(3, ArgsList), 
	StationClass = atom_to_list(lists:nth(4, ArgsList)), 
	UtcOffsetMs = list_to_integer(atom_to_list(lists:nth(5, ArgsList))), 
	StationNumber = lists:nth(6, ArgsList),
	{InterfaceName, MulticastAddr, ReceivePort, StationClass, StationNumber, UtcOffsetMs}.

registerAtLocalNameservice(Name) ->
	register(Name, self()),
	debug("starter registered", ?DEBUG).
	
unregisterAtLocalNameservice(Name) ->
	unregister(Name),
	debug("starter unregistered", ?DEBUG).
		
debug(Text, true) ->
	io:format("starter_module: ~p~n", [Text]);
debug(_Text, false) ->
	ok.
