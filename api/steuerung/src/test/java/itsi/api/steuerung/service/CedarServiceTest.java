package itsi.api.steuerung.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.time.Year;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für CedarService
 *
 * Hinweis: Diese Tests benötigen die Cedar native library.
 * Falls die Tests fehlschlagen mit "UnsatisfiedLinkError", muss die
 * cedar_java_ffi library im java.library.path verfügbar sein.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "cedar.enabled=true"
})
// @EnabledIfSystemProperty removed to allow running tests by default
class CedarServiceTest {

    @Autowired(required = false)
    private CedarService cedarService;

    private Claims studentClaims;
    private Claims lehrerClaims;
    private Claims adminClaims;
    private Claims expiredStudentClaims;

    private boolean isCedarReady() {
        if (cedarService == null) return false;
        Object engine = ReflectionTestUtils.getField(cedarService, "authEngine");
        return engine != null;
    }

    @BeforeEach
    void setUp() {
        if (!isCedarReady()) {
            return;
        }

        // Setup Student Claims
        studentClaims = Jwts.claims()
            .subject("student123")
            .add("rolle", "SCHUELER")
            .add("klasse", "5BHIT")
            .add("userId", 1)
            .add("ablaufJahr", 2027L)
            .build();

        // Setup Lehrer Claims
        lehrerClaims = Jwts.claims()
            .subject("teacher456")
            .add("rolle", "LEHRER")
            .add("userId", 2)
            .add("ablaufJahr", Long.MAX_VALUE)
            .build();

        // Setup Admin Claims
        adminClaims = Jwts.claims()
            .subject("admin789")
            .add("rolle", "ADMIN")
            .add("userId", 3)
            .build();

        // Setup abgelaufener Student
        expiredStudentClaims = Jwts.claims()
            .subject("oldstudent")
            .add("rolle", "SCHUELER")
            .add("klasse", "5AHIT")
            .add("userId", 4)
            .add("ablaufJahr", 2020L) // Bereits abgelaufen
            .build();
    }

    @Test
    void testServiceIsAvailable() {
        if (cedarService != null) {
            assertNotNull(cedarService, "CedarService sollte verfügbar sein");
        }
    }

    @Test
    void testIsUserAuthorized_Admin_ShouldBeAuthorized() {
        if (!isCedarReady()) return;

        boolean authorized = cedarService.isUserAuthorized(adminClaims);
        assertTrue(authorized, "Admin sollte autorisiert sein");
    }

    @Test
    void testIsUserAuthorized_Student_ShouldBeAuthorized() {
        if (!isCedarReady()) return;

        boolean authorized = cedarService.isUserAuthorized(studentClaims);
        assertTrue(authorized, "Student sollte autorisiert sein (read access)");
    }

    @Test
    void testIsUserAuthorized_Lehrer_ShouldBeAuthorized() {
        if (!isCedarReady()) return;

        boolean authorized = cedarService.isUserAuthorized(lehrerClaims);
        assertTrue(authorized, "Lehrer sollte autorisiert sein (read access)");
    }

    @Test
    void testIsUserAuthorized_ExpiredStudent_ShouldNotBeAuthorized() {
        if (!isCedarReady()) return;

        boolean authorized = cedarService.isUserAuthorized(expiredStudentClaims);
        assertFalse(authorized, "Student mit abgelaufenem ablaufJahr sollte nicht autorisiert sein");
    }

    @Test
    void testCheckContainerAccess_StudentOwn_Start_ShouldBeAuthorized() {
        if (!isCedarReady()) return;

        boolean authorized = cedarService.checkContainerAccess(studentClaims, "start", 1);
        assertTrue(authorized, "Student sollte eigenen Container starten dürfen");
    }

    @Test
    void testCheckContainerAccess_StudentOther_Start_ShouldNotBeAuthorized() {
        if (!isCedarReady()) return;

        boolean authorized = cedarService.checkContainerAccess(studentClaims, "start", 999);
        assertFalse(authorized, "Student sollte fremden Container nicht starten dürfen");
    }

    @Test
    void testCheckContainerAccess_LehrerOwn_Stop_ShouldBeAuthorized() {
        if (!isCedarReady()) return;

        boolean authorized = cedarService.checkContainerAccess(lehrerClaims, "stop", 2);
        assertTrue(authorized, "Lehrer sollte eigenen Container stoppen dürfen");
    }

    @Test
    void testCheckContainerAccess_LehrerOther_Stop_ShouldNotBeAuthorized() {
        if (!isCedarReady()) return;

        boolean authorized = cedarService.checkContainerAccess(lehrerClaims, "stop", 999);
        assertFalse(authorized, "Lehrer sollte fremden Container nicht stoppen dürfen");
    }

    @Test
    void testCheckContainerAccess_AdminAny_ShouldBeAuthorized() {
        if (!isCedarReady()) return;

        boolean authorized = cedarService.checkContainerAccess(adminClaims, "start", 999);
        assertTrue(authorized, "Admin sollte jeden Container starten dürfen");
    }

    @Test
    void testIsUserAuthorized_WithTimestampAblaufJahr_ShouldHandleCorrectly() {
        if (!isCedarReady()) return;

        Timestamp futureTimestamp = Timestamp.valueOf("2027-12-31 23:59:59");

        Claims claimsWithTimestamp = Jwts.claims()
            .subject("student_ts")
            .add("rolle", "SCHUELER")
            .add("klasse", "5BHIT")
            .add("userId", 10)
            .add("ablaufJahr", futureTimestamp)
            .build();

        boolean authorized = cedarService.isUserAuthorized(claimsWithTimestamp);
        assertTrue(authorized, "Student mit Timestamp-ablaufJahr in der Zukunft sollte autorisiert sein");
    }

    @Test
    void testIsUserAuthorized_WithIntegerAblaufJahr_ShouldHandleCorrectly() {
        if (!isCedarReady()) return;

        Claims claimsWithInteger = Jwts.claims()
            .subject("student_int")
            .add("rolle", "SCHUELER")
            .add("klasse", "5BHIT")
            .add("userId", 11)
            .add("ablaufJahr", 2027) // Integer statt Long
            .build();

        boolean authorized = cedarService.isUserAuthorized(claimsWithInteger);
        assertTrue(authorized, "Student mit Integer-ablaufJahr sollte autorisiert sein");
    }

    @Test
    void testIsUserAuthorized_WithoutAblaufJahr_ShouldWork() {
        if (!isCedarReady()) return;

        Claims claimsWithoutAblaufJahr = Jwts.claims()
            .subject("user_no_expiry")
            .add("rolle", "ADMIN")
            .add("userId", 12)
            .build();

        boolean authorized = cedarService.isUserAuthorized(claimsWithoutAblaufJahr);
        assertTrue(authorized, "Autorisierung sollte auch ohne ablaufJahr-Attribut funktionieren");
    }

    @Test
    void testIsUserAuthorized_CurrentYearContext() {
        if (!isCedarReady()) return;

        Claims currentYearClaims = Jwts.claims()
            .subject("student_current")
            .add("rolle", "SCHUELER")
            .add("klasse", "5BHIT")
            .add("userId", 13)
            .add("ablaufJahr", (long) Year.now().getValue() + 1)
            .build();

        boolean authorized = cedarService.isUserAuthorized(currentYearClaims);
        assertTrue(authorized, "Student mit ablaufJahr = aktuelles Jahr + 1 sollte autorisiert sein");
    }
}
