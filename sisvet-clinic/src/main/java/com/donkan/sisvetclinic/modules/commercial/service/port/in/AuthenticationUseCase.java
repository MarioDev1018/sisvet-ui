package com.donkan.sisvetclinic.modules.commercial.service.port.in;

import com.donkan.sisvetclinic.modules.commercial.service.dto.request.LoginRequest;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.AuthResult;

public interface AuthenticationUseCase {

    AuthResult login(LoginRequest request);
}