// Admin roles can perform any action on any resource
permit(
    principal,
    action,
    resource
)
when {
    principal.rolle == "ADMIN"
};

// Teachers can grade tasks
permit(
    principal,
    action == Action::"grade",
    resource
)
when {
    principal.rolle == "LEHRER"
};

// Teachers and Students can read
permit(
    principal,
    action == Action::"read",
    resource
)
when {
    principal.rolle == "LEHRER" || principal.rolle == "SCHUELER"
};

// Deny access if the user account is expired
forbid(
    principal,
    action,
    resource
)
when {
    principal.ablaufJahr < context.currentYear
};