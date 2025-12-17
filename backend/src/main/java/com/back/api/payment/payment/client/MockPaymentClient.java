package com.back.api.payment.payment.client;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.back.api.payment.payment.dto.request.PaymentConfirmCommand;
import com.back.api.payment.payment.dto.response.PaymentConfirmResult;

@Component
@Profile({"dev", "test", "perf", "prod"})
public class MockPaymentClient implements PaymentClient {

	@Override
	public PaymentConfirmResult confirm(PaymentConfirmCommand command) {
		// 항상 성공한다고 가정
		return new PaymentConfirmResult(
			"mock_payment_key_" + command.orderKey(),
			command.amount(),
			true
		);
	}
}
