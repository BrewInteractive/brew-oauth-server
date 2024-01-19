package com.brew.oauth20.server.provider.tokengrant;

import com.brew.oauth20.server.data.RefreshToken;
import com.brew.oauth20.server.data.enums.GrantType;
import com.brew.oauth20.server.fixture.ClientModelFixture;
import com.brew.oauth20.server.fixture.RefreshTokenFixture;
import com.brew.oauth20.server.fixture.TokenRequestModelFixture;
import com.brew.oauth20.server.model.*;
import com.brew.oauth20.server.service.ClientService;
import com.brew.oauth20.server.service.RefreshTokenService;
import com.brew.oauth20.server.service.TokenService;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Base64;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TokenGrantProviderRefreshTokenTest {
    @Mock
    RefreshTokenService refreshTokenService;
    @Mock
    TokenService tokenService;
    @Mock
    ClientService clientService;

    @InjectMocks
    private TokenGrantProviderRefreshToken tokenGrantProviderRefreshToken;

    private static Stream<Arguments> should_validate_refresh_token_provider() {

        var clientModelFixture = new ClientModelFixture();
        var client = clientModelFixture.createRandomOne(1, new GrantType[]{GrantType.refresh_token});
        var tokenRequestFixture = new TokenRequestModelFixture();

        var validTokenRequest = tokenRequestFixture.createRandomOne(new GrantType[]{GrantType.refresh_token});
        validTokenRequest.setClient_id(client.clientId());
        validTokenRequest.setClient_secret(client.clientSecret());

        var tokenRequestWithoutRefreshToken = tokenRequestFixture.createRandomOne(new GrantType[]{GrantType.refresh_token});
        tokenRequestWithoutRefreshToken.setClient_id(client.clientId());
        tokenRequestWithoutRefreshToken.setClient_secret(client.clientSecret());
        tokenRequestWithoutRefreshToken.setRefresh_token("");

        var tokenRequestWithoutClient = tokenRequestFixture.createRandomOne(new GrantType[]{GrantType.refresh_token});
        tokenRequestWithoutClient.setClient_id("");
        tokenRequestWithoutClient.setClient_secret("");

        String authorizationCode = Base64.getEncoder().withoutPadding().encodeToString((client.clientId() + ":" + client.clientSecret()).getBytes());

        var pair = Pair.of(client.clientId(), client.clientSecret());

        return Stream.of(
                //valid case client credentials from authorization code
                Arguments.of(client,
                        authorizationCode,
                        validTokenRequest,
                        new ValidationResultModel(true, null),
                        pair),
                //valid case client credentials from request model
                Arguments.of(client,
                        "",
                        validTokenRequest,
                        new ValidationResultModel(true, null),
                        pair),
                //valid case client credentials from request model
                Arguments.of(client,
                        null,
                        validTokenRequest,
                        new ValidationResultModel(true, null),
                        pair),
                //invalid case client credentials from authorization code
                Arguments.of(client,
                        authorizationCode,
                        validTokenRequest,
                        new ValidationResultModel(false, "unauthorized_client"),
                        null),
                //no refresh code provided case
                Arguments.of(client,
                        authorizationCode,
                        tokenRequestWithoutRefreshToken,
                        new ValidationResultModel(false, "invalid_request"),
                        pair),
                //no client credentials provided case
                Arguments.of(client,
                        "",
                        tokenRequestWithoutClient,
                        new ValidationResultModel(false, "unauthorized_client"),
                        pair),
                //client not found case
                Arguments.of(null,
                        authorizationCode,
                        validTokenRequest,
                        new ValidationResultModel(false, "unauthorized_client"),
                        pair)
        );
    }

    private static Stream<Arguments> should_generate_token_from_valid_request() {

        var clientModelFixture = new ClientModelFixture();
        var client = clientModelFixture.createRandomOne(1, new GrantType[]{GrantType.refresh_token});
        var tokenRequestFixture = new TokenRequestModelFixture();

        var validTokenRequest = tokenRequestFixture.createRandomOne(new GrantType[]{GrantType.refresh_token});
        validTokenRequest.setClient_id(client.clientId());
        validTokenRequest.setClient_secret(client.clientSecret());

        var tokenRequestWithoutRefreshToken = tokenRequestFixture.createRandomOne(new GrantType[]{GrantType.refresh_token});
        tokenRequestWithoutRefreshToken.setClient_id(client.clientId());
        tokenRequestWithoutRefreshToken.setClient_secret(client.clientSecret());
        tokenRequestWithoutRefreshToken.setRefresh_token("");

        var tokenRequestWithoutClient = tokenRequestFixture.createRandomOne(new GrantType[]{GrantType.refresh_token});
        tokenRequestWithoutClient.setClient_id("");
        tokenRequestWithoutClient.setClient_secret("");

        String authorizationCode = Base64.getEncoder().withoutPadding().encodeToString((client.clientId() + ":" + client.clientSecret()).getBytes());
        var pair = Pair.of(client.clientId(), client.clientSecret());

        var refreshTokenFixture = new RefreshTokenFixture();
        var refreshToken = refreshTokenFixture.createRandomOne();
        var tokenModel = TokenModel.builder().build();

        return Stream.of(
                Arguments.of(client,
                        authorizationCode,
                        validTokenRequest,
                        refreshToken,
                        new TokenResultModel(tokenModel, null),
                        pair),
                Arguments.of(client,
                        authorizationCode,
                        validTokenRequest,
                        refreshToken,
                        new TokenResultModel(null, "unauthorized_client"),
                        null)
        );
    }

    @BeforeEach
    public void setUp() {
        Mockito.reset(clientService);
        Mockito.reset(refreshTokenService);
    }

    @MethodSource
    @ParameterizedTest
    void should_validate_refresh_token_provider(ClientModel clientModel,
                                                String authorizationCode,
                                                TokenRequestModel tokenRequest,
                                                ValidationResultModel expectedResult,
                                                Pair<String, String> clientCredentialsPair) {
        // Arrange
        if (!tokenRequest.client_id.isEmpty() && !tokenRequest.client_secret.isEmpty())
            when(clientService.getClient(tokenRequest.client_id, tokenRequest.client_secret))
                    .thenReturn(clientModel);
        if (!StringUtils.isEmpty(authorizationCode))
            when(clientService.decodeClientCredentials(authorizationCode))
                    .thenReturn(clientCredentialsPair == null ? Optional.empty() : Optional.of(clientCredentialsPair));

        // Act
        var result = tokenGrantProviderRefreshToken.validate(authorizationCode, tokenRequest);

        // Assert
        assertThat(result).isEqualTo(expectedResult);
    }

    @MethodSource
    @ParameterizedTest
    void should_generate_token_from_valid_request(ClientModel clientModel,
                                                  String authorizationCode,
                                                  TokenRequestModel tokenRequest,
                                                  RefreshToken refreshToken,
                                                  TokenResultModel tokenResultModel,
                                                  Pair<String, String> clientCredentialsPair) {

        // Arrange
        when(clientService.getClient(tokenRequest.client_id, tokenRequest.client_secret))
                .thenReturn(clientModel);
        when(clientService.decodeClientCredentials(authorizationCode))
                .thenReturn(clientCredentialsPair == null ? Optional.empty() : Optional.of(clientCredentialsPair));
        when(refreshTokenService.revokeRefreshToken(tokenRequest.client_id, tokenRequest.refresh_token, clientModel.refreshTokenExpiresInDays()))
                .thenReturn(refreshToken);
        when(tokenService.generateToken(clientModel, refreshToken.getClientUser().getUserId(), tokenRequest.state, tokenRequest.additional_claims, refreshToken.getToken()))
                .thenReturn(tokenResultModel.getResult());

        // Act
        var result = tokenGrantProviderRefreshToken.generateToken(authorizationCode, tokenRequest);

        // Assert
        assertThat(result).isEqualTo(tokenResultModel);
    }
}
