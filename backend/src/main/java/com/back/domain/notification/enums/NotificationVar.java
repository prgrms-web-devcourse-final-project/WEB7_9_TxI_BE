package com.back.domain.notification.enums;

import lombok.Getter;

@Getter
//Notification Variation이라는 의미 ( 공통적인 요소가 아닌 도메인별 개별적인 요소를 다룸 )
public enum NotificationVar {
	//1. 회원가입 완료 - 사용 데이터 : 유저 이름
	SIGN_UP(
		"회원가입 완료",
		NotificationTypes.SIGNUP
	) {
		@Override
		public String formatMessage(NotificationContext context) {
			return String.format("%s님, 저희 waitFair에 오신것을 환영합니다.", context.userName());
		}
	},
	//사전등록 완료 - 사용 데이터 : 이벤트 제목
	PRE_REGISTER_DONE(
		"사전등록 완료",
		NotificationTypes.PRE_REGISTER
	) {
		@Override
		public String formatMessage(NotificationContext context) {
			return String.format("[%s]사전등록 완료하였습니다.\n티켓팅 시작일에 알림을 보내드리겠습니다.", context.eventTitle());
		}
	},
	// 사전등록 취소
	PRE_REGISTER_CANCEL(
		"사전등록 취소",
			NotificationTypes.PRE_REGISTER
	){
		@Override
		public String formatMessage(NotificationContext context) {
			return String.format("[%s]사전등록을 취소하였습니다.", context.eventTitle());
		}
	},
	//티켓팅 당일 - 사용 데이터 : 이벤트 제목
	//티켓팅 대기열_대기상태 (waiting) - 사용 데이터 : 이벤트 제목
	QUEUE_WAITING(
		"대기열 순서 기다리는중",
		NotificationTypes.QUEUE_ENTRIES
	) {
		@Override
		public String formatMessage(NotificationContext context) {
			return String.format("[%s]\n%d번째 순서에 배치되었습니다.", context.eventTitle(), context.waitingNum());
		}
	},
	//티켓팅 대기열_입장상태, 티켓팅 시작 (entered) - 사용 데이터 : 이벤트 제목
	QUEUE_ENTERED(
		"티켓팅 시작",
		NotificationTypes.QUEUE_ENTRIES
	) {
		@Override
		public String formatMessage(NotificationContext context) {
			return String.format("[%s]\n지금부터 15분간 티켓을 구매하실 수 있습니다.", context.eventTitle());
		}
	},
	// 티켓팅 대기열 만료 (expired) - 사용 데이터 : 이벤트 제목
	QUEUE_EXPIRED(
		"제한 시간 초과",
		NotificationTypes.QUEUE_ENTRIES
	) {
		@Override
		public String formatMessage(NotificationContext context) {
			return String.format("[%s]\n아쉽게도 티켓팅 가능 시간이 초과되었습니다.\n다음 기회를 노려주세요..", context.eventTitle());
		}
	},
	// 대기열 뒤로 보내기
	//티켓 결제 완료 - 사용 데이터 : 이벤트 제목, 결제 금액
	PAYMENT_SUCCESS(
		"티켓 구매 완료",
		NotificationTypes.PAYMENT
	) {
		@Override
		public String formatMessage(NotificationContext context) {
			return String.format("[%s]\n티켓 1매가 결제되었습니다\n결제금액: %d원", context.eventTitle(), context.amount());
		}
	};
	//결제 실패

	private final String title;
	private final NotificationTypes frontType;

	NotificationVar(String title, NotificationTypes frontType) {
		this.title = title;
		this.frontType = frontType;
	}

	public abstract String formatMessage(NotificationContext context);
}
