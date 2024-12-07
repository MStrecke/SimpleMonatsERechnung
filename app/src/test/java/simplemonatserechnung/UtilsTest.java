package simplemonatserechnung;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class UtilsTest {
    @Test
    void testNull2String() {
        assertEquals(Utils.null2String("abc"), "abc");
        assertEquals(Utils.null2String(null), "");
    }

    @Test
    void testExtendFilenameBeforExtension() {
        assertEquals(Utils.extendFilenameBeforExtension("abcde.fgh", "_addme"), "abcde_addme.fgh");
        assertEquals(Utils.extendFilenameBeforExtension("abcde.fgh", null), "abcde.fgh");
        assertEquals(Utils.extendFilenameBeforExtension(null, "abcd"), null);
    }
}
