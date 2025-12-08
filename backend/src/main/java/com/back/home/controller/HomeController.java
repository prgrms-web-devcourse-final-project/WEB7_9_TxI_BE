package com.back.home.controller;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Home", description = "홈 컨트롤러")
public class HomeController {

	@GetMapping(produces = MediaType.TEXT_HTML_VALUE)
	public String home() throws UnknownHostException {

		InetAddress localhost = InetAddress.getLocalHost();

		return """
			<h1>Welcome to Rest1</h1>
			<p>Server IP Address: %s</p>
			<p>Server Host Name: %s</p>
			<div>
				<a href="swagger-ui/index.html">API 문서로 이동</a>
			</div>
			""".formatted(localhost.getHostAddress(), localhost.getHostName());
	}
}

