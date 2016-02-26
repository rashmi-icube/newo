package org.icube.owen.test.individual;

import org.icube.owen.ObjectFactory;
import org.icube.owen.individual.Login;
import org.junit.Test;

public class LoginTest {
	Login l = (Login) ObjectFactory.getInstance("org.icube.owen.individual.Login");
    
	@Test
	public void testLogin(){
		try {
			l.login("emp2@i-cube.in", "abc123");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}
