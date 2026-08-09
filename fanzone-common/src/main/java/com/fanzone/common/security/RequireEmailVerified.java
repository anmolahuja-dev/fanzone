package com.fanzone.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method as requiring email verification.
 * Unverified users will receive a 403 response with code "AUTH_EMAIL_NOT_VERIFIED".
 * 
 * Usage: Apply to controller methods that should only be accessible to verified users
 * (e.g., creating posts, comments, or any write operation).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireEmailVerified {
}
