package proxyserver;

import proxyserver.exceptions.InvalidNumberException;

/**
 * Die Klasse Command bietet einen einheitlichen Zugang zu den Operationen an,
 * die mit Kommandos durchgefuehrt werden sollten.
 * Dazu zaehlen:
 * Pruefung auf gueltiges Kommando bei vorhandensein einer Nachricht vom Client
 * Zugang zu den laut Aufgabenstellung zu implementierenden Kommandos unter der Garantie, dass
 * der RFC 1939 Standard eingehalten wird.
 * @author Francis
 *
 */
public class Command {

	private final String USER = "USER";
	private final String PASS = "PASS";
	private final String CAPA = "CAPA";
	private final String AUTH = "AUTH";
	private final String STAT = "STAT";
	private final String LIST = "LIST";
	private final String RETR = "RETR";
	private final String DELE = "DELE";
	private final String NOOP = "NOOP";
	private final String RSET = "RSET";
	private final String UIDL = "UIDL";
	private final String QUIT = "QUIT";
	
	public Command(){
		
	}
	
	/**
	 * Liefert String "USER  name" nach  RFC 1939 Standard zurueck.
	 * @param name Benutzername
	 * @return "USER " + name
	 */
	public String user(String name){
		return USER + " " + name;
	}
	
	/**
	 * Liefert String "PASS  pass" nach  RFC 1939 Standard zurueck.
	 * @param pass Passwort
	 * @return "PASS " + pass
	 */
	public String pass(String pass){
		return PASS + " " + pass;
	}
	
	/**
	 * Liefert String "CAPA" nach  RFC 5034 Standard zurueck.
	 * @return "CAPA"
	 */
	public String capa(){
		return CAPA;
	}
	/**
	 * Liefert String "AUTH" nach  RFC 5034 Standard zurueck.
	 * @return "AUTH"
	 */
	public String auth(){
		return AUTH;
	}
	
	/**
	 * Liefert String "STAT" nach  RFC 1939 Standard zurueck.
	 * @return "STAT"
	 */
	public String stat(){
		return STAT;
	}
	
	/**
	 * Liefert String "LIST" nach  RFC 1939 Standard zurueck.
	 * @return "LIST"
	 */
	public String list(){
		return LIST;
	}
	
	/**
	 * Liefert String "LIST  n" nach  RFC 1939 Standard zurueck, mit n = Zahl.
	 * @param n Positive Zahl.
	 * @return "LIST " + n
	 * @throws InvalidNumberException wenn n < 0
	 */
	public String list(int n) throws InvalidNumberException{
		if(n < 0){
			throw new InvalidNumberException("n muss groe�er gleich 0 sein.");
		}
		
		return LIST + " " + n;
	}
	
	/**
	 * Liefert String "RETR  n" nach  RFC 1939 Standard zurueck.
	 * @param n Positive Zahl
	 * @return "RETR " + n
	 * @throws InvalidNumberException 
	 */
	public String retr(int n) throws InvalidNumberException{
		if(n < 0){
			throw new InvalidNumberException("n muss groe�er gleich 0 sein.");
		}
		
		return RETR + " " + n;
	}
	
	/**
	 * Liefert String "DELE n" nach  RFC 1939 Standard zurueck.
	 * @param n Positive Zahl
	 * @return "DELE " + n
	 * @throws InvalidNumberException 
	 */
	public String dele(int n) throws InvalidNumberException{
		if(n < 0){
			throw new InvalidNumberException("n muss groe�er gleich 0 sein.");
		}
		return DELE + " " + n;
	}
	
	/**
	 * Liefert String "NOOP" nach  RFC 1939 Standard zurueck.
	 * @return "NOOP"
	 */
	public String noop(){
		return NOOP;
	}
	
	/**
	 * Liefert String "RSET" nach  RFC 1939 Standard zurueck.
	 * @return "RSET"
	 */
	public String rset(){
		return RSET;
	}
	
