package io.github.sinri.stark.logging.aliyun.sls.putter.entity;

import com.google.protobuf.DynamicMessage;
import io.github.sinri.stark.logging.aliyun.sls.putter.protocol.LogEntityDescriptors;
import org.jspecify.annotations.NullMarked;

/**
 * @see <a href=
 *         "https://help.aliyun.com/zh/sls/developer-reference/api-sls-2020-12-30-struct-logcontent">LogContent</a>
 */
public class LogContent {
    private final String key;
    private final String value;

    public LogContent(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }


    public DynamicMessage toProtobuf() {
        var contentDescriptor = LogEntityDescriptors.getInstance().getContentDescriptor();
        return DynamicMessage.newBuilder(contentDescriptor)
                             .setField(contentDescriptor.findFieldByName("Key"), key)
                             .setField(contentDescriptor.findFieldByName("Value"), value)
                             .build();
    }

    /**
     * Calculates and returns the probable size of the log content in bytes.
     * <p>
     * 使用UTF-8编码的平均字符长度3倍作为估计值。
     *
     * @return The total size in bytes of the UTF-8 encoded key and value combined.
     */
    public int getProbableSize() {
        return (key.length() + value.length()) * 3;
    }
}
