package mware_lib;

import java.net.InetAddress;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Francis und Fabian
 * 
 *         Stellt Request/Reply-Protokoll bereit und hat somit auch einen Socket
 *         bzw. Serversocket
 */

public class CommunicationModule {

	private ServerSocket serverSocket;
	private ObjectInputStream input;
	private static InetAddress localHost;
	private static final int COMMUNICATIONMODULEPORT = 50001;
	private static final int REQUEST = 0;
	private static final int REPLY = 1;
	private boolean isAlive;
	private static int messageIDCounter;

	private static List<Thread> communicationThreadList;
	private RequestDemultiplexer demultiplexer;

	public CommunicationModule() {
		this.demultiplexer = new RequestDemultiplexer();
		this.isAlive = true;
		messageIDCounter = 0;

		try {
			this.serverSocket = new ServerSocket(COMMUNICATIONMODULEPORT);
			communicationThreadList = new ArrayList<Thread>();
			localHost = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void waitingForMessages() {
		Socket socket;
		while (this.isAlive) {
			try {
				socket = this.serverSocket.accept();
				this.input = new ObjectInputStream(socket.getInputStream());
				MessageADT m = (MessageADT) this.input.readObject();
				// Request
				if (m.getMessageType() == REQUEST) {
					requestToServant(m);
				}
				// Reply
				else if (m.getMessageType() != REPLY) {
					replyToProxy(m);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		for (Thread item : communicationThreadList) {
			((CommunicationModuleThread) item).interrupt();
		}
	}

	private void requestToServant(MessageADT m) {
		this.demultiplexer.pass(m);
	}

	private void replyToProxy(MessageADT mReturn) {
		for (Thread item : communicationThreadList) {
			if (((CommunicationModuleThread) item).getSendMessage()
					.getMessageID() == mReturn.getMessageID()) {
				((CommunicationModuleThread) item).setReceivedMessage(mReturn);
				item.notify();
			}
		}

	}

	public static CommunicationModuleThread getNewCommunicationThread(
			MessageADT m) {
		CommunicationModuleThread c = new CommunicationModuleThread(m);
		communicationThreadList.add(c);
		c.start();
		return c;
	}
	
	public static synchronized int messageIDCounter(){
		return messageIDCounter++;
	}

	public static void deleteThread(CommunicationModuleThread c) {
		communicationThreadList.remove(c);
	}

	public static InetAddress getLocalHost() {
		return localHost;
	}

	public static int getCommunicationmoduleport() {
		return COMMUNICATIONMODULEPORT;
	}

	public void setAlive(boolean isAlive) {
		this.isAlive = isAlive;
	}
	
	public static void debugPrint(String text){
		if(ObjectBroker.DEBUG){
			System.out.println(text);
		}
	}

}