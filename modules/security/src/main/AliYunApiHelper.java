package lila.security;

import org.apache.commons.lang3.StringUtils;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;

public class AliYunApiHelper {

    public static String getSignature(AliYunApiEmailParam emailParam) throws Exception {
        String param  = prepareParamStrURLEncoder(buildSortedMap(emailParam));
        String toSign = "POST" + "&" + URLEncoder.encode("/", "utf8") + "&"
                + getUtf8Encoder(param);
        byte[] bytes = HmacSHA1Encrypt(toSign, emailParam.getAccessKeySecret() + "&");
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static Map<String, Object> buildSortedMap(AliYunApiEmailParam emailParam) {
        TreeMap<String, Object> params = new TreeMap<String, Object>();
        params.put("AccessKeyId", emailParam.getAccessKeyId());
        params.put("Action", emailParam.getAction());
        params.put("Format", emailParam.getFormat());
        params.put("RegionId", emailParam.getRegionId());
        params.put("SignatureMethod", emailParam.getSignatureMethod());
        params.put("SignatureNonce", emailParam.getSignatureNonce());
        params.put("SignatureVersion", emailParam.getSignatureVersion());
        params.put("Timestamp", emailParam.getTimestamp());
        params.put("Version", emailParam.getVersion());
        params.put("AccountName", emailParam.getAccountName());
        params.put("AddressType", emailParam.getAddressType());
        params.put("HtmlBody", emailParam.getHtmlBody());
        params.put("ReplyToAddress", emailParam.getReplyToAddress());
        params.put("Subject", emailParam.getSubject());
        params.put("TagName", emailParam.getTagName());
        params.put("TextBody", emailParam.getTextBody());
        params.put("ToAddress", emailParam.getToAddress());
        return params;
    }

    public static String prepareParamStrURLEncoder(Map<String, Object> params) {
        try {
            StringBuffer param = new StringBuffer();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (StringUtils.isBlank(entry.getKey()) || null == entry.getValue()) {
                    continue;
                }
                param.append(getUtf8Encoder(entry.getKey()) + "=" + getUtf8Encoder(entry.getValue().toString()) + "&");
            }
            return param.substring(0, param.lastIndexOf("&"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getUtf8Encoder(String param) throws UnsupportedEncodingException {
        return URLEncoder.encode(param, "utf8")
                .replaceAll("\\+", "%20")
                .replaceAll("\\*", "%2A")
                .replaceAll("%7E", "~");
    }

    private static final String MAC_NAME = "HmacSHA1";
    private static final String ENCODING = "UTF-8";

    private static byte[] HmacSHA1Encrypt(String encryptText, String encryptKey) throws Exception {
        byte[] data = encryptKey.getBytes(ENCODING);
        //根据给定的字节数组构造一个密钥,第二参数指定一个密钥算法的名称
        SecretKey secretKey = new SecretKeySpec(data, MAC_NAME);
        //生成一个指定 Mac 算法 的 Mac 对象
        Mac mac = Mac.getInstance(MAC_NAME);
        //用给定密钥初始化 Mac 对象
        mac.init(secretKey);
        byte[] text = encryptText.getBytes(ENCODING);
        //完成 Mac 操作
        return mac.doFinal(text);
    }

    public static String getUTCTimeStr() {
        // 1、取得本地时间：
        Calendar cal = Calendar.getInstance();
        // 2、取得时间偏移量：
        int zoneOffset = cal.get(Calendar.ZONE_OFFSET);
        // 3、取得夏令时差：
        int dstOffset = cal.get(Calendar.DST_OFFSET);
        // 4、从本地时间里扣除这些差量，即可以取得UTC时间：
        cal.add(Calendar.MILLISECOND, -(zoneOffset + dstOffset));
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());
        //System.out.println("时间------" + date);
        String[] strs = date.split(" ");
        return strs[0] + "T" + strs[1] + "Z";
    }

}