	/**
	 * Liefert String "UIDL n" nach  RFC 1939 Standard zurueck.
	 * @param n Positive Zahl
	 * @return "UIDL " + n
	 * @throws InvalidNumberException 
	 */
	public String uidl(int n) throws InvalidNumberException{
		if(n < 0){
			throw new InvalidNumberException("n muss groe�er gleich 0 sein.");
		}
		return UIDL + " " + n;
	}
	
	/**
	 * Liefert String "QUIT" nach  RFC 1939 Standard zurueck.
	 * @return "QUIT"
	 */
	public String quit(){
		return QUIT;
	}
	
	/**
	 * Liefert true zurueck, wenn die msg - die empfangene Nachricht -
	 * den RFC 1939 Standard erfuellt, also mit einem Kommando beginnt
	 * und nur gueltige Parameter folgen sofern welche anzugeben sind.
	 * @param msg Die empfangene Nachricht
	 * @return true, wenn msg mit gueltigem Kommando beginnt und nur 
	 * gueltige Parameter folgen, sonst false.
	 */
	public boolean isValid(String msg){
		String[] command = msg.split(" ", 2);
		switch(command[0].toUpperCase()) {
			case USER : return isValidUSER(command);
			case PASS : return isValidPASS(command);
			case CAPA : return isValidCAPA(command);
			case AUTH : return isValidAUTH(command);
			case STAT : return isValidSTAT(command);
			case LIST : return isValidLIST(command);
			case RETR : return isValidRETR(command);
			case DELE : return isValidDELE(command);
			case NOOP : return isValidNOOP(command);
			case RSET : return isValidRSET(command);
			case UIDL : return isValidUIDL(command);
			case QUIT : return isValidQUIT(command);
		}
		
		return false;
	}
	
	/**
	 * Prueft auf Einhaltung der USER-Kommando-Signatur gemae� RFC 1939 Standard.
	 * @param command gesplittete msg.
	 * @return true wenn Benutzername angegeben sonst false.
	 */
	private boolean isValidUSER(String[] command){
		if(command.length == 1){
			return false;
		}
		
		return isValidStringParameter(command);
	}
	
	/**
	 * Prueft auf Einhaltung der PASS-Kommando-Signatur gemae� RFC 1939 Standard.
	 * @param command gesplittete msg.
	 * @return true wenn PASS angegeben sonst false.
	 */
	private boolean isValidPASS(String[] command){
		if(command.length == 1){
			return false;
		}
		
		return isValidStringParameter(command);
	}
	
	/**
	 * Prueft auf Einhaltung der CAPA-Kommando-Signatur gemae� RFC 5034 Standard.
	 * @param command gesplittete msg.
	 * @return true wenn CAPA ohne Parameter sonst false.
	 */
	private boolean isValidCAPA(String[] command){
		if(command.length == 1){
			return true;
		}
		
		return false;
	}
	
	/**
	 * Prueft auf Einhaltung der AUTH-Kommando-Signatur gemae� RFC 5034 Standard.
	 * @param command gesplittete msg.
	 * @return true wenn AUTH ohne Parameter sonst false.
	 */
	private boolean isValidAUTH(String[] command){
		if(command.length == 1){
			return true;
		}
		
		return false;
	}
	
	/**
	 * Prueft auf Einhaltung der STAT-Kommando-Signatur gemae� RFC 1939 Standard.
	 * @param command gesplittete msg.
	 * @return true wenn STAT ohne Parameter sonst false.
	 */
	private boolean isValidSTAT(String[] command){
		if(command.length == 1){
			return true;
		}
		
		return false;
	}
	
	/**
	 * Prueft auf Einhaltung der LIST-Kommando-Signatur gemae� RFC 1939 Standard.
	 * @param command gesplittete msg.
	 * @return true wenn LIST ohne Parameter ODER Parameter = int >= 0, sonst false;
	 */
	private boolean isValidLIST(String[] command){
		if(command.length == 1){
			return true;
		}
		
		return isValidIntegerParameter(command);
	}
	
