import java.lang.reflect.Proxy;

public class Lecture8CacheProxy {
    public static void main(String args[]){
        HardWorkerImpl workerDelegate = new HardWorkerImpl();
        System.out.println(workerDelegate.createCube(5).toString());
        // File rootDir = new File("cacheStrange");
        HardWorker worker = (HardWorker) Proxy.newProxyInstance
                (ClassLoader.getSystemClassLoader(), workerDelegate.getClass().getInterfaces(),
                        new CacheableInvocationHandler(workerDelegate));
    }
}
