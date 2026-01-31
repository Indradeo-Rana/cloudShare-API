package in.indra.cloudshareapi;

import me.paulschwarz.springdotenv.DotenvPropertySource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static me.paulschwarz.springdotenv.DotenvPropertySource.*;

@SpringBootApplication
public class CloudshareapiApplication {

	public static void main(String[] args) {
		// Add this line BEFORE SpringApplication.run
		addToEnvironment();
		SpringApplication.run(CloudshareapiApplication.class, args);
	}

	private static void addToEnvironment() {
	}

}
