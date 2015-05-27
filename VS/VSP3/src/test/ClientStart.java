package test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import accessor_one.SomeException110;
import accessor_one.SomeException112;
import mware_lib.CommunicationModule;
import mware_lib.NameService;
import mware_lib.ObjectBroker;

public class ClientStart extends Thread{

	public ClientStart() {
		// TODO Auto-generated constructor stub
	}
	
	public void run(){
		accessor_one_test();
		accessor_two_test();
	}
	
	public void accessor_one_test(){
	
		CommunicationModule.setCommunicatiomoduleport(50003);
	
		String host = null;
		try {
			host = InetAddress.getLocalHost().getCanonicalHostName();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		ObjectBroker objBroker = ObjectBroker.init(host, 50000, true);
	
		NameService nameSvc = objBroker.getNameService();
		String namec1 = "c1";
		String namec2 = "c2";
		Object rawObjRef1 = nameSvc.resolve(namec1);
		Object rawObjRef2 = nameSvc.resolve(namec2);
	
		accessor_one.ClassOneImplBase remoteObj1 = accessor_one.ClassOneImplBase.narrowCast(rawObjRef1);
		accessor_one.ClassTwoImplBase remoteObj2 = accessor_one.ClassTwoImplBase.narrowCast(rawObjRef2);
		
		/**Params c1**/
		String returnvalc1 = null;
		String c1param1 = "test";
		int c1param2 = 4;
		
		/**Params c2 methodOne**/
		double c2param1 = 2.0;
		int returnvalc2m1;
		
		/**Params c2 methodTwo**/
		double returnvalc2m2;
		/**Test ClassOneAO methodOne**/
		try {
			
			returnvalc1 = remoteObj1.methodOne(c1param1, c1param2);
			System.out.println("accessor_one.ClassOneImplBase (\"" + namec1 + "\")");
			System.out.println("methodOne");
			System.out.println("param1 = " + c1param1 + " param2 = " + c1param2);
			System.out.println("return value = " + returnvalc1);
		} catch (accessor_one.SomeException112 e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("accessor_one.ClassOneImplBase (\"" + namec1 + "\"");
			System.out.println("methodOne");
			System.out.println("param1 = \"" + c1param1 + "\" param2 = " + c1param2);
			System.out.println("accessor_one.SomeException112 with message \"" + e.getMessage() + "\"");
		}
		
		/**Test ClassTwoAO methodOne**/
		try {
			returnvalc2m1 = remoteObj2.methodOne(2.0);
			System.out.println("accessor_one.ClassTwoImplBase (\"" + namec2 + "\")");
			System.out.println("methodOne");
			System.out.println("param1 = " + c2param1);
			System.out.println("return value = " + returnvalc2m1);
		} catch (SomeException110 e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("accessor_one.ClassTwoImplBase (\"" + namec2 + "\"");
			System.out.println("methodOne");
			System.out.println("param1 = " + c2param1);
			System.out.println("accessor_one.SomeException110 with message \"" + e.getMessage() + "\"");
		}
		
		/**Test ClassTwoAO methodTwo**/
		try {
			returnvalc2m2 = remoteObj2.methodTwo();
			System.out.println("accessor_one.ClassTwoImplBase (\"" + namec2 + "\")");
			System.out.println("methodTwo");
			System.out.println("no params");
			System.out.println("return value = " + returnvalc2m2);
		} catch (SomeException112 e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("accessor_one.ClassTwoImplBase (\"" + namec2 + "\"");
			System.out.println("methodTwo");
			System.out.println("no params");
			System.out.println("accessor_one.SomeException112 with message \"" + e.getMessage() + "\"");

		}

		objBroker.shutDown();
	
	}
	
	public void accessor_two_test(){
		CommunicationModule.setCommunicatiomoduleport(50003);
		
		String host = null;
		try {
			host = InetAddress.getLocalHost().getCanonicalHostName();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		ObjectBroker objBroker = ObjectBroker.init(host, 50000, true);
	
		NameService nameSvc = objBroker.getNameService();
		String namec3 = "c3";
		Object rawObjRef3 = nameSvc.resolve(namec3);
	
		accessor_two.ClassOneImplBase remoteObj3 = accessor_two.ClassOneImplBase.narrowCast(rawObjRef3);
		
		/**Params c3**/
		double returnvalc3;
		
		/**Params c3 methodOne**/
		String c3param1m1_1 = "cute sloth";
		double c3param2m1_1 = 3.2;
		String c3param1m1_2 = null;
		double c3param2m1_2= 3.2;
		String c3param1m1_3 = "cute sloth";
		double c3param2m1_3= 1.9;
		
		/**Params c3 methodTwo**/
		String c3param1m2_1 = "the monkey with shoes";
		double c3param2m2_1 = 1.9;
		String c3param1m2_2 = null;
		double c3param2m2_2 = 1.9;
		String c3param1m2_3 = "the monkey with shoes";
		double c3param2m2_3 = 2.3;
		String c3param1m2_4 = "the monkey without shoes";
		double c3param2m2_4 = 1.9;
		
		/**Test ClassOneAT methodOne**/
		try {
			/**1.Test: normale-Test**/
			returnvalc3 = remoteObj3.methodOne(c3param1m1_1, c3param2m1_1);
			System.out.println("accessor_two.ClassOneImplBase (\"" + namec3 + "\")");
			System.out.println("methodOne");
			System.out.println("param1 = " + c3param1m1_1 + " param2 = " + c3param2m1_1);
			System.out.println("return value = " + returnvalc3);
			
			/**2.Test: null-Test**/
			returnvalc3 = remoteObj3.methodOne(c3param1m1_2, c3param2m1_2);
			System.out.println("accessor_two.ClassOneImplBase (\"" + namec3 + "\")");
			System.out.println("methodOne");
			System.out.println("param1 = " + c3param1m1_2 + " param2 = " + c3param2m1_2);
			System.out.println("return value = " + returnvalc3);
			
			/**3.Test: Exception112-Test**/
			returnvalc3 = remoteObj3.methodOne(c3param1m1_3, c3param2m1_3);
			System.out.println("accessor_two.ClassOneImplBase (\"" + namec3 + "\")");
			System.out.println("methodOne");
			System.out.println("param1 = " + c3param1m1_3 + " param2 = " + c3param2m1_3);
			System.out.println("return value = " + returnvalc3);
		} catch (accessor_two.SomeException112 e) {
			//e.printStackTrace();
			System.out.println("accessor_one.ClassOneImplBase (\"" + namec3 + "\"");
			System.out.println("methodOne");
			System.out.println("param1 = \"" + c3param1m1_3 + "\" param2 = " + c3param2m1_3);
			System.out.println("accessor_one.SomeException112 with message \"" + e.getMessage() + "\"");
		}
		
		/**Test ClassOneAT methodTwo**/
		try {
			/**1.Test: Normal-Test**/
			returnvalc3 = remoteObj3.methodTwo(c3param1m2_1, c3param2m2_1);
			System.out.println("accessor_two.ClassOneImplBase (\"" + namec3 + "\")");
			System.out.println("methodOne");
			System.out.println("param1 = " + c3param1m2_1 + " param2 = " + c3param2m2_1);
			System.out.println("return value = " + returnvalc3);
			
			/**2.Test: null-Test**/
			returnvalc3 = remoteObj3.methodTwo(c3param1m2_2, c3param2m2_2);
			System.out.println("accessor_two.ClassOneImplBase (\"" + namec3 + "\")");
			System.out.println("methodOne");
			System.out.println("param1 = " + c3param1m2_2 + " param2 = " + c3param2m2_2);
			System.out.println("return value = " + returnvalc3);
			
			/**3.Test: Exception112-Test**/
			returnvalc3 = remoteObj3.methodTwo(c3param1m2_3, c3param2m2_3);
			System.out.println("accessor_two.ClassOneImplBase (\"" + namec3 + "\")");
			System.out.println("methodOne");
			System.out.println("param1 = " + c3param1m2_3 + " param2 = " + c3param2m2_3);
			System.out.println("return value = " + returnvalc3);
			
		} catch (accessor_two.SomeException112 e) {
			//e.printStackTrace();
			System.out.println("accessor_two.ClassOneImplBase (\"" + namec3 + "\"");
			System.out.println("methodOne");
			System.out.println("param1 = \"" + c3param1m2_3 + "\" param2 = " + c3param2m2_3);
			System.out.println("accessor_two.SomeException112 with message \"" + e.getMessage() + "\"");
		} catch (accessor_two.SomeException304 e) {
			//e.printStackTrace();
			System.out.println("accessor_two.ClassOneImplBase (\"" + namec3 + "\"");
			System.out.println("methodOne");
			System.out.println("param1 = \"" + c3param1m2_4 + "\" param2 = " + c3param2m2_4);
			System.out.println("accessor_two.SomeException304 with message \"" + e.getMessage() + "\"");
		}
		
		try{
			/**4.Test: Exception304-Test**/
			returnvalc3 = remoteObj3.methodTwo(c3param1m2_4, c3param2m2_4);
			System.out.println("accessor_two.ClassOneImplBase (\"" + namec3 + "\")");
			System.out.println("methodOne");
			System.out.println("param1 = " + c3param1m2_4 + " param2 = " + c3param2m2_4);
			System.out.println("return value = " + returnvalc3);
		} catch (accessor_two.SomeException112 e) {
			//e.printStackTrace();
			System.out.println("accessor_two.ClassOneImplBase (\"" + namec3 + "\"");
			System.out.println("methodOne");
			System.out.println("param1 = \"" + c3param1m2_3 + "\" param2 = " + c3param2m2_3);
			System.out.println("accessor_two.SomeException112 with message \"" + e.getMessage() + "\"");
		} catch (accessor_two.SomeException304 e) {
			//e.printStackTrace();
			System.out.println("accessor_two.ClassOneImplBase (\"" + namec3 + "\"");
			System.out.println("methodOne");
			System.out.println("param1 = \"" + c3param1m2_4 + "\" param2 = " + c3param2m2_4);
			System.out.println("accessor_two.SomeException304 with message \"" + e.getMessage() + "\"");
		}
		

		objBroker.shutDown();
		
	}
}