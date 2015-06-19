-module(receiver).
-import(werkzeug, [getUTC/0, openSe/2, openSeA/2, openRec/3, openRecA/3, logging/2]).
-export([delivery/3, init/6, start/6]).

-define(NAME, lists:flatten(io_lib:format("receiver@~p", [node()]))).
-define(LOGFILE, lists:flatten(io_lib:format("~p.log", [?NAME]))).
-define(DEBUG, true).

%%%%%%%%%%%OLD&%%%%%%%%%%%%%%%%%%%%%
start(InterfaceName, MulticastAddr, ReceivePort, SenderPID, StationClass, UtcOffsetMs) ->
	%Diese Methode benoetigt beim ersten Aufruf einige ms die das Senden
	%und wuerde beim ersten Aufruf von mehreren Prozessen zu eine fast 
	%identischen Zeit den selben Slot erzeugen
	crypto:rand_uniform(1, 26),
	TimeSyncPID = spawn(timesync, start, [StationClass, UtcOffsetMs, SenderPID]),
	debug("timesync spawned", ?DEBUG),
	SlotReservationPID = spawn(slotreservation, start, [SenderPID]),
	debug("slotreservation spawned", ?DEBUG),
	ReceiverDeliveryPID = spawn(receiver, delivery, [stationAlive, SlotReservationPID, TimeSyncPID]),
	spawn(receiver, init, [InterfaceName, MulticastAddr, string:to_integer(atom_to_list(ReceivePort)), ReceiverDeliveryPID, TimeSyncPID, SenderPID])
.

debug(Text, true) ->
	io:format("starter_module: ~p~n", [Text]);
debug(_Text, false) ->
	ok.

%%%%%%%%%%%%%%%%%%%%%NEW%%%%%%%%%%%%%%%%%%%%%
%Initialisiert den Socket und geht dann in die Schleife.
init(InterfaceName, MulticastAddr, {ReceivePort, _}, ReceiverDeliveryPID, TimeSyncPID, SenderPID) ->
	HostAddress = getHostAddress(InterfaceName),
	Socket = openRecA(MulticastAddr, HostAddress, ReceivePort),
	gen_udp:controlling_process(Socket, self()),
	SlotsUsed = initSlotPositions(24),
		SenderPID ! {getPID, self()},
	receive
		{pid, MessageGenPID} ->
			debug("MessageGenPID received", ?DEBUG)
	end,
	TimeSyncPID ! {getTime, self()},
	receive
		{currentTime, TimeStamp} ->
			debug("timestamp received", ?DEBUG)
	end,
	
	T = getUTC(),
	AdditionalTimeToWait =  1000 - (T rem 1000),
	timer:send_after(AdditionalTimeToWait, self(), startInitialListen),
	wait(),
	Time = getUTC(),
	loopInitial(Socket, SlotsUsed, ReceiverDeliveryPID, TimeSyncPID, MessageGenPID, Time, Time, [], 1)
	
	%loop(0, 0, SlotsUsed, Socket, ReceiverDeliveryPID, TimeSyncPID, TimeStamp, stationAlive, MessageGenPID, [], 0, initialListen)
.

%Warten bis Zeitdifferenz zu 1 Sekunde abgelaufen ist.
wait() ->
	receive
		startInitialListen ->
			ok
	end
.

%Initialisert die Liste mit den Slot-Positionen.
%Hierbei gilt: Slot-Position = Slot-Nr.
initSlotPositions(NumPos) ->
	initSlotPositions([], NumPos, 0)
.

%Initialisert die Liste mit den Slot-Positionen.
%Hierbei gilt: Slot-Position = Slot-Nr.
initSlotPositions(SlotsUsed, NumPos, Counter) when NumPos >= Counter ->
	initSlotPositions(lists:append(SlotsUsed, [0]), NumPos, Counter + 1);
initSlotPositions(SlotsUsed, _NumPos, _Counter) ->
	SlotsUsed
.

