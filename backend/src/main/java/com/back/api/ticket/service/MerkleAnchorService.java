package com.back.api.ticket.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.domain.ticket.entity.TicketTransferHistory;
import com.back.domain.ticket.repository.TicketTransferHistoryRepository;
import com.back.global.utils.MerkleUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 티켓 양도 이력 Merkle Root 검증 서비스
 *
 * 블록체인의 핵심 원리(위변조 감지)를 차용하여
 * 데이터 무결성 검증 기능을 제공합니다.
 *
 * 사용 시나리오:
 * - 문제 발생 시 특정 티켓의 양도 이력 무결성 검증
 * - Loki에 기록된 Merkle Root와 현재 DB 상태 비교
 *
 * 프로덕션 환경 / 추가 구현 시에 해당 서비스 활용해서 양도 이력 검증 구현
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MerkleAnchorService {

	private final TicketTransferHistoryRepository transferHistoryRepository;

	// 특정 티켓의 현재 Merkle Root 계산
	@Transactional(readOnly = true)
	public String computeTicketMerkleRoot(Long ticketId) {
		List<TicketTransferHistory> histories =
			transferHistoryRepository.findByTicketIdOrderByTransferredAtDesc(ticketId);

		if (histories.isEmpty()) {
			return "";
		}

		List<String> hashes = histories.stream()
			.map(TicketTransferHistory::computeHash)
			.toList();

		return MerkleUtil.buildRoot(hashes);
	}

	/**
	 * 특정 티켓의 Merkle Root 검증
	 * Loki에 기록된 expectedRoot와 현재 DB 상태 비교
	 *
	 * @param expectedRoot Loki에서 가져온 예상 Merkle Root
	 */
	@Transactional(readOnly = true)
	public boolean verifyTicketHistory(Long ticketId, String expectedRoot) {
		String currentRoot = computeTicketMerkleRoot(ticketId);

		boolean isValid = currentRoot.equals(expectedRoot);

		if (!isValid) {
			log.warn("[MERKLE_VERIFY_FAILED] ticketId={}, expected={}, actual={}",
				ticketId, expectedRoot, currentRoot);
		} else {
			log.info("[MERKLE_VERIFY_SUCCESS] ticketId={}, root={}", ticketId, currentRoot);
		}

		return isValid;
	}
}
