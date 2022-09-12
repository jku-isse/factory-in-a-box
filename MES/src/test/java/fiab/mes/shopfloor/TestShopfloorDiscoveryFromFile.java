package fiab.mes.shopfloor;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import fiab.mes.ShopfloorConfigurations.JsonFilePersistedDiscovery;

@Tag("UnitTest")
class TestShopfloorDiscoveryFromFile {

	@Test
	void test() {
		JsonFilePersistedDiscovery jfpd = new JsonFilePersistedDiscovery("TestShopfloorParticipantAddresses");
		assert(jfpd.endpoints.size() == 2);
	}

}
