package com.nextcommunity.nextlearn;

import com.nextcommunity.nextlearn.configuration.FileStorageConfig;
<<<<<<< HEAD
=======
import com.nextcommunity.nextlearn.entity.User;
import com.nextcommunity.nextlearn.repository.UserRepository;
>>>>>>> 465e66e (update backend NextLearn)
import com.nextcommunity.nextlearn.services.document.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
<<<<<<< HEAD
=======
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
>>>>>>> 465e66e (update backend NextLearn)

@SpringBootApplication
@EnableConfigurationProperties({
		FileStorageConfig.class
})
public class NextlearnApplication implements CommandLineRunner {
	@Autowired
	private DocumentService documentService;
<<<<<<< HEAD
=======
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder encoder;

>>>>>>> 465e66e (update backend NextLearn)
	public static void main(String[] args) {
		SpringApplication.run(NextlearnApplication.class, args);

	}
	@Override
	public void run(String... args) throws Exception {
		// Corriger les chemins des documents existants
		documentService.fixDocumentPaths();
<<<<<<< HEAD
=======

		Optional<User> adminAccount = userRepository.findByUserrole("ADMIN");

		if (adminAccount.isEmpty()) {
			User newAdmin = new User();
			newAdmin.setNom("Giovanni");
			newAdmin.setPrenom("Charles");
			newAdmin.setClasse("Licence 3");
			newAdmin.setEmail("ebodegiovanni@gmail.com");
			newAdmin.setPassword(encoder.encode("nextlearngiovanni"));
			newAdmin.setUserrole("ADMIN");
			userRepository.save(newAdmin);
			System.out.println("Admin account created successfully at startup");
		} else {
			System.out.println("Admin already exists");
		}
>>>>>>> 465e66e (update backend NextLearn)
	}

}
