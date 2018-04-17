package org.keycloak.summit.infinispan.users;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TestConfig {

    // Should those clusters be tested? By default yes. Use false just in case that some cluster is temporarily offline for example
    private boolean testAzr;
    private boolean testAws;
    private boolean testGce;

    private String ssoProject;

    private String adminPassword;

    private String azrRouteUrl;
    private String awsRouteUrl;
    private String gceRouteUrl;

    public boolean isTestAzr() {
        return testAzr;
    }

    public void setTestAzr(boolean testAzr) {
        this.testAzr = testAzr;
    }

    public boolean isTestAws() {
        return testAws;
    }

    public void setTestAws(boolean testAws) {
        this.testAws = testAws;
    }

    public boolean isTestGce() {
        return testGce;
    }

    public void setTestGce(boolean testGce) {
        this.testGce = testGce;
    }

    public String getSsoProject() {
        return ssoProject;
    }

    public void setSsoProject(String ssoProject) {
        this.ssoProject = ssoProject;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getAzrRouteUrl() {
        return azrRouteUrl;
    }

    public void setAzrRouteUrl(String azrRouteUrl) {
        this.azrRouteUrl = azrRouteUrl;
    }

    public String getAwsRouteUrl() {
        return awsRouteUrl;
    }

    public void setAwsRouteUrl(String awsRouteUrl) {
        this.awsRouteUrl = awsRouteUrl;
    }

    public String getGceRouteUrl() {
        return gceRouteUrl;
    }

    public void setGceRouteUrl(String gceRouteUrl) {
        this.gceRouteUrl = gceRouteUrl;
    }

    public void setupDefaults() {
        if (ssoProject == null) {
            throw new IllegalStateException("Need to set ssoProject in configuration before calling setDefaults");
        }
        if (adminPassword == null) {
            throw new IllegalStateException("Need to set adminPassword in configuration before calling setDefaults");
        }

        testAzr = true;
        testAws = true;
        testGce = true;

        awsRouteUrl = "https://secure-sso-" + ssoProject + ".apps.summit-aws.sysdeseng.com";
        azrRouteUrl = "https://secure-sso-" + ssoProject + ".apps.summit-azr.sysdeseng.com";
        gceRouteUrl = "https://secure-sso-" + ssoProject + ".apps.summit-gce.sysdeseng.com";
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("TestConfig [")
                .append(" ssoProject=" + ssoProject)
                .append(", testAzr=" + testAzr)
                .append(", testAws=" + testAws)
                .append(", testGce=" + testGce)
                .append(", awsRouteUrl=" + awsRouteUrl)
                .append(", azrRouteUrl=" + azrRouteUrl)
                .append(", gceRouteUrl=" + gceRouteUrl);
        if (adminPassword != null) {
            builder.append(", adminPassword=****");
        }

        return builder.toString();
    }
}
