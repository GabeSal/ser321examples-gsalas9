package distributed;

import distributed.protocol.SubtaskRequest;
import distributed.protocol.SubtaskResult;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NodeTest {

    @Test
    void testCorrectComputationWithDelay() {
        List<Integer> input = List.of(1, 2, 3);
        int result = Node.computeWithDelay(input, 0);
        assertEquals(6, result);
    }

    @Test
    void testFaultyComputationWithDelay() {
        List<Integer> input = List.of(2, 3, 4);
        int result = Node.computeFaulty(input, 0);  // 2 * 3 * 4 = 24
        assertEquals(24, result);
    }

    @Test
    void testSubtaskRequestSerialization() throws IOException {
        SubtaskRequest request = SubtaskRequest.newBuilder()
                .addAllList(List.of(5, 10, 15))
                .setDelayMs(100)
                .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        request.writeDelimitedTo(out);

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        SubtaskRequest parsed = SubtaskRequest.parseDelimitedFrom(in);

        assertNotNull(parsed);
        assertEquals(request.getListList(), parsed.getListList());
        assertEquals(request.getDelayMs(), parsed.getDelayMs());
    }

    @Test
    void testSubtaskResultSerialization() throws IOException {
        SubtaskResult result = SubtaskResult.newBuilder()
                .setSum(42)
                .setNodeId("node-xyz")
                .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        result.writeDelimitedTo(out);

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        SubtaskResult parsed = SubtaskResult.parseDelimitedFrom(in);

        assertNotNull(parsed);
        assertEquals(42, parsed.getSum());
        assertEquals("node-xyz", parsed.getNodeId());
    }
}