%1 Sekunde lang zuhören, Slots sammeln, dann freie Slots an die Slotreservation senden.
loopInitial(Socket, SlotsUsed, ReceiverDeliveryPID, TimeSyncPID, MessageGenPID, StartTime, RoundTime, PacketList, SlotCount) ->
	receive
		{udp, _ReceiveSocket, _Address, _Port, Packet} ->
			{SlotsUsedNew, PacketListNew, SlotCountNew, RoundTimeNew} = listenAnalyse(PacketList, Packet, TimeSyncPID, ReceiverDeliveryPID, SlotsUsed, RoundTime, getUTC(), SlotCount),
			WaitingTime = getUTC() - StartTime,
			case SlotCountNew == 26 of
				true ->
					io:format("Receiver.erl: Waiting finished with WaitingTime: ~p. Goal: 1000 ~n",[WaitingTime]),
					synchronize(PacketListNew, TimeSyncPID),
					synchronizeSlot(PacketListNew, ReceiverDeliveryPID),
					%sendFreeSlots(SlotsUsedNew, ReceiverDeliveryPID, 1),
					ReceiverDeliveryPID ! {sendInitialSlot, MessageGenPID},
					Time = getUTC(),
					loop(Socket, SlotsUsedNew, ReceiverDeliveryPID, TimeSyncPID, MessageGenPID, Time, Time, [], 1);
				false ->
					synchronize(PacketListNew, TimeSyncPID),
					synchronizeSlot(PacketListNew, ReceiverDeliveryPID),
					loopInitial(Socket, SlotsUsedNew, ReceiverDeliveryPID, TimeSyncPID, MessageGenPID, StartTime, RoundTimeNew, PacketListNew, SlotCountNew)
			end
	after
		1000 ->
			InitialSlot = crypto:rand_uniform(1, 26),%random:uniform(25),
			ReceiverDeliveryPID ! {slot, reset, InitialSlot},
			MessageGenPID ! {initialSlot, InitialSlot},
			SlotsUsedNew = insertInSlotsUsed(initSlotPositions(24), InitialSlot),
			sendFreeSlots(SlotsUsedNew, ReceiverDeliveryPID, 1),
			logging(?LOGFILE, lists:flatten(io_lib:format("Receiver.erl: Frames Total: ~p!~n", [1]))),
			Time = getUTC(),
			loop(Socket, SlotsUsedNew, ReceiverDeliveryPID, TimeSyncPID, MessageGenPID, Time, Time, [], 1)
	end.

loop(Socket, SlotsUsed, ReceiverDeliveryPID, TimeSyncPID, MessageGenPID, StartTime, RoundTime, PacketList, SlotCount) ->
	receive
		{udp, _ReceiveSocket, _Address, _Port, Packet} ->
			{SlotsUsedNew, PacketListNew, SlotCountNew, RoundTimeNew} = listenAnalyse(PacketList, Packet, TimeSyncPID, ReceiverDeliveryPID, SlotsUsed, RoundTime, getUTC(), SlotCount),
			WaitingTime = getUTC() - StartTime,
			case SlotCountNew == 26 of
				true ->
					io:format("Receiver.erl: Waiting finished with WaitingTime: ~p. Goal: 1000 ~n",[WaitingTime]),
					synchronize(PacketListNew, TimeSyncPID),
					synchronizeSlot(PacketListNew, ReceiverDeliveryPID),
					%sendFreeSlots(SlotsUsedNew, ReceiverDeliveryPID, 1),
					%ReceiverDeliveryPID ! {sendInitialSlot, MessageGenPID},
					Time = getUTC(),
					loop(Socket, SlotsUsedNew, ReceiverDeliveryPID, TimeSyncPID, MessageGenPID, Time, Time, [], 1);
				false ->
					synchronize(PacketListNew, TimeSyncPID),
					synchronizeSlot(PacketListNew, ReceiverDeliveryPID),
					loopInitial(Socket, SlotsUsedNew, ReceiverDeliveryPID, TimeSyncPID, MessageGenPID, StartTime, RoundTimeNew, PacketListNew, SlotCountNew)
			end
	after
		1000 ->
			InitialSlot = crypto:rand_uniform(1, 26),%random:uniform(25),
			ReceiverDeliveryPID ! {slot, reset, InitialSlot},
			MessageGenPID ! {initialSlot, InitialSlot},
			SlotsUsedNew = insertInSlotsUsed(initSlotPositions(24), InitialSlot),
			sendFreeSlots(SlotsUsedNew, ReceiverDeliveryPID, 1),
			logging(?LOGFILE, lists:flatten(io_lib:format("Receiver.erl: Frames Total: ~p!~n", [1]))),
			Time = getUTC(),
			loop(Socket, SlotsUsedNew, ReceiverDeliveryPID, TimeSyncPID, MessageGenPID, Time, Time, [], 1)
	end.


