package org.icube.owen.test.initiative;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.initiative.InitiativeHelper;
import org.junit.Test;

public class InitiativeHelperTest {
	InitiativeHelper ih = (InitiativeHelper) ObjectFactory.getInstance("org.icube.owen.initiative.InitiativeHelper");
	int companyId = 1;

	@Test
	public void testGetInitiativeCount() {
		List<Map<String, Object>> initiativeCountMapList = new ArrayList<>();
		initiativeCountMapList = ih.getInitiativeCount(companyId);
		assertNotNull(initiativeCountMapList);
	}

}
