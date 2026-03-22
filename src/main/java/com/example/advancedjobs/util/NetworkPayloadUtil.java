package com.example.advancedjobs.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.minecraft.network.FriendlyByteBuf;

public final class NetworkPayloadUtil {
    private NetworkPayloadUtil() {
    }

    public static void writeCompressedString(FriendlyByteBuf buf, String payload) {
        buf.writeByteArray(compress(payload));
    }

    public static String readCompressedString(FriendlyByteBuf buf) {
        return decompress(buf.readByteArray());
    }

    private static byte[] compress(String payload) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(payload.getBytes(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to compress network payload", exception);
        }
        return output.toByteArray();
    }

    private static String decompress(byte[] payload) {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(payload))) {
            return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to decompress network payload", exception);
        }
    }
}
