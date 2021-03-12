package lila.security;

public class AliYunApiEmailParam {

    private String accessKeyId;
    private String accessKeySecret;
    private String format;
    private String regionId;
    private String signatureMethod;
    private String signatureNonce;
    private String signatureVersion;
    private String timestamp;
    private String version;
    private String accountName;
    private String action;
    private String addressType;
    private String htmlBody;
    private String replyToAddress;
    private String subject;
    private String tagName;
    private String textBody;
    private String toAddress;

    public AliYunApiEmailParam(
            String accessKeyId,
            String accessKeySecret,
            String format,
            String regionId,
            String signatureMethod,
            String signatureNonce,
            String signatureVersion,
            String timestamp,
            String version,
            String accountName,
            String action,
            String addressType,
            String htmlBody,
            String replyToAddress,
            String subject,
            String tagName,
            String textBody,
            String toAddress) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.format = format;
        this.regionId = regionId;
        this.signatureMethod = signatureMethod;
        this.signatureNonce = signatureNonce;
        this.signatureVersion = signatureVersion;
        this.timestamp = timestamp;
        this.version = version;
        this.accountName = accountName;
        this.action = action;
        this.addressType = addressType;
        this.htmlBody = htmlBody;
        this.replyToAddress = replyToAddress;
        this.subject = subject;
        this.tagName = tagName;
        this.textBody = textBody;
        this.toAddress = toAddress;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public String getFormat() {
        return format;
    }

    public String getRegionId() {
        return regionId;
    }

    public String getSignatureMethod() {
        return signatureMethod;
    }

    public String getSignatureNonce() {
        return signatureNonce;
    }

    public String getSignatureVersion() {
        return signatureVersion;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getVersion() {
        return version;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getAction() {
        return action;
    }

    public String getAddressType() {
        return addressType;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public String getReplyToAddress() {
        return replyToAddress;
    }

    public String getSubject() {
        return subject;
    }

    public String getTagName() {
        return tagName;
    }

    public String getTextBody() {
        return textBody;
    }

    public String getToAddress() {
        return toAddress;
    }
}
