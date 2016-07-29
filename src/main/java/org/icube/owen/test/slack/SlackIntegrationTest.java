package org.icube.owen.test.slack;

import org.icube.owen.ObjectFactory;
import org.icube.owen.slack.SlackIntegration;
import org.junit.Test;

public class SlackIntegrationTest {
	SlackIntegration si = (SlackIntegration) ObjectFactory.getInstance("org.icube.owen.slack.SlackIntegration");
    int companyId = 1;
    
    @Test
    public void testSendMessage(){
    	si.sendMessage(companyId,"You have new questions to answer\nPlease login to answer\n<http://engage.owenanalytics.com|engage.owenanalytics.com>");
    }
}
