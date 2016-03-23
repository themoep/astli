package org.androidlibid.proto.match;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.androidlibid.proto.Fingerprint;
import org.androidlibid.proto.ao.EntityService;
import org.androidlibid.proto.ao.EntityServiceFactory;
import org.androidlibid.proto.ast.ASTClassDefinition;
import org.androidlibid.proto.ast.ASTToFingerprintTransformer;
import org.androidlibid.proto.ast.Node;
import org.androidlibid.proto.logger.MyLogger;
import org.jf.baksmali.baksmaliOptions;
import org.jf.dexlib2.iface.ClassDef;

/**
 *
 * @author Christof Rabensteiner <christof.rabensteiner@gmail.com>
 */
public class MatchFingerprintsAlgorithm implements AndroidLibIDAlgorithm {

    private EntityService service; 
    private Map<String, String> mappings = new HashMap<>();
    private final List<? extends ClassDef> classDefs;
    private final baksmaliOptions options;
    private final ASTToFingerprintTransformer ast2fpt = new ASTToFingerprintTransformer();
    private final Comparator<Fingerprint> sortByEuclidDESCComparator;
    
    private static final Logger LOG = MyLogger.getLogger( MatchFingerprintsAlgorithm.class.getName() );
       
    public MatchFingerprintsAlgorithm(baksmaliOptions options, List<? extends ClassDef> classDefs) {
        this.options = options;
        this.classDefs = classDefs;
        
        sortByEuclidDESCComparator = new Comparator<Fingerprint>() {
            @Override
            public int compare(Fingerprint that, Fingerprint other) {
                double thatNeedleLength  = that.euclideanNorm();
                double otherNeedleLength = other.euclideanNorm();
                if (thatNeedleLength > otherNeedleLength) return  1;
                if (thatNeedleLength < otherNeedleLength) return -1;
                return 0;
            }};
        
    }
    
    @Override
    public boolean run() {
        try {
            service = EntityServiceFactory.createService();
            
            if(options.isObfuscated) {
                ProGuardMappingFileParser parser = new ProGuardMappingFileParser(options.mappingFile); 
//                mappings = parser.parseMappingFileOnClassLeve();
                mappings = parser.parseMappingFileOnMethodLevel();
            }
            
            FingerprintService fingerprintService = new FingerprintService(service);
            PackageInclusionCalculator packageInclusionCalculator = new PackageInclusionCalculator(new ClassInclusionCalculator(new FingerprintMatcher(1000)));
            
            MatchingStrategy strategy = new MatchOnMethodLevelWithInclusionStrategy(
                fingerprintService, packageInclusionCalculator, new ResultEvaluator(fingerprintService));
            
            Map<String, Fingerprint> packagePrints = generatePackagePrints(); 
            
            Map<MatchingStrategy.Status, Integer> stats = strategy.matchPrints(packagePrints);
            
            LOG.info("Stats: ");
            for(MatchingStrategy.Status key : MatchingStrategy.Status.values()) {
                LOG.log(Level.INFO, "{0}: {1}", new Object[]{key.toString(), stats.get(key)});
            }
            
//            int amountFirstMatches = stats.get(MatchingStrategy.Status.OK);
//            if(amountFirstMatches > 0) {
//                System.out.println("avg diff on first machted: " +  frmt.format(totalDiffToFirstMatch / amountFirstMatches));
//            }
 
       } catch (SQLException | IOException ex) {
            Logger.getLogger(MatchFingerprintsAlgorithm.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }
    
    private Fingerprint transformClassDefToFingerprint(ClassDef classDef, String obfsClassName) throws IOException {
        ASTClassDefinition classDefinition = new ASTClassDefinition(options, classDef);
        Map<String, Node> ast = classDefinition.createASTwithNames();
        
        List<Fingerprint> methods = new ArrayList<>(); 
        Fingerprint classFingerprint = new Fingerprint();
                
        for(String obfsMethodName : ast.keySet()) {
            Node node = ast.get(obfsMethodName);
            Fingerprint methodFingerprint = ast2fpt.createFingerprint(node);
            
            if(methodFingerprint.euclideanNorm() > 1.0f) {
                String methodName = translateName(obfsClassName + ":" + obfsMethodName);
                methodFingerprint.setName(methodName);
                methods.add(methodFingerprint);
                classFingerprint.add(methodFingerprint);
            }
        }
        
        Collections.sort(methods, sortByEuclidDESCComparator);
        
        for(Fingerprint method : methods) {
            classFingerprint.addChild(method);
        }
        
        return classFingerprint;
    }

    private String translateName(String obfuscatedName) {
        if(options.isObfuscated && mappings.get(obfuscatedName) != null) {
            return mappings.get(obfuscatedName);
        }
        return obfuscatedName;
    }
    
    public String extractPackageName(String className) {
        return className.substring(0, className.lastIndexOf("."));
    }

    public String transformClassName(String className) {
        className = className.replace('/', '.');
        return className.substring(1, className.length() - 1);
    }

    //TODO: Swap out.
    private Map<String, Fingerprint> generatePackagePrints() throws IOException {
        
        Map<String, Fingerprint> packagePrints = new HashMap<>();
            
        for(ClassDef def : classDefs) {
            String obfClassName = transformClassName(def.getType());
            String className =    translateName(obfClassName);
            
            String packageName =  extractPackageName(className);

            Fingerprint classFingerprint = transformClassDefToFingerprint(def, obfClassName);
            
            if(classFingerprint.getChildren().isEmpty()) {
                continue;
            }
            
            classFingerprint.setName(className);
            Fingerprint packageFingerprint;

            if(packagePrints.containsKey(packageName)) {
                packageFingerprint = packagePrints.get(packageName);
            } else {
                packageFingerprint = new Fingerprint(packageName);
                packagePrints.put(packageName, packageFingerprint);
            }

            packageFingerprint.add(classFingerprint);
            packageFingerprint.addChild(classFingerprint);
        }

        return packagePrints;
    }

}
