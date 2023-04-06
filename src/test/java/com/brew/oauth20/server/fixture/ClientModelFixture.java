package com.brew.oauth20.server.fixture;

import com.brew.oauth20.server.data.enums.GrantType;
import com.brew.oauth20.server.data.enums.ResponseType;
import com.brew.oauth20.server.fixture.abstracts.Fixture;
import com.brew.oauth20.server.model.ClientModel;
import org.instancio.Instancio;
import org.instancio.Model;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.instancio.Select.field;

public class ClientModelFixture extends Fixture<ClientModel> {

    private final Integer defaultGrantSize = 2;
    private final Boolean defaultIssueRefreshTokens = false;
    private final ResponseType[] defaultResponseTypeOptions = ResponseType.values();
    private final GrantType[] defaultGrantTypeOptions = GrantType.values();
    private final Integer defaultRedirectUriSize = 2;

    private final GrantModelFixture grantModelFixture;
    private final RedirectUriModelFixture redirectUriModelFixture;

    public ClientModelFixture() {
        this.grantModelFixture = new GrantModelFixture();
        this.redirectUriModelFixture = new RedirectUriModelFixture();
    }

    public ClientModel createRandomOne() {
        return createRandomOne(this.defaultGrantSize, this.defaultResponseTypeOptions);
    }

    public ClientModel createRandomOne(Boolean issueRefreshTokens) {
        return Instancio.of(clientModel(this.defaultGrantSize, issueRefreshTokens, this.defaultResponseTypeOptions, this.defaultGrantTypeOptions))
                .create();
    }

    public ClientModel createRandomOne(Integer grantSize, ResponseType[] responseTypeOptions) {
        return Instancio.of(clientModel(grantSize, this.defaultIssueRefreshTokens, responseTypeOptions, this.defaultGrantTypeOptions))
                .create();
    }

    public ClientModel createRandomOne(Integer grantSize, GrantType[] grantTypeOptions) {
        return Instancio.of(clientModel(grantSize, this.defaultIssueRefreshTokens, this.defaultResponseTypeOptions, grantTypeOptions))
                .create();
    }

    public List<ClientModel> createRandomList(Integer size) {
        return createRandomList(size, this.defaultGrantSize, this.defaultIssueRefreshTokens, this.defaultResponseTypeOptions, this.defaultGrantTypeOptions);
    }

    public List<ClientModel> createRandomList(Integer size, Integer grantSize, Boolean issueRefreshTokens, ResponseType[] responseTypeOptions, GrantType[] grantTypeOptions) {
        return Instancio.ofList(clientModel(grantSize, issueRefreshTokens, responseTypeOptions, grantTypeOptions))
                .size(size)
                .create();
    }


    private Model<ClientModel> clientModel(Integer grantSize, Boolean issueRefreshTokens, ResponseType[] responseTypeOptions, GrantType[] grantTypeOptions) {
        return Instancio.of(ClientModel.class)
                .supply(field(ClientModel::clientSecret), () -> encodeClientSecret(faker.lordOfTheRings().character()))
                .supply(field(ClientModel::issueRefreshTokens), () -> issueRefreshTokens)
                .supply(field(ClientModel::grantList), () -> grantModelFixture.createRandomList(grantSize, responseTypeOptions, grantTypeOptions))
                .supply(field(ClientModel::redirectUriList), () -> redirectUriModelFixture.createRandomList(this.defaultRedirectUriSize))
                .toModel();
    }

    private String encodeClientSecret(String clientSecret) {
        byte[] encodedBytes = Base64.getEncoder().encode(clientSecret.getBytes(StandardCharsets.UTF_8));
        return new String(encodedBytes, StandardCharsets.UTF_8);
    }
}


