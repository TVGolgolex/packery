package de.pascxl.packery.utils;

/*
 * MIT License
 *
 * Copyright (c) 2024 Mario Kurz
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import com.google.common.base.Preconditions;
import lombok.NonNull;

import javax.annotation.Nullable;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

public class StringUtils {

    private static final Random SECURE_RANDOM = new SecureRandom();
    private static final char[] DEFAULT_ALPHABET_UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    public static @NonNull String generateRandomString(int length) {
        Preconditions.checkArgument(length > 0, "Can only generate string which is longer to 0 chars");
        var buffer = new StringBuilder(length);
        for (var i = 0; i < length; i++) {
            var nextCharIdx = SECURE_RANDOM.nextInt(DEFAULT_ALPHABET_UPPERCASE.length);
            buffer.append(DEFAULT_ALPHABET_UPPERCASE[nextCharIdx]);
        }
        return buffer.toString();
    }

    public static boolean endsWithIgnoreCase(@NonNull String string, @NonNull String suffix) {
        var suffixLength = suffix.length();
        return string.regionMatches(true, string.length() - suffixLength, suffix, 0, suffixLength);
    }

    public static boolean startsWithIgnoreCase(@NonNull String string, @NonNull String prefix) {
        return string.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    public static @Nullable String toLower(@Nullable String string) {
        return string == null ? null : string.toLowerCase(Locale.ROOT);
    }

    public static @Nullable String toUpper(@Nullable String string) {
        return string == null ? null : string.toUpperCase(Locale.ROOT);
    }

    public static @NonNull String repeat(char c, int times) {
        Preconditions.checkArgument(times >= 0, "Can only copy a char 0 or more times, not negative times");
        if (times == 0) {
            return "";
        }
        if (times == 1) {
            return Character.toString(c);
        }
        var s = new char[times];
        Arrays.fill(s, c);
        return new String(s);
    }
}
