package com.back.domain.store.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "store_seq")
	@SequenceGenerator(
		name = "store_seq",
		sequenceName = "store_seq",
		allocationSize = 1
	)
	private Long id;

	@Column(name = "name", nullable = false, length = 30)
	private String name;

	@Column(
		name = "registration_number",
		nullable = false,
		length = 16
	)
	private String registrationNumber;

	@Column(name = "address", nullable = false)
	private String address;

	@Column(name = "deleted_at")
	private LocalDateTime deleteDate;

	@OneToMany(mappedBy = "store", fetch = FetchType.LAZY)
	private List<User> managers = new ArrayList<>();

	@Builder
	private Store(
		String name,
		String registrationNumber,
		String address
	) {
		this.name = name;
		this.registrationNumber = registrationNumber;
		this.address = address;
	}

	public void addManager(User user) {
		managers.add(user);
		user.changeStore(this);
	}

	public void softDelete() {
		this.deleteDate = LocalDateTime.now();
	}
}
