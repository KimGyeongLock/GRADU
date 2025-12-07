package com.example.gradu.global.security;

import com.example.gradu.global.exception.ErrorCode;
import com.example.gradu.global.exception.auth.AuthException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class StudentAccessAspect {

    @Before("@annotation(com.example.gradu.global.security.CheckStudentAccess) && args(sid,..)")
    public void checkAccess(String sid) {

        String currentId = SecurityUtil.getCurrentStudentId();

        if (currentId == null) {
            throw new AuthException(ErrorCode.AUTH_UNAUTHENTICATED);
        }

        if (!currentId.equals(sid)) {
            throw new AuthException(ErrorCode.AUTH_FORBIDDEN);
        }
    }
}
