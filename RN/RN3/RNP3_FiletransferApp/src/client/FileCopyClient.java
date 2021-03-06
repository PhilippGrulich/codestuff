package client;

/* FileCopyClient.java
 Version 0.1 - Muss erg�nzt werden!!
 Praktikum 3 Rechnernetze BAI4 HAW Hamburg
 Autoren:
 */

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

import fc_adt.*;

public class FileCopyClient extends Thread {

	// -------- Constants
	public final static boolean TEST_OUTPUT_MODE = false;

	public final int SERVER_PORT = 23000;
	public final int CLIENTCOPY_PORT = 23001;

	public final int UDP_PACKET_SIZE = 1008;

	// -------- Public parms
	public String servername;

	public String sourcePath;

	public String destPath;

	public int windowSize;

	public long serverErrorRate;

	// -------- Variables
	// current default timeout in nanoseconds
	// 100000000L := 0,1s
	//
	// private long timeoutValue = 100000000L;
	// laut RFC2988 ist timeout initial auf 3s zu setzen
	private long timeoutValue = 3000000000L;
	private static long MAXTIMEOUTVALUE = 60000000000L;

	// Round-Trip-Time
	private long RTT = 0;
	// Smoothed RTT (geschaetzte RTT)
	private long SRTT;
	// RTT_Varianz
	private long RTTVAR;
	//Timer-Genauigkeit -> Wert? -> erstmal etwas hoeher gesetzt 1s
	private long measurementPrecision = 1000000000L;

	private DatagramSocket client;
	private boolean connectionEstablished;

	private ReceiveAckClient raCl;

	private FileInputStream readFileInput;
	byte[] fileData;
	// bereits gelesene Datenpakete aus der Datei
	private int justReaded = 0;
	// insgesamt zu lesende Datenpakete aus der Datei
	private int needsToRead = 0;
	// Flag um zu signalisieren, ob letztes Datenpaket bereits gelesen wurde
	private boolean eofReached = false;

	// Sendepufferpointer
	private List<FCpacket> sendBuf;
	private Semaphore accessBuffer;
	private int sendbase = 0;
	private int nextSeqNum = 1;

	//Analyse-Variablen
	private long totalTransmissionTime;
	private int totalTimerInterrupts = 0;
	private int totalReceivedAcks = 0;
	//Fuer alle verlustfrei gesendeten Pakete
	private long midRTTForAllGoodAcks;
	private long midRTTForAllAcks = 0;
	
	// Constructor
	public FileCopyClient(String serverArg, String sourcePathArg,
			String destPathArg, String windowSizeArg, String errorRateArg) {
		servername = serverArg;
		sourcePath = sourcePathArg;
		destPath = destPathArg;
		windowSize = Integer.parseInt(windowSizeArg);
		serverErrorRate = Long.parseLong(errorRateArg);

		try {
			this.client = new DatagramSocket(CLIENTCOPY_PORT,
					InetAddress.getLocalHost());
			this.readFileInput = new FileInputStream(this.sourcePath);
			this.sendBuf = new LinkedList<FCpacket>();
			this.accessBuffer = new Semaphore(1);

		} catch (UnknownHostException e) {
			System.out.println("can't create an new datagramsocket object");
			e.printStackTrace();
		} catch (SocketException e) {
			System.out.println("can't create an new datagramsocket object");
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			System.out.println("can't read file");
			e.printStackTrace();
		}
	}

