import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.IOException;
import java.io.InputStream;

public class SevenZFileInputStream extends InputStream {
    private final SevenZFile sevenZFile;
    private boolean endReached = false;

    public SevenZFileInputStream(SevenZFile sevenZFile) {
        this.sevenZFile = sevenZFile;
    }

    private final byte[] oneByte = new byte[1];

    @Override
    public int read() throws IOException {
        int read = read(oneByte, 0, 1);
        if (read == -1) return -1;
        return oneByte[0] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (endReached) return -1;
        int read = sevenZFile.read(b, off, len);
        if (read == -1) endReached = true;
        return read;
    }
}
