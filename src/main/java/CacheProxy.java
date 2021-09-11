import java.io.File;
import java.lang.reflect.Proxy;

public class CacheProxy {
    public static Object cache (Object delegate){
        return cache (delegate, new File ("cache"));
    }
    public static Object cache (Object delegate , File directoryCache){
        return Proxy.newProxyInstance (ClassLoader.getSystemClassLoader(), delegate.getClass().getInterfaces(),
                        new CacheableInvocationHandler(delegate ,directoryCache));
    }
}
