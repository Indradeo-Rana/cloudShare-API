package in.indra.cloudshareapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class ClerkJwtAuthFilter extends OncePerRequestFilter {

    @Value("${clerk.issuer}")
    private String clerkIssuer;

    private final ClerkJwksProvider jwksProvider;

    @Override  //this method allows to  write our custom filter logic (JWT validation) & also check
    // "Should this request go forward or be blocked?"  & this method invoked automatically
    protected void doFilterInternal(HttpServletRequest request,  // contains URL, headers, Token HTTP method ,body(if needed)
                                    HttpServletResponse response, // used to send response back to client
                                    FilterChain filterChain)  //  remote control to next filter in chain
            throws ServletException, IOException {

        //  Allow webhooks without JWT or public (no login required) endpoints
        if (request.getRequestURI().contains("/webhooks")  ||
                request.getRequestURI().contains("public/") ||
                request.getRequestURI().contains("download/") ||
                request.getRequestURI().contains("/test")) {
            filterChain.doFilter(request, response);
            return;
        }

        // if required token, then read Authorization header
        String authHeader = request.getHeader("Authorization");

        //  Fixed condition or private ( login required) endpoints
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Authorization header missing or invalid...");
            return;
        }

        try {
            String token = authHeader.substring(7);
            String[] chunks = token.split("\\.");  // header.payload.signature

            if (chunks.length < 3) { // JWT should have 3 parts i.e  header.payload.signature
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Invalid JWT token format so plz check it once!");
                return;
            }

            String headerJson =
                    new String(Base64.getUrlDecoder().decode(chunks[0]));

            ObjectMapper mapper = new ObjectMapper();
            JsonNode headerNode = mapper.readTree(headerJson);

            if (!headerNode.has("kid")) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "JWT token missing 'kid' in header. Something is wrong!");
                return;
            }

            String kid = headerNode.get("kid").asText();
            PublicKey publicKey = jwksProvider.getPublicKey(kid);

            // verify token
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .setAllowedClockSkewSeconds(60)
                    .requireIssuer(clerkIssuer)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String clerkId = claims.getSubject(); // extract Authenticated user identity

            // store the authentication info in SecurityContext like who is the user , it's role etc
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            clerkId,
                            null,
                            Collections.singletonList(
                                    new SimpleGrantedAuthority("ROLE_ADMIN")
                            )
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authenticationToken);

            filterChain.doFilter(request, response);  // continue the filter chain

        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Invalid JWT token auth : " + e.getMessage());
            return;
        }
    }
}