	/**
	 * Prueft auf Einhaltung der RETR-Kommando-Signatur gemae� RFC 1939 Standard.
	 * @param command gesplittete msg.
	 * @return true wenn RETR mit Parameter = int >= 0, sonst false;
	 */
	private boolean isValidRETR(String[] command){
		if(command.length == 1){
			return false;
		}
		
		return isValidIntegerParameter(command);
	}
	
	/**
	 * Prueft auf Einhaltung der DELE-Kommando-Signatur gemae� RFC 1939 Standard.
	 * @param command gesplittete msg.
	 * @return true wenn DELE mit Parameter = int >= 0, sonst false;
	 */
	private boolean isValidDELE(String[] command){
		if(command.length == 1){
			return false;
		}
		return isValidIntegerParameter(command);
	}
	
	/**
	 * Prueft auf Einhaltung der NOOP-Kommando-Signatur gemae� RFC 1939 Standard.
	 * @param command gesplittete msg.
	 * @return true wenn NOOP ohne Parameter, sonst false
	 */
	private boolean isValidNOOP(String[] command){
		if(command.length == 1){
			return true;
		}
		return false;
	}
	
	/**
	 * Prueft auf Einhaltung der RSET-Kommando-Signatur gemae� RFC 1939 Standard.
	 * @param command gesplittete msg.
	 * @return true wenn RSET ohne Parameter, sonst false.
	 */
	private boolean isValidRSET(String[] command){
		if(command.length == 1){
			return true;
		}
		return false;
	}
	
	/**
	 * Prueft auf Einhaltung der UIDL-Kommando-Signatur gemae� RFC 1939 Standard.
	 * @param command gesplittete msg.
	 * @return true wenn UIDL ohne Parameter ODER Parameter = int >= 0, sonst false;
	 */
	private boolean isValidUIDL(String[] command){
		if(command.length == 1){
			return true;
		}
		
		return isValidIntegerParameter(command);
	}
	
	/**
	 * Prueft auf Einhaltung der QUIT-Kommando-Signatur gemae� RFC 1939 Standard.
	 * @param command gesplittete msg.
	 * @return true wenn QUIT ohne Parameter, sonst false
	 */
	private boolean isValidQUIT(String[] command){
		if(command.length == 1){
			return true;
		}
		
		return false;
	}
	
	/**
	 * Prueft ob der String der dem Kommando der msg folgte nur 1 Parameter enthaelt. 
	 * @param command Die msg
	 * @return true wenn dem Kommando nur 1 Parameter folgte, sonst false
	 */
	private boolean isValidStringParameter(String[] command){
			//Befehle mit anschließendem "leeren" Parameter nicht akzeptieren
			if(command[1].matches("^\\s*$")){
				return false;
			}
			String[] commandtmp = command[1].split(" ");
			
			//Wenn Kommando mit mehr als 1 Parameter gesendet wurde return false;
			if(commandtmp.length > 1){
				return false;
			}
				
			return true;
	}
	
	/**
	 * Prueft ob der erwartete Integer-Parameter, der dem Kommando folgte,
	 * ein Integer-Parameter ist und ob dieser groe�er als -1 ist.
	 * @param command Die msg
	 * @return true, wenn der Parameter der dem Kommando folgte ein int >= 0 ist, sonst false.
	 */
	private boolean isValidIntegerParameter(String[] command){
		String[] commandtmp = command[1].split(" ");
		
		if(commandtmp.length > 1){
			return false;
		}
		
		if(commandtmp[0].matches("[1-9][0-9]*")){
			int value = Integer.parseInt(commandtmp[0]);
			
			if(value < 0){
				return false;
			}
			
			return true;
			
		}else {
			return false;
		}
	}
	
}
