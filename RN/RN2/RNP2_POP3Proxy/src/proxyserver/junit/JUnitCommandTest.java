package proxyserver.junit;

import static org.junit.Assert.*;

import org.junit.Test;

import proxyserver.Command;
import proxyserver.exceptions.InvalidNumberException;

public class JUnitCommandTest {

	private Command c = new Command();
	
	@Test
	public void checkIfCommandsAreValid(){

		assertTrue(c.isValid("user test"));
		assertTrue(c.isValid("paSs bla"));
		assertTrue(c.isValid("staT"));
		assertTrue(c.isValid("LiST"));
		assertTrue(c.isValid("LIST 1"));
		assertTrue(c.isValid("RetR 3"));
		assertTrue(c.isValid("DELE 3"));
		assertTrue(c.isValid("NOop"));
		assertTrue(c.isValid("RsEt"));
		assertTrue(c.isValid("UiDl"));
		assertTrue(c.isValid("UiDl 2"));
		assertTrue(c.isValid("QUIT"));

		assertTrue(!c.isValid("User"));
		//BUG: Leerzeichen ist nach "User" erlaubt
		//-> Fixed in isValidStringParameter(String[] command)
		assertTrue(!c.isValid("User "));
		assertTrue(!c.isValid("paSs"));
		//BUG: Leerzeichen ist nach "Pass" erlaubt
		//-> Fixed in isValidStringParameter(String[] command)
		assertTrue(!c.isValid("paSs "));
		assertTrue(!c.isValid("staT "));
		assertTrue(!c.isValid("staT foo"));
		assertTrue(!c.isValid("LiST f"));
		//leerzeichenbehandlung?!
		assertTrue(!c.isValid("LIST "));
		assertTrue(!c.isValid("RetR"));
		//BUG: Leerzeichen wird bei "Retr" als valider Parameter angenommen
		//-> Fixed in isValidStringParameter(String[] command)
		assertTrue(!c.isValid("RetR "));
		assertTrue(!c.isValid("DELE"));
		//BUG: Leerzeichen wird bei "Dele" als valider Parameter angenommen
		//-> Fixed in isValidStringParameter(String[] command)
		assertTrue(!c.isValid("DELE "));
		assertTrue(!c.isValid("NOop f"));
		//leerzeichenbehandlung?!
		assertTrue(!c.isValid("NOop "));
		assertTrue(!c.isValid("RsEt f"));
		//leerzeichenbehandlung?!
		assertTrue(!c.isValid("RsEt "));
		assertTrue(!c.isValid("UiDl f"));
		//leerzeichenbehandlung?!
		assertTrue(!c.isValid("UiDl "));
		assertTrue(!c.isValid("QUIT f"));
		//leerzeichenbehandlung?!
		assertTrue(!c.isValid("QUIT "));
	}
	
	@Test (expected = InvalidNumberException.class)
	public void checkInvalidNumberException() throws InvalidNumberException{
		assertEquals("", c.list(-2));
		assertEquals("", c.retr(-2));
		assertEquals("", c.dele(-2));
		assertEquals("", c.uidl(-2));
	}
	
	@Test
	public void checkConcatinationOfListRetrDeleUidl(){

		try {
			assertEquals("LIST 2", c.list(2));
			assertEquals("RETR 2", c.retr(2));
			assertEquals("DELE 2", c.dele(2));
			assertEquals("UIDL 2", c.uidl(2));

			assertNotEquals("LIST 1", c.list(2));
			assertNotEquals("RETR 1", c.retr(2));
			assertNotEquals("DELE 1", c.dele(2));
			assertNotEquals("UIDL 1", c.uidl(2));
		} catch (InvalidNumberException e) {
			e.printStackTrace();
		}
	}

}
