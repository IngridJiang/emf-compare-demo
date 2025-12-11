package demo;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit test for EMF Compare functionality
 */
public class EmfCompareTest {

    @BeforeAll
    public static void setup() {
        // Register XMI factory
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                .put("xmi", new XMIResourceFactoryImpl());
    }

    @Test
    public void testCompareModels() throws Exception {
        // Load the source and target XMI files
        ResourceSet rs = new ResourceSetImpl();

        String baseDir = System.getProperty("user.dir");
        File sourceFile = Paths.get(baseDir, "src", "main", "resources", "models", "source.xmi").toFile();
        File targetFile = Paths.get(baseDir, "src", "main", "resources", "models", "target.xmi").toFile();

        assertTrue(sourceFile.exists(), "Source file should exist: " + sourceFile.getAbsolutePath());
        assertTrue(targetFile.exists(), "Target file should exist: " + targetFile.getAbsolutePath());

        Resource left = rs.getResource(URI.createFileURI(sourceFile.getAbsolutePath()), true);
        Resource right = rs.getResource(URI.createFileURI(targetFile.getAbsolutePath()), true);

        assertNotNull(left, "Left resource should not be null");
        assertNotNull(right, "Right resource should not be null");
        assertFalse(left.getContents().isEmpty(), "Left resource should have contents");
        assertFalse(right.getContents().isEmpty(), "Right resource should have contents");

        // Perform comparison
        IComparisonScope scope = new DefaultComparisonScope(left, right, null);
        Comparison comparison = EMFCompare.builder().build().compare(scope);

        assertNotNull(comparison, "Comparison result should not be null");

        // Print differences
        System.out.println("\n=== EMF Compare Test Results ===");
        System.out.println("Total differences found: " + comparison.getDifferences().size());

        int diffCount = 0;
        for (Diff diff : comparison.getDifferences()) {
            diffCount++;
            System.out.println("\nDiff #" + diffCount);
            System.out.println("  Type   : " + diff.eClass().getName());
            System.out.println("  Kind   : " + diff.getKind());
            System.out.println("  Source : " + diff.getSource());

            if (diff instanceof ReferenceChange rc) {
                String refName = rc.getReference() != null ? rc.getReference().getName() : "<null>";
                EObject value = rc.getValue();
                String valueClass = value != null ? value.eClass().getName() : "<null>";

                Object nameVal = null;
                if (value != null && value.eClass().getEStructuralFeature("name") != null) {
                    nameVal = value.eGet(value.eClass().getEStructuralFeature("name"));
                }

                System.out.println("  Ref    : " + refName);
                System.out.println("  Value  : " + valueClass +
                        (nameVal != null ? " (name=" + nameVal + ")" : ""));
            }
        }

        // Verify that differences were found (since we know source and target are different)
        assertTrue(comparison.getDifferences().size() > 0,
                "Should find differences between source and target models");
    }

    @Test
    public void testSpecificDifferences() throws Exception {
        // Load and compare models
        ResourceSet rs = new ResourceSetImpl();

        String baseDir = System.getProperty("user.dir");
        File sourceFile = Paths.get(baseDir, "src", "main", "resources", "models", "source.xmi").toFile();
        File targetFile = Paths.get(baseDir, "src", "main", "resources", "models", "target.xmi").toFile();

        Resource left = rs.getResource(URI.createFileURI(sourceFile.getAbsolutePath()), true);
        Resource right = rs.getResource(URI.createFileURI(targetFile.getAbsolutePath()), true);

        IComparisonScope scope = new DefaultComparisonScope(left, right, null);
        Comparison comparison = EMFCompare.builder().build().compare(scope);

        // Count different types of changes
        long deletions = comparison.getDifferences().stream()
                .filter(diff -> diff.getKind() == DifferenceKind.DELETE)
                .count();

        long changes = comparison.getDifferences().stream()
                .filter(diff -> diff.getKind() == DifferenceKind.CHANGE)
                .count();

        System.out.println("\n=== Detailed Analysis ===");
        System.out.println("Deletions (from source perspective): " + deletions);
        System.out.println("Changes: " + changes);

        // We expect differences between the models (new Author class, isbn attribute, authors reference)
        // Note: EMF Compare shows these as DELETE from LEFT perspective since they exist in RIGHT but not LEFT
        assertTrue(deletions > 0 || changes > 0, "Should have differences between models");
    }
}
