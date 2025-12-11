package demo;

import java.io.File;
import java.nio.file.Paths;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

public class RunEmfCompare {

    public static void main(String[] args) throws Exception {
        // 1) 注册 XMI factory，方便加载 .xmi
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                .put("xmi", new XMIResourceFactoryImpl());

        ResourceSet rs = new ResourceSetImpl();

        // 2) 解析 source / target 文件路径
        String baseDir = System.getProperty("user.dir"); // 工程根目录
        File sourceFile = Paths.get(baseDir, "src", "main", "resources", "models", "source.xmi").toFile();
        File targetFile = Paths.get(baseDir, "src", "main", "resources", "models", "target.xmi").toFile();

        System.out.println("Source: " + sourceFile.getAbsolutePath());
        System.out.println("Target: " + targetFile.getAbsolutePath());

        // 3) 加载资源
        Resource left = rs.getResource(
                URI.createFileURI(sourceFile.getAbsolutePath()), true);
        Resource right = rs.getResource(
                URI.createFileURI(targetFile.getAbsolutePath()), true);

        // 简单检查 root
        EObject leftRoot = left.getContents().isEmpty() ? null : left.getContents().get(0);
        EObject rightRoot = right.getContents().isEmpty() ? null : right.getContents().get(0);
        System.out.println("Left root  : " + (leftRoot != null ? leftRoot.eClass().getName() : "null"));
        System.out.println("Right root : " + (rightRoot != null ? rightRoot.eClass().getName() : "null"));

        // 4) 构造比较范围并执行 EMF Compare
        IComparisonScope scope = new DefaultComparisonScope(left, right, null);
        Comparison comparison = EMFCompare.builder().build().compare(scope);

        // 5) 打印所有 diff
        System.out.println("\n=== EMF Compare Differences ===");
        int i = 1;
        for (Diff diff : comparison.getDifferences()) {
            System.out.println("Diff #" + (i++));
            System.out.println("  Type   : " + diff.eClass().getName());
            System.out.println("  Kind   : " + diff.getKind());
            System.out.println("  Source : " + diff.getSource());

            if (diff instanceof ReferenceChange rc) {
                String refName = rc.getReference() != null ? rc.getReference().getName() : "<null>";
                EObject value = rc.getValue();
                String valueClass = value != null ? value.eClass().getName() : "<null>";

                // 尝试读一下 name 属性（如果有）
                Object nameVal = null;
                if (value != null && value.eClass().getEStructuralFeature("name") != null) {
                    nameVal = value.eGet(value.eClass().getEStructuralFeature("name"));
                }

                System.out.println("  Ref    : " + refName);
                System.out.println("  Value  : " + valueClass +
                        (nameVal != null ? " (name=" + nameVal + ")" : ""));
            }
            System.out.println();
        }

        System.out.println("Diff count = " + comparison.getDifferences().size());
    }
}
