package com.back.domain.notification.enums;

import lombok.Builder;

@Builder
public record NotificationContext( // 알림 메세지 만드는 용도
								   String eventTitle,
								   Long amount,
								   String userName, //User 엔티티의 nickname 컬럼 사용
								   Long waitingNum //대기열 입장 순서
) {
}