package org.icube.owen.explore;

import java.util.Map;

import org.icube.owen.survey.Question;

public class MeResponseAnalysis {
	private Question question;
	private Map<String, MeResponse> teamResponseMap;
	private MeResponse meResponseAggregate;

	public Question getQuestion() {
		return question;
	}

	public void setQuestion(Question question) {
		this.question = question;
	}

	public Map<String, MeResponse> getTeamResponseMap() {
		return teamResponseMap;
	}

	public void setTeamResponseMap(Map<String, MeResponse> teamResponseMap) {
		this.teamResponseMap = teamResponseMap;
	}

	public MeResponse getMeResponseAggregate() {
		return meResponseAggregate;
	}

	public void setMeResponseAggregate(MeResponse meResponseAggregate) {
		this.meResponseAggregate = meResponseAggregate;
	}

}
