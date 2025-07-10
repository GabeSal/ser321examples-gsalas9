package example.grpcclient;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import service.EchoGrpc;
import service.JokeGrpc;
import service.ClientRequest;
import service.ServerResponse;
import service.JokeReq;
import service.JokeRes;
import service.JokeSetReq;
import service.JokeSetRes;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ServerTest {
    private static Server server;
    private static ManagedChannel channel;
    private static EchoGrpc.EchoBlockingStub echoStub;
    private static JokeGrpc.JokeBlockingStub jokeStub;

    @BeforeClass
    public static void startServer() throws IOException {
        // Start your gRPC server on port 9002 (match your Node/main)
        server = ServerBuilder.forPort(9002)
                .addService(new EchoImpl())
                .addService(new JokeImpl())
                .build()
                .start();

        // Create a channel & stubs to talk to it
        channel  = ManagedChannelBuilder.forAddress("localhost", 9002)
                .usePlaintext()
                .build();
        echoStub = EchoGrpc.newBlockingStub(channel);
        jokeStub = JokeGrpc.newBlockingStub(channel);
    }

    @AfterClass
    public static void stopServer() throws InterruptedException {
        if (channel != null) {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
        if (server != null) {
            server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void parrot() {
        // success case
        ClientRequest okReq = ClientRequest.newBuilder().setMessage("test").build();
        ServerResponse okRes = echoStub.parrot(okReq);
        assertTrue(okRes.getIsSuccess());
        assertEquals("test", okRes.getMessage());

        // no-message cases
        ClientRequest emptyReq = ClientRequest.newBuilder().setMessage("").build();
        ServerResponse emptyRes = echoStub.parrot(emptyReq);
        assertFalse(emptyRes.getIsSuccess());
        assertEquals("No message provided", emptyRes.getError());

        ClientRequest nullReq = ClientRequest.newBuilder().build();
        ServerResponse nullRes = echoStub.parrot(nullReq);
        assertFalse(nullRes.getIsSuccess());
        assertEquals("No message provided", nullRes.getError());
    }

    @Test
    public void joke() {
        // initial list of jokes (must match your JokeImpl)
        JokeReq one = JokeReq.newBuilder().setNumber(1).build();
        JokeRes r1 = jokeStub.getJoke(one);
        assertEquals(1, r1.getJokeCount());
        assertEquals("Did you hear the rumor about butter? Well, I'm not going to spread it!",
                r1.getJoke(0));

        // next two
        JokeReq two = JokeReq.newBuilder().setNumber(2).build();
        JokeRes r2 = jokeStub.getJoke(two);
        assertEquals(2, r2.getJokeCount());
        assertEquals("What do you call someone with no body and no nose? Nobody knows.",
                r2.getJoke(0));
        assertEquals("I don't trust stairs. They're always up to something.",
                r2.getJoke(1));

        // exhaust remaining jokes
        JokeRes r3 = jokeStub.getJoke(two);
        assertEquals(2, r3.getJokeCount());
        assertEquals("How do you get a squirrel to like you? Act like a nut.", r3.getJoke(0));
        assertEquals("I am out of jokes...",                      r3.getJoke(1));

        // out‐of‐jokes again
        JokeRes r4 = jokeStub.getJoke(two);
        assertEquals(1, r4.getJokeCount());
        assertEquals("I am out of jokes...", r4.getJoke(0));

        // setJoke error: no payload
        JokeSetReq bad1 = JokeSetReq.newBuilder().build();
        JokeSetRes sr1  = jokeStub.setJoke(bad1);
        assertFalse(sr1.getOk());

        // setJoke error: empty string
        JokeSetReq bad2 = JokeSetReq.newBuilder().setJoke("").build();
        JokeSetRes sr2  = jokeStub.setJoke(bad2);
        assertFalse(sr2.getOk());

        // add a new one
        JokeSetReq add = JokeSetReq.newBuilder().setJoke("whoop").build();
        JokeSetRes sr3  = jokeStub.setJoke(add);
        assertTrue(sr3.getOk());

        // then it should be the *only* joke returned
        JokeReq oneMore = JokeReq.newBuilder().setNumber(1).build();
        JokeRes r5      = jokeStub.getJoke(oneMore);
        assertEquals(1, r5.getJokeCount());
        assertEquals("whoop", r5.getJoke(0));
    }
}