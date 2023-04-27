package com.brew.oauth20.server.integration;

import com.brew.oauth20.server.data.ActiveRefreshToken;
import com.brew.oauth20.server.data.enums.GrantType;
import com.brew.oauth20.server.data.enums.ResponseType;
import com.brew.oauth20.server.fixture.*;
import com.brew.oauth20.server.mapper.AuthorizationCodeMapper;
import com.brew.oauth20.server.mapper.RefreshTokenMapper;
import com.brew.oauth20.server.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseAuthorizeControllerTest {
    protected final String notAuthorizedRedirectUri = "http://www.not-authorized-uri.com";
    protected String authorizedRedirectUri;
    protected String authorizedAuthCode;
    protected String authorizedClientId;
    protected String authorizedClientSecret;
    protected String authorizedRefreshToken;
    protected String authorizedLoginSignupEndpoint;
    protected String authorizedState;
    protected Faker faker;
    @Autowired
    protected AuthorizationCodeRepository authorizationCodeRepository;
    @Autowired
    protected ActiveAuthorizationCodeRepository activeAuthorizationCodeRepository;
    @Autowired
    protected ClientRepository clientRepository;
    @Autowired
    protected ClientGrantRepository clientGrantRepository;
    @Autowired
    protected GrantRepository grantRepository;
    @Autowired
    protected RedirectUriRepository redirectUriRepository;
    @Autowired
    protected ClientsUserRepository clientsUserRepository;
    @Autowired
    protected ActiveRefreshTokenRepository activeRefreshTokenRepository;
    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;
    @Autowired
    protected Environment env;
    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    void setup() {
        this.faker = new Faker();
        var clientsGrantFixture = new ClientGrantFixture();
        var grantFixture = new GrantFixture();
        var redirectUrisFixture = new RedirectUriFixture();
        var activeAuthorizationCodeFixture = new ActiveAuthorizationCodeFixture();
        var clientsUserFixture = new ClientsUserFixture();
        var activeRefreshTokenFixture = new ActiveRefreshTokenFixture();

        var authCodeGrant = grantFixture.createRandomOne(new ResponseType[]{ResponseType.code}, new GrantType[]{GrantType.authorization_code});
        var clientCredGrant = grantFixture.createRandomOne(new ResponseType[]{ResponseType.code}, new GrantType[]{GrantType.client_credentials});
        var refreshTokenGrant = grantFixture.createRandomOne(new ResponseType[]{ResponseType.code}, new GrantType[]{GrantType.refresh_token});
        var clientsGrantAuthCode = clientsGrantFixture.createRandomOne(new ResponseType[]{ResponseType.code});
        var clientsGrantClientCred = clientsGrantFixture.createRandomOne(new ResponseType[]{ResponseType.code});
        var clientsGrantRefreshToken = clientsGrantFixture.createRandomOne(new ResponseType[]{ResponseType.code});
        var redirectUris = redirectUrisFixture.createRandomOne();
        var activeAuthorizationCode = activeAuthorizationCodeFixture.createRandomOne(redirectUris.getRedirectUri());

        var clientsUser = clientsUserFixture.createRandomOne();

        var client = clientsUser.getClient();

        var savedClient = clientRepository.save(client);

        var savedClientUser = clientsUserRepository.save(clientsUser);

        ActiveRefreshToken activeRefreshToken = activeRefreshTokenFixture.createRandomOne(savedClientUser);

        authorizedRefreshToken = activeRefreshToken.getToken();

        activeRefreshTokenRepository.save(activeRefreshToken);

        var existingRefreshToken = RefreshTokenMapper.INSTANCE.toRefreshToken(activeRefreshToken);

        refreshTokenRepository.save(existingRefreshToken);

        activeAuthorizationCode.setClient(savedClient);
        activeAuthorizationCode.setUserId(savedClientUser.getUserId());
        activeAuthorizationCodeRepository.save(activeAuthorizationCode);

        var authorizationCode = AuthorizationCodeMapper.INSTANCE.toAuthorizationCode(activeAuthorizationCode);

        authorizationCode.setClient(savedClient);
        authorizationCode.setUserId(savedClientUser.getUserId());
        authorizationCodeRepository.save(authorizationCode);

        var savedAuthCodeGrant = grantRepository.save(authCodeGrant);
        var savedClientCredGrant = grantRepository.save(clientCredGrant);
        var savedRefreshTokenGrant = grantRepository.save(refreshTokenGrant);

        redirectUris.setClient(savedClient);
        redirectUriRepository.save(redirectUris);

        clientsGrantAuthCode.setClient(savedClient);
        clientsGrantAuthCode.setGrant(savedAuthCodeGrant);
        clientGrantRepository.save(clientsGrantAuthCode);

        clientsGrantClientCred.setClient(savedClient);
        clientsGrantClientCred.setGrant(savedClientCredGrant);
        clientGrantRepository.save(clientsGrantClientCred);

        clientsGrantRefreshToken.setClient(savedClient);
        clientsGrantRefreshToken.setGrant(savedRefreshTokenGrant);
        clientGrantRepository.save(clientsGrantRefreshToken);

        authorizedClientId = client.getClientId();
        authorizedClientSecret = client.getClientSecret();
        authorizedRedirectUri = redirectUris.getRedirectUri();
        authorizedAuthCode = authorizationCode.getCode();
        authorizedLoginSignupEndpoint = env.getProperty("LOGIN_SIGNUP_ENDPOINT", "https://test.com/login");
        authorizedState = URLEncoder.encode(faker.lordOfTheRings().character().replace(" ", ""), StandardCharsets.UTF_8);

    }


    @AfterAll
    void emptyData() {
        authorizationCodeRepository.deleteAll();
        activeAuthorizationCodeRepository.deleteAll();
        clientGrantRepository.deleteAllInBatch();
        redirectUriRepository.deleteAllInBatch();
        clientRepository.deleteAll();
        grantRepository.deleteAllInBatch();
    }

    protected ResultActions postAuthorize(String redirectUri, String clientId, String responseType, String state, String cookieValue) throws Exception {
        Map<String, String> requestBodyMap = new HashMap<>();
        requestBodyMap.put("redirect_uri", redirectUri);
        requestBodyMap.put("client_id", clientId);
        requestBodyMap.put("response_type", responseType);
        requestBodyMap.put("state", state);

        String requestBody = new ObjectMapper().writeValueAsString(requestBodyMap);

        if (cookieValue.isBlank())
            return this.mockMvc.perform(post("/oauth/authorize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));
        return this.mockMvc.perform(post("/oauth/authorize")
                .cookie(new Cookie("SESSION_ID", cookieValue))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

    }

    protected ResultActions postAuthorize(String redirectUri, String clientId, String responseType) throws Exception {
        return postAuthorize(redirectUri, clientId, responseType, "", "");
    }

    protected ResultActions postAuthorize(String redirectUri, String clientId, String responseType, String cookieValue) throws Exception {
        return postAuthorize(redirectUri, clientId, responseType, "", cookieValue);
    }

    protected ResultActions getAuthorize(String redirectUri, String clientId, String responseType, String state, String cookieValue) throws Exception {
        if (cookieValue.isBlank())
            return this.mockMvc.perform(get("/oauth/authorize")
                    .queryParam("redirect_uri", redirectUri)
                    .queryParam("client_id", clientId)
                    .queryParam("response_type", responseType)
                    .queryParam("state", state));
        return this.mockMvc.perform(get("/oauth/authorize")
                .cookie(new Cookie("SESSION_ID", cookieValue))
                .queryParam("redirect_uri", redirectUri)
                .queryParam("client_id", clientId)
                .queryParam("response_type", responseType)
                .queryParam("state", state));
    }

    protected ResultActions getAuthorize(String redirectUri, String clientId, String responseType) throws Exception {
        return getAuthorize(redirectUri, clientId, responseType, "", "");
    }

    protected ResultActions getAuthorize(String redirectUri, String clientId, String responseType, String cookieValue) throws Exception {
        return getAuthorize(redirectUri, clientId, responseType, "", cookieValue);
    }
}
