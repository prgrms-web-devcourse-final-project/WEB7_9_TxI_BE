package com.back.api.payment.payment.client;

import com.back.api.payment.payment.dto.PaymentConfirmCommand;
import com.back.api.payment.payment.dto.PaymentConfirmResult;

public interface PaymentClient {
	PaymentConfirmResult confirm(PaymentConfirmCommand command);
}
