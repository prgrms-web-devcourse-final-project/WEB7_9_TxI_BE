package com.back.api.seat.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.back.domain.event.entity.Event;
import com.back.domain.seat.entity.SeatGrade;
import com.back.support.helper.EventHelper;
import com.back.support.helper.SeatHelper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
@DisplayName("AdminSeatController 통합 테스트")
class AdminSeatControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EventHelper eventHelper;

	@Autowired
	private SeatHelper seatHelper;

	private Event testEvent;

	@BeforeEach
	void setUp() {
		seatHelper.clearSeat();
		eventHelper.clearEvent();

		testEvent = eventHelper.createEvent("테스트 콘서트");
	}

	@Nested
	@DisplayName("좌석 대량 커스텀 생성 (POST /api/v1/admin/events/{eventId}/seats/bulk)")
	class BulkCreateSeatsTests {

		@Test
		@DisplayName("성공: 여러 좌석을 한 번에 생성")
		void bulkCreateSeats_Success() throws Exception {
			String requestBody = """
				{
					"seats": [
					{"seatCode": "A1", "grade": "VIP", "price": 150000},
					{"seatCode": "A2", "grade": "VIP", "price": 150000},
					{"seatCode": "B1", "grade": "R", "price": 100000}
					]
				}
				""";

			mockMvc.perform(post("/api/v1/admin/events/{eventId}/seats/bulk", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("좌석을 대량 생산했습니다"))
				.andExpect(jsonPath("$.data", hasSize(3)))
				.andExpect(jsonPath("$.data[0].seatCode").value("A1"))
				.andExpect(jsonPath("$.data[0].grade").value("VIP"))
				.andExpect(jsonPath("$.data[0].price").value(150000))
				.andExpect(jsonPath("$.data[1].seatCode").value("A2"))
				.andExpect(jsonPath("$.data[2].seatCode").value("B1"))
				.andExpect(jsonPath("$.data[2].grade").value("R"));
		}

		@Test
		@DisplayName("실패: 빈 좌석 리스트")
		void bulkCreateSeats_EmptyList() throws Exception {
			String requestBody = """
				{
					"seats": []
				}
				""";

			mockMvc.perform(post("/api/v1/admin/events/{eventId}/seats/bulk", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("실패: 요청 내부에 중복된 좌석 코드 (같은 grade + seatCode)")
		void bulkCreateSeats_InternalDuplicate() throws Exception {
			String requestBody = """
				{
					"seats": [
						{"seatCode": "A1", "grade": "VIP", "price": 150000},
						{"seatCode": "A1", "grade": "VIP", "price": 150000}
					]
				}
				""";

			mockMvc.perform(post("/api/v1/admin/events/{eventId}/seats/bulk", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("요청 내 중복 좌석")));
		}

		@Test
		@DisplayName("실패: DB에 이미 존재하는 좌석 코드")
		void bulkCreateSeats_DbDuplicate() throws Exception {
			seatHelper.createSeat(testEvent, "A1", SeatGrade.VIP, 150000);

			String requestBody = """
				{
					"seats": [
						{"seatCode": "A1", "grade": "VIP", "price": 150000},
						{"seatCode": "A2", "grade": "VIP", "price": 150000}
					]
				}
				""";

			mockMvc.perform(post("/api/v1/admin/events/{eventId}/seats/bulk", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("이미 존재하는 좌석")));
		}

		@Test
		@DisplayName("성공: 다른 grade라면 같은 seatCode 허용")
		void bulkCreateSeats_SameSeatCodeDifferentGrade() throws Exception {
			String requestBody = """
				{
					"seats": [
						{"seatCode": "A1", "grade": "VIP", "price": 150000},
						{"seatCode": "A1", "grade": "R", "price": 100000}
					]
				}
				""";

			mockMvc.perform(post("/api/v1/admin/events/{eventId}/seats/bulk", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data", hasSize(2)));
		}

		@Test
		@DisplayName("실패: 존재하지 않는 이벤트")
		void bulkCreateSeats_EventNotFound() throws Exception {
			String requestBody = """
				{
					"seats": [
						{"seatCode": "A1", "grade": "VIP", "price": 150000}
					]
				}
				""";

			mockMvc.perform(post("/api/v1/admin/events/{eventId}/seats/bulk", 999999L)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("좌석 자동 생성 (POST /api/v1/admin/events/{eventId}/seats/auto)")
	class AutoCreateSeatsTests {

		@Test
		@DisplayName("성공: 행-열 기반 자동 생성 (3행 5열)")
		void autoCreateSeats_Success() throws Exception {
			String requestBody = """
				{
					"rows": 3,
					"cols": 5,
					"defaultGrade": "R",
					"defaultPrice": 100000
				}
				""";

			mockMvc.perform(post("/api/v1/admin/events/{eventId}/seats/auto", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value(containsString("좌석을 자동 생성했습니다")))
				.andExpect(jsonPath("$.message").value(containsString("총 15개")))
				.andExpect(jsonPath("$.message").value(containsString("3행 x 5열")))
				.andExpect(jsonPath("$.data", hasSize(15)))
				.andExpect(jsonPath("$.data[0].seatCode").value("A1"))
				.andExpect(jsonPath("$.data[4].seatCode").value("A5"))
				.andExpect(jsonPath("$.data[5].seatCode").value("B1"))
				.andExpect(jsonPath("$.data[14].seatCode").value("C5"));
		}

		@Test
		@DisplayName("실패: rows > 26 (알파벳 한계 초과)")
		void autoCreateSeats_RowsExceedsLimit() throws Exception {
			String requestBody = """
				{
					"rows": 27,
					"cols": 5,
					"defaultGrade": "R",
					"defaultPrice": 100000
				}
				""";

			mockMvc.perform(post("/api/v1/admin/events/{eventId}/seats/auto", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("실패: cols > 100")
		void autoCreateSeats_ColsExceedsLimit() throws Exception {
			String requestBody = """
				{
					"rows": 5,
					"cols": 101,
					"defaultGrade": "R",
					"defaultPrice": 100000
				}
				""";

			mockMvc.perform(post("/api/v1/admin/events/{eventId}/seats/auto", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("실패: null 필드들")
		void autoCreateSeats_NullFields() throws Exception {
			String requestBody = """
				{
					"rows": null,
					"cols": null,
					"defaultGrade": null,
					"defaultPrice": null
				}
				""";

			mockMvc.perform(post("/api/v1/admin/events/{eventId}/seats/auto", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("실패: 음수 price")
		void autoCreateSeats_NegativePrice() throws Exception {
			String requestBody = """
				{
					"rows": 2,
					"cols": 3,
					"defaultGrade": "R",
					"defaultPrice": -1000
				}
				""";

			mockMvc.perform(post("/api/v1/admin/events/{eventId}/seats/auto", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("실패: 이미 존재하는 좌석과 충돌")
		void autoCreateSeats_ConflictWithExisting() throws Exception {
			seatHelper.createSeat(testEvent, "A1", SeatGrade.R, 50000);

			String requestBody = """
				{
					"rows": 2,
					"cols": 2,
					"defaultGrade": "R",
					"defaultPrice": 100000
				}
				""";

			mockMvc.perform(post("/api/v1/admin/events/{eventId}/seats/auto", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("이미 존재하는 좌석")));
		}
	}

	@Nested
	@DisplayName("단일 좌석 생성 (POST /api/v1/admin/events/{eventId}/seats/single)")
	class CreateSingleSeatTests {

		@Test
		@DisplayName("성공: 단일 좌석 생성")
		void createSingleSeat_Success() throws Exception {
			String requestBody = """
				{
					"seatCode": "VIP1",
					"grade": "VIP",
					"price": 200000
				}
				""";

			mockMvc.perform(post("/api/v1/admin/events/{eventId}/seats/single", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("좌석을 생성했습니다."))
				.andExpect(jsonPath("$.data.seatCode").value("VIP1"))
				.andExpect(jsonPath("$.data.grade").value("VIP"))
				.andExpect(jsonPath("$.data.price").value(200000))
				.andExpect(jsonPath("$.data.seatStatus").value("AVAILABLE"));
		}

		@Test
		@DisplayName("실패: 이미 존재하는 좌석 코드")
		void createSingleSeat_Duplicate() throws Exception {
			seatHelper.createSeat(testEvent, "VIP1", SeatGrade.VIP, 200000);

			String requestBody = """
				{
					"seatCode": "VIP1",
					"grade": "VIP",
					"price": 200000
				}
				""";

			mockMvc.perform(post("/api/v1/admin/events/{eventId}/seats/single", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("이미 존재하는 좌석")));
		}
	}

	@Nested
	@DisplayName("좌석 수정 (PUT /api/v1/admin/events/{eventId}/seats/{seatId})")
	class UpdateSeatTests {

		@Test
		@DisplayName("성공: 좌석 정보 수정")
		void updateSeat_Success() throws Exception {
			var savedSeat = seatHelper.createSeat(testEvent, "A1", SeatGrade.R, 100000);

			String requestBody = """
				{
					"seatCode": "A2",
					"grade": "VIP",
					"price": 150000,
					"seatStatus": "AVAILABLE"
				}
				""";

			mockMvc.perform(put("/api/v1/admin/events/{eventId}/seats/{seatId}", testEvent.getId(), savedSeat.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("좌석을 수정했습니다."))
				.andExpect(jsonPath("$.data.seatCode").value("A2"))
				.andExpect(jsonPath("$.data.grade").value("VIP"))
				.andExpect(jsonPath("$.data.price").value(150000));
		}

		@Test
		@DisplayName("성공: seatCode나 grade가 변경되지 않으면 중복 체크 스킵")
		void updateSeat_NoChangeNoDuplicateCheck() throws Exception {
			var savedSeat = seatHelper.createSeat(testEvent, "A1", SeatGrade.R, 100000);

			String requestBody = """
				{
					"seatCode": "A1",
					"grade": "R",
					"price": 120000,
					"seatStatus": "SOLD"
				}
				""";

			mockMvc.perform(put("/api/v1/admin/events/{eventId}/seats/{seatId}", testEvent.getId(), savedSeat.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.price").value(120000))
				.andExpect(jsonPath("$.data.seatStatus").value("SOLD"));
		}

		@Test
		@DisplayName("실패: 다른 좌석과 중복되는 (grade + seatCode)로 수정")
		void updateSeat_Duplicate() throws Exception {
			seatHelper.createSeat(testEvent, "A1", SeatGrade.VIP, 150000);
			var savedSeat2 = seatHelper.createSeat(testEvent, "A2", SeatGrade.R, 100000);

			String requestBody = """
				{
					"seatCode": "A1",
					"grade": "VIP",
					"price": 100000,
					"seatStatus": "AVAILABLE"
				}
				""";

			mockMvc.perform(put("/api/v1/admin/events/{eventId}/seats/{seatId}", testEvent.getId(), savedSeat2.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("이미 존재하는 좌석")));
		}

		@Test
		@DisplayName("실패: 존재하지 않는 좌석 수정")
		void updateSeat_NotFound() throws Exception {
			String requestBody = """
				{
					"seatCode": "A1",
					"grade": "VIP",
					"price": 150000,
					"seatStatus": "AVAILABLE"
				}
				""";

			mockMvc.perform(put("/api/v1/admin/events/{eventId}/seats/{seatId}", testEvent.getId(), 999999L)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("단일 좌석 삭제 (DELETE /api/v1/admin/events/{eventId}/seats/{seatId})")
	class DeleteSeatTests {

		@Test
		@DisplayName("성공: 좌석 삭제")
		void deleteSeat_Success() throws Exception {
			var savedSeat = seatHelper.createSeat(testEvent, "A1", SeatGrade.R, 100000);

			mockMvc.perform(
					delete("/api/v1/admin/events/{eventId}/seats/{seatId}", testEvent.getId(), savedSeat.getId()))
				.andDo(print())
				.andExpect(status().isNoContent())
				.andExpect(jsonPath("$.message").value("좌석을 삭제했습니다."));
		}

		@Test
		@DisplayName("실패: 존재하지 않는 좌석 삭제")
		void deleteSeat_NotFound() throws Exception {
			mockMvc.perform(delete("/api/v1/admin/events/{eventId}/seats/{seatId}", testEvent.getId(), 999999L))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("이벤트의 모든 좌석 삭제 (DELETE /api/v1/admin/events/{eventId}/seats)")
	class DeleteAllEventSeatsTests {

		@Test
		@DisplayName("성공: 이벤트의 모든 좌석 삭제")
		void deleteAllEventSeats_Success() throws Exception {
			seatHelper.createSeat(testEvent, "A1", SeatGrade.VIP, 150000);
			seatHelper.createSeat(testEvent, "A2", SeatGrade.R, 100000);
			seatHelper.createSeat(testEvent, "B1", SeatGrade.S, 80000);

			mockMvc.perform(delete("/api/v1/admin/events/{eventId}/seats", testEvent.getId()))
				.andDo(print())
				.andExpect(status().isNoContent())
				.andExpect(jsonPath("$.message").value("이벤트의 모든 좌석을 삭제했습니다."));
		}

		@Test
		@DisplayName("성공: 좌석이 없어도 성공")
		void deleteAllEventSeats_NoSeats() throws Exception {
			mockMvc.perform(delete("/api/v1/admin/events/{eventId}/seats", testEvent.getId()))
				.andDo(print())
				.andExpect(status().isNoContent());
		}
	}

	@Nested
	@DisplayName("서비스 레이어 검증 로직 테스트")
	class ServiceLayerValidationTests {

		@Test
		@DisplayName("grade별 그룹핑으로 DB 쿼리 최적화 확인")
		void validateDuplicateSeats_GradeGrouping() throws Exception {
			seatHelper.createSeat(testEvent, "A1", SeatGrade.VIP, 150000);

			String requestBody = """
				{
					"seats": [
						{"seatCode": "A1", "grade": "VIP", "price": 150000},
						{"seatCode": "A2", "grade": "VIP", "price": 150000},
						{"seatCode": "B1", "grade": "R", "price": 100000},
						{"seatCode": "B2", "grade": "R", "price": 100000}
					]
				}
				""";

			mockMvc.perform(post("/api/v1/admin/events/{eventId}/seats/bulk", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("VIP:A1")));
		}

		@Test
		@DisplayName("수정 시 자기 자신은 제외하고 중복 체크")
		void validateDuplicateSeatsOnUpdate_ExcludeSelf() throws Exception {
			var savedSeat1 = seatHelper.createSeat(testEvent, "A1", SeatGrade.VIP, 150000);

			String requestBody = """
				{
					"seatCode": "A2",
					"grade": "VIP",
					"price": 150000,
					"seatStatus": "AVAILABLE"
				}
				""";

			mockMvc.perform(put("/api/v1/admin/events/{eventId}/seats/{seatId}", testEvent.getId(), savedSeat1.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isOk());
		}

		@Test
		@DisplayName("다른 이벤트의 같은 좌석 코드는 허용")
		void validateDuplicateSeats_DifferentEvent() throws Exception {
			var anotherEvent = eventHelper.createEvent("다른 이벤트");
			seatHelper.createSeat(anotherEvent, "A1", SeatGrade.VIP, 150000);

			String requestBody = """
				{
					"seats": [
						{"seatCode": "A1", "grade": "VIP", "price": 150000}
					]
				}
				""";

			mockMvc.perform(post("/api/v1/admin/events/{eventId}/seats/bulk", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isCreated());
		}
	}

	@Nested
	@DisplayName("엣지 케이스 테스트")
	class EdgeCaseTests {

		@Test
		@DisplayName("최대 행(26) 자동 생성")
		void autoCreateSeats_MaxRows() throws Exception {
			String requestBody = """
				{
					"rows": 26,
					"cols": 1,
					"defaultGrade": "A",
					"defaultPrice": 50000
				}
				""";

			mockMvc.perform(post("/api/v1/admin/events/{eventId}/seats/auto", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data", hasSize(26)))
				.andExpect(jsonPath("$.data[0].seatCode").value("A1"))
				.andExpect(jsonPath("$.data[25].seatCode").value("Z1"));
		}

		@Test
		@DisplayName("최대 열(100) 자동 생성")
		void autoCreateSeats_MaxCols() throws Exception {
			String requestBody = """
				{
					"rows": 1,
					"cols": 100,
					"defaultGrade": "A",
					"defaultPrice": 50000
				}
				""";

			mockMvc.perform(post("/api/v1/admin/events/{eventId}/seats/auto", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data", hasSize(100)))
				.andExpect(jsonPath("$.data[99].seatCode").value("A100"));
		}

		@Test
		@DisplayName("가격 0원 허용")
		void createSeat_ZeroPrice() throws Exception {
			String requestBody = """
				{
					"seatCode": "FREE1",
					"grade": "A",
					"price": 0
				}
				""";

			mockMvc.perform(post("/api/v1/admin/events/{eventId}/seats/single", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.price").value(0));
		}

		@Test
		@DisplayName("좌석 상태를 SOLD로 수정")
		void updateSeat_ChangeToSold() throws Exception {
			var savedSeat = seatHelper.createSeat(testEvent, "A1", SeatGrade.R, 100000);

			String requestBody = """
				{
					"seatCode": "A1",
					"grade": "R",
					"price": 100000,
					"seatStatus": "SOLD"
				}
				""";

			mockMvc.perform(put("/api/v1/admin/events/{eventId}/seats/{seatId}", testEvent.getId(), savedSeat.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.seatStatus").value("SOLD"));
		}

		@Test
		@DisplayName("좌석 상태를 RESERVED로 수정")
		void updateSeat_ChangeToReserved() throws Exception {
			var savedSeat = seatHelper.createSeat(testEvent, "A1", SeatGrade.R, 100000);

			String requestBody = """
				{
					"seatCode": "A1",
					"grade": "R",
					"price": 100000,
					"seatStatus": "RESERVED"
				}
				""";

			mockMvc.perform(put("/api/v1/admin/events/{eventId}/seats/{seatId}", testEvent.getId(), savedSeat.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.seatStatus").value("RESERVED"));
		}
	}
}
