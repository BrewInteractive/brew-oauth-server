package com.brew.oauth20.server.provider.tokengrant;

import com.brew.oauth20.server.data.enums.GrantType;
import com.brew.oauth20.server.exception.ClientsUserNotFoundException;
import com.brew.oauth20.server.model.TokenRequestModel;
import com.brew.oauth20.server.model.TokenResultModel;
import com.brew.oauth20.server.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TokenGrantProviderClientCredentials extends BaseTokenGrantProvider {
    @Autowired
    TokenService tokenService;

    protected TokenGrantProviderClientCredentials() {
        super();
        this.grantType = GrantType.client_credentials;
    }

    @Override
    public TokenResultModel generateToken(String authorizationHeader, TokenRequestModel tokenRequest) {
        try {
            var validationResult = super.validate(authorizationHeader, tokenRequest);

            if (Boolean.FALSE.equals(validationResult.getResult()))
                return new TokenResultModel(null, validationResult.getError());

            var tokenModel = tokenService.generateToken(client, tokenRequest.state);

            return new TokenResultModel(tokenModel, null);
        } catch (ClientsUserNotFoundException e) {
            return new TokenResultModel(null, "unauthorized_client");
        }
    }
}
