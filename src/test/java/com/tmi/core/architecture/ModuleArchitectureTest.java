package com.tmi.core.architecture;

import static com.tmi.core.architecture.ArchitectureClasses.PRODUCTION;
import static com.tmi.core.architecture.ArchitectureClasses.ROOT;
import static com.tmi.core.architecture.ArchitectureClasses.SHARED_KERNEL;
import static com.tmi.core.architecture.ArchitectureClasses.isModuleApi;
import static com.tmi.core.architecture.ArchitectureClasses.moduleOf;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 모듈 경계 규칙 R1~R3 (docs/ai/architecture-proposal.md §5). */
class ModuleArchitectureTest {

    // 위반 1건: common ↔ infra 순환 — common.config.RestClientConfig → infra.similarity.SimilarityClient,
    //          infra.similarity.exception.SimilarityException → common.exception.BaseException.
    // 해소 작업: Similarity 빈 설정을 common 밖으로 이동 (feature_list ARCH-A1). 해소 후 @Disabled 제거.
    @Disabled("R1 위반 1건(common↔infra 순환) — ARCH-A1 완료 시 해제. docs/ai/current-state.md §6")
    @Test
    @DisplayName("R1 모듈 사이 순환 의존이 없다")
    void r1_modulesAreFreeOfCycles() {
        ArchRule rule =
                slices().matching(ROOT + ".(*)..").namingSlices("$1").should().beFreeOfCycles();
        rule.check(PRODUCTION);
    }

    // 위반 19건, 2개 클래스: answer.service.AnswerService → sentence.repository / sentence.domain / sentence.exception /
    //          infra.similarity(.dto, .exception), answer.dto.AnswerResponse → sentence.domain.SentenceEntity.
    // 해소 작업: similarity 모듈 승격 + SentenceReader/SentenceDetail 도입 (D1=B: ARCH-B2, ARCH-B3 / D1=A′: ARCH-A2 에서 규칙 교체).
    @Disabled(
            "R2 위반 19건(answer → sentence·infra 내부 패키지) — ARCH-B3(B) 완료 시 해제, A′ 면 ARCH-A2 에서 규칙 교체. docs/ai/current-state.md §6")
    @Test
    @DisplayName("R2 다른 모듈은 base package(공개 API)로만 접근한다 — common 은 예외")
    void r2_otherModulesAreAccessedOnlyThroughTheirApi() {
        ArchRule rule = classes()
                .that()
                .resideInAPackage(ROOT + "..")
                .should(onlyAccessOtherModulesThroughApi())
                .as("다른 모듈의 하위(internal) 패키지에 의존하지 않는다");
        rule.check(PRODUCTION);
    }

    // 위반 2건: common.config.RestClientConfig.similarityClient() → infra.similarity.SimilarityClient (반환 타입, 클래스 참조).
    // 해소 작업: ARCH-A1. 해소 후 @Disabled 제거.
    @Disabled("R3 위반 2건(RestClientConfig → infra.similarity) — ARCH-A1 완료 시 해제. docs/ai/current-state.md §6")
    @Test
    @DisplayName("R3 common 은 어떤 기능 모듈에도 의존하지 않는다")
    void r3_commonDoesNotDependOnFeatureModules() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage(ROOT + "." + SHARED_KERNEL + "..")
                .should()
                .dependOnClassesThat(com.tngtech.archunit.base.DescribedPredicate.describe(
                        "기능 모듈 클래스",
                        (JavaClass target) -> moduleOf(target)
                                .filter(module -> !module.equals(SHARED_KERNEL))
                                .isPresent()));
        rule.check(PRODUCTION);
    }

    private static ArchCondition<JavaClass> onlyAccessOtherModulesThroughApi() {
        return new ArchCondition<>("only access other modules through their base package") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                Optional<String> originModule = moduleOf(origin);
                if (originModule.isEmpty() || originModule.get().equals(SHARED_KERNEL)) {
                    return; // 루트(CoreApplication) 제외, common 출발 의존은 R3 가 검사 (중복 보고 방지)
                }
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    Optional<String> targetModule = moduleOf(target);
                    boolean crossesModule = targetModule.isPresent()
                            && !targetModule.get().equals(originModule.get())
                            && !targetModule.get().equals(SHARED_KERNEL);
                    if (crossesModule && !isModuleApi(target)) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }
}
