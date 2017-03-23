package co.zync.zync;

import co.zync.zync.utils.ZyncCrypto;
import org.junit.Test;

import java.util.Arrays;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        byte[] data = new byte[] {-126, 9, 79, 108, 106, -103, 81, 88, -101, -122, -77, 67, -34, 44, -44, 96, 83, -72, 112, 96, -128, 38, 61};
        System.out.println(new String(ZyncCrypto.decrypt(data, "password", "salt".getBytes(), "iv".getBytes())));
    }
}