	public void runFileCopyClient() {
		//************TRANSMISSION TIME START************
		this.totalTransmissionTime = System.nanoTime();
		
		FCpacket controlPacket = makeControlPacket();
		
		DatagramPacket udpControlPacket;
		try {
			// +8 Bytes, wegen der Sequenznummer -> werden diese nicht mit drauf
			// gerechnet
			// werden auf der Serverseite die uebergebenen Parameter nicht
			// korrekt erkannt
			udpControlPacket = new DatagramPacket(
					controlPacket.getSeqNumBytesAndData(),
					controlPacket.getLen() + 8,
					InetAddress.getByName(this.servername), SERVER_PORT);

		} catch (UnknownHostException e1) {
			System.out.println("couldn't get inetadresse by name");
			e1.printStackTrace();
			udpControlPacket = null;
			this.connectionEstablished = false;
		}

		this.sendBuf.add(controlPacket);
		readFile();

		// Client starten um die ACK's des Servers zu receiven
		// weiterhin wird das Control-Packet versendet
		try {
			if (udpControlPacket != null) {
				this.raCl = new ReceiveAckClient();
				this.raCl.start();

				startTimer(controlPacket);
				this.client.send(udpControlPacket);

				this.connectionEstablished = true;
			}
		} catch (IOException e) {
			System.out.println("couldn't write first packet");
			e.printStackTrace();

		}
		// die sendbase wird nur "weiter geschoben", wenn auch ein ACK ankam,
		// daher kann hier die senbase als Bedingung fuer die Schleife verwendet
		// werden
		while (this.connectionEstablished && this.sendbase <= this.needsToRead) {
			// durch die Liste wird bei 0 angefangen zu zaehlen, d.h. exklusive
			// windowsize - 1
			if (this.nextSeqNum < this.sendbase + this.windowSize
					&& this.nextSeqNum < this.sendBuf.size()) {

				try {
					this.accessBuffer.acquire();
					sendNextSeqNumPacket();
					this.accessBuffer.release();
				} catch (InterruptedException e) {
					System.out.println("can't acquire semaphore");
					e.printStackTrace();
				}
			}/*
			 * aktives warten -> klappt aber nicht ganz -> deadlock else{
			 * synchronized (this) { try { System.out.println("start to wait");
			 * this.wait(); System.out.println("after wait"); } catch
			 * (InterruptedException e) { System.out.println("can continue");
			 * e.printStackTrace(); } } }
			 */
		}

		try {
			this.readFileInput.close();
		} catch (IOException e) {
			System.out.println("couldn't close fileinputstream");
			e.printStackTrace();
		}

		ReceiveAckClient.currentThread().interrupt();
		this.client.close();

		/**
		 * Pruefung, ob auch alle Pakete "geacked" wurden
		 */
		/*int checkCounter = 0;
		for (FCpacket item : this.sendBuf) {
			if (item.isValidACK()) {
				checkCounter++;
			}
		}*/
		
		// plus 1, da controlpacket ebenfalls in dem Sendepuffer enthalten
		if (this.totalReceivedAcks == this.justReaded + 1) {
			System.out.println("successful sent!");
		} else {
			System.out
					.println("something went wrong! not all packets sent right!");
		}
		

		//************TRANSMISSION TIME END************
		this.totalTransmissionTime = System.nanoTime() - this.totalTransmissionTime;

		//************TOTAL MID VALUE OF RTT FOR ALL ACKS************
		this.midRTTForAllGoodAcks /= this.totalReceivedAcks;
		this.midRTTForAllAcks /= this.totalReceivedAcks;

		System.out.println("TIME IN SEC: " + this.totalTransmissionTime / Math.pow(10, 9));
		System.out.println("TIMERINTERRUPTS: " + this.totalTimerInterrupts);
		System.out.println("RECEIVEDACKS: " + this.totalReceivedAcks);
		System.out.println("MIDRTT FOR GOOD ACKS IN MS: " + this.midRTTForAllGoodAcks / Math.pow(10, 6));
		System.out.println("MIDRTT FOR ALL ACKS IN MS: " + this.midRTTForAllAcks / Math.pow(10, 6));
		
		writeLogFile();
		writeCsvFile();
		
	}

	/**
	 * Aus der zu sendenden Datei die Bytes lesen und im Anschluss die ersten
	 * Pakete in den Sendepuffer legen
	 */
	private void readFile() {
		try {
			this.fileData = Files.readAllBytes(Paths.get(sourcePath));

			// anzahl der Pakete die insgesamt in den Sendepuffer geschrieben
			// werden muessen
			// Math.ceil rundet immer auf, aber durch dir vorherige division
			// bringt dies auch
			// nichts mehr, da dort bereits abgerundet wird.
			// this.needsToRead = (int) Math.ceil(this.fileData.length /
			// (UDP_PACKET_SIZE - 8));
			this.needsToRead = this.fileData.length / (UDP_PACKET_SIZE - 8) + 1;
			System.out.println("datalength in readFile(): "
					+ this.fileData.length);

		} catch (IOException e) {
			System.out.println("can't read all bytes from sourcepath");
			e.printStackTrace();
		}

		// da bereits das controlPacket im Sendepuffer ist, brauch nur ein Paket
		// weniger gelesen werden als die windowsize gross ist
		storePacketsInSendBuffer(windowSize - 1);
	}

