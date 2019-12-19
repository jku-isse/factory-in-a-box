package fiab.mes.mockactors.transport;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;

class TestTransportPositionLookup {

	@Test
	void test() throws UnknownHostException {
		String host = "192.168.0.168";
		
		InetAddress inetAddr = InetAddress.getByName(host);
		System.out.println((inetAddr.getAddress()[0]+256)%256);
		System.out.println((inetAddr.getAddress()[1]+256)%256);
		System.out.println((inetAddr.getAddress()[2]+256)%256);
		System.out.println((inetAddr.getAddress()[3]+256)%256);
	}

}