listenAnalyse(PacketList, Packet, TimeSyncPID, ReceiverDeliveryPID, SlotsUsed, RoundTime, NowTime, SlotCount) when (NowTime - RoundTime) >= 40 ->
	{StationTyp, _Paylod, Slot, Timestamp} = message_to_string(Packet),
	SlotsUsedNew = insertInSlotsUsed(SlotsUsed, Slot),
	PacektListNew = lists:append(PacketList, [{SlotCount, StationTyp, Timestamp, Slot}]),
	%ReceiverDeliveryPID ! {delete, SlotCount},
	{SlotsUsedNew, PacektListNew, SlotCount + 1, getUTC()};
listenAnalyse(PacketList, Packet, TimeSyncPID, ReceiverDeliveryPID, SlotsUsed, RoundTime, NowTime, SlotCount) when (NowTime - RoundTime) < 40 ->
	{StationTyp, _Paylod, Slot, Timestamp} = message_to_string(Packet),
	SlotsUsedNew = insertInSlotsUsed(SlotsUsed, Slot),
	PacektListNew = lists:append(PacketList, [{SlotCount, StationTyp, Timestamp, Slot}]),
	%ReceiverDeliveryPID ! {delete, SlotCount},
	{SlotsUsedNew, PacektListNew, SlotCount, RoundTime}
.

synchronize([], _TimeSyncPID) ->
	ok;
synchronize([First | Rest], TimeSyncPID) ->
		{SendFlag, List} = synchronize(First, Rest, searchCollisions),
		synchronize(SendFlag, First, TimeSyncPID, send),
		synchronize(List, TimeSyncPID)
.

synchronize(First, [], searchCollisions) ->
	{true, []};
synchronize(First, [Head | Tail], searchCollisions) when element(1, Head) == element(1, First)->
		{false, skip(element(1, First), Tail)};
synchronize(First, [Head | Tail], searchCollisions) when element(1, Head) /= element(1, First)->		
		{true, lists:append([Head], Tail)}
.

synchronize(true, First, TimeSyncPID, send) ->
	{_SlotCount, StationTyp, Timestamp, _Slot} = First,
	TimeSyncPID ! {times, StationTyp, Timestamp};
synchronize(false, _First, _TimeSyncPID, send) ->
	ok
.



synchronizeSlot([], _ReceiverDeliveryPID) ->
	ok;
synchronizeSlot([First | Rest], ReceiverDeliveryPID) ->
		{SendFlag, List} = synchronizeSlot(First, Rest, searchCollisions),
		synchronizeSlot(SendFlag, First, ReceiverDeliveryPID, send),
		synchronizeSlot(List, ReceiverDeliveryPID)
.
synchronizeSlot(First, [], searchCollisions) ->
	{true, []};
synchronizeSlot(First, [Head | Tail], searchCollisions) when element(1, Head) == element(1, First)->
		{false, skip(element(1, First), Tail)};
synchronizeSlot(First, [Head | Tail], searchCollisions) when element(1, Head) /= element(1, First)->		
		{true, lists:append([Head], Tail)}
.

synchronizeSlot(true, First, ReceiverDeliveryPID, send) ->
	{_SlotCount, StationTyp, _Timestamp, Slot} = First,
	ReceiverDeliveryPID ! {slotUsed, Slot};
synchronizeSlot(false, _First, _ReceiverDeliveryPID, send) ->
	ok
.

skip(_Slot, []) ->
	[];
skip(Slot, [Head | Tail]) when element(1, Head) == Slot ->
	skip(Slot, Tail);
skip(Slot, [Head | Tail]) ->
	Tail
