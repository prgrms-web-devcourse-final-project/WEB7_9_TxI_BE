package com.back.api.seed.dto;

import com.back.api.seed.dto.response.SeedResult;

public interface ScenarioSeeder {
	String key();

	void reset();

	SeedResult seed();
}
