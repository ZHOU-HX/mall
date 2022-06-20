package com.hxzhou.mall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.hxzhou.mall.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    //在支付宝创建的应用的id
    private String app_id = "2021000120697631";

    // 商户私钥，您的PKCS8格式RSA2私钥
    private String merchant_private_key = "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQCYCmi17e6yQrUWM/aGopV7CaDYQUB/63+BthXG5J6NjhPkplRmXtOz67ztHUKxTXD1Aglexg6csivJVUMLnq53Cw0yIbJIqb/t+HP27/bIf64kB42+sdZd0GwFuVOrotG6F/+naGvwXVuoH+3wR9XBvnq/ZNm29snO7LpOpuM0iqV+Bv7ZcGTIzyWqtuEq70Zvnw+nK6XGliJ3cO5Zs/yjowRSWibreZrTwF3ryDEoBmcRNjipwFcp03vQa6d9Ux4QQ6Q2EHIHq7p8yHFUGszhBOATEyFMKnWgegZ3bKRjSaguqfwjK+5PWvA87ihB6keg9yrfVM2W9lRVZplVt8mhAgMBAAECggEALZiBin3OZbtHn176Aapkdb+Pcwsukq5HUNkQctNBEWaYZKvVB9ZMWtrJj7qEs7Nruq5frQJsg2WTaA+fya5O1/iCfpRIaS4vFui5rkaMldhkmv2A7r1ackv8+UJ46zIq/0oPK19Pb4gv4p2gt7xzSGUdXqSZ/hrIrrujq860uJ6clKdcXuumZhbTy3f5r5Wy5bAOP3wZLQ4tpSE11JVjsAYzHkU6/krGl/9J9OV7Zg/GZl3Es7DTwJu+5kMenFEilsHf7RgL+Z9xXVJZQD+H26k63uskRKOuZqx0/O2n+1mkuTaFZ9fhMHWoAgQQc/p7Yr59d+KqTEd4Wiwbqy3deQKBgQDeIHbNWTK623fEiMvH9Hh2bSyJ00fYNdOLj2qUZckXr0zvl50UYRlUFGgAbHV/TOtAu9oEwzaws0u44ybFZGMzNN+4nYAYQsJdxbSE14Eg9IrpTDvV6CO8N7mauN+fOx53VXy8MawjUcNg9Jaq2+8fPgXuAZ/dhG1d5UNF3ylvgwKBgQCvOd/lJIrPBtQ3PTjy7IpRdp73T3hB1zWlbPNFEBan7OMhaPYAs7i09TGGNk9vQBAtKCj5IyNAlg7kiDffXpLuRWJtL3nNiu4rLs97Lz7iMqSMxks0XWJODLq/CU7TSGX1zm17kZBZA7Py55auw4r96G14fbv+wK/ScM8vNbjVCwKBgQC/V2u/PGUMvV0QOV6BIkBak3TYTN4Ii2VtRN9kccGl22YExa+UDIiK+wETzFnyjMnh+2+0xoxn7ir+Wv3c77rcM2G8YEAlpexUZqg3/oPHZZ/7k6W/f7a4va/ube6D9aMq3MMUQh2sVxRcgpT/HXGAkib9fPy/DwI+7ezjVXcWDQKBgQCkd5TwFal4AffkVUGDuYNmTQmOJXhBjEcdGzMG6r/pJA5YCJWGOSowJ0gxqf5K9H16dQxoM5PeBjq6RCT1MmaE4Hku9HXF+BXuUESMYd8LHu+V9nUWFGS1z3rk3k6vP2/8FKJsbkdnVXNslfS32nA2PBesFZAD4EXnJt1AeZMf2wKBgQCCa3X08v/vR5fgPbickkVvoh2oqyOSVkPT6x97u3upO1G5PKUWkg4zXiWRN1kFbom53A7LGycy7fqVcV1JRmn8ySPXltG+02TL3hlLBfbWzI0AImS6NYQcX0EgXLxogLY14izO6Y2EETlki1f+baeY7sgsGvoIzrys7CCfV0S+zQ==";

    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    private String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAy6zi1l24fcx79la4EF+d8wtg44YgHQyDZa1uT+t19AEw+RP45MVQkksMr3AhM5JVkVn0oyqGP1RIQZ3bo8owQlaDRU2fC9zV0XN1nflKEPu6BrBmW9c5GDl3R4Rb9hfQWE4cgAlymIXomE8Kyzps4NFwCBa5bZ8yEXIoQ1y9xvrl7Z+pFWlDaWJV1qRpLvJc4GQ1i/8SkbBHOT/HtfPjYQNCW2iNp78IS669LufxOjUT5Qb2bUjHpiD6QVbca2eOJy6sXt7S3D0P2ePDRUuZRoRLQ+0J2GFU+G3mblW/FBxI4rEL2aePmUbfQOpJyJ91YtVZtuu4aeSXy95FrCE0hQIDAQAB";

    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    private String notify_url = "https://5451j2c778.picp.vip/payed/notify";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    private String return_url = "http://member.mall.com/memberOrder.html";

    // 签名方式
    private String sign_type = "RSA2";

    // 收单时间
    private String timeout = "1m";

    // 字符编码格式
    private String charset = "utf-8";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    private String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    public String pay(PayVo vo) throws AlipayApiException {

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
                + "\"timeout_express\":\"" + timeout + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应："+result);

        return result;

    }
}
