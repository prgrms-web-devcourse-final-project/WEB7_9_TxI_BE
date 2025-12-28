package com.back.api.seed.controller;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.seed.dto.ScenarioSeeder;
import com.back.api.seed.dto.response.SeedResult;
import com.back.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Profile({"perf", "dev"})
@RequestMapping("/internal/seed")
public class SeedController {

	private final List<ScenarioSeeder> seeders;

	@PostMapping
	public ApiResponse<SeedResult> run(@RequestParam String scenario) {
		ScenarioSeeder seeder = seeders.stream()
			.filter(seed -> seed.key().equals(scenario))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Unknown scenario: " + scenario));

		seeder.reset();
		return ApiResponse.ok(seeder.seed());
	}
}
