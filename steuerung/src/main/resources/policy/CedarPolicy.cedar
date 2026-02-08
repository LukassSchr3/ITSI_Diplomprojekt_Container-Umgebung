// Lehrer dürfen alle Schüler ihrer Klasse sehen
permit(
    principal is User,
    action == Action::"viewStudent",
    resource is Student
) when {
    principal.rolle == "LEHRER" &&
    principal.klasse == resource.klasse
};

// Admin darf alles
permit(
    principal is User,
    action,
    resource
) when {
    principal.rolle == "ADMIN"
};

// Schüler dürfen nur ihre eigenen Daten sehen
permit(
    principal is User,
    action == Action::"viewStudent",
    resource is Student
) when {
    principal.rolle == "SCHUELER" &&
    principal.username == resource.username
};

// Zugriff abgelaufen
forbid(
    principal is User,
    action,
    resource
) when {
    principal.ablaufJahr < context.currentYear
};