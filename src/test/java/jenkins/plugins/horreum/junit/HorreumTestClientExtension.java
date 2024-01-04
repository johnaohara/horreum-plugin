package jenkins.plugins.horreum.junit;

import io.hyperfoil.tools.HorreumClient;
import io.hyperfoil.tools.horreum.api.data.Test;
import org.junit.Assert;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static jenkins.plugins.horreum.HorreumPluginTestBase.HORREUM_PASSWORD;
import static jenkins.plugins.horreum.HorreumPluginTestBase.HORREUM_USER;
import static org.junit.Assert.assertNull;

public class HorreumTestClientExtension extends HorreumTestExtension implements BeforeEachCallback, AfterEachCallback {

    public static HorreumClient horreumClient;

    public static Test dummyTest;

    protected void initialiseRestClients() {
        System.out.println(String.format("Horreum URL: %s ; Horreum User: %s ; Horreum Password: %s", HORREUM_BASE_URL, HORREUM_USER, HORREUM_PASSWORD));
        horreumClient = new HorreumClient.Builder()
                .horreumUrl(HORREUM_BASE_URL + "/")
                .horreumUser(HORREUM_USER)
                .horreumPassword(HORREUM_PASSWORD)
                .build();

        Assert.assertNotNull(horreumClient);
    }

    @Override
    protected void beforeSuite(ExtensionContext context) throws Exception {
        super.beforeSuite(context);
        initialiseRestClients();
    }

    @Override
    public void close() throws Throwable  {
        horreumClient.close();
        super.close();
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        assertNull(dummyTest);
        Test test = new Test();
        test.name = context.getUniqueId();
        test.owner = "dev-team";
        test.description = "This is a dummy test";
        dummyTest = horreumClient.testService.add(test);
        Assert.assertNotNull(dummyTest);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        horreumClient.testService.delete(dummyTest.id);
        dummyTest = null;
    }
}
