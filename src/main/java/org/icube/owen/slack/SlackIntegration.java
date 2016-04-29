package org.icube.owen.slack;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.icube.owen.TheBorg;
import org.json.JSONObject;

public class SlackIntegration extends TheBorg {

	/**
	 * Send a message to Slack 
	 * @param companyId - company ID for db connection
	 * @param message - actual message to be sent
	 */
	public void sendMessage(int companyId, String message) {

		try {
			JSONObject obj = new JSONObject();
			obj.put("text", message);
			sendPost(companyId, obj);
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(SlackIntegration.class).error("Exception while sending a message via Slack", e);
		}
	}

	/**
	 * The HTTP POST request to send the message to slack using a JSONObject
	 * @param companyId - company ID for db connection
	 * @param jsonObj - message to be sent in a JSONObject format
	 * 
	 */
	private void sendPost(int companyId, JSONObject jsonObj) {
		try {
			//TODO hpatel :  get this URL from the database
			String url = "https://hooks.slack.com/services/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"; // i-cube general channel
			URL obj = new URL(url);
			HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

			// add request header
			con.setRequestMethod("POST");
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

			String urlParameters = "payload=" + jsonObj.toString();

			// Send post request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();

			int responseCode = con.getResponseCode();
			org.apache.log4j.Logger.getLogger(SlackIntegration.class).debug("\nSending 'POST' request to URL : " + url);
			org.apache.log4j.Logger.getLogger(SlackIntegration.class).debug("Post parameters : " + urlParameters);
			org.apache.log4j.Logger.getLogger(SlackIntegration.class).debug("Response Code : " + responseCode);

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			// print result
			org.apache.log4j.Logger.getLogger(SlackIntegration.class).debug(response.toString());
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(SlackIntegration.class).error("Exception while sending a message through HTTP Post to Slack", e);

		}
	}
}
