package io.github.sinri.stark.logging.aliyun.sls.putter.entity;

import com.google.protobuf.DynamicMessage;
import io.github.sinri.stark.logging.aliyun.sls.putter.protocol.LogEntityDescriptors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * LogItem实体。
 *
 * @see <a href=
 *         "https://help.aliyun.com/zh/sls/developer-reference/api-sls-2020-12-30-struct-logitem"
 *         >LogItem 实体格式定义</a>
 */
public class LogItem {
    private final int time;
    private final List<LogContent> contents;
    private @Nullable Integer nanoPartOfTime = null;

    public LogItem(int time) {
        this.time = time;
        this.contents = new ArrayList<>();
    }

    public LogItem(long time) {
        this.time = Math.toIntExact(time / 1000);
        this.nanoPartOfTime = (int) (time % 1000);
        this.contents = new ArrayList<>();
    }

    public int getTime() {
        return time;
    }

    public List<LogContent> getContents() {
        return contents;
    }

    public LogItem addContent(LogContent content) {
        contents.add(content);
        return this;
    }

    public LogItem addContent(String key, String value) {
        contents.add(new LogContent(key, value));
        return this;
    }

    @Nullable
    public Integer getNanoPartOfTime() {
        return nanoPartOfTime;
    }

    public LogItem setNanoPartOfTime(@Nullable Integer nanoPartOfTime) {
        this.nanoPartOfTime = nanoPartOfTime;
        return this;
    }

    public DynamicMessage toProtobuf() {
        var logDescriptor = LogEntityDescriptors.getInstance().getLogDescriptor();
        DynamicMessage.Builder builder = DynamicMessage.newBuilder(logDescriptor)
                                                       .setField(logDescriptor.findFieldByName("Time"), time);
        contents.forEach(
                content -> builder.addRepeatedField(logDescriptor.findFieldByName("Contents"), content.toProtobuf()));
        if (nanoPartOfTime != null) {
            builder.setField(logDescriptor.findFieldByName("Time_ns"), nanoPartOfTime);
        }
        return builder.build();
    }

    /**
     * Computes and returns the probable size of all log contents combined.
     * The size is calculated by summing the probable sizes of individual
     * {@link LogContent} instances
     * in the content list. If the list is empty, the method returns 0.
     *
     * @return The total probable size of all log contents in bytes.
     */
    public int getProbableSize() {
        int sum = 0;
        for (int i = 0; i < getContents().size(); i++) {
            LogContent content = getContents().get(i);
            sum += content.getProbableSize();
        }
        return sum;
    }
}
