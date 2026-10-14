package eu.hohenegger.accesscontrol.permission;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Renders the policy as Markdown for domain experts: no code, only the plain-language descriptions of
 * relationships and actions. {@code PermissionsDocumentTest} keeps PERMISSIONS.md in sync with it.
 */
public final class PolicyDocument {

    private PolicyDocument() {
    }

    public static String render(Policy policy) {
        List<Relationship<?>> relationships = Relationship.all().stream()
                .filter(relationship -> !policy.actionsAllowedFor(relationship).isEmpty())
                .toList();
        List<Action<?>> actions = Action.all().stream()
                .filter(action -> !policy.relationshipsAllowedTo(action).isEmpty())
                .toList();

        StringBuilder markdown = new StringBuilder();
        markdown.append("# Permissions\n\n")
                .append("<!-- Generated from the policy by PermissionsDocumentTest. Do not edit by hand: regenerate with\n")
                .append("     ./mvnw test -Dtest=PermissionsDocumentTest -Dpermissions.update -->\n\n")
                .append("Who may do what in the homeowners' portal. Anything not listed here is not allowed.\n")
                .append("A relationship only counts while it lasts: when a lease ends, a flat is sold or a delegation is\n")
                .append("revoked, the permissions that came with it end immediately.\n\n");

        markdown.append("## Overview\n\n|  | ")
                .append(relationships.stream().map(relationship -> capitalize(relationship.name())).collect(Collectors.joining(" | ")))
                .append(" |\n|---|")
                .append(relationships.stream().map(relationship -> ":---:").collect(Collectors.joining("|")))
                .append("|\n");
        for (Action<?> action : actions) {
            List<Relationship<?>> allowed = policy.relationshipsAllowedTo(action);
            markdown.append("| ").append(capitalize(action.description())).append(" | ")
                    .append(relationships.stream().map(relationship -> allowed.contains(relationship) ? "✔" : "").collect(Collectors.joining(" | ")))
                    .append(" |\n");
        }

        markdown.append("\n## Relationships\n");
        for (Relationship<?> relationship : relationships) {
            markdown.append("\n### ").append(capitalize(relationship.name())).append("\n\n")
                    .append(relationship.description()).append("\n\nThey may:\n\n");
            List<Action<?>> allowed = policy.actionsAllowedFor(relationship);
            // in definition order, so the document doesn't depend on the order in which rule classes are loaded
            for (Action<?> action : actions) {
                if (allowed.contains(action)) {
                    markdown.append("- ").append(action.description()).append("\n");
                }
            }
        }
        return markdown.toString();
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
