package com.brew.oauth20.server.provider;

import com.brew.oauth20.server.provider.authorizetype.BaseAuthorizeTypeProvider;
import com.brew.oauth20.server.service.ClientService;

public class MockAuthorizeTypeProviderNotExistingInIoc extends BaseAuthorizeTypeProvider {
    public MockAuthorizeTypeProviderNotExistingInIoc(ClientService clientService) {
        super(clientService);
    }
}
