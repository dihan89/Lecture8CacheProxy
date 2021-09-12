import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class CacheableInvocationHandler implements InvocationHandler {
    private final Object delegate;
    private final Map<String, Map<ArgumentsObjs, Object>> cache;
    private final File rootDirectory;

    public CacheableInvocationHandler(Object delegate) {
        this(delegate, new File("cache"));
    }

    public CacheableInvocationHandler(Object delegate, File directory) {
        this.delegate = delegate;
        cache = new HashMap<>();
        this.rootDirectory = directory;
        if (!rootDirectory.exists())
            rootDirectory.mkdir();
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        method.setAccessible(true);
        if (!method.isAnnotationPresent(Cacheable.class))
            return method.invoke(delegate, args);
        Cacheable annotationCash = method.getAnnotation(Cacheable.class);
        return (annotationCash.saveTofile()) ? invokeFromCacheFile(method, args, annotationCash) :
                                                 invokeFromCacheRAM(method, args, annotationCash);
    }


    private Object invokeFromCacheFile(Method method, Object[] args, Cacheable cacheableAnnotation)  {
        method.setAccessible(true);
        File directory = (cacheableAnnotation.folderNameCache().equals("")) ?
                new File(rootDirectory, getFullName(method)) :
                new File(rootDirectory, cacheableAnnotation.folderNameCache());
        if (!directory.exists())
            directory.mkdir();
        int[] indexes = cacheableAnnotation.indexes();
        ArgumentsObjs arg = Arrays.equals(indexes, new int[]{-1}) ? new ArgumentsObjs(args) :
                new ArgumentsObjs(args, indexes);
        if (cacheableAnnotation.zip()) {//zip
            return invokeFromCacheFileZip(method,args,cacheableAnnotation, directory, arg);
        }
        Object returnResult;
        Map<ArgumentsObjs, Object> cacheWithSameHashCode = null;
        File fileWithResult = new File(directory, arg.hashCode() + ".ser");
        if (fileWithResult.exists())
            try (ObjectInputStream reader = new ObjectInputStream(new FileInputStream(fileWithResult))) {
                System.out.println("CACHE FILE EXISTS");
                try {
                    cacheWithSameHashCode = (Map<ArgumentsObjs, Object>) reader.readObject();
                } catch (ClassNotFoundException exc){
                    System.out.printf("Object in file %s is not Map<>:  %s", fileWithResult, exc.getMessage());
                }
                assert cacheWithSameHashCode != null;
                returnResult = cacheWithSameHashCode.get(arg);
                if (returnResult != null) {
                    System.out.println("FROM CACHE FILE");
                    return returnResult;
                }
            } catch (IOException exc) {
                System.out.println("This file is not correct: " + fileWithResult.getName() + " " + exc.getMessage());
                cacheWithSameHashCode = new HashMap<>();
            }
        else {
            cacheWithSameHashCode = new HashMap<>();
        }
        returnResult = getResultFromDelegate(delegate, method, args);
        Object cacheResult = List.class.isAssignableFrom(method.getReturnType()) ?
                getReducantList((List<?>) returnResult, cacheableAnnotation) : returnResult;
        cacheWithSameHashCode.put(arg, cacheResult);
        System.out.println("NEW RESULT");
        if (!fileWithResult.exists() || fileWithResult.delete())
            try (FileOutputStream fos = new FileOutputStream(fileWithResult)) {
                ObjectOutputStream writer = new ObjectOutputStream(fos);
                writer.writeObject(cacheWithSameHashCode);
            } catch (IOException exc) {
                System.out.println("Can not write object! " + exc.getMessage());
            }
        return returnResult;
    }


    private Object invokeFromCacheFileZip(Method method, Object[] args,
            Cacheable cacheableAnnotation, File directory, ArgumentsObjs arg)  {
        Object returnResult;
        Map<ArgumentsObjs, Object> cacheWithSameHashCode = null;
        String fileResultNameZip = arg.hashCode() + ".zip";
        File zipped = new File(directory, fileResultNameZip);
        if (zipped.exists()) {
            byte[] cacheFileByteArray = ZipArchiver.expand(new File(directory, fileResultNameZip));
            try (ObjectInputStream reader = new ObjectInputStream(new ByteArrayInputStream(cacheFileByteArray))) {
                System.out.println("CACHE FILE EXISTS");
                cacheWithSameHashCode = (Map<ArgumentsObjs, Object>) reader.readObject();
                returnResult = cacheWithSameHashCode.get(arg);
                if (returnResult != null) {
                    System.out.println("FROM CACHE FILE");
                    return returnResult;
                }
            } catch (IOException exc) {
                System.out.println("This file is not correct: " + fileResultNameZip + " " + exc.getMessage());
                cacheWithSameHashCode = new HashMap<>();
            }   catch (ClassNotFoundException exc) {
                System.out.println("This class is not correct: " + fileResultNameZip + " " + exc.getMessage());
            }
        }  else {
            cacheWithSameHashCode = new HashMap<>();
        }
        returnResult = getResultFromDelegate(delegate, method, args);
        Object cacheResult = List.class.isAssignableFrom(method.getReturnType()) ?
                getReducantList((List<?>) returnResult, cacheableAnnotation) : returnResult;
        assert cacheWithSameHashCode != null;
        cacheWithSameHashCode.put(arg, cacheResult);
        System.out.println("NEW RESULT");
        if (!zipped.exists() || zipped.delete())
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                ObjectOutputStream writer = new ObjectOutputStream(bos);
                writer.writeObject(cacheWithSameHashCode);
                ZipArchiver.compress(bos.toByteArray(), directory, fileResultNameZip);
            } catch (IOException exc) {
                System.out.println("Can not write object to zip-archive! " + exc.getMessage());
            }
        return returnResult;
    }


    private Object invokeFromCacheRAM(Method method, Object[] args, Cacheable cacheableAnnotation) {
        int[] indexes = cacheableAnnotation.indexes();
        ArgumentsObjs arg = Arrays.equals(indexes, new int[] {-1} ) ? new ArgumentsObjs(args) :
                new ArgumentsObjs(args, indexes);
        String methFullName = getFullName(method);
        Map<ArgumentsObjs, Object> cacheMethod = cache.get(methFullName);
        if (cacheMethod == null){
            cacheMethod = new HashMap<>();
            cache.put(methFullName, cacheMethod);
        } else{
            Object result = cacheMethod.get(arg);
            if (result != null) {
                System.out.println("FROM CACHE RAM");
                return result;
            }
        }
        Object returnResult = getResultFromDelegate(delegate, method, args);
        Object cacheResult = List.class.isAssignableFrom(method.getReturnType()) ?
                getReducantList((List<?>) returnResult, cacheableAnnotation) : returnResult;
        cacheMethod.put(arg, cacheResult);
        System.out.println("NEW RESULT");
        return returnResult;
    }
    private String getFullName(Method method) {
        Class<?> [] arrayParameterTypes = method.getParameterTypes();
        StringBuilder name = new StringBuilder(method.getName());
        for (Class<?> arrayParameterType : arrayParameterTypes)
            name.append("-").append(arrayParameterType.getName());
        return name.toString();
    }

    public List<?> getReducantList(List<?> list, Cacheable cachAnnotaton){
        if (cachAnnotaton.maxLengthList() >= list.size())
            return (ArrayList.class.isAssignableFrom(list.getClass()) ||
                    LinkedList.class.isAssignableFrom(list.getClass()))? list : new ArrayList<>(list);
        return new ArrayList<Object>(list.subList(0, cachAnnotaton.maxLengthList()));
    }
    private Object getResultFromDelegate(Object delegate, Method method, Object[] args){
        Object returnResult = null;
        method.setAccessible(true);
        try{
            returnResult = method.invoke(delegate, args);
        } catch (IllegalAccessException exc){
            System.out.printf("There is a problem with access to method %s:  %s", method.getName(), exc.getMessage());
        }  catch (InvocationTargetException exc){
            System.out.printf("There is a problem in the method %s:  %s", method.getName(), exc.getMessage());
        }
        return  returnResult;
    }

}

