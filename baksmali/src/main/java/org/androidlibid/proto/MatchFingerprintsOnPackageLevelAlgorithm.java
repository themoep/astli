package org.androidlibid.proto;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.androidlibid.proto.ao.Clazz;
import org.androidlibid.proto.ao.EntityService;
import org.androidlibid.proto.ao.Package;
import org.androidlibid.proto.ao.EntityServiceFactory;
import org.androidlibid.proto.ao.Method;
import org.androidlibid.proto.ao.VectorEntity;
import org.androidlibid.proto.ast.ASTClassDefinition;
import org.androidlibid.proto.ast.ASTToFingerprintTransformer;
import org.androidlibid.proto.ast.Node;
import org.apache.commons.lang.StringUtils;
import org.jf.baksmali.baksmaliOptions;
import org.jf.dexlib2.iface.ClassDef;

/**
 *
 * @author Christof Rabensteiner <christof.rabensteiner@gmail.com>
 */
public class MatchFingerprintsOnPackageLevelAlgorithm implements AndroidLibIDAlgorithm {

    private EntityService service; 
    private Map<String, String> mappings = new HashMap<>();
    private final List<? extends ClassDef> classDefs;
    private final baksmaliOptions options;
    private final ASTToFingerprintTransformer ast2fpt = new ASTToFingerprintTransformer();
    private final NumberFormat frmt = new DecimalFormat("#0.00");  
    
    private double totalDiffToFirstMatch = 0.0d;
    
