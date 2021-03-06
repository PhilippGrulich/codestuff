package test;

/**
 * Verweise zum Entwurf:
 * <Klassendiagramm> : Implementierung der Klasse im Package test
 * 
 * Startet den Test der Middleware mit 1 Server und 1 Client.
 * @author Francis
 *
 */
public class MiddlewareTest {

	public MiddlewareTest() {
	}

	/**
	 * Der Server muss vor dem Client gestartet worden sein.
	 * 
	 * @param args Wenn args[0] = 0 dann Serverstart mit args[1] = Host des Nameservice 
	 * und args[2] = Port des Nameservice, sonst
	 * args[0] = 1 dann Clientstart mit args[1] = Host des Nameservice 
	 * und args[2] = Port des Nameservice.
	 */
	public static void main(String[] args) {

		if (args.length != 3) {
			usage();
			System.exit(-1);
		}

		switch (Integer.valueOf(args[0])) {
		case 0:
			ServerStart server = new ServerStart(args[1],
					Integer.valueOf(args[2]));
			server.start();
			break;
		case 1:
			ClientStart client = new ClientStart(args[1],
					Integer.valueOf(args[2]));
			client.setName("0");
			client.start();
			ClientStart client2 = new ClientStart(args[1],Integer.valueOf(args[2]));
			client2.setName("1");
			client2.start();	
			ClientStart client3 = new ClientStart(args[1],Integer.valueOf(args[2]));
			client3.setName("1");
			client3.start();
			break;
		}
	}

	private static void usage() {
		System.out
				.println("ServerStart: java/MiddlewareTest 0 <nameservice-host> <nameservice-port>");
		System.out
				.println("ClientStart: java/MiddlewareTest 1 <nameservice-host> <nameservice-port>");
	}
}
