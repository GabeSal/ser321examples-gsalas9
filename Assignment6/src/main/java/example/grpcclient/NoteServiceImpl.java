package example.grpcclient;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import service.NoteServiceGrpc;
import service.CreateNoteRequest;
import service.CreateNoteResponse;
import service.GetNotesResponse;
import service.DeleteNoteRequest;
import service.DeleteNoteResponse;
import service.Note;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class NoteServiceImpl extends NoteServiceGrpc.NoteServiceImplBase {
    // Persist notes in memory
    private final Map<Long,String> store = new ConcurrentHashMap<>();
    private final AtomicLong nextId         = new AtomicLong(1);

    @Override
    public void createNote(CreateNoteRequest req,
                           StreamObserver<CreateNoteResponse> obs) {
        String text = req.getNote();
        if (text == null || text.isEmpty()) {
            obs.onNext(CreateNoteResponse.newBuilder()
                    .setOk(false)
                    .setError("Note cannot be empty")
                    .build());
            obs.onCompleted();
            return;
        }
        long id = nextId.getAndIncrement();
        store.put(id, text);

        obs.onNext(CreateNoteResponse.newBuilder()
                .setOk(true)
                .setId(id)
                .build());
        obs.onCompleted();
    }

    @Override
    public void getNotes(Empty _unused,
                         StreamObserver<GetNotesResponse> obs) {
        GetNotesResponse.Builder b = GetNotesResponse.newBuilder();
        store.forEach((id, note) ->
                b.addNotes(Note.newBuilder().setId(id).setNote(note).build()));
        obs.onNext(b.build());
        obs.onCompleted();
    }

    @Override
    public void deleteNote(DeleteNoteRequest req,
                           StreamObserver<DeleteNoteResponse> obs) {
        long id = req.getId();
        if (store.remove(id) != null) {
            obs.onNext(DeleteNoteResponse.newBuilder().setOk(true).build());
        } else {
            obs.onNext(DeleteNoteResponse.newBuilder()
                    .setOk(false)
                    .setError("No note with ID " + id)
                    .build());
        }
        obs.onCompleted();
    }
}