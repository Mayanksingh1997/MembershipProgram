package com.firstclub.firstclub.helper;

import com.firstclub.firstclub.constants.AuthConstants;
import com.firstclub.firstclub.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class RequestContextHelper {

    public String getAuthenticatedUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(AuthConstants.USER_ID_ATTRIBUTE);
        if (userId == null) {
            throw new AuthException("Authenticated user not found", HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        }
        return userId.toString();
    }
}
