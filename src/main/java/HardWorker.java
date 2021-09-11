import java.util.HashMap;
import java.util.List;

public interface HardWorker {
    @Cacheable(flag = true)
    Cube createCube(int edge);

    @Cacheable(flag = false)
    Parallelepiped createParallelepiped(int edge1, int edge2, int edge3);

    @Cacheable(flag = true, indexes = {0,2})
    Parallelepiped createParallelepiped2(int edge1, int edge2, int edge3);

    @Cacheable(flag = true, indexes = {0,2},  folderNameCache = "ab-create3")
    Parallelepiped createParallelepiped3(int edge1, int edge2, int edge3);

    @Cacheable(flag = true, maxLengthList = 3)
    List<Cube> createCubes(List<Integer> edges);

    @Cacheable(flag = true, zip = true)
    Parallelepiped createParallelepipedZip(int edge1, int edge2, int edge3);

}
