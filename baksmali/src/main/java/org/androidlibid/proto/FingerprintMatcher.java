package org.androidlibid.proto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.androidlibid.proto.ao.FingerprintEntity;
import org.androidlibid.proto.ao.FingerprintService;

/**
 *
 * @author Christof Rabensteiner <christof.rabensteiner@gmail.com>
 */
public class FingerprintMatcher {


    FingerprintService service; 
    private double diffThreshold = 0.1d; 

    public FingerprintMatcher(FingerprintService service) {
        this.service = service;
    }
    
    public List<Fingerprint> matchFingerprints(Fingerprint needle) {
        
        List<Fingerprint> matches = new ArrayList<>();
        
        for(FingerprintEntity candidateEntity : service.getFingerprintEntities()) {
            
            Fingerprint candidate = new Fingerprint(candidateEntity);
            if(needle.euclideanDiff(candidate) < diffThreshold) {
                matches.add(candidate);
            }  
            
        }
        
        Collections.sort(matches, new Comparator<Fingerprint>() {
            @Override
            public int compare(Fingerprint print, Fingerprint other) {
                return (print.getVector().euclideanNorm() - other.getVector().euclideanNorm()) > 0 ? 1 : -1;
            }
    
        }); 
        
        return matches; 
    }
}
