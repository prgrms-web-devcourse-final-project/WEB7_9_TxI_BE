package com.back.domain.event.entity;

import java.time.LocalDateTime;

import com.back.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String title;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EventCategory category;

	//@Lob
	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false)
	private String place;

	private String imageUrl;

	@Column(nullable = false)
	private Integer minPrice;

	@Column(nullable = false)
	private Integer maxPrice;

	@Column(nullable = false)
	private LocalDateTime preOpenAt;

	@Column(nullable = false)
	private LocalDateTime preCloseAt;

	@Column(nullable = false)
	private LocalDateTime ticketOpenAt;

	@Column(nullable = false)
	private LocalDateTime ticketCloseAt;

	@Column(nullable = false)
	private Integer maxTicketAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EventStatus status;

	@Column(nullable = false)
	private boolean deleted = false;

	@Builder
	public Event(String title, EventCategory category, String description, String place,
		String imageUrl, Integer minPrice, Integer maxPrice,
		LocalDateTime preOpenAt, LocalDateTime preCloseAt,
		LocalDateTime ticketOpenAt, LocalDateTime ticketCloseAt,
		Integer maxTicketAmount, EventStatus status) {
		validatePrice(minPrice, maxPrice);
		this.title = title;
		this.category = category;
		this.description = description;
		this.place = place;
		this.imageUrl = imageUrl;
		this.minPrice = minPrice;
		this.maxPrice = maxPrice;
		this.preOpenAt = preOpenAt;
		this.preCloseAt = preCloseAt;
		this.ticketOpenAt = ticketOpenAt;
		this.ticketCloseAt = ticketCloseAt;
		this.maxTicketAmount = maxTicketAmount;
		this.status = status != null ? status : EventStatus.READY;
		this.deleted = false;
	}

	public void changeBasicInfo(String title, EventCategory category, String description, String place,
		String imageUrl) {
		this.title = title;
		this.category = category;
		this.description = description;
		this.place = place;
		this.imageUrl = imageUrl;
	}

	public void changePriceInfo(Integer minPrice, Integer maxPrice, Integer maxTicketAmount) {
		validatePrice(minPrice, maxPrice);
		this.minPrice = minPrice;
		this.maxPrice = maxPrice;
		this.maxTicketAmount = maxTicketAmount;
	}

	public void changePeriod(LocalDateTime preOpenAt, LocalDateTime preCloseAt,
		LocalDateTime ticketOpenAt, LocalDateTime ticketCloseAt) {
		this.preOpenAt = preOpenAt;
		this.preCloseAt = preCloseAt;
		this.ticketOpenAt = ticketOpenAt;
		this.ticketCloseAt = ticketCloseAt;
	}

	public void changeStatus(EventStatus status) {
		this.status = status != null ? status : this.status;
	}

	public void delete() {
		this.deleted = true;
	}

	public boolean isDeleted() {
		return this.deleted;
	}

	private void validatePrice(Integer minPrice, Integer maxPrice) {
		if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
			throw new IllegalArgumentException("최소 가격은 최대 가격보다 클 수 없습니다.");
		}
	}
}
