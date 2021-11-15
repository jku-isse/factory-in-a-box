package fiab.mes.shopfloor;

import org.junit.jupiter.api.Test;

import fiab.mes.ShopfloorConfigurations.JsonFilePersistedDiscovery;

class TestShopfloorDiscoveryFromFile {

	@Test
	void test() {
		JsonFilePersistedDiscovery jfpd = new JsonFilePersistedDiscovery("TestShopfloorParticipantAddresses");
		assert(jfpd.endpoints.size() == 2);
	}

}
