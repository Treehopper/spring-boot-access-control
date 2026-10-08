package eu.hohenegger.accesscontrol.web;

import eu.hohenegger.accesscontrol.config.CommunityConfig;
import eu.hohenegger.accesscontrol.config.SecurityConfig;
import eu.hohenegger.accesscontrol.permission.AccessRules;
import eu.hohenegger.accesscontrol.permission.HomeownersPolicy;
import eu.hohenegger.accesscontrol.permission.Permissions;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** The web slice plus everything the permission checks need. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@WebMvcTest
@Import({CommunityConfig.class, SecurityConfig.class, HomeownersPolicy.class, Permissions.class, AccessRules.class})
@interface AccessControlWebMvcTest {
}
