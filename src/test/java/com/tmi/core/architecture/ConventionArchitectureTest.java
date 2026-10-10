package com.tmi.core.architecture;

import static com.tmi.core.architecture.ArchitectureClasses.PRODUCTION;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.util.UriBuilderFactory;

/** 기계 검증 가능한 컨벤션 R5~R7 (docs/ai/architecture-proposal.md §5, conventions-proposal.md). */
class ConventionArchitectureTest {

    private static final List<Class<? extends Annotation>> HTTP_BINDING_ANNOTATIONS = List.of(
            RequestBody.class,
            RequestParam.class,
            PathVariable.class,
            RequestHeader.class,
            RequestPart.class,
            CookieValue.class,
            RequestAttribute.class);

    /** HTTP Interface 가 어노테이션 없이 해석하는 특수 타입 (estimated — HttpServiceProxyFactory 기본 resolver 기준). */
    private static final List<Class<?>> HTTP_SPECIAL_PARAMETER_TYPES =
            List.of(URI.class, HttpMethod.class, UriBuilderFactory.class);

    // 위반 1건: sentence.service.SentenceService.getRandom(Level, Category) 가 SentenceEntity 반환.
    // 해소 작업: 서비스가 응답 DTO 반환 (conventions-proposal.md §3.1, feature_list FIX-R5 — 구조 결정과 무관).
    @Disabled("R5 위반 1건(SentenceService.getRandom → SentenceEntity) — FIX-R5 완료 시 해제. docs/ai/current-state.md §6")
    @Test
    @DisplayName("R5 @Service 의 public 메서드는 @Entity 를 반환하지 않는다")
    void r5_servicesDoNotReturnEntities() {
        ArchRule rule = methods()
                .that()
                .arePublic()
                .and()
                .areDeclaredInClassesThat()
                .areAnnotatedWith(Service.class)
                .should(notReturnEntity());
        rule.check(PRODUCTION);
    }

    // 위반 1건: answer.service.AnswerService.score → loadSentence (AnswerService.java:28). @Transactional 이 무효.
    // 해소 작업: feature_list FIX-F4.
    @Disabled("R6 위반 1건(AnswerService.score → loadSentence) — FIX-F4 완료 시 해제. docs/ai/current-state.md §6")
    @Test
    @DisplayName("R6 @Transactional 메서드를 같은 클래스 안에서 호출하지 않는다 (self-invocation)")
    void r6_transactionalMethodsAreNotSelfInvoked() {
        ArchRule rule = classes().should(notSelfInvokeTransactionalMethods());
        rule.check(PRODUCTION);
    }

    // 위반 1건: infra.similarity.SimilarityClient.score(SimilarityRequest) — @RequestBody 누락 → 호출 시 항상
    // IllegalStateException.
    // 해소 작업: feature_list FIX-F1.
    @Disabled("R7 위반 1건(SimilarityClient.score @RequestBody 누락) — FIX-F1 완료 시 해제. docs/ai/current-state.md §6")
    @Test
    @DisplayName("R7 HTTP Interface 메서드의 파라미터에는 바인딩 어노테이션이 있다")
    void r7_httpInterfaceParametersAreBound() {
        ArchRule rule = methods()
                .that()
                .areMetaAnnotatedWith(HttpExchange.class)
                .or()
                .areAnnotatedWith(HttpExchange.class)
                .should(haveBoundParameters());
        rule.check(PRODUCTION);
    }

    private static ArchCondition<JavaMethod> notReturnEntity() {
        return new ArchCondition<>("not return an @Entity type (including generic arguments)") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                // Optional<Entity>, List<Entity>, Page<Entity> 의 제네릭 인자까지 검사
                method.getReturnType().getAllInvolvedRawTypes().stream()
                        .filter(type -> type.isAnnotatedWith(Entity.class))
                        .forEach(type -> events.add(SimpleConditionEvent.violated(
                                method, method.getFullName() + " returns @Entity " + type.getSimpleName())));
            }
        };
    }

    // 한계: 메서드 레벨 @Transactional 만 본다. 클래스 레벨 @Transactional 클래스 안의 self-invocation 은
    // 바깥 메서드도 같은 트랜잭션 설정을 받으므로 대부분 무해하다고 보고 검사하지 않는다.
    private static ArchCondition<JavaClass> notSelfInvokeTransactionalMethods() {
        return new ArchCondition<>("not call own @Transactional methods") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (JavaMethodCall call : javaClass.getMethodCallsFromSelf()) {
                    if (!call.getTargetOwner().equals(javaClass)) {
                        continue;
                    }
                    Optional<JavaMethod> target = call.getTarget().resolveMember();
                    if (target.isPresent() && target.get().isAnnotatedWith(Transactional.class)) {
                        events.add(SimpleConditionEvent.violated(call, call.getDescription()));
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaMethod> haveBoundParameters() {
        return new ArchCondition<>("annotate every parameter with an HTTP binding annotation") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                for (JavaParameter parameter : method.getParameters()) {
                    boolean bound = HTTP_BINDING_ANNOTATIONS.stream().anyMatch(parameter::isAnnotatedWith);
                    boolean special = HTTP_SPECIAL_PARAMETER_TYPES.stream()
                            .anyMatch(type -> parameter.getRawType().isAssignableTo(type));
                    if (!bound && !special) {
                        events.add(SimpleConditionEvent.violated(
                                method,
                                method.getFullName() + " parameter[" + parameter.getIndex() + "] "
                                        + parameter.getRawType().getSimpleName() + " has no binding annotation"));
                    }
                }
            }
        };
    }
}
