package io.github.cputnama11y.screenshot2webhook.client;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public record HTTPRequestMultipartBody(byte[] bytes, String boundary) {
    public String contentType() {
        return "multipart/form-data; boundary=" + this.boundary();
    }

    public byte[] body() {
        return this.bytes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final String DEFAULT_MIMETYPE = "text/plain";

        public record MultiPartRecord(String fieldName, String fileName, String contentType, Object content) {
        }

        List<MultiPartRecord> parts;

        protected Builder() {
            this.parts = new ArrayList<>();
        }

        public Builder part(String fieldName, String fieldValue) {
            MultiPartRecord part = new MultiPartRecord(fieldName, null, DEFAULT_MIMETYPE, null);
            this.parts.add(part);
            return this;
        }

        public Builder part(String fieldName, String fieldValue, String contentType) {
            MultiPartRecord part = new MultiPartRecord(fieldName, null, contentType, fieldValue);
            this.parts.add(part);
            return this;
        }

        public Builder part(String fieldName, Object fieldValue, String contentType, String fileName) {
            MultiPartRecord part = new MultiPartRecord(fieldName, fileName, contentType, fieldValue);
            this.parts.add(part);
            return this;
        }

        public HTTPRequestMultipartBody build() throws IOException {
            String boundary = new BigInteger(256, new SecureRandom()).toString();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (MultiPartRecord record : parts) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("--").append(boundary).append("\r\n").append("Content-Disposition: form-data; name=\"").append(record.fieldName());
                if (record.fileName() != null) {
                    stringBuilder.append("\"; filename=\"").append(record.fileName());
                }
                out.write(stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
                out.write("\"\r\n".getBytes(StandardCharsets.UTF_8));
                Object content = record.content();
                switch (content) {
                    case String s -> {
                        if (record.contentType() == null)
                            out.write(("\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                        else
                            out.write(("Content-Type: " + record.contentType() + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                        out.write(s.getBytes(StandardCharsets.UTF_8));
                    }
                    case byte[] bytes1 -> {
                        if (record.contentType() == null)
                            out.write("Content-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                        else
                            out.write(("Content-Type: " + record.contentType() + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                        out.write(bytes1);
                    }
                    case File file -> {
                        if (record.contentType() == null)
                            out.write("Content-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                        else
                            out.write(("Content-Type: " + record.contentType() + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                        Files.copy(file.toPath(), out);
                    }
                    case null, default -> {
                        if (record.contentType() == null)
                            out.write("Content-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                        else
                            out.write(("Content-Type: " + record.contentType() + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                        ObjectOutputStream objectOutputStream = new ObjectOutputStream(out);
                        objectOutputStream.writeObject(content);
                        objectOutputStream.flush();
                    }
                }
                out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }
            out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return new HTTPRequestMultipartBody(out.toByteArray(), boundary);
        }


    }
}




