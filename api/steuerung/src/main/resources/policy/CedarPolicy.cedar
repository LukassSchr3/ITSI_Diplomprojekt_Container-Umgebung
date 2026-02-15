// ====================================
// Cedar ABAC Policy für Schüler/Lehrer/Admin System
// ====================================

// ADMIN: Kann alles
permit(
    principal,
    action,
    resource
)
when {
    principal.rolle == "ADMIN"
};

// LEHRER: Kann Aufgaben bewerten (grade)
permit(
    principal,
    action == Action::"grade",
    resource
)
when {
    principal.rolle == "LEHRER"
};

// LEHRER: Kann schreiben (write)
permit(
    principal,
    action == Action::"write",
    resource
)
when {
    principal.rolle == "LEHRER"
};

// LEHRER & SCHÜLER: Können lesen (read)
permit(
    principal,
    action == Action::"read",
    resource
)
when {
    principal.rolle == "LEHRER" || principal.rolle == "SCHUELER"
};

// DENY: Abgelaufene Accounts
forbid(
    principal,
    action,
    resource
)
when {
    principal.ablaufJahr < context.currentYear
};