	/**
	 * liest die naechsten n * UPD_PACKET_SIZE - 8 bytes aus dem glesenen file,
	 * erzeugt FCpackets und legt diese in den Sendepuffer ab
	 * 
	 * @param n
	 *            Anzahl der Pakete die erstellt werden sollen
	 */
	private void storePacketsInSendBuffer(int n) {
		int fileByteSize = UDP_PACKET_SIZE - 8;
		byte[] packetTmp;
		for (int i = 0; i < n; i++) {

			// da das letzte Paket nicht unbedingt UDP_PACKE_SIZE haben muss,
			// muss an dieser Stelle die zu lesenden bytes neu berechnet werden
			if (this.needsToRead == 1
					|| this.needsToRead - 1 == this.justReaded) {
				int packetLength = this.fileData.length % fileByteSize;
				packetTmp = new byte[packetLength];

				System.arraycopy(this.fileData, this.justReaded * fileByteSize,
						packetTmp, 0, packetLength);
				this.sendBuf.add(new FCpacket(this.justReaded + 1, packetTmp,
						packetLength));
				this.eofReached = true;
				this.justReaded++;
				break;
			} else {
				packetTmp = new byte[fileByteSize];
				System.arraycopy(this.fileData, this.justReaded * fileByteSize,
						packetTmp, 0, fileByteSize);
				this.sendBuf.add(new FCpacket(this.justReaded + 1, packetTmp,
						fileByteSize));
				this.justReaded++;
			}

		}
	}

	/**
	 * Sendet das naechste Packet aus dem Sendepuffer an den Server
	 */
	private void sendNextSeqNumPacket() {
		FCpacket nextPacket = this.sendBuf.get(this.nextSeqNum);
		try {
			DatagramPacket udpNextPacket = new DatagramPacket(
					nextPacket.getSeqNumBytesAndData(),
					nextPacket.getLen() + 8,
					InetAddress.getByName(this.servername), SERVER_PORT);
			startTimer(nextPacket);
			this.client.send(udpNextPacket);
			this.nextSeqNum++;
		} catch (IOException e) {
			System.out.println("couldn't send next packet");
			e.printStackTrace();
		}
	}

	/**
	 * timer start, sofern das paket zum aller ersten mal versendet wird
	 * Timer Operations
	 */
	public void startTimer(FCpacket packet) {
		/* Create, save and start timer for the given FCpacket */
		FC_Timer timer = new FC_Timer(timeoutValue, this, packet.getSeqNum());
		packet.setTimer(timer);
		// Timestamp setzen wurde hier hinein verlagert
		packet.setTimestamp(System.nanoTime());
		timer.start();
	}

	/**
	 * on retransmit, da hier der timeout-wert verdoppelt wird, sofern das paket erneut 
	 * gesendet werden muss
	 * @param packet
	 */
	public void startTimerOnRetransmit(FCpacket packet) {
		/* Create, save and start timer for the given FCpacket */
		long newTimeout = 2 * this.timeoutValue;
		if(newTimeout > MAXTIMEOUTVALUE){
			newTimeout = MAXTIMEOUTVALUE;
		}
		FC_Timer timer = new FC_Timer(newTimeout, this,
				packet.getSeqNum());
		packet.setTimer(timer);
		// Timestamp setzen wurde hier hinein verlagert
		//packet.setTimestamp(System.nanoTime());
		timer.start();
	}

	/**
	 * timer interrupten, sofern das ACK des paketes erfolgreich ankam
	 * 
	 * @param packet
	 */
	public void cancelTimer(FCpacket packet) {
		/* Cancel timer for the given FCpacket */
		testOut("Cancel Timer for packet" + packet.getSeqNum());
		
		if (packet.getTimer() != null) {
			packet.getTimer().interrupt();
		}
		
		// Nachdem die erste RTT gemessen wurde, werden die Werte gesetzt
		if (this.RTT == 0) {
			this.RTT = System.nanoTime() - packet.getTimestamp();
			
			//************ADD NEXT RTT************
			this.midRTTForAllGoodAcks += this.RTT;

			//************ADD NEXT RTT FOR ALL ACKS************
			this.midRTTForAllAcks += this.RTT;
			
			this.SRTT = this.RTT;
			this.RTTVAR = (long) (0.5 * this.RTT);
			//this.timeoutValue = this.SRTT + (4 * this.RTTVAR);
			this.timeoutValue = this.RTT + Math.max(this.measurementPrecision, 4 * this.RTTVAR);

		} else if (!packet.isRetransmit()) {
			this.RTT = System.nanoTime() - packet.getTimestamp();
			
			//************ADD NEXT RTT************
			this.midRTTForAllGoodAcks += this.RTT;
			//************ADD NEXT RTT FOR ALL ACKS************
			this.midRTTForAllAcks += this.RTT;
			
			computeTimeoutValue();
		}
		else{
			//************ADD NEXT RTT FOR ALL ACKS************
			this.midRTTForAllAcks += System.nanoTime() - packet.getTimestamp();
		}
	}

