package com.guvi.busapp;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import javax.crypto.SecretKey;

public class GenerateKey {
    public static void main(String[] args) {
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        // This encoder produces standard Base64
        String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());
        System.out.println("Generated HS512 Key (Base64): " + base64Key);
    }
}