package eu.hohenegger.accesscontrol.permission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The logger for all permission-related activity. INFO: enforced checks, filtered lists, persona switches and
 * administrative changes. DEBUG additionally: every single policy decision.
 */
public final class Audit {

    public static final Logger LOG = LoggerFactory.getLogger("access");

    private Audit() {
    }
}
