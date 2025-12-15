package com.back.api.notification.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 테스트용 트랜잭션 헬퍼
 * Self-Invocation 문제를 해결하기 위해 별도 컴포넌트로 분리
 */
@Component
public class TransactionHelper {

	/**
	 * 별도 트랜잭션으로 이벤트 발행
	 * @TransactionalEventListener(AFTER_COMMIT)가 동작하려면 트랜잭션이 커밋되어야 함
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void executeInNewTransaction(Runnable action) {
		action.run();
	}
}
