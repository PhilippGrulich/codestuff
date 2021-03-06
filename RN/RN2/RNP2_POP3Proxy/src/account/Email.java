package account;

/**
 * Zur besseren Verwaltung der Emails eine Klasse die sie repraesentiert
 * @author Francis Opoku und Fabian Reiber
 *
 */

public class Email {

	private String text;
	private int size;
	private boolean checked;
	private String uidl;
	
	public Email(String text, int size, String uidl){
		this.text = text;
		this.size = size;
		this.checked = false;
		this.uidl = uidl;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public boolean isChecked() {
		return checked;
	}

	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	public String getUidl() {
		return uidl;
	}

	public void setUidl(String uidl) {
		this.uidl = uidl;
	}
	
	
}
