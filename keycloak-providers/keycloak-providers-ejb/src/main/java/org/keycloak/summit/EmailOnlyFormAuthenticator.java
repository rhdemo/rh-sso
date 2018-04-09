/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.summit;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.events.Details;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Random;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class EmailOnlyFormAuthenticator extends AbstractUsernameFormAuthenticator implements Authenticator {

    private Random random = new Random();

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String email = formData.getFirst("email");
        String code = formData.getFirst("code");

        if (formData.containsKey("cancel")) {
            context.resetFlow();
            return;
        }

        if (email != null) {
            if (!validateEmail(email)) {
                context.challenge(context.form().addError(new FormMessage("Invalid email")).createForm("login-email-only.ftl"));
                return;
            }
            UserModel user = context.getSession().users().getUserByEmail(email, context.getRealm());
            if (user == null) {
                // Register user
                user = context.getSession().users().addUser(context.getRealm(), email);
                user.setEnabled(true);
                user.setEmail(email);

                context.setUser(user);

                context.getAuthenticationSession().setAuthNote(Details.REMEMBER_ME, "true");

                context.success();
            } else {
                String key = generateCode();
                context.getAuthenticationSession().setAuthNote("email-key", key);

                String textBody = "Login code: \n" + key;
                String htmlBody = "<p>Login code:</p><code style=\"font-size:2em;\">" + key + "</code>";

                try {
                    context.getSession().getProvider(EmailSenderProvider.class).send(context.getRealm().getSmtpConfig(), user, "Login code for Summit 2018", textBody, htmlBody);
                } catch (EmailException e) {
                    e.printStackTrace();
                }

                context.setUser(user);
                context.challenge(context.form().setAttribute("email", user.getEmail()).createForm("login-email-only-code.ftl"));
            }
        } else if (code != null) {
            String sessionKey = context.getAuthenticationSession().getAuthNote("email-key");
            if (sessionKey.equals(code)) {
                context.getAuthenticationSession().setAuthNote(Details.REMEMBER_ME, "true");

                context.success();
            } else {
                context.challenge(context.form().setAttribute("email", context.getUser().getEmail()).addError(new FormMessage("Invalid code")).createForm("login-email-only-code.ftl"));
            }
        } else {
            context.form().createErrorPage(Response.Status.INTERNAL_SERVER_ERROR);
            return;
        }
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        context.challenge(context.form().createForm("login-email-only.ftl"));
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(9));
        }
        return sb.toString();
    }

    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            return false;
        }

        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException ex) {
            return false;
        }
        return true;
    }

}
