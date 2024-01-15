package jenkins.plugins.horreum.junit;

import io.hyperfoil.tools.horreum.infra.common.HorreumResources;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;

import java.util.Map;

import static io.hyperfoil.tools.horreum.infra.common.Const.DEFAULT_KC_ADMIN_PASSWORD;
import static io.hyperfoil.tools.horreum.infra.common.Const.DEFAULT_KC_ADMIN_USERNAME;
import static io.hyperfoil.tools.horreum.infra.common.Const.DEFAULT_KC_DB_PASSWORD;
import static io.hyperfoil.tools.horreum.infra.common.Const.DEFAULT_KC_DB_USERNAME;
import static io.hyperfoil.tools.horreum.infra.common.Const.DEFAULT_KEYCLOAK_NETWORK_ALIAS;
import static io.hyperfoil.tools.horreum.infra.common.Const.DEFAULT_POSTGRES_NETWORK_ALIAS;
import static io.hyperfoil.tools.horreum.infra.common.Const.HORREUM_DEV_KEYCLOAK_ADMIN_PASSWORD;
import static io.hyperfoil.tools.horreum.infra.common.Const.HORREUM_DEV_KEYCLOAK_ADMIN_USERNAME;
import static io.hyperfoil.tools.horreum.infra.common.Const.HORREUM_DEV_KEYCLOAK_DB_PASSWORD;
import static io.hyperfoil.tools.horreum.infra.common.Const.HORREUM_DEV_KEYCLOAK_DB_USERNAME;
import static io.hyperfoil.tools.horreum.infra.common.Const.HORREUM_DEV_KEYCLOAK_IMAGE;
import static io.hyperfoil.tools.horreum.infra.common.Const.HORREUM_DEV_KEYCLOAK_NETWORK_ALIAS;
import static io.hyperfoil.tools.horreum.infra.common.Const.HORREUM_DEV_POSTGRES_IMAGE;
import static io.hyperfoil.tools.horreum.infra.common.Const.HORREUM_DEV_POSTGRES_NETWORK_ALIAS;

public class HorreumTestExtension implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

    public static String HORREUM_BASE_URL;
    private static boolean started = false;

    private static GenericContainer<?> horreumContainer;

//    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        synchronized (HorreumTestExtension.class) {
            if (!started) {
                beforeSuite(extensionContext);
                started = true;
            }
        }
    }

//    @Override
    public void close() throws Throwable {
        synchronized (HorreumTestExtension.class) {
            try {
                stopContainers();
                started = false;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    protected void beforeSuite(ExtensionContext context) throws Exception {
        if ( !started )
            HORREUM_BASE_URL = startContainers();
    }

    private String startContainers() throws Exception {

        try {

            String keycloakImage = "quay.io/keycloak/keycloak:23.0.3"; // getProperty(HORREUM_DEV_KEYCLOAK_IMAGE);
            String postgresImage = "postgres:16"; //getProperty(HORREUM_DEV_POSTGRES_IMAGE);

            if (keycloakImage == null || postgresImage == null) {
                throw new RuntimeException("Test container images are not defined");
            }

            //todo: pick up from configuration
            Map<String, String> containerArgs = Map.of(
                    HORREUM_DEV_KEYCLOAK_IMAGE, keycloakImage,
                    HORREUM_DEV_KEYCLOAK_NETWORK_ALIAS, DEFAULT_KEYCLOAK_NETWORK_ALIAS,
                    HORREUM_DEV_POSTGRES_IMAGE, postgresImage,
                    HORREUM_DEV_POSTGRES_NETWORK_ALIAS, DEFAULT_POSTGRES_NETWORK_ALIAS,
                    HORREUM_DEV_KEYCLOAK_DB_USERNAME, DEFAULT_KC_DB_USERNAME,
                    HORREUM_DEV_KEYCLOAK_DB_PASSWORD, DEFAULT_KC_DB_PASSWORD,
                    HORREUM_DEV_KEYCLOAK_ADMIN_USERNAME, DEFAULT_KC_ADMIN_USERNAME,
                    HORREUM_DEV_KEYCLOAK_ADMIN_PASSWORD, DEFAULT_KC_ADMIN_PASSWORD

            );
            Map<String, String> infraEnv = HorreumResources.startContainers(containerArgs);

//         TODO:: start Horreum container

            horreumContainer = new GenericContainer<>("quay.io/hyperfoil/horreum:0cc5268e3bbf4dec64c715eac0d11178a7d45168").withExposedPorts(8080);
            horreumContainer.addEnv("KC_HTTPS_CERTIFICATE_FILE", "/tmp/keycloak-tls.crt");
            horreumContainer.addEnv("QUARKUS_DATASOURCE_JDBC_URL", infraEnv.get("quarkus.datasource.jdbc.url").replaceAll("localhost", "172.17.0.1"));
            horreumContainer.addEnv("QUARKUS_DATASOURCE_MIGRATION_JDBC_URL", infraEnv.get("quarkus.datasource.migration.jdbc.url").replaceAll("localhost", "172.17.0.1"));
            horreumContainer.addEnv("QUARKUS_DATASOURCE_USERNAME", infraEnv.get("horreum.db.username"));
            horreumContainer.addEnv("QUARKUS_HTTP_HTTP2", "false");
//            horreumContainer.addEnv("HORREUM_URL", "${HORREUM_HORREUM_URL}");
            horreumContainer.addEnv("QUARKUS_DATASOURCE_MIGRATION_PASSWORD", infraEnv.get("horreum.db.password"));
            horreumContainer.addEnv("QUARKUS_DATASOURCE_JDBC_ADDITIONAL_JDBC_PROPERTIES_SSL", "false");
            horreumContainer.addEnv("QUARKUS_DATASOURCE_JDBC_ADDITIONAL_JDBC_PROPERTIES_SSLMODE", "disable");
            horreumContainer.addEnv("QUARKUS_DATASOURCE_PASSWORD", infraEnv.get("horreum.db.password"));
            horreumContainer.addEnv("QUARKUS_OIDC_AUTH_SERVER_URL", infraEnv.get("quarkus.oidc.auth-server-url").replaceAll("localhost", "172.17.0.1"));
            horreumContainer.addEnv("HORREUM_KEYCLOAK_URL", infraEnv.get("keycloak.host").replaceAll("localhost", "172.17.0.1"));
            horreumContainer.addEnv("QUARKUS_LOG_LEVEL", "INFO");

            horreumContainer.setNetwork(HorreumResources.getNetwork());


//         log.info("Waiting for Horreum infrastructure to start");


//         writeOutputToFile(horreumContainer, "horreum_1");
            horreumContainer.start();
            HorreumResources.waitForContainerReady(horreumContainer, "started in");

            return String.format("http://localhost:%d", horreumContainer.getMappedPort(8080));
        } catch (Exception e) {
//         log.fatal("Could not start Horreum services", e);
            HorreumResources.stopContainers();
            throw e;
        }

    }

    private void stopContainers() throws Exception {
        try {
//         log.info("Stopping Horreum IT resources");
            HorreumResources.stopContainers();

//         TODO:: Stop horreum container
            if ( horreumContainer != null ) {
                horreumContainer.stop();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
