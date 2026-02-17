package com.a508.onestep.global.response;

import org.springframework.http.HttpStatus;

public enum ErrorCode implements BaseCode {

    TEST_ERROR(500, "TEST_ERROR", "테스트 에러입니다."),

    // 4xx
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "RESOURCE_NOT_FOUND", "리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED.value(), "METHOD_ERROR", "요청 메서드가 올바르지 않습니다."),

    // user
    USER_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "USER001", "회원을 찾을 수 없습니다."),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST.value(), "USER002", "회원의 닉네임이 양식에 맞지 않습니다."),
    INVALID_USER_STATUS(HttpStatus.BAD_REQUEST.value(), "USER003", "비활성화된 회원입니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.BAD_REQUEST.value(), "USER004", "회원의 이메일이 이미 등록되어있습니다."),

    SURVEY_LOG_NOT_FOUND(HttpStatus.BAD_REQUEST.value(), "SURVEY001", "설문 조사 기록을 조회할 수 없습니다."),

    // jwt
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED.value(), "TOKEN001", "토큰이 유효하지 않습니다."),
    TOKEN_SIGNATURE_INVALID(HttpStatus.UNAUTHORIZED.value(), "TOKEN002", "토큰 서명이 유효하지 않습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED.value(), "TOKEN003", "토큰 기한이 유효하지 않습니다."),
    TOKEN_UNSUPPORTED(HttpStatus.UNAUTHORIZED.value(), "TOKEN004", "지원하지 않는 토큰입니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED.value(), "TOKEN005", "토큰이 비어있습니다."),
    UNMATCHED_REFRESH_TOKEN(HttpStatus.BAD_REQUEST.value(), "TOKEN006", "리프레시 토큰이 일치하지 않습니다."),

    // auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED", "인증되지 않은 접근입니다."),
    ACCESS_DENIED(HttpStatus.UNAUTHORIZED.value(), "ACCESS_DENIED", "허용되지 않은 접근 권한입니다."),

    // Kakao
    KAKAO_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR.value(), "KAKAO001", "카카오 API 호출 중 오류가 발생했습니다."),
    KAKAO_TOKEN_REQUEST_FAILED(HttpStatus.BAD_REQUEST.value(), "KAKAO002", "카카오 액세스 토큰 요청에 실패했습니다."),
    KAKAO_USER_INFO_FAILED(HttpStatus.BAD_REQUEST.value(), "KAKAO003", "카카오 사용자 정보가 비어있습니다."),
    KAKAO_INVALID_TOKEN(HttpStatus.UNAUTHORIZED.value(), "KAKAO004", "유효하지 않은 카카오 토큰입니다."),
    KAKAO_MAP_NOT_FOUND(HttpStatus.BAD_REQUEST.value(), "KAKAO005", "카카오에서 근처의 맵이 존재하지 않습니다."),

    // letter
    LETTER_ALREADY_WRITTEN_TODAY(HttpStatus.BAD_REQUEST.value(),"LETTER001","편지는 하루에 한 번만 작성할 수 있습니다."),
    LETTER_ALREADY_RECEIVED_TODAY(HttpStatus.BAD_REQUEST.value(),"LETTER002","편지는 하루에 한 번만 받을 수 있습니다."),
    LETTER_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "LETTER003", "편지를 찾을 수 없습니다."),
    LETTER_ALREADY_SAVED(HttpStatus.CONFLICT.value(), "LETTER004", "이미 보관된 편지입니다."),
    LETTER_ALREADY_DELETED(HttpStatus.CONFLICT.value(), "LETTER005", "이미 삭제된 편지입니다."),
    NO_OPEN_RECEIVER(HttpStatus.CONFLICT.value(), "LETTER006", "현재 편지를 받을 수 있는 사용자가 없습니다."),
    USER_STATUS_NOT_OPENED(HttpStatus.BAD_REQUEST.value(), "LETTER007", "수신 모드가 비활성화되어 있어 편지를 받을 수 없습니다."),

    // challenge
    CHALLENGE_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "CHALLENGE001", "해당 챌린지를 조회할 수 없습니다."),
    CHALLENGE_ALREADY_DONE(HttpStatus.BAD_REQUEST.value(),                  "CHALLENGE002",
    "이미 해당 챌린지를 완료했습니다."),
    RECOMMENDATION_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "CHALLENGE003", "추천 챌린지를 조회할 수 없습니다."),

    // calendar
    CALENDAR_RETRIEVAL_FAIL(HttpStatus.INTERNAL_SERVER_ERROR.value(), "CALENDAR001", "월별 캘린더 정보를 조회하는 중 오류가 발생했습니다."),
    INVALID_DATE_FORMAT(HttpStatus.BAD_REQUEST.value(), "CALENDAR002", "날짜의 형식이 잘못되었습니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST.value(), "CALENDAR003", "입력 가능한 날짜 범위가 아닙니다."),
    CANNOT_CONVERT_TO_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR.value(), "CALENDAR004", "캘린서 상세 정보를 표시 형식으로 변환하는 중 문제가 발생했습니다."),

    // pet
    PET_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "PET001", "사용자의 펫을 조회할 수 없습니다."),

    // route
    INVALID_PLACE_CATEGORY(HttpStatus.BAD_REQUEST.value(), "ROUTE001", "유효한 카테고리가 아닙니다."),
    INVALID_LATITUDE(HttpStatus.BAD_REQUEST.value(), "ROUTE002", "유효한 위도가 아닙니다."),
    INVALID_LONGITUDE(HttpStatus.BAD_REQUEST.value(), "ROUTE003", "유효한 경도가 아닙니다."),
    INVALID_RADIUS(HttpStatus.BAD_REQUEST.value(), "ROUTE004", "유효한 반경이 아닙니다."),
    INVALID_PAGE_SIZE(HttpStatus.BAD_REQUEST.value(), "ROUTE005", "요청 페이지가 너무 많습니다."),
    ROUTE_LEVEL_NOT_FOUND(HttpStatus.BAD_REQUEST.value(), "ROUTE006", "루트 레벨에 대한 가이드가 존재하지 않습니다."),
    ROUTE_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "ROUTE007", "해당 루트 세션이 존재하지 않습니다."),
    ROUTE_ALREADY_COMPLETED(HttpStatus.CONFLICT.value(), "ROUTE008", "이미 해당 루트 세션이 완료되었습니다."),
    ROUTE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST.value(), "ROUTE009", "이미 진행 중인 세션입니다."),

    // room
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "ROOM001", "해당 방을 조회할 수 없습니다."),
    ROOM_EXPIRED(HttpStatus.BAD_REQUEST.value(), "ROOM002", "방의 세션이 만료되었습니다."),
    ROOM_FULL(HttpStatus.BAD_REQUEST.value(), "ROOM003", "방의 인원이 가득 찼습니다."),
    ALREADY_ENTERED(HttpStatus.BAD_REQUEST.value(), "ROOM004", "이미 입장한 방입니다."),
    NOT_PARTICIPANT(HttpStatus.BAD_REQUEST.value(), "ROOM005", "입장한 적 없는 유저입니다."),

    // message
    FAILED_SEND_MESSAGE(HttpStatus.INTERNAL_SERVER_ERROR.value(), "MESSAGE001", "메시지 보내기에 실패했습니다."),

    // kafka
    KAFKA_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR.value(), "KAFKA001", "Kafka에서 message를 보내는 데에 실패하였습니다."),

    // s3
    EMPTY_FILE_UPLOAD(HttpStatus.BAD_REQUEST.value(), "S3001", "비어 있는 파일입니다."),
    FILE_UPLOAD_FAIL(HttpStatus.INTERNAL_SERVER_ERROR.value(), "S3002", "파일 업로드 과정에서 문제가 발생했습니다."),

    // room
    TROUBLE_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "ROOM001", "trouble을 찾을 수 없습니다."),

    // solution
    SOLUTION_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "SOLUTION001", "해당 ID(PK)를 찾을 수 없습니다."),

    // 5xx
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_SERVER_ERROR", "서버 에러입니다."),
    ;

    private final int httpStatus;
    private final String code;
    private final String message;

    @Override
    public int getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    ErrorCode(int httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
