package com.a508.onestep.global.client.genai;

import com.a508.onestep.domain.room.entity.RoomMessage;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Component
public class GenAiClient {

    private final GoogleGenAiChatModel chatModel;
    private final Executor taskExecutor;

    public GenAiClient(GoogleGenAiChatModel chatModel,
                       @Qualifier("taskExecutor") Executor taskExecutor) {
        this.chatModel = chatModel;
        this.taskExecutor = taskExecutor;
    }

    public CompletableFuture<String> summarizeMessages(List<RoomMessage> messages, String topic) {
        if (messages == null || messages.isEmpty()) {
            return CompletableFuture.completedFuture("대화 내용이 없습니다.");
        }

        return CompletableFuture.supplyAsync(() -> {
            String conversation = buildConversation(messages);
            String prompt = buildPrompt(topic, conversation);
            return chatModel.call(prompt);
        }, taskExecutor);
    }

    public CompletableFuture<String> summarizeFormattedMessages(List<String> formattedMessages, String topic) {
        if (formattedMessages == null || formattedMessages.isEmpty()) {
            return CompletableFuture.completedFuture("대화 내용이 없습니다.");
        }

        return CompletableFuture.supplyAsync(() -> {
            String conversation = String.join("\n", formattedMessages);
            String prompt = buildPrompt(topic, conversation);
            return chatModel.call(prompt);
        }, taskExecutor);
    }

    /**
     * 채팅 메시지 목록을 프롬프트에 사용할 대화 문자열로 변환
     */
    private String buildConversation(List<RoomMessage> messages) {
        return messages.stream()
                .map(msg -> msg.getSenderCode() + ": " + msg.getContent())
                .collect(Collectors.joining("\n"));
    }

    private String buildPrompt(String topic, String conversation) {
        return String.format("""
            # Role
            너는 집단 채팅 상담 로그를 근거로, 고민자에 대한 토론의 핵심을 정리하고 정서적인 위로를 전하는 역할이다.
            모든 결론과 위로는 반드시 채팅 내용에서 도출되어야 하며,
            채팅에 없는 생각이나 주제와 관련 없는 내용을 추가해서는 안 된다.
    
            # Input Format Info
            - 채팅 로그는 다음 형식으로 제공된다:
              [USERCODE: 발언 내용]
            - 각 USERCODE는 서로 다른 참여자를 의미한다.
    
            # Task
            주제 '%s'에 대해 주어진 채팅 대화를 분석하여,
            대화 속에서 드러난 고민의 핵심과 감정 흐름을 바탕으로 결론과 위로를 작성해.
    
            # Writing Rules
            - 전체는 4~6문장, 정확히 2문단으로 작성할 것
            - 문단 사이에는 줄바꿈 한 번만 사용할 것
            - 마크다운 형식 사용 금지 (제목, 목록, 기호, 강조 표현 등 일절 사용하지 말 것)
    
            [1문단]
            - 1~3문장
            - 채팅 전반에서 도출된 핵심 내용과 공감 내용
            - 여러 USERCODE의 발언에서 공통적으로 반복된 반응이나 시각을 요약
    
            [2문단]
            - 1~3문장
            - 채팅에서 드러난 고민 당사자의 감정을 짚어 공감
            - 대화 맥락에 맞는 현실적이고 과장되지 않은 위로로 마무리
    
            공통 규칙
            - 반드시 채팅에 실제로 등장한 고민, 감정, 의견만 사용할 것
            - 반복되거나 다수에게 공감받은 관점을 우선 반영할 것
            - 불필요한 USERCODE의 말은 무시할 것
            - 훈계조, 비난, 단정 금지
            - 일반론적 조언 및 희망 고문식 위로 금지
            - USERCODE, 발언자, 특정 인물 직접 언급 금지
            - 진부한 위로 문장 사용 금지
    
            # 예시1
            계속되는 불합격 통보로 위축되고 많이 힘들다는 고민에 많은 사람들이 공감하고 있어요. 이 어려움이 결코 혼자만의 문제가 아니라는 점을 모두가 이야기해 주고 있고, 자신도 한때 같은 시간을 겪었다는 사람들도 있어요.
            
            경험이 쌓이다 보면 언젠가 터지듯, 당신의 미래 역시 분명 밝게 열릴 거예요. 너무 스스로를 몰아붙이지 말고, 지금 이 시간을 성장의 과정이라고 생각하며 하루하루 잘 지내보세요.
    
            # 예시2
            남들의 화려한 스펙과 열심히 살아가는 모습에 위축되고 압박감을 느낀다는 고민에 많은 사람들이 공감하고 있어요. 누군가는 취업에는 운의 요소도 분명 작용한다는 의견과 함께, 꾸준히 성장을 이어간다면 이력서도 충분히 채워질 수 있을 거라는 격려를 전하고 있어요.

            지금은 남들과 비교하며 많이 위축되고 이력서가 텅 비어 보일 수 있지만, 이런 고민은 결코 당신만의 것이 아니에요. 자신만의 속도로 꾸준한 성장세를 만들어 간다면, 분명 이력서도 차곡차곡 채워질 것이고 좋은 기회도 찾아올 거예요.
            
            # Chat Log
            %s
    
            # Output
        """, topic, conversation);
    }

}