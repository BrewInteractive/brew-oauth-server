package com.brew.oauth20.server.controller;

import com.brew.oauth20.server.component.UserCookieManager;
import com.brew.oauth20.server.data.enums.ResponseType;
import com.brew.oauth20.server.exception.UnsupportedServiceTypeException;
import com.brew.oauth20.server.model.AuthorizeRequestModel;
import com.brew.oauth20.server.service.factory.AuthorizeTypeProviderFactory;
import com.brew.oauth20.server.service.AuthorizationCodeService;
import com.brew.oauth20.server.service.ClientUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
public class AuthorizeController {
    private static final String DEFAULT_AUTHORIZATION_CODE_EXPIRES_MS = "300000";
    @Autowired
    private UserCookieManager userCookieManager;
    @Autowired
    private AuthorizationCodeService authorizationCodeService;
    @Autowired
    private AuthorizeTypeProviderFactory authorizeTypeProviderFactory;
    @Autowired
    private ClientUserService clientUserService;
    @Value("${oauth.login_signup_endpoint}")
    private String loginSignupEndpoint;
    @Autowired
    private Environment env;

    @GetMapping(value = "/oauth/authorize")
    public ResponseEntity<String> authorizeGet(
            @Valid @ModelAttribute("authorizeRequest") AuthorizeRequestModel authorizeRequest,
            BindingResult validationResult,
            HttpServletRequest request,
            HttpServletResponse response) {
        return authorize(authorizeRequest, validationResult, request,
                URLDecoder.decode(request.getQueryString(), StandardCharsets.UTF_8));
    }

    @PostMapping(value = "/oauth/authorize")
    public ResponseEntity<String> authorizePost(@Valid @RequestBody AuthorizeRequestModel authorizeRequest,
                                                BindingResult validationResult,
                                                HttpServletRequest request) {
        return authorize(authorizeRequest, validationResult, request, convertToParameters(authorizeRequest));
    }

    private ResponseEntity<String> authorize(AuthorizeRequestModel authorizeRequest,
                                             BindingResult validationResult,
                                             HttpServletRequest request,
                                             String parameters) {
        try {
            /* request parameters validation */
            if (validationResult.hasErrors())
                return generateErrorResponse("invalid_request", parameters, authorizeRequest.getRedirect_uri());

            /* authorize type validator */
            var authorizeTypeProvider = authorizeTypeProviderFactory
                    .getService(ResponseType.fromValue(authorizeRequest.getResponse_type()));

            var authorizeTypeValidationResult = authorizeTypeProvider.validate(authorizeRequest.getClient_id(),
                    authorizeRequest.getRedirect_uri());

            if (Boolean.FALSE.equals(authorizeTypeValidationResult.getResult()))
                return generateErrorResponse(authorizeTypeValidationResult.getError(), parameters,
                        authorizeRequest.getRedirect_uri());

            /* user cookie and authorization code */
            var userIdOptional = userCookieManager.getUser(request);

            /* not logged-in user redirect login signup */
            if (userIdOptional.isEmpty()) {
                if (loginSignupEndpoint.isBlank())
                    throw new IllegalStateException("LOGIN_SIGNUP_ENDPOINT is not set in the environment variables");

                return generateLoginResponse(loginSignupEndpoint, parameters);
            }

            var expiresMs = env.getProperty("oauth.authorization_code_expires_ms",
                    DEFAULT_AUTHORIZATION_CODE_EXPIRES_MS);

            var userId = userIdOptional.get();

            var clientUser = clientUserService.create(authorizeRequest.getClient_id(), userId);

            var code = authorizationCodeService.createAuthorizationCode(authorizeRequest.getRedirect_uri(),
                    Long.parseLong(expiresMs),
                    clientUser);

            /* logged-in user redirect with authorization code */
            return generateSuccessResponse(code, authorizeRequest.getRedirect_uri(), parameters, userId);
        } catch (UnsupportedServiceTypeException e) {
            return generateErrorResponse("unsupported_response_type", parameters,
                    authorizeRequest.redirect_uri);
        } catch (Exception e) {
            return generateErrorResponse("server_error", parameters, authorizeRequest.getRedirect_uri());
        }
    }

    private String convertToParameters(AuthorizeRequestModel authorizeRequest) {
        var queryStringBuilder = new StringBuilder();
        queryStringBuilder
                .append("response_type=").append(authorizeRequest.getResponse_type())
                .append("&redirect_uri=").append(authorizeRequest.getRedirect_uri())
                .append("&client_id=").append(authorizeRequest.getClient_id());
        if (!authorizeRequest.getState().isBlank())
            queryStringBuilder.append("&state=").append(authorizeRequest.getState());
        return queryStringBuilder.toString();
    }

    private ResponseEntity<String> generateErrorResponse(String error, String parameters, String redirectUri) {
        var headers = new HttpHeaders();
        if (redirectUri != null) {
            var location = UriComponentsBuilder.fromUriString(redirectUri)
                    .query(parameters)
                    .queryParam("error", error)
                    .build()
                    .toUri();
            headers.setContentType(MediaType.TEXT_HTML);
            headers.setLocation(location);
        }
        return new ResponseEntity<>(error, headers, HttpStatus.FOUND);
    }

    private ResponseEntity<String> generateLoginResponse(String loginSignupEndpoint, String parameters) {
        var location = UriComponentsBuilder.fromUriString(loginSignupEndpoint)
                .query(parameters)
                .build()
                .toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        headers.setLocation(location);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    private ResponseEntity<String> generateSuccessResponse(String code, String redirectUri, String parameters, String userId) {
        var location = UriComponentsBuilder.fromUriString(redirectUri)
                .query(parameters)
                .queryParam("code", code)
                .queryParam("user_id", userId)
                .build()
                .toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        headers.setLocation(location);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
