package de.pascxl.packery;

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

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Packery {

    public static final UUID SYSTEM_UUID = UUID.fromString("0f0f0f0f-0f0f-0f0f-f0f0-0f0f0f0f0f0f");
    public static String BRANDING = "Packery";
    public static boolean DEV_MODE = false;
    public static Logger LOGGER = Logger.getLogger(BRANDING);

    public static void debug(Level level, Class<?> executedClass, String string, Object... var) {
        if (DEV_MODE) {
            LOGGER.log(level, executedClass.getSimpleName() + ": " + buildMessage(string, var));
        }
    }

    public static void log(Level level, Class<?> executedClass, String string, Object... var) {
        LOGGER.log(level, executedClass.getSimpleName() + ": " + buildMessage(string, var));
    }

    private static String buildMessage(String s, Object... objects) {
        if (s == null) {
            return null;
        }
        var message = s.replace("{NEXT_LINE}", "\n");
        if (message.contains("{")) {
            for (int i = 0; i < objects.length; i++) {
                message = message.replace("{" + i + "}", String.valueOf(objects[i]));
            }
        }
        return message;
    }

}
