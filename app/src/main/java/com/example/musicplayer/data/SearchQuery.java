package com.example.musicplayer.data;

/**
 * 生成 Room LIKE 查询用的模式串。
 *
 * <p>用户输入里的 {@code %} 与 {@code _} 是 SQL 通配符，直接拼进查询会把“50%”
 * 当成“50 开头”。这里统一转义，并在 DAO 的查询里用 {@code ESCAPE '\'} 声明转义符。</p>
 */
public final class SearchQuery {

    private SearchQuery() {
    }

    /**
     * 把关键词转成“包含”匹配的模式串：两端补 {@code %}，内部转义通配符。
     * 空输入返回 {@code "%"}，即匹配全部曲目。
     */
    public static String likePattern(String input) {
        if (input == null) {
            return "%";
        }
        String keyword = input.trim();
        if (keyword.isEmpty()) {
            return "%";
        }
        StringBuilder pattern = new StringBuilder(keyword.length() + 4);
        pattern.append('%');
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            if (c == '\\' || c == '%' || c == '_') {
                pattern.append('\\');
            }
            pattern.append(c);
        }
        pattern.append('%');
        return pattern.toString();
    }
}
