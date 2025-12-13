package com.back.api.payment.payment.client;

import com.back.api.payment.payment.dto.request.PaymentConfirmCommand;
import com.back.api.payment.payment.dto.response.PaymentConfirmResult;

public interface PaymentClient {
	PaymentConfirmResult confirm(PaymentConfirmCommand command);
}
