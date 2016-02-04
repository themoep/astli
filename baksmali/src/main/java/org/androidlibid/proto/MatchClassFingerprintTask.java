/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.androidlibid.proto;

import java.util.List;
import java.util.concurrent.Callable;
import net.java.ao.EntityManager;
import org.androidlibid.proto.ao.FingerprintService;
import org.jf.baksmali.baksmaliOptions;
import org.jf.dexlib2.iface.ClassDef;

/**
 *
 * @author Christof Rabensteiner <christof.rabensteiner@gmail.com>
 */
public class MatchClassFingerprintTask implements Callable<Boolean>{
    
    private final ClassDef classDef;
    private final baksmaliOptions options;
    private final FingerprintService service;

    public MatchClassFingerprintTask(ClassDef classDef, baksmaliOptions options, FingerprintService service) {
        this.classDef = classDef;
        this.options = options;
        this.service = service;
    }
    
    @Override public Boolean call() throws Exception {
        ASTClassDefinition classDefinition = new ASTClassDefinition(options, classDef);
        List<Node> ast = classDefinition.createAST();
        
        ASTToFingerprintTransformer ast2fpt = new ASTToFingerprintTransformer();
        
        Fingerprint classFingerprint = new Fingerprint();
        
        for(Node node : ast) {
            Fingerprint methodFingerprint = ast2fpt.createFingerprint(node);
            classFingerprint.add(methodFingerprint);
        }
        
        FingerprintMatcher matcher = new FingerprintMatcher(service);
        
        System.out.println("Matches for " + classDef.getType());
        
        for(Fingerprint match : matcher.matchFingerprints(classFingerprint)) {
            System.out.println(match);
            System.out.println("Difference: " + match.euclideanDiff(classFingerprint));
        }
        
        return true;
    }
}
