package jenkins.plugins.horreum;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.runner.Description;

// Copied from https://issues.jenkins.io/browse/JENKINS-48466
public class JenkinsExtension extends org.jvnet.hudson.test.JenkinsRule implements BeforeEachCallback, AfterEachCallback {
   @Override
   public void beforeEach(ExtensionContext context) throws Exception {
      this.testDescription = Description.createTestDescription(
            context.getTestClass().map(Class::getName).orElse(null),
            context.getTestMethod().map(Method::getName).orElse(null)
      );
      try {
         before();
      } catch (Throwable throwable) {
         throw new Exception(throwable);
      }
   }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        after();
    }

   @Override
   public void recipe() throws Exception {
   }
}