	/**
	 * fuer Retransmit zustaendig -> d.h. hier wird die "samplertt" alias RTT nicht neu
	 * berechnet, da diese immer nur dann berechnet wird, wenn paket NICHT nochmals versendet
	 * wird, da timer abgelaufen ist. Deshalb wird startTimerOnRetransmit ausgefuehrt und
	 * das Retransmit-Flag gesetzt.
	 * specific task performed at timeout
	 */
	public void timeoutTask(long seqNum) {
		if (!this.client.isClosed()) {
			try {
				this.accessBuffer.acquire();
				FCpacket p = this.sendBuf.get((int) seqNum);
				
				p.setRetransmit(true);
				startTimerOnRetransmit(p);
				
				this.client.send(new DatagramPacket(p.getSeqNumBytesAndData(),
						p.getLen() + 8, InetAddress.getByName(this.servername),
						SERVER_PORT));
				this.accessBuffer.release();
				
				//************COUNTING TIMER INTERRUPTS************
				this.totalTimerInterrupts++;
				
			} catch (IOException e) {
				System.out.println("couldn't send datagram to server");
				e.printStackTrace();
			} catch (InterruptedException e) {
				// System.out.println("can't acquire semaphore");
				// e.printStackTrace();
			}
		}
	}

	/**
	 * Ohne Parameter, da "sampleRTT" bereits im cancleTimer berechnet wurde ->
	 * "sampleRTT" = RTT Computes the current timeout value (in nanoseconds)
	 */
	public void computeTimeoutValue() {
		float x = 0.25f;
		float y = x / 2;
		this.RTTVAR = (long) (((1 - x) * this.RTTVAR) + (x * Math.abs(this.SRTT
				- this.RTT)));
		this.SRTT = (long) ((1 - y) * this.SRTT + (y * this.RTT));		
		this.timeoutValue = this.SRTT + Math.max(this.measurementPrecision, 4 * this.RTTVAR);
	}

	/**
	 * wird nicht verwendet, aber zur vervollstaendigung noch im code enthalten
	 * Computes the current timeout value (in nanoseconds)
	 */
	public void computeTimeoutValue(long sampleRTT) {
		float x = 0.25f;
		float y = x / 2;
		this.RTTVAR = (long) (((1 - x) * this.RTTVAR) + (x * Math.abs(this.SRTT
				- sampleRTT)));
		this.SRTT = (long) ((1 - y) * this.SRTT + (y * sampleRTT));
		this.timeoutValue = this.SRTT + Math.max(this.measurementPrecision, 4 * this.RTTVAR);
	}

	/**
	 * erzeugt das erste zu sendene Paket an den Server Return value: FCPacket
	 * with (0 destPath;windowSize;errorRate)
	 */
	public FCpacket makeControlPacket() {
		/*
		 * Create first packet with seq num 0. Return value: FCPacket with (0
		 * destPath ; windowSize ; errorRate)
		 */
		String sendString = destPath + ";" + windowSize + ";" + serverErrorRate;
		byte[] sendData = null;
		try {
			sendData = sendString.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return new FCpacket(0, sendData, sendData.length);
	}

	public void testOut(String out) {
		if (TEST_OUTPUT_MODE) {
			System.err.printf("%,d %s: %s\n", System.nanoTime(), Thread
					.currentThread().getName(), out);
		}
	}

	public static void main(String argv[]) throws Exception {
		FileCopyClient myClient = new FileCopyClient(argv[0], argv[1], argv[2],
				argv[3], argv[4]);
		myClient.runFileCopyClient();
	}
	
	private void writeLogFile(){
		PrintWriter writer;
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter("logfile.txt", true)));
			writer.println("WINDOWSIZE/ERRORRATE: " + this.windowSize + "/" + this.serverErrorRate);
			writer.println("TIME IN SEC: " + this.totalTransmissionTime / Math.pow(10, 9));
			writer.println("TIMERINTERRUPTS: " + this.totalTimerInterrupts);
			writer.println("RECEIVEDACKS: " + this.totalReceivedAcks);
			writer.println("MIDRTT FOR GOOD ACKS IN MS: " + this.midRTTForAllGoodAcks / Math.pow(10, 6));
			writer.println("MIDRTT FOR ALL ACKS IN MS: " + this.midRTTForAllAcks / Math.pow(10, 6));
			writer.print("***************************\n");
			
