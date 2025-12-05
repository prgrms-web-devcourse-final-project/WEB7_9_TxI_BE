package com.back.global.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SequenceGenerator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_seq")
	@SequenceGenerator(
		name = "global_seq",          // JPA에서 부를 이름
		sequenceName = "global_seq",  // 실제 PostgreSQL에 생성될 시퀀스 이름
		allocationSize = 50           // 미리 땡겨올 ID 개수 -> 성능
	)
	@Setter(AccessLevel.PROTECTED)
	private Long id;

	@Column(name = "created_at")
	@CreatedDate
	private LocalDateTime createAt;

	@Column(name = "modified_at")
	@LastModifiedDate
	private LocalDateTime modifiedAt;

	protected BaseEntity(Long id) {
		this.id = id;
	}
}
