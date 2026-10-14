package eu.hohenegger.accesscontrol.permission;

/**
 * Contributes permissions to the {@link Policy}, in the same way a {@code Jackson2ObjectMapperBuilderCustomizer}
 * contributes to the object mapper. Declare one per feature:
 *
 * <pre>{@code
 * @Bean
 * PolicyCustomizer pollPermissions() {
 *     return policy -> policy
 *             .allow(OWNERS).to(VIEW_POLLS, VOTE)
 *             .allow(DISTRICT_MANAGERS).to(VIEW_POLLS, CREATE_POLL);
 * }
 * }</pre>
 */
@FunctionalInterface
public interface PolicyCustomizer {

    void customize(Policy.Builder policy);
}
