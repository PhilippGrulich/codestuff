package account;

import java.util.ArrayList;
import java.util.List;

/**
 * Repraesentiert den Abholaccount fuer jedes konfigurierte Konto
 * Fuehrt eine Liste aller Mails zum zugehoerigen Account
 * @author Fabian Reiber und Francis Opoku
 *
 */
public class Collectionaccount {
	
	private List<Email> mailList;
	
	public Collectionaccount(){
		this.mailList = new ArrayList<Email>();
	}

	public void addMails(List<Email> mailList){
		this.mailList.addAll(mailList);
	}
	
	public void removeList(List<Email> mailList){
		this.mailList.removeAll(mailList);
	}
	
	public List<Email> getMailList() {
		return this.mailList;
	}
}
