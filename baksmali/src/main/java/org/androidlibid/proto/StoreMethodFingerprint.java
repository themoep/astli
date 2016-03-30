package org.androidlibid.proto;

import com.google.common.collect.Multimap;
import java.util.Map;
import org.androidlibid.proto.ast.Node;
import org.androidlibid.proto.ast.ASTToFingerprintTransformer;
import org.androidlibid.proto.ast.ASTClassDefinition;
import java.util.concurrent.Callable;
import org.androidlibid.proto.ao.Clazz;
import org.androidlibid.proto.ao.EntityService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jf.baksmali.baksmaliOptions;
import org.jf.dexlib2.iface.ClassDef;

/**
 *
 * @author Christof Rabensteiner <christof.rabensteiner@gmail.com>
 */
public class StoreMethodFingerprint implements Callable<Void> {
    
    private final ClassDef classDef;
    private final baksmaliOptions options;
    private final EntityService service;
    
    private static final Logger LOGGER = LogManager.getLogger(StoreMethodFingerprint.class);

    public StoreMethodFingerprint(ClassDef classDef, baksmaliOptions options, EntityService service) {
        this.classDef = classDef;
        this.options = options;
        this.service = service;
    }
    
    @Override public Void call() throws Exception {
        
        ASTClassDefinition classDefinition = new ASTClassDefinition(options, classDef);
        
        Map<String, Node> ast = classDefinition.createASTwithNames();
        
        ASTToFingerprintTransformer ast2fpt = new ASTToFingerprintTransformer();
        
        String className     = SmaliNameConverter.convertTypeFromSmali(classDef.getType());
        String packageName   = SmaliNameConverter.extractPackageNameFromClassName(className);
        String mvnIdentifier = options.mvnIdentifier;

        Fingerprint classFingerprint = new Fingerprint(className);
                
        for(String methodSignature : ast.keySet()) {
            Fingerprint methodFingerprint = ast2fpt.createFingerprint(ast.get(methodSignature));
            
            LOGGER.info("* {}", methodSignature);
            LOGGER.info("** ast" );
            LOGGER.info(ast.get(methodSignature));
            LOGGER.info("** fingerprint" );
            LOGGER.info(methodFingerprint);
            
            if(methodFingerprint.euclideanNorm() > 1.0d) {
                methodFingerprint.setName(className + ":" + methodSignature);
                classFingerprint.add(methodFingerprint);
                classFingerprint.addChild(methodFingerprint);
            }
        }
        
        if(classFingerprint.euclideanNorm() > 0.0d) {
            Clazz clazz = service.saveClass(classFingerprint.getVector().toBinary(), className, packageName, mvnIdentifier);
            
            for(Fingerprint method : classFingerprint.getChildren()) {
                service.saveMethod(method.getVector().toBinary(), method.getName(), method.euclideanNorm(), clazz);
            }
        }
        
        return null;
    }
    
}
