package mware_lib;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import shared_types.NameServiceRequest;
import shared_types.RemoteObjectRef;

/**
 * 
 * @author Fabian
 * 
 *         Ist der Stellvertreter des Namensdienstes. Dieser bekommt durch die
 *         Methodenaufrufe rebind/resolve die Aufforderung beim eigentlichen
 *         entfernten Namensdienst die jeweilige Anfrage zu stellen. Dies
 *         geschieht mittels Sockets.
 */
public class NameServiceProxy extends NameService {

	private ServerSocket serverSocket;
	private Socket socket;
	private ObjectInputStream input;
	private ObjectOutputStream output;
	private String serviceHost;
	private int servicePort;

	public NameServiceProxy(String serviceHost, int port) {
		this.serviceHost = serviceHost;
		this.servicePort = port;
		CommunicationModule.debugPrint(this.getClass(), "initialized");
	}

	@Override
	public void rebind(Object servant, String name) {
		if (servant != null) {
			/*
			 * vsp3_sequ_server_start: 3.2.1.1: neue entfernte Objekt-Referenz
			 * erzeugen 3.2.1.1.3: entfernte Objekt-Referenz erhalten
			 */
			// RemoteObjectRef rof =
			// ReferenceModule.createNewRemoteRef(servant);
			RemoteObjectRef rof = ReferenceModule.createNewRemoteRef(servant);
			/*
			 * vsp3_sequ_server_start: 3.2.1.2: Neue Nachricht fuer Nameservice
			 * erzeugen
			 */
			NameServiceRequest n = null;
			try {
				n = new NameServiceRequest("rebind", name, rof, InetAddress
						.getLocalHost().getCanonicalHostName(),
						this.servicePort);
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			}
			try {
				this.socket = new Socket(this.serviceHost, this.servicePort);
				this.output = new ObjectOutputStream(
						this.socket.getOutputStream());
				/*
				 * vsp3_sequ_server_start: 4: Nachricht an Nameservice senden
				 */
				this.output.writeObject(n);

				CommunicationModule.debugPrint(this.getClass(),
						"send request (rebind) to nameservice");

				this.output.close();
				this.socket.close();
			} catch (IOException e) {
				System.out.println(this.getClass()
						+ ": cannot send a request (rebind) to nameservice!");
			}
			CommunicationModule.debugPrint(this.getClass(), "new Servant: "
					+ servant + "with name: " + name + " rebinded");
		}
	}

	@Override
	public Object resolve(String name) {
		NameServiceRequest request = null;
		NameServiceRequest n = null;
		try {
			this.serverSocket = new ServerSocket(0);

			n = new NameServiceRequest("resolve", name, null, InetAddress
					.getLocalHost().getCanonicalHostName(),
					this.serverSocket.getLocalPort());
			
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			this.socket = new Socket(this.serviceHost, this.servicePort);
			this.output = new ObjectOutputStream(this.socket.getOutputStream());
			CommunicationModule.debugPrint(this.getClass(),
					"send request (resolve) to nameservice");
			this.output.writeObject(n);

			this.socket = this.serverSocket.accept();
			this.input = new ObjectInputStream(this.socket.getInputStream());
			request = (NameServiceRequest) this.input.readObject();

			this.input.close();
			this.output.close();
			this.socket.close();
			this.serverSocket.close();
		} catch (IOException e) {
			System.out.println(this.getClass()
					+ ": cannot send a request (resolve) to nameservice");
		} catch (ClassNotFoundException e) {
			System.out.println(this.getClass()
					+ ": cannot get object from nameservice");
		}

		CommunicationModule.debugPrint(this.getClass(), "Service: " + name
				+ " resolved");

		if (request.getObjectRef() != null) {
			return request.getObjectRef();
		}
		return null;
	}

}