.
%ALT, wird nicht mehr benötigt wenn loopInitial funktioniert.
%loop(Collisions, Received, SlotsUsed, Socket, ReceiverDeliveryPID, TimeSyncPID, OldTime, stationAlive, MessageGenPID, PacketList, Frames, initialListen) ->
	%io:format("1~n",[]),
	%{ok, {Address, Port, Packet}} = gen_udp:recv(Socket, 34),
	%logging(?LOGFILE, lists:flatten(io_lib:format("1time at receiver: ~p~n", [getUTC()]))),
%	receive	
%		%Any ->
		 % io:format("~p~n",[Any]);
%		{udp, _ReceiveSocket, _Address, _Port, Packet} -> 
%			debug("received new packet~n",?DEBUG),
%			%io:format("1.5~n",[]),
%			{_CollisionDetected, SlotsUsedNew} = getSlotNumber(SlotsUsed, Packet, PacketList),
%			PacketListNew = PacketList ++ [Packet],
%			TimeSyncPID ! {getTime, self()},
%			receive
%			      {currentTime, CurrentTime} ->
%				     {SlotsUsedNewNew, NewTime, FramesNew} = isFrameFinished(CurrentTime, OldTime, SlotsUsedNew, TimeSyncPID, ReceiverDeliveryPID, Frames),
%				     case FramesNew > Frames of
%						true ->
%							loop(Collisions, Received, SlotsUsedNewNew, Socket, ReceiverDeliveryPID, TimeSyncPID, NewTime, stationAlive, MessageGenPID, [], FramesNew);
%						false ->
%							loop(Collisions, Received, SlotsUsedNewNew, Socket, ReceiverDeliveryPID, TimeSyncPID, NewTime, stationAlive, MessageGenPID, PacketListNew, FramesNew, initialListen)
%				      end
%			end;
%		kill ->
%			kill(Socket, ReceiverDeliveryPID, TimeSyncPID)
%	after
%		1000 ->
%			InitialSlot = crypto:rand_uniform(1, 26),%random:uniform(25),
%			ReceiverDeliveryPID ! {slot, reset, InitialSlot},
%			MessageGenPID ! {initialSlot, InitialSlot},
%			SlotsUsedNew = insertInSlotsUsed(initSlotPositions(24), InitialSlot),
%			sendFreeSlots(SlotsUsedNew, ReceiverDeliveryPID, 1),
%			logging(?LOGFILE, lists:flatten(io_lib:format("1Frames Total: ~p!~n", [Frames + 1]))),
%			loop(Collisions, Received, SlotsUsedNew, Socket, ReceiverDeliveryPID, TimeSyncPID, OldTime, stationAlive, MessageGenPID, [], Frames + 1)
%	end.

%loop(Collisions, Received, SlotsUsed, Socket, ReceiverDeliveryPID, TimeSyncPID, OldTime, stationAlive, MessageGenPID, PacketList, Frames) ->

	%logging(?LOGFILE, lists:flatten(io_lib:format("2time at receiver: ~p~n", [getUTC()]))),
