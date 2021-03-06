package mware_lib;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


/**
 * 
 * @author Fabian
 * 
 *         Führt je eine Tabelle (Key-Value) in der Key die entfernte
 *         Objektreferenz darstellt und der Value die lokale Objektreferenz
 *         (Proxy bzw. Servant). Dieses Modul erzeugt beim Anmelden eines
 *         entfernten Objektes beim Namensdienst eine entfernte Objektreferenz.
 *         Realisiert wird das über die Implementierung von hashcode und equals
 *         auf Seiten der entfernten Objektreferenz.
 */

public class ReferenceModule {

	private static Map<RemoteObjectRef, Object> mapRemoteServant = new HashMap<RemoteObjectRef, Object>();
	private static Map<RemoteObjectRef, Object> mapRemoteProxy = new HashMap<RemoteObjectRef, Object>();
	
	/**
	 * Erzeugt eine neue entfernte Objekt-Referenz zu einem gegebenen Servant
	 * 
	 * @param myObject
	 *            Servant zu dem eine neue entfertne Objekt-Referenz erzeugt
	 *            werden soll
	 * @return neue entfernte Objekt-Referenz
	 */
	public static synchronized RemoteObjectRef createNewRemoteRef(Object myObject) {
		int port = CommunicationModule.getCommunicationmoduleport();
		int objectNumber = -1;

		if (myObject instanceof accessor_one.ClassOneImplBase) {
			objectNumber = 1;
		} else if (myObject instanceof accessor_one.ClassTwoImplBase) {
			objectNumber = 2;
		} else if (myObject instanceof accessor_two.ClassOneImplBase) {
			objectNumber = 3;
		}

		RemoteObjectRef rawObjRef = null;

		/*
		 * vsp3_sequ_server_start: 3.2.1.1.1: Neue Referenz erzeugen
		 */
		rawObjRef = new RemoteObjectRef(CommunicationModule.getLocalHost(),
				port, System.currentTimeMillis(), objectNumber);

		/*
		 * vsp3_sequ_server_start: 3.2.1.1.2: Servant in Tabelle abspeichern
		 */
		servantToTable(rawObjRef, myObject);
		CommunicationModule
				.debugPrint("mware_lib.ReferenceModule: new RemoteObjectRef created an saved in servantlist.");
		return rawObjRef;
	}

	/**
	 * Sichert die zuvor erzeugte entfernte Objekt-Referenz in die Tabelle
	 * 
	 * @param rawObjRef
	 *            entfernte Objekt-Referenz
	 * @param myObject
	 *            Servant
	 */
	private static synchronized void servantToTable(RemoteObjectRef rawObjRef,
			Object myObject) {
		CommunicationModule
				.debugPrint("mware_lib.ReferenceModule: save new servant in servantlist.");

		mapRemoteServant.put(rawObjRef, myObject);
	}

	/**
	 * Prueft, ob ein rawObjRef bereits als Proxy vorhanden ist
	 * 
	 * @param rawObjRef
	 *            Proxy
	 * @return wenn Proxy in Tabell true, sonst false
	 */
	public static synchronized boolean contains(Object rawObjRef) {
		CommunicationModule
				.debugPrint("mware_lib.ReferenceModule: check if proxy is in proxylist.");
		return mapRemoteProxy.containsKey(rawObjRef);
	}

	/**
	 * Gibt den Proxy zu einer entfernten Objekt-Referenz zurueck, sofern dieser
	 * in der Liste ist
	 * 
	 * @param rawObjRef
	 *            entfernte Objekt-Referenz zu einem Proxy
	 * @return Proxy wenn dieser in der Liste ist, sonst null
	 */
	public static synchronized Object getProxy(RemoteObjectRef rawObjRef) {
		for (Entry<RemoteObjectRef, Object> item : mapRemoteProxy.entrySet()) {
			if (item.getKey().equals(rawObjRef)) {
				CommunicationModule
						.debugPrint("mware_lib.ReferenceModule: return proxy from proxylist.");
				return item.getValue();
			}
		}
		CommunicationModule
				.debugPrint("mware_lib.ReferenceModule: proxy not in proxylist.");
		return null;
	}

	/**
	 * fuegt einen neuen Proxy zu der Proxy-Tabelle hinzu
	 * 
	 * @param rawObj
	 *            entfernte Objekt-Referenz zu einem Proxy
	 * @param remoteObj
	 *            Proxy
	 */
	public static synchronized void add(RemoteObjectRef rawObj, Object remoteObj) {
		CommunicationModule
				.debugPrint("mware_lib.ReferenceModule: add new proxy to proxylist.");
		mapRemoteProxy.put(rawObj, remoteObj);
	}

	/**
	 * Holt zu der entfernten Objekt-Referenz den zugehoerigen Servant aus der
	 * Tabelle
	 * 
	 * @param rawObjRef
	 *            entfernte Objekt-Referenz zum Servant
	 * @return Servant, wenn dieser in der Tabelle ist, sonst null
	 */
	public static synchronized Object getServant(RemoteObjectRef rawObjRef) {
		for (Entry<RemoteObjectRef, Object> item : mapRemoteServant.entrySet()) {
			if (item.getKey().equals(rawObjRef)) {
				CommunicationModule
						.debugPrint("mware_lib.ReferenceModule: get servant from servantlist");
				return item.getValue();
			}
		}
		CommunicationModule
				.debugPrint("mware_lib.ReferenceModule: servant not in servantlist.");
		return null;
	}
}
