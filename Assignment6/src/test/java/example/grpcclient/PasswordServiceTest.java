package example.grpcclient;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.*;
import service.CaesarGrpc;
import service.PasswordReq;
import service.PasswordRes;
import service.SaveReq;
import service.SaveRes;
import service.PasswordList;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class PasswordServiceTest {
    private static io.grpc.Server server;
    private static CaesarGrpc.CaesarBlockingStub stub;

    @BeforeClass
    public static void startServer() throws Exception {
        // build & start an in-process or on-port server
        server = io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
                .forPort(9002)
                .addService(new example.grpcclient.CaesarImpl())
                .build()
                .start();

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 9002)
                .usePlaintext()
                .build();
        stub = CaesarGrpc.newBlockingStub(channel);
    }

    @AfterClass
    public static void stopServer() throws Exception {
        server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    public void encryptAndDecryptFlow() {
        // happy path
        SaveRes cres = stub.encrypt(
                SaveReq.newBuilder()
                        .setName("alice")
                        .setPassword("Secret123")
                        .build());
        assertTrue(cres.getOk());

        // listPasswords should contain "alice"
        PasswordList list = stub.listPasswords(Empty.getDefaultInstance());
        assertTrue(list.getPassListList().contains("alice"));

        // decrypt should return the original
        PasswordRes pres = stub.decrypt(
                PasswordReq.newBuilder()
                        .setName("alice")
                        .build());
        assertTrue(pres.getOk());
        assertEquals("Secret123", pres.getPassword());
    }

    @Test
    public void encryptEmptyNameOrPassword() {
        // empty name
        SaveRes r1 = stub.encrypt(
                SaveReq.newBuilder()
                        .setName("")
                        .setPassword("pw")
                        .build());
        assertFalse(r1.getOk());
        assertEquals("Both name and password must be non-empty", r1.getError());

        // empty password
        SaveRes r2 = stub.encrypt(
                SaveReq.newBuilder()
                        .setName("bob")
                        .setPassword("")
                        .build());
        assertFalse(r2.getOk());
    }

    @Test
    public void decryptNonexistent() {
        PasswordRes res = stub.decrypt(
                PasswordReq.newBuilder()
                        .setName("no_such")
                        .build());
        assertFalse(res.getOk());
        assertTrue(res.getError().contains("No password saved under name"));
    }
}