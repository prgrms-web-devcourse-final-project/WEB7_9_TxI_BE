package com.back.global.utils;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.back.global.error.exception.ErrorException;

@DisplayName("MerkleUtil 단위 테스트")
class MerkleUtilTest {

	@Nested
	@DisplayName("buildRoot 메서드")
	class BuildRoot {

		@Test
		@DisplayName("빈 리스트 → 빈 문자열 반환")
		void emptyList_returnsEmptyString() {
			String result = MerkleUtil.buildRoot(Collections.emptyList());
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("null 리스트 → 빈 문자열 반환")
		void nullList_returnsEmptyString() {
			String result = MerkleUtil.buildRoot(null);
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("단일 해시 → 그대로 반환")
		void singleHash_returnsSameHash() {
			String hash = "abc123";
			String result = MerkleUtil.buildRoot(List.of(hash));
			assertThat(result).isEqualTo(hash);
		}

		@Test
		@DisplayName("두 개 해시 → 결합 후 SHA-256")
		void twoHashes_returnsCombinedHash() {
			List<String> hashes = Arrays.asList("hash1", "hash2");
			String result = MerkleUtil.buildRoot(hashes);

			// 수동 검증: SHA-256(hash1 + hash2)
			String expected = MerkleUtil.sha256("hash1" + "hash2");
			assertThat(result).isEqualTo(expected);
		}

		@Test
		@DisplayName("네 개 해시 → 2레벨 Merkle Tree")
		void fourHashes_buildsTwoLevelTree() {
			List<String> hashes = Arrays.asList("a", "b", "c", "d");
			String result = MerkleUtil.buildRoot(hashes);

			// Level 1: SHA256(a+b), SHA256(c+d)
			String ab = MerkleUtil.sha256("a" + "b");
			String cd = MerkleUtil.sha256("c" + "d");

			// Level 2 (Root): SHA256(ab + cd)
			String expected = MerkleUtil.sha256(ab + cd);

			assertThat(result).isEqualTo(expected);
		}

		@Test
		@DisplayName("홀수 개 해시 → 마지막 복제하여 처리")
		void oddNumberOfHashes_duplicatesLastForProcessing() {
			List<String> hashes = Arrays.asList("a", "b", "c");
			String result = MerkleUtil.buildRoot(hashes);

			// Level 1: SHA256(a+b), SHA256(c+c) (c 복제)
			String ab = MerkleUtil.sha256("a" + "b");
			String cc = MerkleUtil.sha256("c" + "c");

			// Level 2 (Root): SHA256(ab + cc)
			String expected = MerkleUtil.sha256(ab + cc);

			assertThat(result).isEqualTo(expected);
		}

		@Test
		@DisplayName("동일 입력 → 동일 결과 (결정적)")
		void sameInput_sameOutput_deterministic() {
			List<String> hashes = Arrays.asList("x", "y", "z");

			String result1 = MerkleUtil.buildRoot(hashes);
			String result2 = MerkleUtil.buildRoot(hashes);

			assertThat(result1).isEqualTo(result2);
		}

		@Test
		@DisplayName("입력 순서 변경 → 다른 결과")
		void differentOrder_differentResult() {
			List<String> hashes1 = Arrays.asList("a", "b");
			List<String> hashes2 = Arrays.asList("b", "a");

			String result1 = MerkleUtil.buildRoot(hashes1);
			String result2 = MerkleUtil.buildRoot(hashes2);

			assertThat(result1).isNotEqualTo(result2);
		}

		@Test
		@DisplayName("MAX_LEAVES 초과 → ErrorException 발생")
		void tooManyLeaves_throwsErrorException() {
			// MAX_LEAVES + 1 개의 해시 생성
			List<String> tooManyHashes = IntStream.range(0, MerkleUtil.MAX_LEAVES + 1)
				.mapToObj(i -> "hash" + i)
				.toList();

			assertThatThrownBy(() -> MerkleUtil.buildRoot(tooManyHashes))
				.isInstanceOf(ErrorException.class)
				.hasMessageContaining("처리할 데이터가 너무 많습니다");
		}
	}

	@Nested
	@DisplayName("sha256 메서드")
	class Sha256 {

		@Test
		@DisplayName("SHA-256 해시값 64자 hex 반환")
		void sha256_returns64CharHex() {
			String result = MerkleUtil.sha256("hello");
			assertThat(result).hasSize(64);
			assertThat(result).matches("[0-9a-f]+");
		}

		@Test
		@DisplayName("동일 입력 → 동일 해시")
		void sameInput_sameHash() {
			String result1 = MerkleUtil.sha256("test");
			String result2 = MerkleUtil.sha256("test");
			assertThat(result1).isEqualTo(result2);
		}

		@Test
		@DisplayName("다른 입력 → 다른 해시")
		void differentInput_differentHash() {
			String result1 = MerkleUtil.sha256("test1");
			String result2 = MerkleUtil.sha256("test2");
			assertThat(result1).isNotEqualTo(result2);
		}

		@Test
		@DisplayName("알려진 SHA-256 결과와 일치")
		void knownSha256Value() {
			// "hello"의 SHA-256 해시값
			String expected = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
			String result = MerkleUtil.sha256("hello");
			assertThat(result).isEqualTo(expected);
		}
	}

	@Nested
	@DisplayName("verify 메서드")
	class Verify {

		@Test
		@DisplayName("유효한 Proof → 검증 성공")
		void validProof_returnsTrue() {
			// 간단한 2-노드 트리: [a, b] → root = SHA256(a+b)
			String a = "a";
			String b = "b";
			String root = MerkleUtil.sha256(a + b);

			// 'a'를 검증하려면, proof는 [b, isLeft=false]
			List<MerkleUtil.ProofNode> proof = List.of(
				new MerkleUtil.ProofNode(b, false)
			);

			boolean result = MerkleUtil.verify(a, proof, root);
			assertThat(result).isTrue();
		}

		@Test
		@DisplayName("잘못된 Root → 검증 실패")
		void invalidRoot_returnsFalse() {
			String a = "a";
			String b = "b";
			String wrongRoot = "wrong_root_hash";

			List<MerkleUtil.ProofNode> proof = List.of(
				new MerkleUtil.ProofNode(b, false)
			);

			boolean result = MerkleUtil.verify(a, proof, wrongRoot);
			assertThat(result).isFalse();
		}
	}
}
