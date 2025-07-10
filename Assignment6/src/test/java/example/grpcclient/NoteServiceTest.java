package example.grpcclient;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.*;
import service.*;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class NoteServiceTest {
    private static io.grpc.Server server;
    private static NoteServiceGrpc.NoteServiceBlockingStub stub;

    @BeforeClass
    public static void startServer() throws Exception {
        // build & start an in-process or on-port server
        server = io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
                .forPort(9002)
                .addService(new example.grpcclient.NoteServiceImpl())
                .build()
                .start();

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 9002)
                .usePlaintext()
                .build();
        stub = NoteServiceGrpc.newBlockingStub(channel);
    }

    @AfterClass
    public static void stopServer() throws Exception {
        server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    public void createNote_and_listAndDelete() {
        // create
        CreateNoteResponse cres = stub.createNote(
                CreateNoteRequest.newBuilder()
                        .setNote("Integration test note")
                        .build());
        assertTrue(cres.getOk());
        long id = cres.getId();
        assertTrue(id > 0);

        // list: must include our note
        GetNotesResponse list = stub.getNotes(Empty.getDefaultInstance());
        boolean found = list.getNotesList().stream()
                .anyMatch(n -> n.getId() == id && "Integration test note".equals(n.getNote()));
        assertTrue("Created note should appear in list", found);

        // delete
        DeleteNoteResponse dres = stub.deleteNote(
                DeleteNoteRequest.newBuilder()
                        .setId(id)
                        .build());
        assertTrue(dres.getOk());
    }

    @Test
    public void createEmptyNote_fails() {
        CreateNoteResponse res = stub.createNote(
                CreateNoteRequest.newBuilder()
                        .setNote("")
                        .build());
        assertFalse(res.getOk());
        assertEquals("Note cannot be empty", res.getError());
    }

    @Test
    public void deleteNonexistentNote_fails() {
        DeleteNoteResponse res = stub.deleteNote(
                DeleteNoteRequest.newBuilder()
                        .setId(999999)
                        .build());
        assertFalse(res.getOk());
        assertTrue(res.getError().contains("No note with ID"));
    }
}