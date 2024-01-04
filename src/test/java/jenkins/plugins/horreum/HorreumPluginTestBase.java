package jenkins.plugins.horreum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jenkins.plugins.horreum.junit.HorreumTestClientExtension;
import jenkins.plugins.horreum.junit.HorreumTestExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;


@ExtendWith(HorreumTestClientExtension.class)
public class HorreumPluginTestBase {
	public static final String HORREUM_UPLOAD_CREDENTIALS = "horreum-creds";
	public static final String HORREUM_USER = "user";
	public static final String HORREUM_PASSWORD = "secret";


	@RegisterExtension
	public JenkinsExtension j = new JenkinsExtension();
	private Map<Domain, List<Credentials>> credentials;

	void registerBasicCredential(String id, String username, String password) {
		credentials.get(Domain.global()).add(
				new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL,
						id, "", username, password));
		SystemCredentialsProvider.getInstance().setDomainCredentialsMap(credentials);
	}

	@BeforeEach
	public void init() {
		credentials = new HashMap<>();
		credentials.put(Domain.global(), new ArrayList<Credentials>());
		this.registerBasicCredential(HORREUM_UPLOAD_CREDENTIALS, HORREUM_USER, HORREUM_PASSWORD);

		HorreumGlobalConfig globalConfig = HorreumGlobalConfig.get();
		if (globalConfig != null) {
			globalConfig.setBaseUrl(HorreumTestExtension.HORREUM_BASE_URL);
		} else {
			System.out.println("Can not find Horreum Global Config");
		}
	}
}
