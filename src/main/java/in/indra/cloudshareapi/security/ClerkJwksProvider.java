package in.indra.cloudshareapi.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigInteger;
import java.net.URI;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component  // spring creates 1 instance of this class &  Lifecycle is managed by spring
public class ClerkJwksProvider {

    @Value("${clerk.jwks-url}")
    private String jwksUrl; // fetching URL from application.properties file

    // backend fetches keys once and store in memory for 1 hour to avoid frequent network calls
    private final Map<String, PublicKey> keyCache  = new HashMap<>();
    private long lastFetchTime = 0;
    private static final long CACHE_TTL = 3600000; // 1 hour

    // get public keyId from cache or fetch from clerk
    public PublicKey getPublicKey (String kid) throws Exception{
        if(keyCache.containsKey(kid) && System.currentTimeMillis() - lastFetchTime < CACHE_TTL){ // check if keyId is already cached and cached
            return keyCache.get(kid);
        }
        refreshKeys();  // calling method to fetch keys from clerk
        return keyCache.get(kid);
    }

    private void refreshKeys() throws Exception {
        ObjectMapper mapper = new ObjectMapper(); // jackson object mapper to parse JSON
        JsonNode jwks = mapper.readTree(URI.create(jwksUrl).toURL().openStream()); // makes HTTP call to fetch keys from clerk

        JsonNode keys = jwks.get("keys");
        for(JsonNode keyNode : keys){
            String kid = keyNode.get("kid").asText();
            String kty = keyNode.get("kty").asText();
            String alg = keyNode.get("alg").asText();

            if("RSA".equals(kty) && "RS256".equals(alg)){
                String n = keyNode.get("n").asText();
                String e = keyNode.get("e").asText();
                PublicKey publicKey = createPublicKey(n, e); // pass modulus and exponent to create public key and call method
                keyCache.put(kid, publicKey);
            }
        }
        lastFetchTime = System.currentTimeMillis();
    }

    private PublicKey createPublicKey(String modulus, String exponent) throws  Exception {
        byte[] modulusBytes = Base64.getUrlDecoder().decode(modulus); // decode base64url encoded strings
        byte[] exponentBytes = Base64.getUrlDecoder().decode(exponent);

        BigInteger modulusInt = new BigInteger(1, modulusBytes); // convert byte arrays to big integers
        BigInteger exponentInt = new BigInteger(1, exponentBytes);

        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulusInt, exponentInt); // convert raw JSON data into a real cryptographic public key
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(keySpec);
    }

}