    public MatchFingerprintsOnPackageLevelAlgorithm(baksmaliOptions options, List<? extends ClassDef> classDefs) {
        this.options = options;
        this.classDefs = classDefs;
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
            
            Map<String, Fingerprint> packagePrints = generatePackagePrints(); 
            
//            Map<FingerprintMatchTaskResult, Integer> stats = matchPackagePrints(packagePrints);
//            Map<FingerprintMatchTaskResult, Integer> stats = matchPackageOverClassPrints(packagePrints);
            Map<FingerprintMatchTaskResult, Integer> stats = matchPackagesOnMethodLevel(packagePrints);
            
            System.out.println("Stats: ");
            for(FingerprintMatchTaskResult key : FingerprintMatchTaskResult.values()) {
                System.out.println(key.toString() + ": " + stats.get(key));
            }
            
            int amountFirstMatches = stats.get(FingerprintMatchTaskResult.OK);
            
            if(amountFirstMatches > 0) {
                System.out.println("avg diff on first machted: " +  frmt.format(totalDiffToFirstMatch / amountFirstMatches));
            }
        } catch (SQLException | IOException ex) {
            Logger.getLogger(MatchFingerprintsOnPackageLevelAlgorithm.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }
    
    private Fingerprint transformClassDefToFingerprint(ClassDef classDef, String obfsClassName) throws IOException {
        ASTClassDefinition classDefinition = new ASTClassDefinition(options, classDef);
        Map<String, Node> ast = classDefinition.createASTwithNames();
        
        Fingerprint classFingerprint = new Fingerprint();
                
        for(String obfsMethodName : ast.keySet()) {
            Node node = ast.get(obfsMethodName);
            Fingerprint methodFingerprint = ast2fpt.createFingerprint(node);
            
            if(methodFingerprint.euclideanNorm() > 1.0f) {
                String methodName = translateName(obfsClassName + ":" + obfsMethodName);
                methodFingerprint.setName(methodName);
                classFingerprint.addChild(methodFingerprint);
                classFingerprint.add(methodFingerprint);
            }
        }
        
        return classFingerprint;
    }

    private FingerprintMatchTaskResult evaluateResult(Fingerprint needle, 
            FingerprintMatcher.Result result) throws SQLException {
        
        String needleName = needle.getName();
        Fingerprint nameMatch = result.getMatchByName();
        List<Fingerprint> matchesByDistance = result.getMatchesByDistance();
        
        if(nameMatch == null) {
            System.out.println(needleName + ": not mached by name");
            return FingerprintMatchTaskResult.NO_MATCH_BY_NAME;
        } else {
            
            int position;
            
            for (position = 0; position < matchesByDistance.size(); position++) {
                if(matchesByDistance.get(position).getName().equals(needleName)) {
                    break;
                }
            }
            
            if(position > 0) {
                System.out.println("--------------------------------------------");
                System.out.println("Needle: ");
                System.out.println(needle);
                
                System.out.println("Match By Name: ");
                System.out.println(nameMatch);
                
                System.out.println("euc. diff: " + frmt.format(needle.euclideanDiff(nameMatch)) + "; in betweeners: " + position);
                
//                System.out.println("closer matches:");
//                for(int j = 0; j < position; j++) {
//                    System.out.println(matchesByDistance.get(j).getName() + " ("
//                            + frmt.format(needle.euclideanDiff(matchesByDistance.get(j))) + ")");
//                }
                
                if(position == matchesByDistance.size()) {
                    System.out.println(needleName + ": not mached by distance.");
                    System.out.println("--------------------------------------------");
                    return FingerprintMatchTaskResult.NO_MATCH_BY_DISTANCE;
                } else {
                    System.out.println(needleName + ": found at position " + (position + 1));
                    System.out.println("--------------------------------------------");
                    return FingerprintMatchTaskResult.NOT_PERFECT;
                } 
            } else {
                double diff = needle.euclideanDiff(nameMatch);
                totalDiffToFirstMatch += diff;
                System.out.println(needleName + ": machted correctly with diff: " + frmt.format(diff) );
                System.out.print("    Diff to next in lines: " );
                
                int counter = 0;
                for (Fingerprint matchByDistance : matchesByDistance) {
                    System.out.print(frmt.format(needle.euclideanDiff(matchByDistance)) + ", ");
                    if(counter++ > 10) break;
                } 
                System.out.print("\n");
                
                return FingerprintMatchTaskResult.OK;
            }
        }
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

    private Map<String, Fingerprint> generatePackagePrints() throws IOException {
        
        Map<String, Fingerprint> packagePrints = new HashMap<>();
            
        for(ClassDef def : classDefs) {
            String obfClassName = transformClassName(def.getType());
            String className =    translateName(obfClassName);
            String packageName =  extractPackageName(className);

            Fingerprint classFingerprint = transformClassDefToFingerprint(def, obfClassName);
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

    private Map<FingerprintMatchTaskResult, Integer> matchPackagePrints(Map<String, Fingerprint> packagePrints) throws SQLException {
        
        Map<FingerprintMatchTaskResult, Integer> stats = new HashMap<>();
        for(FingerprintMatchTaskResult key : FingerprintMatchTaskResult.values()) {
            stats.put(key, 0);
        }
        
        FingerprintMatcher matcher = new FingerprintMatcher(1000);

        List<VectorEntity> haystackEntities = new ArrayList<VectorEntity>(service.findPackages());
        List<Fingerprint>  haystack  = new ArrayList<>(haystackEntities.size());
        for(VectorEntity v : haystackEntities) {
                    haystack.add(new Fingerprint(v));
                }

        
        for(Fingerprint needle : packagePrints.values()) {
            
            if(needle.getName().startsWith("android")) continue;
            if(needle.getName().equals("")) continue;
        
            FingerprintMatcher.Result matches = matcher.matchFingerprints(haystack, needle);
            FingerprintMatchTaskResult result = evaluateResult(needle, matches);
            stats.put(result, stats.get(result) + 1);
        }
        
        return stats;
    }   
    
    private Map<FingerprintMatchTaskResult, Integer> matchPackageOverClassPrints(Map<String, Fingerprint> packagePrints) throws SQLException {
        
        Map<FingerprintMatchTaskResult, Integer> stats = new HashMap<>();
        for(FingerprintMatchTaskResult key : FingerprintMatchTaskResult.values()) {
            stats.put(key, 0);
        }
        
        FingerprintMatcher matcher = new FingerprintMatcher(1000);

        for(Fingerprint packageNeedle : packagePrints.values()) {
            
            if(packageNeedle.getName().startsWith("android")) continue;
            if(packageNeedle.getName().equals("")) continue;
            
            int level = StringUtils.countMatches(packageNeedle.getName(), ".");
            
            List<Fingerprint> haystack = new ArrayList<>();
            
            for (Package pckg : service.findPackagesByDepth(level)) {
                for(VectorEntity v : pckg.getClasses()) {
                    haystack.add(new Fingerprint(v));
                }
            }

            for (Fingerprint needle : packageNeedle.getChildren()) {
                FingerprintMatcher.Result matches = matcher.matchFingerprints(haystack, needle);
                FingerprintMatchTaskResult result = evaluateResult(needle, matches);
                stats.put(result, stats.get(result) + 1);
            }
        }
        
        return stats;
    }
    
    private Map<FingerprintMatchTaskResult, Integer> matchPackagesOnMethodLevel(
            Map<String, Fingerprint> packagePrints) throws SQLException {
    
        Map<FingerprintMatchTaskResult, Integer> stats = new HashMap<>();
        for(FingerprintMatchTaskResult key : FingerprintMatchTaskResult.values()) {
            stats.put(key, 0);
        }
        
        FingerprintMatcher matcher = new FingerprintMatcher(1000);
        
        int count = 0;
        
        for(Fingerprint packageNeedle : packagePrints.values()) {
            
            System.out.println(((float)(count++) / packagePrints.size()) * 100 + "%"); 
            
            if(packageNeedle.getName().startsWith("android")) continue;
            if(packageNeedle.getName().equals("")) continue;
         
            int level = StringUtils.countMatches(packageNeedle.getName(), ".");
            
            List<Fingerprint> haystack = new ArrayList<>();
            
            for (Package pckg : service.findPackagesByDepth(level)) {
                for(VectorEntity clazz : pckg.getClasses()) {
                    Clazz clazz1 = (Clazz) clazz;
                    for (Method m : clazz1.getMethods()) {
                        haystack.add(new Fingerprint(m));
                    }
                }
            }
            
            for(Fingerprint classNeedle : packageNeedle.getChildren()) {
                for(Fingerprint methodNeedle : classNeedle.getChildren()) {
                    FingerprintMatcher.Result matches = matcher.matchFingerprints(haystack, methodNeedle);
                    FingerprintMatchTaskResult result = evaluateResult(methodNeedle, matches);
                    stats.put(result, stats.get(result) + 1);
                }
            }
        }
        
        return stats;
        
    }
    
}
