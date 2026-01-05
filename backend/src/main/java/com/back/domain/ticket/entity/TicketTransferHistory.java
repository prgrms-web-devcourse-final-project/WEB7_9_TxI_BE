package com.back.domain.ticket.entity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.back.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "ticket_transfer_history",
	indexes = {
		@Index(name = "idx_transfer_history_ticket_id", columnList = "ticket_id")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketTransferHistory extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "ticket_id", nullable = false)
	private Long ticketId;

	@Column(name = "from_user_id", nullable = false)
	private Long fromUserId;

	@Column(name = "to_user_id", nullable = false)
	private Long toUserId;

	@Column(name = "transferred_at", nullable = false)
	private LocalDateTime transferredAt;

	@Builder
	private TicketTransferHistory(Long ticketId, Long fromUserId, Long toUserId) {
		this.ticketId = ticketId;
		this.fromUserId = fromUserId;
		this.toUserId = toUserId;
		this.transferredAt = LocalDateTime.now();
	}

	// 양도 이력 기록
	public static TicketTransferHistory record(Long ticketId, Long fromUserId, Long toUserId) {
		return TicketTransferHistory.builder()
			.ticketId(ticketId)
			.fromUserId(fromUserId)
			.toUserId(toUserId)
			.build();
	}

	/**
	 * Merkle Tree용 SHA-256 해시 생성
	 * 데이터 무결성 검증을 위한 해시값 계산
	 */
	public String computeHash() {
		String data = String.join(":",
			String.valueOf(ticketId),
			String.valueOf(fromUserId),
			String.valueOf(toUserId),
			transferredAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
		);

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
			return bytesToHex(hashBytes);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA-256 algorithm not available", e);
		}
	}

	private String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
