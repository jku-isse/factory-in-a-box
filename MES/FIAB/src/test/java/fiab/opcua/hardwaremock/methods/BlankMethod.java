package fiab.opcua.hardwaremock.methods;

public class BlankMethod extends Methods {

	@Override
	public void invoke() {
		System.out.println("Blank Method invoked!");
	}
	
	@Override
	public String getInfo() {
		return "This is a blank method - nothing happens!";
	}

}
