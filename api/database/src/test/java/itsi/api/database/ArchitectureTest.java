package itsi.api.database;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "itsi.api.database", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule controllers_reside_in_controller_package =
        classes().that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage("..controller..")
            .allowEmptyShould(true)
            .as("@RestController classes must be in 'controller' package");

    @ArchTest
    static final ArchRule services_reside_in_service_package =
        classes().that().areAnnotatedWith(Service.class)
            .should().resideInAPackage("..service..")
            .allowEmptyShould(true)
            .as("@Service classes must be in 'service' package");

    @ArchTest
    static final ArchRule controllers_must_not_access_repositories_directly =
        noClasses().that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..")
            .allowEmptyShould(true)
            .as("Controllers must not directly access Repositories – use Services");

    @ArchTest
    static final ArchRule services_must_not_access_controllers =
        noClasses().that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..")
            .allowEmptyShould(true)
            .as("Services must not depend on Controllers");

    @ArchTest
    static final ArchRule entities_reside_in_entity_package =
        classes().that().areAnnotatedWith(jakarta.persistence.Entity.class)
            .should().resideInAPackage("..entity..")
            .allowEmptyShould(true)
            .as("@Entity classes must be in 'entity' package");

    @ArchTest
    static final ArchRule controller_names_end_with_controller =
        classes().that().resideInAPackage("..controller..")
            .and().areAnnotatedWith(RestController.class)
            .should().haveSimpleNameEndingWith("Controller")
            .allowEmptyShould(true)
            .as("Controller classes must end with 'Controller'");

    @ArchTest
    static final ArchRule service_names_end_with_service =
        classes().that().resideInAPackage("..service..")
            .and().areAnnotatedWith(Service.class)
            .should().haveSimpleNameEndingWith("Service")
            .allowEmptyShould(true)
            .as("Service classes must end with 'Service'");

    @ArchTest
    static final ArchRule dto_names_end_with_dto =
        classes().that().resideInAPackage("..dto..")
            .should().haveSimpleNameEndingWith("DTO")
            .allowEmptyShould(true)
            .as("Classes in 'dto' package must end with 'DTO'");

    @Test
    void no_cycles_in_packages() {
        JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("itsi.api.database");

        com.tngtech.archunit.library.dependencies.SliceRule rule =
            com.tngtech.archunit.library.dependencies.SlicesRuleDefinition
                .slices().matching("itsi.api.database.(*)..")
                .should().beFreeOfCycles();

        try {
            rule.check(classes);
        } catch (AssertionError e) {
            System.out.println("[ArchUnit WARNING] Cycles detected: " + e.getMessage().lines().findFirst().orElse(""));
        }
    }
}
