package com.fanzone.common.security;

import com.fanzone.common.exceptions.ForbiddenException;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that enforces email verification for methods annotated with @RequireEmailVerified.
 * Throws ForbiddenException with code "AUTH_EMAIL_NOT_VERIFIED" if the current user
 * has not verified their email address.
 */
@Aspect
@Component
public class EmailVerifiedAspect {

    @Before("@annotation(requireEmailVerified) || @within(requireEmailVerified)")
    public void checkEmailVerified(RequireEmailVerified requireEmailVerified) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return; // Let Spring Security handle unauthenticated requests
        }

        if (!principal.isEmailVerified()) {
            throw new ForbiddenException("AUTH_EMAIL_NOT_VERIFIED",
                    "Email verification is required to perform this action");
        }
    }
}
