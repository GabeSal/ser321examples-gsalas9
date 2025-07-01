package distributed;

import distributed.protocol.SubtaskRequest;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LeaderTest {

    @Test
    void testComputeWithDelayAccuracy() {
        List<Integer> input = List.of(1, 2, 3, 4);
        int result = Leader.computeWithDelay(input, 0);
        assertEquals(10, result, "Sum should be 10");
    }

    @Test
    void testComputeWithDelayTiming() {
        List<Integer> input = List.of(1, 2, 3);
        int delay = 100;  // ms
        long start = System.currentTimeMillis();
        Leader.computeWithDelay(input, delay);
        long end = System.currentTimeMillis();

        assertTrue(end - start >= delay * input.size(), "Delay timing not respected");
    }

    @Test
    void testPartitionEvenSplit() {
        List<Integer> input = List.of(1, 2, 3, 4, 5, 6);
        List<List<Integer>> parts = Leader.partitionList(input, 3);
        assertEquals(3, parts.size());
        assertEquals(List.of(1, 2), parts.get(0));
        assertEquals(List.of(3, 4), parts.get(1));
        assertEquals(List.of(5, 6), parts.get(2));
    }

    @Test
    void testPartitionUnevenSplit() {
        List<Integer> input = List.of(1, 2, 3, 4, 5);
        List<List<Integer>> parts = Leader.partitionList(input, 3);
        assertEquals(3, parts.size());
        assertEquals(2, parts.get(0).size());  // first gets extra
        assertEquals(2, parts.get(1).size());
        assertEquals(1, parts.get(2).size());
    }

    @Test
    void testSubtaskSerialization() throws Exception {
        SubtaskRequest original = SubtaskRequest.newBuilder()
                .addAllList(List.of(1, 2, 3))
                .setDelayMs(100)
                .build();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        original.writeDelimitedTo(baos);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        SubtaskRequest parsed = SubtaskRequest.parseDelimitedFrom(bais);

        assertEquals(original.getListList(), parsed.getListList());
        assertEquals(original.getDelayMs(), parsed.getDelayMs());
    }

}