%	receive	
%		{udp, _ReceiveSocket, _Address, _Port, Packet} -> 
%			debug("received new packet~n",?DEBUG),
%			{CollisionDetected, SlotsUsedNew} = getSlotNumber(SlotsUsed, Packet, PacketList),
%			PacketListNew = PacketList ++ [Packet],
%			{CollisionsNew, ReceivdNew} = loop(CollisionDetected, Collisions, Received, Packet, ReceiverDeliveryPID, TimeSyncPID),
%			TimeSyncPID ! {getTime, self()},
%			receive
%			      {currentTime, CurrentTime} ->
%					 logging(?LOGFILE, lists:flatten(io_lib:format("Timediff: ~p~n", [CurrentTime - OldTime]))),
%				     {SlotsUsedNewNew, NewTime, FramesNew} = isFrameFinished(CurrentTime, OldTime, SlotsUsedNew, TimeSyncPID, ReceiverDeliveryPID, Frames),
%				     case FramesNew > Frames of
%						true ->
%							loop(CollisionsNew, ReceivdNew, SlotsUsedNewNew, Socket, ReceiverDeliveryPID, TimeSyncPID, NewTime, stationAlive, MessageGenPID, [], FramesNew);
%						false ->
%							loop(CollisionsNew, ReceivdNew, SlotsUsedNewNew, Socket, ReceiverDeliveryPID, TimeSyncPID, NewTime, stationAlive, MessageGenPID, PacketListNew, FramesNew)
%					 end
%			end;
%		kill ->
%			kill(Socket, ReceiverDeliveryPID, TimeSyncPID)
%	after
%		1000 ->
%			InitialSlot = crypto:rand_uniform(1, 26),%random:uniform(25),
%			ReceiverDeliveryPID ! {slot, reset, InitialSlot},
%			MessageGenPID ! {initialSlot, InitialSlot},
%			SlotsUsedNew = insertInSlotsUsed(initSlotPositions(24), InitialSlot),
%			sendFreeSlots(SlotsUsedNew, ReceiverDeliveryPID, 1),
%			logging(?LOGFILE, lists:flatten(io_lib:format("2Frames Total: ~p!~n", [Frames + 1]))),
%			loop(Collisions, Received, SlotsUsedNew, Socket, ReceiverDeliveryPID, TimeSyncPID, OldTime, stationAlive, MessageGenPID, [], Frames + 1)
%	end
%.

insertInSlotsUsed(SlotsUsed, SlotNumber) ->
		insertInSlotsUsed(SlotsUsed, SlotNumber, 1).

insertInSlotsUsed([], _SlotNumber, _Counter) ->
	[];
insertInSlotsUsed([First | Rest], SlotNumber, Counter) when SlotNumber == Counter ->
	lists:append([First + 1], Rest);
insertInSlotsUsed([First | Rest], SlotNumber, Counter) ->
		lists:append([First], insertInSlotsUsed(Rest, SlotNumber, Counter + 1))
.
	
%Stellt den Teil der Schleife dar, in dem geloggt wird.
%loop(corrupt, Collisions, Received, _Packet, _ReceiverDeliveryPID, _TimeSyncPID) ->
%	logging(?LOGFILE, lists:flatten(io_lib:format("Received: corrupted Package!~n", []))),
%	{Collisions, Received};
loop(true, Collisions, Received, _Packet, _ReceiverDeliveryPID, _TimeSyncPID) ->
	logging(?LOGFILE, lists:flatten(io_lib:format("Collision detected! Collisions ~p ~n", [Collisions]))),
	%logging(?LOGFILE, lists:flatten(io_lib:format("Package successfully received! Received ~p ~n", [Received]))),
	{Collisions + 1, Received};
loop(false, Collisions, Received, Packet, ReceiverDeliveryPID, TimeSyncPID) ->
	logging(?LOGFILE, lists:flatten(io_lib:format("Package successfully received! Received ~p ~n", [Received]))),
	analyse(Packet, ReceiverDeliveryPID, TimeSyncPID),
	{Collisions, Received + 1}.

%Berechnung korrigieren
isFrameFinished(CurrentTime, OldTime, SlotsUsed, TimeSyncPID, ReceiverDeliveryPID, Frames) when ((CurrentTime - OldTime)) >= 1000 ->
	TimeSyncPID ! {getTime, self()},
	TimeSyncPID ! {nextFrame},
	logging(?LOGFILE, lists:flatten(io_lib:format("Frames Total: ~p!~n", [Frames]))),
	ReceiverDeliveryPID ! totalResetSlotreservation,
	sendFreeSlots(SlotsUsed, ReceiverDeliveryPID, 1),

	receive
		{currentTime, CurrentTimeNew} ->
			ok
	end,
	{[], CurrentTimeNew, Frames + 1};
isFrameFinished(CurrentTime, _OldTime, SlotsUsed, _TimeSyncPID, _ReceiverDeliveryPID, Frames)->
	{SlotsUsed, CurrentTime, Frames}.

sendFreeSlots([], _ReceiverDeliveryPID, _Counter) ->
	ok;
sendFreeSlots([First | Rest], ReceiverDeliveryPID, Counter) when First == 0 ->
	ReceiverDeliveryPID ! {slotUnUsed, Counter},
	sendFreeSlots(Rest, ReceiverDeliveryPID, Counter + 1);
