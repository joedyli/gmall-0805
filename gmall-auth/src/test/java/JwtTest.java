import com.atguigu.core.utils.JwtUtils;
import com.atguigu.core.utils.RsaUtils;
import org.junit.Before;
import org.junit.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    private static final String pubKeyPath = "D:\\project-0805\\rsa\\rsa.pub";

    private static final String priKeyPath = "D:\\project-0805\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "sdfdsf2432@#@LJsdo3454E");
    }

    @Before
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 1);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6MiwidXNlck5hbWUiOiJmZW5nZ2UiLCJleHAiOjE1NzkwNzc5MDF9.PNRv80tYTIG5NktCuuRjQ_EkNvi6R9CK9QsbF60Enq7cW6gcmZcV06-53TihDGvaYtvpa1q9-vPDrGlK7mE2GO49fuvFAYb8Mt3EMGAtDQP5IW8KWv4V2M6bkgXPwZW7Tp_6pw4w8AgiF1zQCA12Va-R1zdc-Fngti-FjsBMMcvxiiKE78DquZw5_p3RqkYqr8XMHgQ1L5QHk0-vJuQiuPY_zoezEP2Su3zvK8OWp0lsXoYqAhqXrOpiGHF5itU6VCIAwt00oZscW3rHF1xEVoJK_nMhXP7FWvQjENa40V5wp1spcHuNDiu3LPkprGEqRsgVLX6ct8cx2n9oS91JlQ";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}
