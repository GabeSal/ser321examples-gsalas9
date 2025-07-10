package example.grpcclient;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import service.CaesarGrpc;
import service.PasswordList;
import service.PasswordReq;
import service.PasswordRes;
import service.SaveReq;
import service.SaveRes;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple in-memory store that:
 *  - on encrypt: picks a random Caesar shift key, applies it to the password,
 *    and remembers both the key and ciphertext under the given name.
 *  - on decrypt: looks up the name, reverses the shift, and returns the plaintext.
 *  - on listPasswords: returns the set of all names stored.
 */
public class CaesarImpl extends CaesarGrpc.CaesarImplBase {

    private static class Entry {
        final int key;
        final String encrypted;
        Entry(int key, String encrypted) {
            this.key = key;
            this.encrypted = encrypted;
        }
    }

    // thread-safe map of name → (shift key + ciphertext)
    private final Map<String,Entry> store = new ConcurrentHashMap<>();
    private final Random rng = new Random();

    @Override
    public void encrypt(SaveReq req, StreamObserver<SaveRes> obs) {
        String name = req.getName();
        String pw   = req.getPassword();

        // Basic validation
        if (name == null || name.isEmpty() || pw == null || pw.isEmpty()) {
            SaveRes err = SaveRes.newBuilder()
                    .setOk(false)
                    .setError("Both name and password must be non-empty")
                    .build();
            obs.onNext(err);
            obs.onCompleted();
            return;
        }

        // Pick a shift in [1..25]
        int shift = rng.nextInt(25) + 1;
        StringBuilder cipher = new StringBuilder(pw.length());
        for (char c : pw.toCharArray()) {
            cipher.append((char)(c + shift));
        }

        // Store it
        store.put(name, new Entry(shift, cipher.toString()));

        SaveRes ok = SaveRes.newBuilder()
                .setOk(true)
                .build();
        obs.onNext(ok);
        obs.onCompleted();
    }

    @Override
    public void decrypt(PasswordReq req, StreamObserver<PasswordRes> obs) {
        String name = req.getName();
        Entry e = store.get(name);
        if (e == null) {
            PasswordRes notFound = PasswordRes.newBuilder()
                    .setOk(false)
                    .setError("No password saved under name: " + name)
                    .build();
            obs.onNext(notFound);
            obs.onCompleted();
            return;
        }

        // Reverse the shift
        StringBuilder plain = new StringBuilder(e.encrypted.length());
        for (char c : e.encrypted.toCharArray()) {
            plain.append((char)(c - e.key));
        }

        PasswordRes ok = PasswordRes.newBuilder()
                .setOk(true)
                .setPassword(plain.toString())
                .build();
        obs.onNext(ok);
        obs.onCompleted();
    }

    @Override
    public void listPasswords(Empty unused, StreamObserver<PasswordList> obs) {
        PasswordList.Builder b = PasswordList.newBuilder();
        store.keySet().forEach(b::addPassList);
        obs.onNext(b.build());
        obs.onCompleted();
    }
}