sendFreeSlots([_First | Rest], ReceiverDeliveryPID, Counter) ->
	sendFreeSlots(Rest, ReceiverDeliveryPID, Counter + 1)
.


%Extrahiert die Daten aus dem Paket und loggt den Inhalt.
analyse(Packet, ReceiverDeliveryPID, TimeSyncPID) ->
	{StationTyp, _Payload, SlotNumber, Timestamp} = message_to_string(Packet),
	ReceiverDeliveryPID ! {slot, SlotNumber},
	TimeSyncPID ! {times, StationTyp, Timestamp}
.

%Extrahiert die Daten aus dem Paket von From bis To.	
%extractIntervall(Binary, From, To) ->
%	binary:decode_unsigned(extractIntervall(Binary, From, To, 1)).

%Extrahiert die Daten aus dem Paket von From bis To.
%extractIntervall([First | Rest], From, To, Counter) when Counter =< To, Counter < From ->
%	extractIntervall(Rest, From, To, Counter + 1);
%extractIntervall([First | Rest], From, To, Counter) when Counter =< To, Counter >= From ->
%	[First] ++ extractIntervall(Rest, From, To, Counter + 1);
%extractIntervall(_Rest, _From, To, Counter) when Counter > To ->
%	[].
	
%Extrahiert die Slot-Nr. aus dem Paket und prüft
%Ob der Slot im nächsten Frame schon von einer anderen Station
%in Gebrauch sein wird.
getSlotNumber(SlotsUsed, Packet, PacketList) ->
	{_StationTyp,_Nutzdaten, SlotNumber, TimestampCurrent} = message_to_string(Packet),
	%{willSlotBeInUse(SlotsUsed, SlotNumber), countSlotNumberUsed(SlotsUsed, SlotNumber)}.
	{willSlotBeInUse(TimestampCurrent, PacketList), countSlotNumberUsed(SlotsUsed, SlotNumber)}.

%Liefert true oder false zurück, je nachdem ob der Slot
%im nächsten Frame von einer anderen Station in Gebrauch sein wird.
%Im Ausnahmefall wird corrupt zurückgeliefert, wenn die Slot-Nr. aus dem Paket > 25 war.
%willSlotBeInUse(SlotsUsed, Timestamp) ->
%	willSlotBeInUse(SlotsUsed, Timestamp, 1).
willSlotBeInUse(TimestampCurrent, PacketList) ->
	logging(?LOGFILE, lists:flatten(io_lib:format("PacketList ~p~n", [PacketList]))),
	willSlotBeInUse(TimestampCurrent, PacketList, length(PacketList), false).

willSlotBeInUse(_TimestampCurrent, _PacketList, PacketListLength, Collision) when PacketListLength == 0 ; Collision == true->
	Collision;
willSlotBeInUse(TimestampCurrent, [Head | Tail], _PacketListLength, _Collision) ->
	{_StationTyp, _Payload, SlotNumber, Timestamp} = message_to_string(Head),
	CollisionNew = checkTimestamp(TimestampCurrent, Timestamp, SlotNumber),
	willSlotBeInUse(TimestampCurrent, Tail, length(Tail), CollisionNew).


checkTimestamp(TimestampCurrent, Timestamp, _SlotNumber) when (TimestampCurrent < (Timestamp - 15)) , (TimestampCurrent > (Timestamp + 15)) ->
	false;
checkTimestamp(TimestampCurrent, Timestamp, SlotNumber) ->
	logging(?LOGFILE, lists:flatten(io_lib:format("Collision detected; TimestampCurrent: ~p Timestamp: ~p SlotNumber: ~p~n", [TimestampCurrent, Timestamp, SlotNumber]))),
	true.
	
