package za.co.capitec.transactiondispute.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {

    private static final String BASE = "za.co.capitec.transactiondispute";
    private static final String ROOT = BASE + "..";

    private JavaClasses importedClasses() {
        return new ClassFileImporter()
                .importPackages(BASE);
    }

    @Test
    void application_ports_should_be_interfaces_only() {
        ArchRule rule = classes()
                .that().resideInAPackage(ROOT + "application.port..")
                .should().beInterfaces()
                .allowEmptyShould(true);

        rule.check(importedClasses());
    }

    @Test
    void controllers_should_reside_in_interfaces_http() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .should().resideInAPackage("..interfaces.http..")
                .allowEmptyShould(true);

        rule.check(importedClasses());
    }

    @Test
    void domain_should_not_depend_on_frameworks() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(ROOT + "domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "reactor..",
                        "jakarta.persistence..",
                        "org.hibernate.."
                )
                .allowEmptyShould(true);

        rule.check(importedClasses());
    }

    @Test
    void modules_should_not_depend_on_other_modules() {
        // disputes must not depend on other modules
        noClasses()
                .that().resideInAPackage(BASE + ".disputes..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        BASE + ".transactions..",
                        BASE + ".security..",
                        BASE + ".notifications.."
                )
                .allowEmptyShould(true)
                .check(importedClasses());

        // transactions must not depend on other modules
        noClasses()
                .that().resideInAPackage(BASE + ".transactions..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        BASE + ".disputes..",
                        BASE + ".security..",
                        BASE + ".notifications.."
                )
                .allowEmptyShould(true)
                .check(importedClasses());

        // security must not depend on other modules
        noClasses()
                .that().resideInAPackage(BASE + ".security..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        BASE + ".disputes..",
                        BASE + ".transactions..",
                        BASE + ".notifications.."
                )
                .allowEmptyShould(true)
                .check(importedClasses());

        // notifications must not depend on other modules
        noClasses()
                .that().resideInAPackage(BASE + ".notifications..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        BASE + ".disputes..",
                        BASE + ".transactions..",
                        BASE + ".security.."
                )
                .allowEmptyShould(true)
                .check(importedClasses());
    }
}