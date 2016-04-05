package org.androidlibid.proto.match;

import java.util.LinkedList;
import java.util.List;
import org.androidlibid.proto.Fingerprint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Christof Rabensteiner <christof.rabensteiner@gmail.com>
 */
public class PackageInclusionCalculator {
    
    private final ClassInclusionCalculator calculator; 
    private final boolean disableRepeatedMatches;
    
    private static final Logger LOGGER = LogManager.getLogger(PackageInclusionCalculator.class.getName());

    public PackageInclusionCalculator(ClassInclusionCalculator calculator, boolean disableRepeatedMatches) {
        this.calculator = calculator;
        this.disableRepeatedMatches = disableRepeatedMatches;
    }
    
    public double computePackageInclusion(List<Fingerprint> superSet, List<Fingerprint> subSet) {
        
        logHeader();
        
        List<Fingerprint> superSetCopy = new LinkedList<>(superSet);
        
        if(subSet.isEmpty()) {
            return 0;
        }
        
        double packageScore = 0;

        for (Fingerprint clazz : subSet) {
            
            if (superSetCopy.isEmpty()) {
                break;
            }
            
            logClassHeader(clazz);
            
            double maxScore = -1;
            Fingerprint maxScoreClazz = null;
            
            for (Fingerprint clazzCandidate : superSetCopy) {
                
                double score = calculator.computeClassInclusion(
                        clazzCandidate.getChildFingerprints(), clazz.getChildFingerprints());
                
                if(Double.isNaN(score) || score < 0) {
                    throw new RuntimeException("Like, srsly?");
                }
                
                if(score > maxScore) {
                    maxScoreClazz = clazzCandidate;
                    maxScore      = score;
                } 
            }
            
            if(maxScore == -1 || maxScoreClazz == null) {
                throw new RuntimeException("fix your code, maniac");
            }
            
            logResult(clazz, maxScoreClazz, maxScore);
            
            packageScore += maxScore;
            
            if(disableRepeatedMatches) {
                if(!superSetCopy.remove(maxScoreClazz)) {
                    throw new RuntimeException("Tried to remove element"
                                + " that is not in the set.");
                }
            }
        }
        
        return packageScore;

    }

    private void logHeader() {
        LOGGER.info("| class | matched | score |"); 
    }

    private void logClassHeader(Fingerprint clazz) {
        
        if(LOGGER.isDebugEnabled()) {
            String clazzName = clazz.getName();
            
            if(clazzName.contains(":")) {
                clazzName = clazzName.substring(clazzName.indexOf(":") + 1);
            }
            
            double perfectScore = calculator.computeClassInclusion(clazz.getChildFingerprints(), clazz.getChildFingerprints()); 
            LOGGER.debug("*** myself: {}, which has {} methods and perfect score : {}.", clazzName, clazz.getChildFingerprints().size(), perfectScore); 
        }
    }

    private void logResult(Fingerprint clazz, Fingerprint maxScoreClazz, double maxScore) {
        
        if(LOGGER.isInfoEnabled()) {
        
            String bestMatchName = maxScoreClazz.getName();
            if(bestMatchName.contains(":")) {
                bestMatchName = bestMatchName.substring(bestMatchName.indexOf(":") + 1);
            }
            
            String clazzName = clazz.getName();
            if(clazzName.contains(":")) {
                clazzName = clazzName.substring(clazzName.indexOf(":") + 1);
            }

            LOGGER.info("| {} | {} | {} |", clazzName, bestMatchName, maxScore); 
        }
    }
}
