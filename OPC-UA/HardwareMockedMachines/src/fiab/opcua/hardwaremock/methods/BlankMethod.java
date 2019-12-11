package fiab.opcua.hardwaremock.methods;

public class BlankMethod extends Methods {

	@Override
	public void invoke() {
		System.out.println("Blank Method invoked!");
	}

}
