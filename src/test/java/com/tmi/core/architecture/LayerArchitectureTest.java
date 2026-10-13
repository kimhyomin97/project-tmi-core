package com.tmi.core.architecture;

import static com.tmi.core.architecture.ArchitectureClasses.PRODUCTION;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

/** 모듈 내부 레이어 규칙 R4 (docs/ai/architecture-proposal.md §5). */
class LayerArchitectureTest {

    @Test
    @DisplayName("R4 controller → service → repository 방향만 허용")
    void r4_layersAreRespected() {
        ArchRule rule = layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .withOptionalLayers(true)
                .layer("Controller")
                .definedBy("..controller..")
                // 선택지 B 의 모듈 공개 API(SentenceReader 등 base package 의 @Service)도 서비스 레이어로 본다
                .layer("Service")
                .definedBy(resideInAPackage("..service..").or(annotatedWith(Service.class)))
                .layer("Repository")
                .definedBy("..repository..")
                .whereLayer("Controller")
                .mayNotBeAccessedByAnyLayer()
                .whereLayer("Service")
                .mayOnlyBeAccessedByLayers("Controller", "Service")
                .whereLayer("Repository")
                .mayOnlyBeAccessedByLayers("Service");
        rule.check(PRODUCTION);
    }

    @Test
    @DisplayName("R4b domain 은 controller·service·repository·dto 에 의존하지 않는다")
    void r4b_domainDoesNotDependOnOuterLayers() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..controller..", "..service..", "..repository..", "..dto..");
        rule.check(PRODUCTION);
    }
}
