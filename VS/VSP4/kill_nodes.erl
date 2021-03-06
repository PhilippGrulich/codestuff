-module(kill_nodes).
-export([start/1]).

start(Index) ->
	%{ok, Hostname} = inet:gethostname(),
	Hostname = net_adm:localhost(),
	Node = list_to_atom("station" ++ atom_to_list(lists:nth(1, Index)) ++ "@" ++ Hostname),
	Result = rpc:call(Node, init, stop, []),
	io:format("killed ~p: ~p~n", [Node, Result]),
	Result1 = rpc:call(list_to_atom("kill_nodes" ++ atom_to_list(lists:nth(1, Index)) ++ "@" ++ Hostname), init, stop, []),
	io:format("and killed me: ~p~n", [Result1]).
