package proxyserver;

import java.util.ArrayList;
import java.util.List;

import account.POP3Account;

/**
 * 
 * @author Fabian Reiber, Francis Opoku
 * 
 */

public class Server  {

	private static List<POP3Account> input = new ArrayList<POP3Account>();
	public static void main(String[] args) {
		// Parameter: int = Maximale Anzahl Clients; input = accounts deren Mails abgerufen werden
		//input.add(new POP3Account("hawrnp1@gmx.de", "", "pop.gmx.net", 995));
		input.add(new POP3Account("bai4rnpA", "", "lab30", 11000));
		input.add(new POP3Account("bai4rnpA", "", "lab31", 11000));
		POP3Proxy server = new POP3Proxy(15, input);
		server.start();
	}

}
