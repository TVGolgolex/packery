package de.pascxl.packery.netty.packet.file;

import de.pascxl.packery.Packery;
import de.pascxl.packery.netty.buffer.ByteBuffer;
import de.pascxl.packery.netty.packet.DefaultPacket;
import de.pascxl.packery.netty.packet.PacketInHandler;
import lombok.Getter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.stream.Stream;

@Getter
public class FilePacket extends DefaultPacket {

    protected String dest;
    protected byte[] bytes;

    public FilePacket() {
    }

    public FilePacket(String dest, byte[] bytes) {
        this.dest = dest;
        this.bytes = bytes;
    }

    @Override
    public void readBuffer(ByteBuffer in) {
        if (in.buffer().readableBytes() != 0) {
            this.dest = in.readString();
            this.bytes = String.join(",", in.readStringCollection()).getBytes();
            build();
        }
    }

    @Override
    public void writeBuffer(ByteBuffer out) {
        out.writeString(dest);
        out.writeStringCollection(Stream.of(bytes).map(String::valueOf).toList());
    }

    public void build() {
        try {
            File file = new File(dest);
            file.getParentFile().mkdirs();
            file.createNewFile();

            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                fileOutputStream.write(bytes);
                fileOutputStream.flush();
            }

        } catch (IOException e) {
            Packery.LOGGER.log(Level.SEVERE, e.getMessage());
        }
    }
}