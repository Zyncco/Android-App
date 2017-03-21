package co.zync.zync;

import android.util.Base64;
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
        String input = "ayylmao";
        byte[] data = ZyncCrypto.encrypt(input.getBytes("UTF-8"), "seCr3_tp4_z");
        byte[] hash = ZyncCrypto.hash(data);

        System.out.println(Arrays.toString(data));
        System.out.println(new String(ZyncCrypto.decrypt(data, "seCr3_tp4_z")));
    }
}