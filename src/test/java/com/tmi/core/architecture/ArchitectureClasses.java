package com.tmi.core.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.Optional;

/**
 * 아키텍처 테스트 공용 — 운영 코드(src/main)만 한 번 import 해서 재사용한다.
 * 모듈 = {@code com.tmi.core} 의 직속 하위 패키지 (docs/ai/proposals/architecture-proposal.md 선택지 B).
 */
final class ArchitectureClasses {

    static final String ROOT = "com.tmi.core";
    static final String SHARED_KERNEL = "common";

    static final JavaClasses PRODUCTION = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    private ArchitectureClasses() {}

    /** {@code com.tmi.core.sentence.repository} → {@code sentence}. 루트 패키지 자체는 empty. */
    static Optional<String> moduleOf(String packageName) {
        if (!packageName.startsWith(ROOT + ".")) {
            return Optional.empty();
        }
        String rest = packageName.substring(ROOT.length() + 1);
        int dot = rest.indexOf('.');
        return Optional.of(dot < 0 ? rest : rest.substring(0, dot));
    }

    static Optional<String> moduleOf(JavaClass javaClass) {
        return moduleOf(javaClass.getPackageName());
    }

    /** 모듈의 base package 에 있으면 공개 API (선택지 B 규칙). */
    static boolean isModuleApi(JavaClass javaClass) {
        return moduleOf(javaClass)
                .map(module -> javaClass.getPackageName().equals(ROOT + "." + module))
                .orElse(false);
    }
}
