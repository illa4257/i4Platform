package i4l;

import java.util.Arrays;
import java.util.List;

public class I4LParserConfig {
    public static List<String> BASIC_KEYWORDS = Arrays.asList("return", "throw");

    public List<Object> code = null;
    public List<String> keywords = null;

    public I4LParserConfig setCode(final List<Object> code) {
        this.code = code;
        return this;
    }

    public I4LParserConfig setKeywords(final List<String> keywords) {
        this.keywords = keywords;
        return this;
    }
}