package in.indra.cloudshareapi.config;


import in.indra.cloudshareapi.security.ClerkJwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration // Tells Spring: “This class contains beans and configuration rules”
@EnableWebSecurity // activates the web security features of Spring Security
@RequiredArgsConstructor
public class SecurityConfig {

    private final ClerkJwtAuthFilter clerkJwtAuthFilter;

    @Bean  // Rules for every incoming HTTP request
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception{
        httpSecurity.cors(Customizer.withDefaults())  // both frontend and backend are on different ports so cors says “I trust this frontend, allow it”
                .csrf(AbstractHttpConfigurer::disable)  // it is used in session-based authentication
                .authorizeHttpRequests(auth -> auth.requestMatchers("/webhooks/**",
                                "/api/v1.0/webhooks/**",
                                "/files/download/**",
                                "/api/v1.0/check",
                                "/files/public/**")
                        .permitAll().anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // here STATELESS means backend does not remember user and every request must contain token
                .addFilterBefore(clerkJwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();  //
    }

    @Bean
    public CorsFilter corsFilter(){
        return  new CorsFilter(corsConfigurationSource());
    }

    private UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("https://rainbow-liger-0af62d.netlify.app/")); //  before it was "http://localhost:5173"
        config.setAllowedMethods(List.of("GET", "POST", "PUT","PATCH", "DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization" , "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}