package com.a508.onestep.domain.room.service;

import com.a508.onestep.domain.room.dto.request.TroubleCreateRequestDto;
import com.a508.onestep.domain.room.dto.response.SolutionResponseDto;
import com.a508.onestep.domain.room.entity.Solution;
import com.a508.onestep.domain.room.entity.Trouble;
import com.a508.onestep.domain.room.repository.SolutionRepository;
import com.a508.onestep.domain.room.repository.TroubleRepository;
import com.a508.onestep.domain.user.repository.UserRepository;
import com.a508.onestep.global.auth.context.UserContext;
import com.a508.onestep.global.auth.context.UserContextHolder;
import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.response.ErrorCode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TroubleServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private SolutionRepository solutionRepository;
    @Mock private TroubleRepository troubleRepository;
    @InjectMocks private TroubleServiceImpl troubleService;

    private String userCode;
    private List<Solution> solutionList;

    @BeforeEach
    void setUp() {
        userCode = "USER001";
        UserContext userContext = UserContext.builder()
                .userCode(userCode)
                .build();
        UserContextHolder.set(userContext);

        solutionList = List.of(
                Solution.builder()
                        .id(1L)
                        .summary("Solution 1")
                        .build(),
                Solution.builder()
                        .id(2L)
                        .summary("Solution 2")
                        .build()
        );
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Nested
    @DisplayName("createTrouble 테스트")
    class CreateTroubleTest {

        @Test
        @DisplayName("Trouble 생성 성공")
        void createTrouble_Success() {
            // given
            String content = "테스트 Trouble 내용";
            TroubleCreateRequestDto requestDto = new TroubleCreateRequestDto(content);

            Trouble savedTrouble = Trouble.builder()
                    .id(1L)
                    .content(content)
                    .userCode(userCode)
                    .build();

            given(userRepository.existsByUserCode(userCode))
                    .willReturn(true);
            given(troubleRepository.save(any(Trouble.class)))
                    .willReturn(savedTrouble);

            // when
            Long troubleId = troubleService.createTrouble(requestDto);

            // then
            assertThat(troubleId).isEqualTo(1L);

            // ArgumentCaptor로 저장된 Trouble 검증
            ArgumentCaptor<Trouble> troubleCaptor = ArgumentCaptor.forClass(Trouble.class);
            verify(troubleRepository).save(troubleCaptor.capture());

            Trouble capturedTrouble = troubleCaptor.getValue();
            assertThat(capturedTrouble.getContent()).isEqualTo(content);
            assertThat(capturedTrouble.getUserCode()).isEqualTo(userCode);
        }

        @Test
        @DisplayName("사용자가 존재하지 않을 때 예외 발생")
        void createTrouble_UserNotFound() {
            // given
            TroubleCreateRequestDto requestDto = new TroubleCreateRequestDto("내용");

            given(userRepository.existsByUserCode(userCode))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> troubleService.createTrouble(requestDto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("baseCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);

            verify(troubleRepository, never()).save(any());
        }

        @Test
        @DisplayName("UserContextHolder에서 올바른 userCode 사용")
        void createTrouble_UseCorrectUserCode() {
            // given
            String expectedUserCode = "USER999";
            UserContext newContext = UserContext.builder()
                    .userCode(expectedUserCode)
                    .build();
            UserContextHolder.set(newContext);

            TroubleCreateRequestDto requestDto = new TroubleCreateRequestDto("내용");

            Trouble savedTrouble = Trouble.builder()
                    .id(1L)
                    .content("내용")
                    .userCode(expectedUserCode)
                    .build();

            given(userRepository.existsByUserCode(expectedUserCode))
                    .willReturn(true);
            given(troubleRepository.save(any(Trouble.class)))
                    .willReturn(savedTrouble);

            // when
            troubleService.createTrouble(requestDto);

            // then
            verify(userRepository).existsByUserCode(expectedUserCode);

            ArgumentCaptor<Trouble> troubleCaptor = ArgumentCaptor.forClass(Trouble.class);
            verify(troubleRepository).save(troubleCaptor.capture());
            assertThat(troubleCaptor.getValue().getUserCode()).isEqualTo(expectedUserCode);
        }

        @Test
        @DisplayName("빈 content로 Trouble 생성")
        void createTrouble_EmptyContent() {
            // given
            TroubleCreateRequestDto requestDto = new TroubleCreateRequestDto("");

            Trouble savedTrouble = Trouble.builder()
                    .id(1L)
                    .content("")
                    .userCode(userCode)
                    .build();

            given(userRepository.existsByUserCode(userCode))
                    .willReturn(true);
            given(troubleRepository.save(any(Trouble.class)))
                    .willReturn(savedTrouble);

            // when
            Long troubleId = troubleService.createTrouble(requestDto);

            // then
            assertThat(troubleId).isEqualTo(1L);
            verify(troubleRepository).save(any(Trouble.class));
        }
    }

    @Nested
    @DisplayName("getAllSolution 테스트")
    class GetAllSolutionTest {

        @Test
        @DisplayName("모든 Solution 조회 성공")
        void getAllSolution_Success() {
            // given
            given(userRepository.existsByUserCode(userCode))
                    .willReturn(true);
            given(solutionRepository.findSolutionsByUserCode(userCode))
                    .willReturn(solutionList);

            // when
            List<SolutionResponseDto> result = troubleService.getAllSolution();

            // then
            assertThat(result).hasSize(2);
            assertThat(result)
                    .extracting("solutionId")
                    .containsExactly(1L, 2L);

            verify(userRepository).existsByUserCode(userCode);
            verify(solutionRepository).findSolutionsByUserCode(userCode);
        }

        @Test
        @DisplayName("Solution이 없을 때 빈 리스트 반환")
        void getAllSolution_EmptyList() {
            // given
            given(userRepository.existsByUserCode(userCode))
                    .willReturn(true);
            given(solutionRepository.findSolutionsByUserCode(userCode))
                    .willReturn(List.of());

            // when
            List<SolutionResponseDto> result = troubleService.getAllSolution();

            // then
            assertThat(result).isEmpty();
            verify(userRepository).existsByUserCode(userCode);
            verify(solutionRepository).findSolutionsByUserCode(userCode);
        }

        @Test
        @DisplayName("사용자가 존재하지 않을 때 예외 발생")
        void getAllSolution_UserNotFound() {
            // given
            given(userRepository.existsByUserCode(userCode))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> troubleService.getAllSolution())
                    .isInstanceOf(BusinessException.class)
                    .extracting("baseCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);

            verify(solutionRepository, never()).findSolutionsByUserCode(anyString());
        }

        @Test
        @DisplayName("여러 Solution이 올바른 순서로 반환")
        void getAllSolution_MultipleInOrder() {
            // given
            List<Solution> multipleSolutions = List.of(
                    Solution.builder().id(1L).summary("First").build(),
                    Solution.builder().id(2L).summary("Second").build(),
                    Solution.builder().id(3L).summary("Third").build()
            );

            given(userRepository.existsByUserCode(userCode))
                    .willReturn(true);
            given(solutionRepository.findSolutionsByUserCode(userCode))
                    .willReturn(multipleSolutions);

            // when
            List<SolutionResponseDto> result = troubleService.getAllSolution();

            // then
            assertThat(result).hasSize(3);
            assertThat(result)
                    .extracting("solutionId")
                    .containsExactly(1L, 2L, 3L);
        }
    }

    @Nested
    @DisplayName("getSolution 테스트")
    class GetSolutionTest {

        @Test
        @DisplayName("Solution 상세 조회 성공")
        void getSolution_Success() {
            // given
            Long solutionId = 1L;
            Solution solution = spy(Solution.builder()
                    .id(solutionId)
                    .summary("Solution 내용")
                    .build());

            given(userRepository.existsByUserCode(userCode))
                    .willReturn(true);
            given(solutionRepository.findById(solutionId))
                    .willReturn(Optional.of(solution));

            // when
            SolutionResponseDto result = troubleService.getSolution(solutionId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getSolutionId()).isEqualTo(solutionId);

            verify(userRepository).existsByUserCode(userCode);
            verify(solutionRepository).findById(solutionId);
            verify(solution).updateReadAt(); // getSolution은 updateReadAt을 호출
        }

        @Test
        @DisplayName("사용자가 존재하지 않을 때 예외 발생")
        void getSolution_UserNotFound() {
            // given
            Long solutionId = 1L;

            given(userRepository.existsByUserCode(userCode))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> troubleService.getSolution(solutionId))
                    .isInstanceOf(BusinessException.class)
                    .extracting("baseCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);

            verify(solutionRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("Solution이 존재하지 않을 때 예외 발생")
        void getSolution_SolutionNotFound() {
            // given
            Long solutionId = 999L;

            given(userRepository.existsByUserCode(userCode))
                    .willReturn(true);
            given(solutionRepository.findById(solutionId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> troubleService.getSolution(solutionId))
                    .isInstanceOf(BusinessException.class)
                    .extracting("baseCode")
                    .isEqualTo(ErrorCode.SOLUTION_NOT_FOUND);

            verify(solutionRepository).findById(solutionId);
        }

        @Test
        @DisplayName("여러 Solution ID로 조회 테스트")
        void getSolution_MultipleSolutions() {
            // given
            List<Long> solutionIds = List.of(1L, 2L, 3L);

            given(userRepository.existsByUserCode(userCode))
                    .willReturn(true);

            for (Long id : solutionIds) {
                Solution solution = Solution.builder()
                        .id(id)
                        .summary("Solution " + id)
                        .build();

                given(solutionRepository.findById(id))
                        .willReturn(Optional.of(solution));
            }

            // when & then
            for (Long id : solutionIds) {
                SolutionResponseDto result = troubleService.getSolution(id);
                assertThat(result.getSolutionId()).isEqualTo(id);
            }
        }

        @Test
        @DisplayName("updateReadAt 메서드 호출 확인")
        void getSolution_UpdateReadAtCalled() {
            // given
            Long solutionId = 1L;
            Solution solution = spy(Solution.builder()
                    .id(solutionId)
                    .summary("Test Solution")
                    .build());

            given(userRepository.existsByUserCode(userCode))
                    .willReturn(true);
            given(solutionRepository.findById(solutionId))
                    .willReturn(Optional.of(solution));

            // when
            troubleService.getSolution(solutionId);

            // then
            verify(solution, times(1)).updateReadAt();
        }
    }
}