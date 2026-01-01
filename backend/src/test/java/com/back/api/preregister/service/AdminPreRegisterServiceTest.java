package com.back.api.preregister.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.preregister.dto.response.PreRegisterListResponse;
import com.back.domain.event.entity.Event;
import com.back.domain.preregister.entity.PreRegister;
import com.back.domain.preregister.repository.PreRegisterRepository;
import com.back.domain.store.entity.Store;
import com.back.domain.user.entity.UserRole;
import com.back.domain.user.repository.UserRepository;
import com.back.support.data.TestUser;
import com.back.support.factory.PreRegisterFactory;
import com.back.support.factory.UserFactory;
import com.back.support.helper.EventHelper;
import com.back.support.helper.StoreHelper;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AdminPreRegisterService 통합 테스트")
class AdminPreRegisterServiceTest {

	@Autowired
	private AdminPreRegisterService adminPreRegisterService;

	@Autowired
	private PreRegisterRepository preRegisterRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EventHelper eventHelper;

	@Autowired
	private StoreHelper storeHelper;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private Event testEvent;

	@BeforeEach
	void setUp() {
		Store store = storeHelper.createStore();
		testEvent = eventHelper.createEvent(store, "테스트 이벤트");
	}

	@Nested
	@DisplayName("사전 등록 목록 조회")
	class GetPreRegistersByEventId {

		@Test
		@DisplayName("이벤트별 사전 등록 목록을 페이징하여 조회")
		void getPreRegistersByEventId_Success() {
			// given: 30명의 사용자 사전 등록
			for (int i = 1; i <= 30; i++) {
				TestUser testUser = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null);
				userRepository.save(testUser.user());

				PreRegister preRegister = PreRegisterFactory.fakePreRegister(testEvent, testUser.user());
				preRegisterRepository.save(preRegister);
			}

			int page = 0;
			int size = 20;

			// when
			Page<PreRegisterListResponse> result = adminPreRegisterService.getPreRegisterByEventId(
				testEvent.getId(),
				page,
				size
			);

			// then
			assertThat(result.getContent()).hasSize(20);
			assertThat(result.getTotalElements()).isEqualTo(30);
			assertThat(result.getTotalPages()).isEqualTo(2);
			assertThat(result.getNumber()).isEqualTo(0);
			assertThat(result.isFirst()).isTrue();
			assertThat(result.isLast()).isFalse();
		}

		@Test
		@DisplayName("두 번째 페이지 조회")
		void getPreRegistersByEventId_SecondPage() {
			// given
			for (int i = 1; i <= 30; i++) {
				TestUser testUser = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null);
				userRepository.save(testUser.user());

				PreRegister preRegister = PreRegisterFactory.fakePreRegister(testEvent, testUser.user());
				preRegisterRepository.save(preRegister);
			}

			int page = 1;
			int size = 20;

			// when
			Page<PreRegisterListResponse> result = adminPreRegisterService.getPreRegisterByEventId(
				testEvent.getId(),
				page,
				size
			);

			// then
			assertThat(result.getContent()).hasSize(10);
			assertThat(result.getTotalElements()).isEqualTo(30);
			assertThat(result.getNumber()).isEqualTo(1);
			assertThat(result.isFirst()).isFalse();
			assertThat(result.isLast()).isTrue();
		}

		@Test
		@DisplayName("사전 등록이 없으면 빈 페이지를 반환")
		void getPreRegistersByEventId_EmptyPage() {
			// given
			int page = 0;
			int size = 20;

			// when
			Page<PreRegisterListResponse> result = adminPreRegisterService.getPreRegisterByEventId(
				testEvent.getId(),
				page,
				size
			);

			// then
			assertThat(result.getContent()).isEmpty();
			assertThat(result.getTotalElements()).isEqualTo(0);
		}
	}

	@Nested
	@DisplayName("사전 등록 인원 수 조회")
	class GetPreRegisterCount {

		@Test
		@DisplayName("이벤트별 사전 등록 인원 수를 조회")
		void getPreRegisterCount_Success() {
			// given: 150명의 사용자 사전 등록
			for (int i = 1; i <= 150; i++) {
				TestUser testUser = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null);
				userRepository.save(testUser.user());

				PreRegister preRegister = PreRegisterFactory.fakePreRegister(testEvent, testUser.user());
				preRegisterRepository.save(preRegister);
			}

			// when
			Long count = adminPreRegisterService.getPreRegisterCountByEventId(testEvent.getId());

			// then
			assertThat(count).isEqualTo(150L);
		}

		@Test
		@DisplayName("사전 등록 인원이 없으면 0 반환")
		void getPreRegisterCount_Zero() {
			// when
			Long count = adminPreRegisterService.getPreRegisterCountByEventId(testEvent.getId());

			// then
			assertThat(count).isZero();
		}

		@Test
		@DisplayName("REGISTERED 상태만 카운트")
		void getPreRegisterCount_OnlyRegisteredStatus() {
			// given: REGISTERED 2명, CANCELED 1명
			TestUser user1 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null);
			TestUser user2 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null);
			TestUser user3 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null);

			userRepository.save(user1.user());
			userRepository.save(user2.user());
			userRepository.save(user3.user());

			preRegisterRepository.save(PreRegisterFactory.fakePreRegister(testEvent, user1.user()));
			preRegisterRepository.save(PreRegisterFactory.fakePreRegister(testEvent, user2.user()));
			preRegisterRepository.save(PreRegisterFactory.fakeCanceledPreRegister(testEvent, user3.user()));

			// when
			Long count = adminPreRegisterService.getPreRegisterCountByEventId(testEvent.getId());

			// then: REGISTERED 상태만 카운트 (2명)
			assertThat(count).isEqualTo(2L);
		}
	}
}