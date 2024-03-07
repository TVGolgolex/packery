package de.pascxl.packery.netty.packet.auth;

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

import de.pascxl.packery.netty.buffer.ByteBuffer;
import de.pascxl.packery.netty.packet.DefaultPacket;
import lombok.Getter;

@Getter
public class AuthPacket extends DefaultPacket {

    protected Authentication authentication;

    public AuthPacket() {
    }

    public AuthPacket(Authentication authentication) {
        super(-400);
        this.authentication = authentication;
    }

    @Override
    public void readBuffer(ByteBuffer in) {
        this.authentication = new Authentication(
                in.readString(),
                in.readUUID()
        );
    }

    @Override
    public void writeBuffer(ByteBuffer out) {
        out.writeString(authentication.namespace());
        out.writeUUID(authentication.uniqueId());
    }

}
