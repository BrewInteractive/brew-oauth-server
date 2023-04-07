package com.brew.oauth20.server.provider.tokengrant;

import com.brew.oauth20.server.data.enums.GrantType;
import com.brew.oauth20.server.fixture.ClientModelFixture;
import com.brew.oauth20.server.fixture.TokenRequestModelFixture;
import com.brew.oauth20.server.model.*;
import com.brew.oauth20.server.service.ClientService;
import com.brew.oauth20.server.service.TokenService;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
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
class TokenGrantProviderClientCredentialsTest {
    @Mock
    TokenService tokenService;
    @Mock
    ClientService clientService;

    @InjectMocks
    private TokenGrantProviderClientCredentials tokenGrantProviderClientCredentials;

    private static Stream<Arguments> should_validate_client_credentials_provider() {

        var clientModelFixture = new ClientModelFixture();
        var client = clientModelFixture.createRandomOne(1, new GrantType[]{GrantType.refresh_token});
        var tokenRequestFixture = new TokenRequestModelFixture();

        var validTokenRequest = tokenRequestFixture.createRandomOne(new GrantType[]{GrantType.refresh_token});
        validTokenRequest.setClient_id(client.clientId());
        validTokenRequest.setClient_secret(client.clientSecret());

        var tokenRequestWithoutClient = tokenRequestFixture.createRandomOne(new GrantType[]{GrantType.refresh_token});
        tokenRequestWithoutClient.setClient_id("");
        tokenRequestWithoutClient.setClient_secret("");

        String authorizationHeader = Base64.getEncoder().withoutPadding().encodeToString((client.clientId() + ":" + client.clientSecret()).getBytes());

        var pair = Pair.of(client.clientId(), client.clientSecret());

        return Stream.of(
                //valid case client credentials from authorization code
                Arguments.of(client,
                        authorizationHeader,
                        validTokenRequest,
                        new ValidationResultModel(true, null),
                        pair),
                //valid case client credentials from request model
                Arguments.of(client,
                        null,
                        validTokenRequest,
                        new ValidationResultModel(true, null),
                        pair),
                //request without client
                Arguments.of(null,
                        authorizationHeader,
                        tokenRequestWithoutClient,
                        new ValidationResultModel(false, "invalid_request"),
                        pair),
                //client not found case
                Arguments.of(null,
                        authorizationHeader,
                        validTokenRequest,
                        new ValidationResultModel(false, "unauthorized_client"),
                        pair)
        );
    }

    private static Stream<Arguments> should_generate_token_from_valid_request() {
        var clientModelFixture = new ClientModelFixture();

        var client = clientModelFixture.createRandomOne(1, new GrantType[]{GrantType.client_credentials});

        var tokenRequestFixture = new TokenRequestModelFixture();

        var validTokenRequest = tokenRequestFixture.createRandomOne(new GrantType[]{GrantType.client_credentials});
        validTokenRequest.setClient_id(client.clientId());
        validTokenRequest.setClient_secret(client.clientSecret());

        var tokenRequestWithoutClient = tokenRequestFixture.createRandomOne(new GrantType[]{GrantType.client_credentials});
        tokenRequestWithoutClient.setClient_id("");
        tokenRequestWithoutClient.setClient_secret("");

        String authorizationHeader = Base64.getEncoder().withoutPadding().encodeToString((client.clientId() + ":" + client.clientSecret()).getBytes());

        var clientCredentials = Pair.of(client.clientId(), client.clientSecret());

        var tokenModel = TokenModel.builder().build();

        return Stream.of(
                Arguments.of(client,
                        authorizationHeader,
                        validTokenRequest,
                        new TokenResultModel(tokenModel, null),
                        clientCredentials),
                Arguments.of(client,
                        authorizationHeader,
                        tokenRequestWithoutClient,
                        new TokenResultModel(null, "invalid_request"),
                        clientCredentials),
                Arguments.of(client,
                        authorizationHeader,
                        validTokenRequest,
                        new TokenResultModel(null, "unauthorized_client"),
                        null)
        );
    }

    @BeforeEach
    public void setUp() {
        Mockito.reset(clientService);
        Mockito.reset(tokenService);
    }

    @MethodSource
    @ParameterizedTest
    void should_validate_client_credentials_provider(ClientModel clientModel,
                                                     String authorizationHeader,
                                                     TokenRequestModel tokenRequest,
                                                     ValidationResultModel expectedResult,
                                                     Pair<String, String> clientCredentialsPair) {
        // Arrange
        if (!tokenRequest.client_id.isEmpty() && !tokenRequest.client_secret.isEmpty()) {
            when(clientService.getClient(tokenRequest.client_id, tokenRequest.client_secret))
                    .thenReturn(clientModel);
        }

        if (!StringUtils.isEmpty(authorizationHeader)) {
            when(clientService.decodeClientCredentials(authorizationHeader))
                    .thenReturn(clientCredentialsPair == null ? Optional.empty() : Optional.of(clientCredentialsPair));
        }

        // Act
        var result = tokenGrantProviderClientCredentials.validate(authorizationHeader, tokenRequest);

        // Assert
        assertThat(result).isEqualTo(expectedResult);
    }

    @MethodSource
    @ParameterizedTest
    void should_generate_token_from_valid_request(ClientModel clientModel,
                                                  String authorizationHeader,
                                                  TokenRequestModel tokenRequest,
                                                  TokenResultModel tokenResultModel,
                                                  Pair<String, String> clientCredentialsPair) {

        // Arrange
        when(clientService.getClient(tokenRequest.client_id, tokenRequest.client_secret))
                .thenReturn(clientModel);

        when(clientService.decodeClientCredentials(authorizationHeader))
                .thenReturn(clientCredentialsPair == null ? Optional.empty() : Optional.of(clientCredentialsPair));

        when(tokenService.generateToken(clientModel, null, tokenRequest.state))
                .thenReturn(tokenResultModel.getResult());

        // Act
        var result = tokenGrantProviderClientCredentials.generateToken(authorizationHeader, tokenRequest);

        // Assert
        assertThat(result).isEqualTo(tokenResultModel);
    }
}
