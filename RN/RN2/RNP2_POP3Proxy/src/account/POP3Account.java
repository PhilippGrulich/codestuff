package account;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Repraesentiert die zu konfigurierenden POP3-Konten
 * @author Fabian Reiber und Francis Opoku
 *
 */
public class POP3Account {

	private String user;
	private String pass;
	private String serveraddress;
	private int port;
	//kein Semaphore fuer address, da nur der Server diese aendern kann
	private InetAddress address;
	private Collectionaccount cAccount;
	private Semaphore connectionSem;
	
	/**
	 * Konstruktor
	 * @param user username
	 * @param pass passwort
	 * @param serveraddress Serveradresse des POP3 Mailaccounts
	 * @param port POP3-Port
	 */
	public POP3Account(String user, String pass, String serveraddress, int port){
		this.user = user;
		this.pass = pass;
		this.serveraddress = serveraddress;
		this.port = port;
		try {
			address = InetAddress.getByName(serveraddress);
		} catch (UnknownHostException e) {
			System.out.println("unable to create inetadress");
			e.printStackTrace();
		}
		this.cAccount = new Collectionaccount();
		this.connectionSem = new Semaphore(1);
	}
	
	/**
	 * Copy-Konstruktor
	 * @param a von a werden alle Eigenschaften uebernommen
	 */
	public POP3Account(POP3Account a){
		this.user = a.getUser();
		this.pass = a.getPass();
		this.serveraddress = a.getServeraddress();
		this.port = a.getPort();
		this.address = a.getAddress();
		this.cAccount = a.getCollectionAccount();
		this.connectionSem = a.getSemaphoren();
	}
	
	/**
	 * Fuegt eine Liste von Emails zu dem bestehenden "Abhol-Accounts" hinzu
	 * @param mailList jeweilige Liste der E-Mails des POP3 Accounts
	 */
	public void addMails(List<Email> mailList){
		try {
			this.connectionSem.acquire();
			this.cAccount.addMails(mailList);
		} catch (InterruptedException e) {
			System.out.println("not possible to acquire sem");
			e.printStackTrace();
		}
		this.connectionSem.release();
	}
	

	/**
	 * Gibt die Liste der Emails des jeweiligen Kontos zurueck die im "Abhol-Account"
	 * liegen
	 * @return jeweilige Liste der E-Mails des POP3 Accounts
	 */
	public List<Email> getMails(){
		List<Email> copyAcc = null;
		
		try {
			this.connectionSem.acquire();
			copyAcc = new ArrayList<Email>();
			copyAcc.addAll(this.cAccount.getMailList());
		} catch (InterruptedException e) {
			System.out.println("not possible to acquire sem");
			e.printStackTrace();
		}
		this.connectionSem.release();	
		return copyAcc;
	}
	
	/**
	 * Loescht Liste von Emails aus dem "Abhol-Account"
	 * @param index Email mit angegebenen Listenindex loeschen
	 */
	public void removeMailList(List<Email> l){
		try {
			this.connectionSem.acquire();
			this.cAccount.removeList(l);
		} catch (InterruptedException e) {
			System.out.println("not possible to acquire sem");
			e.printStackTrace();
		}
		
		this.connectionSem.release();
	}
	
	
	public InetAddress getAddress() {
		return address;
	}

	public void setAddress(String serveraddress) {
		try {
			this.address = InetAddress.getByName(serveraddress);
		} catch (UnknownHostException e) {
			System.out.println("unable to create inetadress");
			e.printStackTrace();
		}
	}

/**************************************************************************/
/************************Getter / Setter***********************************/
/**************************************************************************/
	public int getPort(){
		return port;
	}
	
	public String getServeraddress(){
		return serveraddress;
	}
	
	public String getUser(){
		return user;
	}
	
	public String getPass(){
		return pass;
	}
	
	public Collectionaccount getCollectionAccount(){
		return this.cAccount;
	}
	
	public Semaphore getSemaphoren(){
		return this.connectionSem;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((pass == null) ? 0 : pass.hashCode());
		result = prime * result + port;
		result = prime * result
				+ ((serveraddress == null) ? 0 : serveraddress.hashCode());
		result = prime * result + ((user == null) ? 0 : user.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		POP3Account other = (POP3Account) obj;
		if (pass == null) {
			if (other.pass != null)
				return false;
		} else if (!pass.equals(other.pass))
			return false;
		if (port != other.port)
			return false;
		if (serveraddress == null) {
			if (other.serveraddress != null)
				return false;
		} else if (!serveraddress.equals(other.serveraddress))
			return false;
		if (user == null) {
			if (other.user != null)
				return false;
		} else if (!user.equals(other.user))
			return false;
		return true;
	}		
}
