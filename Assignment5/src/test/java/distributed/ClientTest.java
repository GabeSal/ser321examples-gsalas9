package distributed;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClientTest {

    @Test
    void testParseListValidInput() {
        String input = "1, 2, 3,4";
        List<Integer> result = Client.parseList(input);
        assertEquals(List.of(1, 2, 3, 4), result);
    }

    @Test
    void testParseListEmptyThrows() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            Client.parseList("");
        });
        assertTrue(ex.getMessage().contains("List cannot be empty"));
    }

    @Test
    void testParseListInvalidThrows() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            Client.parseList("a, b, 3");
        });
        assertTrue(ex.getMessage().contains("comma-separated integers"));
    }
}