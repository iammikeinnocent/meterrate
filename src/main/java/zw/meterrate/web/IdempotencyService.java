package zw.meterrate.web;

import org.springframework.stereotype.Service;
import zw.meterrate.domain.IdempotencyKey;
import zw.meterrate.repository.IdempotencyKeyRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Replay protection for unsafe requests: an Idempotency-Key header plus a
 * request-body hash. A repeated key with the SAME body returns the stored
 * response (marked replayed); the same key with a DIFFERENT body is a client
 * bug and is rejected with 409.
 */
@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;

    public IdempotencyService(IdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    public record Outcome(boolean replayed, Integer status, String body) {}

    public Optional<Outcome> replay(String key, String requestBody) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        var requestHash = sha256(requestBody == null ? "" : requestBody);
        return repository.findById(key).map(existing -> {
            if (!existing.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflict(key);
            }
            return new Outcome(true, existing.getResponseStatus(), existing.getResponseBody());
        });
    }

    public void store(String key, String requestBody, int status, String body) {
        if (key == null || key.isBlank()) {
            return;
        }
        var record = new IdempotencyKey();
        record.setKey(key);
        record.setRequestHash(sha256(requestBody == null ? "" : requestBody));
        record.setResponseStatus(status);
        record.setResponseBody(body);
        repository.save(record);
    }

    public static class IdempotencyConflict extends RuntimeException {
        public IdempotencyConflict(String key) {
            super("idempotency key " + key + " was already used with a different request body");
        }
    }

    private static String sha256(String input) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
