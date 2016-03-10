package org.icube.owen.test.survey;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.survey.Batch;
import org.icube.owen.survey.BatchList;
import org.icube.owen.survey.Frequency;
import org.junit.Test;

public class BatchListTest {
	BatchList bl = (BatchList) ObjectFactory.getInstance("org.icube.owen.survey.BatchList");

	@Test
	public void testGetFrequencyLabelMap() {
		Map<Integer, String> getFrequencyLabelMap = bl.getFrequencyLabelMap();
		assertTrue(!getFrequencyLabelMap.isEmpty());
	}

	@Test
	public void testGetBatchList() {
		List<Batch> b = bl.getBatchList();
		assertTrue(!b.isEmpty());
	}

	@Test
	public void testChangeFrequency() {
		assertTrue(bl.changeFrequency(bl.getBatchList().get(0), Frequency.QUARTERLY));
	}

}