			writer.close();
		} catch (IOException e) {
			System.out.println("can't write logfile");
			e.printStackTrace();
		}
	}
	
	private void writeCsvFile(){
		PrintWriter writer;
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter("logfile.csv", true)));
//			writer.println("WINDOWSIZE;ERRORRATE;TIME IN SEC;TIMERINTERRUPTS;RECEIVEDACKS;MIDRTT FOR GOOD ACKS IN MS;MIDRTT FOR ALL ACKS IN MS");
			writer.print(this.windowSize);
			writer.print(";" + this.serverErrorRate);
			writer.print(";" + this.totalTransmissionTime / Math.pow(10, 9));
			writer.print(";" + this.totalTimerInterrupts);
			writer.print(";" + this.totalReceivedAcks);
			writer.print(";" + this.midRTTForAllGoodAcks / Math.pow(10, 6));
			writer.println(";" + this.midRTTForAllAcks / Math.pow(10, 6));
		
			
			writer.close();
		} catch (IOException e) {
			System.out.println("can't write logfile");
			e.printStackTrace();
		}
	}

	/**
	 * Empfangen der Quittungen als eigenen Thread implementiert
	 * 
	 * @author Francis Opoku und Fabian Reiber
	 * 
	 */
	private class ReceiveAckClient extends Thread {

		private boolean isInterrupted = false;

		public void run() {
			byte[] buf = new byte[UDP_PACKET_SIZE];
			DatagramPacket udpPacket = new DatagramPacket(buf, UDP_PACKET_SIZE);
			FCpacket receivedFCpacket = null;

			while (!this.isInterrupted) {
				try {
					client.receive(udpPacket);
					receivedFCpacket = new FCpacket(udpPacket.getData(),
							udpPacket.getLength());
					testOut("received ack of packet: "
							+ receivedFCpacket.getSeqNum());

					if (receivedFCpacket != null) {
						try {
							accessBuffer.acquire();
							int indexOfContainedPacket = sendBuf
									.indexOf(receivedFCpacket);
							if (indexOfContainedPacket != -1) {

								ackReceivedPacket(sendBuf
										.get(indexOfContainedPacket));
								
								if (sendbase == receivedFCpacket.getSeqNum()) {
									int counter = moveSendbase();
									if (!eofReached) {
										storePacketsInSendBuffer(counter);
									}
								}
							}
							accessBuffer.release();
						} catch (InterruptedException e) {
							System.out.println("couldn't acquire semaphore");
							e.printStackTrace();
						}
					}
				} catch (IOException e) {
					System.out.println("receive client was interrupted");
					this.isInterrupted = true;
					// e.printStackTrace();
				}
			}
		}

		/**
		 * Setzt das ACK-Flag auf true und bricht den Timer ab
		 * 
		 * @param receivedPacket
		 *            vom Server empfangene Paket
		 */
		private void ackReceivedPacket(FCpacket receivedPacket) {
			receivedPacket.setValidACK(true);

			//************ONE MORE RECEIVED ACK************
			totalReceivedAcks++;
			
			cancelTimer(receivedPacket);
		}

		/**
		 * setzt die Sendbase so lange neu, bis ein Paket kommt, welches noch
		 * nicht "geacked" wurde. realisiert somit das kumulative ACK
		 * 
		 * @return Anzahl der Pakete die neu in den Sendepuffer gelegt werden
		 *         koennen
		 */
		private int moveSendbase() {
			int counter = 0;
			do {
				sendbase++;
				counter++;
				if (sendbase == sendBuf.size()) {
					break;
				}
			} while (sendBuf.get(sendbase).isValidACK());
			/*
			 * System.out.println("before notify"); synchronized (this.f) {
			 * f.notify(); } System.out.println("after notify");
			 */
			return counter;
		}
	}

}
