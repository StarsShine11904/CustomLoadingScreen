package alexiil.mc.mod.load.json;

import java.io.IOException;
import java.io.InputStream;

import net.minecraft.resource.Resource;

public final class ResourceWrappingInputStream extends InputStream {
    private final Resource res;
    final InputStream from;

    public ResourceWrappingInputStream(Resource res) {
        this.res = res;
        this.from = res.asStream();
    }

    @Override
    public int read() throws IOException {
        return from.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return from.read(b, off, len);
    }

    @Override
    public int available() throws IOException {
        return from.available();
    }

    @Override
    public synchronized void mark(int readlimit) {
        from.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return from.markSupported();
    }

    @Override
    public synchronized void reset() throws IOException {
        from.reset();
    }

    @Override
    public long skip(long n) throws IOException {
        return from.skip(n);
    }

    @Override
    public void close() throws IOException {
        res.close();
    }
}
