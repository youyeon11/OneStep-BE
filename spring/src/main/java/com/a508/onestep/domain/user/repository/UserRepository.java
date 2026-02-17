package com.a508.onestep.domain.user.repository;

import com.a508.onestep.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /*
    UserCode 기반으로 조회하기
     */
    Optional<User> findByUserCode(String userCode);

    /*
    UserCode 존재 여부 확인
     */
    boolean existsByUserCode(String userCode);

    /*
    email 기반으로 조회하기
     */
    Optional<User> findByEmail(String email);

    /**
     * 편지 수신 모드가 활성화된 모든 사용자 조회
     * @return 수신 가능 상태인 사용자 리스트
     */
    List<User> findAllByIsOpenTrue();

//    /*
//    본인을 제외하고 수신 설정(창문)이 활성화된 모든 유저 조회하기
//    - 편지 랜덤 매칭의 후보군을 선정할 때 사용
//     */
//
    @Query(value = "SELECT u.userCode FROM User u WHERE u.inactivatedAt IS NULL ORDER BY u.userCode",
            countQuery = "SELECT COUNT(u) FROM User u WHERE u.inactivatedAt IS NULL")
    Page<String> findActiveUserCodes(Pageable pageable);

    /**
     * 배치 처리용: 여러 유저 코드로 유저 정보 일괄 조회
     * @param userCodes 조회할 유저 코드 목록
     * @return 유저 엔티티 목록
     */
    List<User> findByUserCodeIn(List<String> userCodes);
}
