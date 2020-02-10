package com.atguigu.gmall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;

import com.atguigu.gmall.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    //在支付宝创建的应用的id
    private   String app_id = "2016101200666477";

    // 商户私钥，您的PKCS8格式RSA2私钥
    private  String merchant_private_key = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCffV/3au6Hv3axQHk5t5QCGVjVZO3EeurcFV8D7ZyeXK/r77ai9yLWuTk3yGCSkfjDvyebO+6UXE3FfxWRcVRypFn6RA115ZHN4Bsi9OPIsBAoS2WjCj6NPfNDj5dPWzRDdLTjM+x82Ot9HNZmmFNjQy6wyEAsz0KhL2yRdE6q+khTsWZaTS5gk248LIZ/XGrxSmHgsVT7yodMQ9myX5Z7uV71AD06RCtdBiJSj40kGz2bccxhMt/BsB6Z4EaZjJcQerBVtwavyC/W5OFWZR0JtqYJOXINpTSOCR9XNZn6k0g5k3YtOehvnQr6YuVSUtEO0691QcLSb0SXMLRqfDZFAgMBAAECggEALN4zkkJV6oCVnpX5xJ/9ln2U/SuQrFa8HcAxY8SD7BU9NI3SfpQyC+A91ZCcgn9oUYFEXiqFGt3Az0/KPIl6bWoJGhvtAX7c/uMOH05vinTlhsB3Tl/Cay/DcP1DXwLUeCmr/cMMw66uyRrEwkYWJ3Wt+/PKAhEouHnD/EORIg4kqRtwvb+5o9dkMpjpSMmaM0uTYu9WV+iVtspsvTIAKENXLB3cOWBrQ81LeO5rIQa+3ZHfsZOst74zzOPB+DW6vI2CBR5njbi89JFploVFz8YYXSpGLT3FFN1ZAw0nNqaQOP//tYowgdJ4YyXS/K9v0L369L/LD1hy7IOzi+Q+PQKBgQD8ZA7kpmZaBn/cG561SEJ0tZgRYzJHZNgNvyMIWvcGzKQRfH8uo8TOJo1Hrb4i48sTqIOnt4O6r6TveHYiTSjC7qkLMYoyyBU0+kviBDQlAcG3bfmA8Za9A09OI2EDxHSWzEXrwMcjYEpGxBBKO/Zp7FE0Ib6VNWaAC4n17jhzXwKBgQChxTptBnE3lLoNic07dFmqvb9pme/Wi6xm3t40FXUtjdllaoyTr/fAyzczj53xKrIca6Mh0vIUtUAiaRT1YYqKhe1hLccYlEJnYjH3svVpNUPlE9/2jy3F/3dLmCJeFyi/ZsYYa1bDSEtVwugXVpZvi/TGPX581woKnD/QFWD82wKBgBRPkQtBgNcZ0sxJxLnd+Msfmf88Nl2cde6VRSJ0/5Aig8mMdUexkjLs003DY5u3LS6FzyJ+GDG71NAYp5kXEIKvZhcqg26Wv24l/llP8UpRG9/BD+UajADl9UnaewIsmx1Pjwjr2Jq2MwQC3zS6IphfliFV3dpwE7GWPCH4a76NAoGAJe8h7D7+fZIaruIJhk+aVzd3k0PNwfmuy9CnOn9toukIeYeqv1ccOdFteGHNH6hra86xylS0/7Yg+/C4QXJGEoWlTIx3i4P4rlWQcg09Dxa2fRlUA3U/vpZRWyCprnavz3JCMae6CMEDKXEzC4X6cN3Y4EO7TXr/l81cCWd5/tECgYEA4tE0ES+rTdciT5Vf+/+BepfPM2DK/DSuZQAagnA12ZgAWhJRtsLFRVIl/wCq04CVZcjwfowu8leNeqJAHM2opOoqvsGI8h55DIdJdy6HumVehlR/urRUSvXeL6bIRtBdNLG35yooGjdZE50v9Dn4rz6jpbugGRAA21pnrF05NZM=";
    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    private  String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAm0CbzDSMv9508/xEgXOciNrNyvR6pdJVDQ7iuXpJNXcniJB94G+wB3jn8EDygAwsdYHiQH8B6kp7clQ0tY3EEVy7w8/gleOT5zSG5eqLjtHy7VdWsWiQbDH6Tsl5ZMrr92Ki0QbWxepL642BG/8EeKsi3+/wdSWPV0Ujbw1+lpTID5wpiQbpCYOOs5bAuhJeqJNa4ovVPm238CEzmXCYmpH4fnsh6pjO/BiSLrdMx8Z2xy+cjQ3wcEF0R8vGgOz+q9yiuVkXaPU6emLJ/m8baRlkREszl80AghKNgBuV0771jsRykA81oR+9iJo/tdpvqisGW4TLirnrq1iDLDHZOQIDAQAB";
    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    private  String notify_url;

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    private  String return_url;

    // 签名方式
    private  String sign_type = "RSA2";

    // 字符编码格式
    private  String charset = "utf-8";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    private  String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    public  String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody(); // 二维码支付表单

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应："+result);

        return result;

    }
}
