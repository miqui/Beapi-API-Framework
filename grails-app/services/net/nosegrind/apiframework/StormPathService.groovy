package net.nosegrind.apiframework

import com.stormpath.sdk.account.Account
import com.stormpath.sdk.account.AccountList
import com.stormpath.sdk.account.AccountStatus
import com.stormpath.sdk.application.Application
import com.stormpath.sdk.application.ApplicationList
import com.stormpath.sdk.application.Applications
import com.stormpath.sdk.authc.AuthenticationRequest
import com.stormpath.sdk.authc.UsernamePasswordRequest
import com.stormpath.sdk.client.Client
import com.stormpath.sdk.client.Clients
import com.stormpath.sdk.directory.CustomData
//import com.stormpath.sdk.impl.authc.DefaultAuthenticationResult
import com.stormpath.sdk.impl.account.DefaultAuthenticationResult
import com.stormpath.sdk.impl.query.Order
import com.stormpath.sdk.provider.ProviderAccountRequest
import com.stormpath.sdk.provider.ProviderAccountResult
import com.stormpath.sdk.provider.Providers
import com.stormpath.sdk.tenant.Tenant
import grails.transaction.Transactional
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken

import javax.annotation.PostConstruct

@Transactional
class StormPathService {

    @PostConstruct
    void init() {
        String test = ""
    }

    def createUser(firstname, lastname, email, password, location, dob, facebookImgUrl, facebookLink, authToken, fbid) {

        Client client = Clients.builder().build();

        Tenant tenant = client.getCurrentTenant();
        ApplicationList applications = tenant.getApplications(
                Applications.where(Applications.name().eqIgnoreCase("alumni-main"))
        );

        Application application = applications.iterator().next();

        //Create the account object
        Account account = client.instantiate(Account.class);

        //Set the account properties
        account.setGivenName(firstname);
        account.setSurname(lastname);
        account.setUsername(firstname + "." + lastname); //optional, defaults to email if unset
        account.setEmail(email);
        account.setPassword(password);
        account.setStatus(AccountStatus.UNVERIFIED);

        CustomData customData = account.getCustomData();
        customData.put("delegation-semester", "Spring");
        customData.put("delegation-year", "2003");
        customData.put("current-location", location);
        customData.put("donation-level", "0");

        customData.put("facebook-link", facebookLink);
        customData.put("facebook-id", fbid);
        customData.put("dob", dob);

        customData.put("profile-img", profileImgUrl);

        try {
            //Create the account using the existing Application object
            Account createdAccount = application.createAccount(account);

            if (createdAccount) {
                log.info("Created account: " + createdAccount)
                return [statusMsg: com.tdx.Constants.SUCCESS]
            }
        }
        catch (com.stormpath.sdk.resource.ResourceException e) {
            log.error(e.stormpathError, e)
            return [statusMsg: e.stormpathError.developerMessage.toString(), firstname: firstname, lastname: lastname, email: email, password: password, location: location, dob: dob, facebookImgUrl: facebookImgUrl, facebookLink: facebookLink, fbid: fbid]
        }

    }

    def getUser() {

        Client client = Clients.builder().build();

        Tenant tenant = client.getCurrentTenant();
        ApplicationList applications = tenant.getApplications(
                Applications.where(Applications.name().eqIgnoreCase("alumni-main"))
        );

        Application application = applications.iterator().next();

        Map<String, Object> queryParams = new HashMap<String, Object>();
        queryParams.put("email", "anataliocs@gmail.com");
        AccountList accounts = application.getAccounts(queryParams);

        return accounts
    }

    def getAllUsers() {

        Client client = Clients.builder().build();

        Tenant tenant = client.getCurrentTenant();
        ApplicationList applications = tenant.getApplications(
                Applications.where(Applications.name().eqIgnoreCase("alumni-main"))
        );

        Application application = applications.iterator().next();

        Order order = Order.asc("surname");
        Map<String, Object> queryParams = new HashMap<String, Object>();
        queryParams.put("orderBy", order);
        AccountList accounts = application.getAccounts(queryParams);

        return accounts
    }

    def login(user, pw) {

        Client client = Clients.builder().build();

        Tenant tenant = client.getCurrentTenant();
        ApplicationList applications = tenant.getApplications(
                Applications.where(Applications.name().eqIgnoreCase("alumni-main"))
        );

        Application application = applications.iterator().next();

        //Create an authentication request using the credentials
        AuthenticationRequest request = new UsernamePasswordRequest(user, pw);

        //Now let's authenticate the account with the application:
        try {
            DefaultAuthenticationResult result = application.authenticateAccount(request);

            PreAuthenticatedAuthenticationToken authenticationToken = null;
            Account account = result.getAccount();

            if (account) {
                List<GrantedAuthority> grantedAuths = new ArrayList<>();
                grantedAuths.add(new SimpleGrantedAuthority("ROLE_USER"));

                authenticationToken = new PreAuthenticatedAuthenticationToken(account.email, account, grantedAuths);
                authenticationToken.setAuthenticated(true);
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                return [statusMsg: com.tdx.Constants.SUCCESS]
            }

        } catch (com.stormpath.sdk.resource.ResourceException e) {
            log.error(e.stormpathError, e)
            return [statusMsg: e.stormpathError.developerMessage.toString(), user: user, pw: pw]
        }
    }

    def facebookLogin(accessToken) {
        Client client = Clients.builder().build();

        Tenant tenant = client.getCurrentTenant();
        ApplicationList applications = tenant.getApplications(
                Applications.where(Applications.name().eqIgnoreCase("alumni-main"))
        );

        Application application = applications.iterator().next();

        ProviderAccountRequest request = Providers.FACEBOOK.account()
                .setAccessToken(accessToken)
                .build();

        //Now let's authenticate the account with the application:
        try {

            PreAuthenticatedAuthenticationToken authenticationToken = null;

            ProviderAccountResult result = application.getAccount(request);
            Account account = result.getAccount();

            if (account) {
                List<GrantedAuthority> grantedAuths = new ArrayList<>();
                grantedAuths.add(new SimpleGrantedAuthority("ROLE_USER"));

                authenticationToken = new PreAuthenticatedAuthenticationToken(account.email, account, grantedAuths);
                authenticationToken.setAuthenticated(true);
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }

        } catch (ResourceException ex) {
            System.out.println(ex.getStatus() + " " + ex.getMessage());
        }
    }

    def resetPassword(email) {

        Client client = Clients.builder().build();

        Tenant tenant = client.getCurrentTenant();
        ApplicationList applications = tenant.getApplications(
                Applications.where(Applications.name().eqIgnoreCase("alumni-main"))
        );

        Application application = applications.iterator().next();

        application.sendPasswordResetEmail(email);


    }
}
