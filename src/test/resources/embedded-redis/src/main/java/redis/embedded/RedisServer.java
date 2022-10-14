package redis.embedded;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RedisServer extends AbstractRedisInstance {
    private static final String REDIS_READY_PATTERN = ".*The server is now ready to accept connections on port.*";
    private static final int DEFAULT_REDIS_PORT = 6379;

    public RedisServer() throws IOException {
        this(DEFAULT_REDIS_PORT);
    }

    public RedisServer(Integer port) throws IOException {
        super(port);
        File executable = RedisExecProvider.defaultProvider().get();
        this.args = Arrays.asList(
                executable.getAbsolutePath(),
                "--port", Integer.toString(port)
        );
	}

    public RedisServer(File executable, Integer port) {
        super(port);
        this.args = Arrays.asList(
                executable.getAbsolutePath(),
                "--port", Integer.toString(port)
        );
    }

    public RedisServer(RedisExecProvider redisExecProvider, Integer port) throws IOException {
        super(port);
        this.args = Arrays.asList(
                redisExecProvider.get().getAbsolutePath(),
                "--port", Integer.toString(port)
        );
    }

    RedisServer(List<String> args, int port) {
        super(port);
        this.args = new ArrayList<String>(args);
    }

    public static RedisServerBuilder builder() {
        return new RedisServerBuilder();
    }

    @Override
    protected String redisReadyPattern() {
        return REDIS_READY_PATTERN;
    }
}
