/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.androidlibid.proto;

import org.androidlibid.proto.ast.ASTToFingerprintTransformer;
import org.androidlibid.proto.ast.ASTClassDefinition;
import java.util.List;
import java.util.concurrent.Callable;
import org.androidlibid.proto.ao.ClassEntityService;
import org.jf.baksmali.baksmaliOptions;
import org.jf.dexlib2.iface.ClassDef;

/**
 *
 * @author Christof Rabensteiner <christof.rabensteiner@gmail.com>
 */
public class StoreClassFingerprintTask implements Callable<Boolean>{
    
    private final ClassDef classDef;
    private final baksmaliOptions options;
    private final ClassEntityService service;

    public StoreClassFingerprintTask(ClassDef classDef, baksmaliOptions options, ClassEntityService service) {
        this.classDef = classDef;
        this.options = options;
        this.service = service;
    }
    
    @Override public Boolean call() throws Exception {
        ASTClassDefinition classDefinition = new ASTClassDefinition(options, classDef);
        List<Node> ast = classDefinition.createAST();
        ASTToFingerprintTransformer ast2fpt = new ASTToFingerprintTransformer();
        
        Fingerprint classFingerprint = new Fingerprint();
        classFingerprint.setName(classDef.getType());
        
        for(Node node : ast) {
            Fingerprint methodFingerprint = ast2fpt.createFingerprint(node);
            classFingerprint.add(methodFingerprint);
        }
        
        if(classFingerprint.euclideanNorm() > 0.0d) {
            service.saveFingerprint(classFingerprint);
        }
        
        return true;
    }
}
