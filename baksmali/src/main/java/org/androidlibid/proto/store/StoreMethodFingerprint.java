package org.androidlibid.proto.store;

import java.util.Map;
import org.androidlibid.proto.ast.Node;
import org.androidlibid.proto.ast.ASTToFingerprintTransformer;
import org.androidlibid.proto.ast.ASTClassBuilder;
import java.util.concurrent.Callable;
import org.androidlibid.proto.Fingerprint;
import org.androidlibid.proto.SmaliNameConverter;
import org.androidlibid.proto.ao.Clazz;
import org.androidlibid.proto.ao.EntityService;
import org.androidlibid.proto.ast.ASTBuilderFactory;
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
        
        ASTBuilderFactory astBuilderFactory = new ASTBuilderFactory(options);
        ASTClassBuilder astClassBuilder = new ASTClassBuilder(
                classDef, astBuilderFactory);
        
        Map<String, Node> ast = astClassBuilder.buildASTs();
        
        ASTToFingerprintTransformer ast2fpt = new ASTToFingerprintTransformer();
        
        String className     = SmaliNameConverter.convertTypeFromSmali(classDef.getType());
        String packageName   = SmaliNameConverter.extractPackageNameFromClassName(className);
        String mvnIdentifier = options.mvnIdentifier;
        
        Fingerprint classFingerprint = new Fingerprint(className);
                
        for(String methodSignature : ast.keySet()) {
            
            Node node = ast.get(methodSignature);
            Fingerprint methodFingerprint = ast2fpt.createFingerprint(node);
            
            logMethod(methodSignature, node, methodFingerprint);
            
            if(methodFingerprint.getLength() > 1.0d) {
                methodFingerprint.setName(className + ":" + methodSignature);
                classFingerprint.sumFeatures(methodFingerprint);
                classFingerprint.addChildFingerprint(methodFingerprint);
            } 
        }
        
        if(classFingerprint.getLength() > 0.0d) {
            Clazz clazz = service.saveClass(classFingerprint.getFeatureVector().toBinary(), className, packageName, mvnIdentifier);
            
            for(Fingerprint method : classFingerprint.getChildFingerprints()) {
                service.saveMethod(method.getFeatureVector().toBinary(), method.getName(), method.getLength(), clazz);
            }
        }
        
        return null;
    }

    private void logMethod(String methodSignature, Node ast, Fingerprint methodFingerprint) {
        
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("* {}", methodSignature);
            LOGGER.debug("** ast" );
            LOGGER.debug(ast);
            LOGGER.debug("** fingerprint" );
            LOGGER.debug(methodFingerprint);
        }
    }
}