%Liefert true oder false zurück, je nachdem ob der Slot
%im nächsten Frame von einer anderen Station in Gebrauch sein wird.
%Im Ausnahmefall wird corrupt zurückgeliefert, wenn die Slot-Nr. aus dem Paket > 25 war.
%willSlotBeInUse([_First | Rest], SlotNumber, Counter) when SlotNumber > Counter ->
%	willSlotBeInUse(Rest, SlotNumber, Counter + 1);
%willSlotBeInUse([First | _Rest], SlotNumber, Counter) when (SlotNumber == Counter), (First /= 0) ->
%	true;
%willSlotBeInUse([First | _Rest], SlotNumber, Counter) when (SlotNumber == Counter), (First == 0)->
%	false;
%willSlotBeInUse([], _SlotNumber, _Counter) ->
%	corrupt.

%Zählt die Anzahl der Stationen hoch, die den Slot SlotNumber im
%nächsten Frame benutzen werden.
countSlotNumberUsed(SlotsUsed, SlotNumber) ->
	io:format("incout: ~p~n", [SlotsUsed]),
	Result = countSlotNumberUsed(SlotsUsed, SlotNumber, 1),
	io:format("result: ~p~n", [Result]),
	Result
.

%Zählt die Anzahl der Stationen hoch, die den Slot SlotNumber im
%nächsten Frame benutzen werden.
countSlotNumberUsed([First | Rest], SlotNumber, Counter) when SlotNumber < Counter ->
	lists:append([First], countSlotNumberUsed(Rest, SlotNumber, Counter + 1));
countSlotNumberUsed([First | Rest], SlotNumber, Counter) when SlotNumber == Counter ->
	lists:append([First + 1], Rest);
countSlotNumberUsed(Rest, _SlotNumber, _Counter) ->
	Rest
.

kill(Socket, ReceiverDeliveryPID, TimeSyncPID) ->
	gen_udp:close(Socket),
	ReceiverDeliveryPID ! kill,
	debug("send kill to receiver services~n", ?DEBUG),
	TimeSyncPID ! kill,
	debug("send kill to timesync~n", ?DEBUG),
	debug("Shutdown Receiver~n", ?DEBUG)
.

message_to_string(Packet) ->
	List = binary:bin_to_list(Packet),
	StationTyp = lists:nth(1, List),
	Paylod = erlang:binary_to_list(Packet,2,25),
	Slot = lists:nth(1, erlang:binary_to_list(Packet,26,26)),
	Timestamp = erlang:binary_to_list(Packet,27,34),
	io:format("StationTyp ~p,Paylod ~p,Slot ~p,Timestamp ~p~n", [StationTyp,Paylod,Slot,Timestamp]),
	{StationTyp, Paylod, Slot, Timestamp}.

%%%%%%%%%%%%Receiver Services%%%%%%%%%%%%%
delivery(stationAlive, SlotReservationPID, TimeSyncPID) ->
	receive
		{slot, reset, NextSlot} ->
			SlotReservationPID ! {slot, reset, NextSlot},
			delivery(stationAlive, SlotReservationPID, TimeSyncPID);
		{slotUsed, NextSlot} ->
			SlotReservationPID ! {slotUsed, NextSlot},
			delivery(stationAlive, SlotReservationPID, TimeSyncPID);
		{slotUnUsed, NextSlot} ->
			SlotReservationPID ! {slotUnUsed, NextSlot},
			delivery(stationAlive, SlotReservationPID, TimeSyncPID);
		{times, StationClass, TimeInSlot} ->
			TimeSyncPID ! {times, StationClass, TimeInSlot},
			delivery(stationAlive, SlotReservationPID, TimeSyncPID);
		{sendInitialSlot, MessageGenPID} ->
			SlotReservationPID ! {getSlot, MessageGenPID};
		totalResetSlotreservation ->
			SlotReservationPID ! totalReset;
		{delete, Slot} ->
			 SlotReservationPID ! {delete, Slot};
		kill ->
			SlotReservationPID ! kill,
			debug("send kill to slotreservation~n",?DEBUG),
			debug("receiver services terminated~n",?DEBUG)
	end
.

getHostAddress(InterfaceName) ->
	{ok, IfAddr} = inet:getifaddrs(),
	{_Interface, Addresses} = lists:keyfind(atom_to_list(InterfaceName), 1, IfAddr),
	{addr, HostAddress} = lists:keyfind(addr, 1, Addresses),
	HostAddress.
			
