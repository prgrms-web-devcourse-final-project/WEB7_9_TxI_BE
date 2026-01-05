package com.back.global.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import com.back.global.error.code.CommonErrorCode;
import com.back.global.error.exception.ErrorException;

/**
 * Merkle Tree 유틸리티 클래스
 * 티켓 양도 이력의 무결성 검증을 위한 Merkle Root 계산
 */
public final class MerkleUtil {

	/**
	 * 최대 처리 가능한 리프 노드 수 (메모리 보호)
	 * 10,000개 = log2(10000) ≈ 14 레벨, 충분히 안전
	 */
	public static final int MAX_LEAVES = 10_000;

	/**
	 * 무한루프 방지용 최대 반복 횟수
	 * log2(MAX_LEAVES) + 여유분
	 */
	private static final int MAX_ITERATIONS = 20;

	private MerkleUtil() {
		// 유틸리티 클래스 - 인스턴스화 방지
	}

	// 해시 리스트로부터 Merkle Root 계산
	public static String buildRoot(List<String> hashes) {
		if (hashes == null || hashes.isEmpty()) {
			return "";
		}

		if (hashes.size() == 1) {
			return hashes.get(0);
		}

		if (hashes.size() > MAX_LEAVES) {
			throw new ErrorException(CommonErrorCode.MERKLE_TOO_MANY_LEAVES);
		}

		List<String> currentLevel = new ArrayList<>(hashes);
		int iterations = 0;

		// root가 나올때까지 반복 (무한루프 방지)
		while (currentLevel.size() > 1) {
			if (++iterations > MAX_ITERATIONS) {
				throw new ErrorException(CommonErrorCode.MERKLE_BUILD_FAILED);
			}
			currentLevel = buildNextLevel(currentLevel);
		}

		return currentLevel.get(0);
	}

	/**
	 * 현재 레벨에서 다음 레벨의 해시들을 계산
	 * 홀수 개인 경우 마지막 해시를 복제하여 짝수로 맞춤
	 */
	private static List<String> buildNextLevel(List<String> currentLevel) {
		List<String> nextLevel = new ArrayList<>();

		// 홀수인 경우 마지막 요소 복제
		if (currentLevel.size() % 2 != 0) {
			currentLevel.add(currentLevel.get(currentLevel.size() - 1));
		}

		for (int i = 0; i < currentLevel.size(); i += 2) {
			String left = currentLevel.get(i);
			String right = currentLevel.get(i + 1);
			String combined = sha256(left + right);
			nextLevel.add(combined);
		}

		return nextLevel;
	}

	// SHA-256 해시 계산
	public static String sha256(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			return bytesToHex(hashBytes);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA-256 algorithm not available", e);
		}
	}

	private static String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	/**
	 * 특정 데이터가 Merkle Root에 포함되어 있는지 검증
	 * (Merkle Proof 검증 - 선택적 구현)
	 *
	 * @param leafHash 검증할 리프 해시
	 * @param proof    Merkle Proof (형제 해시들의 경로)
	 * @param root     검증 대상 Merkle Root
	 * @return 검증 성공 여부
	 */
	public static boolean verify(String leafHash, List<ProofNode> proof, String root) {
		String currentHash = leafHash;

		for (ProofNode node : proof) {
			if (node.isLeft()) {
				currentHash = sha256(node.hash() + currentHash);
			} else {
				currentHash = sha256(currentHash + node.hash());
			}
		}

		return currentHash.equals(root);
	}

	// Merkle Proof 노드
	public record ProofNode(String hash, boolean isLeft) {
	}
}
