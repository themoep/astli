package org.androidlibid.proto.match;

import org.androidlibid.proto.ao.FingerprintService;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import org.androidlibid.proto.Fingerprint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Christof Rabensteiner <christof.rabensteiner@gmail.com>
 */
public class WriteResultsToLog implements ResultEvaluator  {
    
    private final NumberFormat frmt = new DecimalFormat("#0.00");
    private final FingerprintService service;
    private static final Logger LOGGER = LogManager.getLogger(WriteResultsToLog.class.getName() );
    
    public WriteResultsToLog(FingerprintService service) {
        this.service = service;
    }
    
    @Override
    public MatchingStrategy.Status evaluateResult(Fingerprint needle, 
            FingerprintMatcher.Result result) {
        
        String needleName = needle.getName();
        Fingerprint nameMatch = result.getMatchByName();
        List<Fingerprint> matchesByDistance = result.getMatchesByDistance();
                
        if(nameMatch == null) {
            try { 
                List<Fingerprint> packagesWithTheSameName = service.findPackagesByName(needleName);
                if(packagesWithTheSameName.isEmpty()) {
                    LOGGER.info("{}: not matched by name", needleName);
                    return MatchingStrategy.Status.NO_MATCH_BY_NAME;
                } else {
                    LOGGER.info("{}: not matched by name, but its in the db {} time(s)", new Object[]{needleName, packagesWithTheSameName.size()});
                    return MatchingStrategy.Status.NO_MATCH_BY_NAME_ALTHOUGH_IN_DB;
                }
            } catch (SQLException ex) {
                LOGGER.error(ex.toString(), ex);
                return MatchingStrategy.Status.NO_MATCH_BY_NAME;
            }
        } else {
            
            int position;
            
            for (position = 0; position < matchesByDistance.size(); position++) {
                if(matchesByDistance.get(position).getName().equals(needleName)) {
                    break;
                }
            }
            
            LOGGER.info("* {}{} (max : {}) found at position {}", 
                    position > 0 ? "NEXT " : "",
                    needle.getName(), 
                    frmt.format(needle.getInclusionScore()), 
                    position);
            
            LOGGER.debug("| {} | {} | {} | {} | {} | {} |", 
                "pos",
                "name",
                "incScore",
                "incScoreN", 
                "eucDiff",
                "eucDiffN"
            );
            
            for(int i = 0; i < result.getMatchesByDistance().size(); i++) {
                Fingerprint matchByDistance = result.getMatchesByDistance().get(i);
                
                double maxLength = Math.max(matchByDistance.getLength(), needle.getLength());
                double eucDiffR  = maxLength - matchByDistance.getDistanceToFingerprint(needle);
               
                if(eucDiffR < 0) eucDiffR = 0;
                eucDiffR = eucDiffR  / maxLength;
                
                LOGGER.debug("| {} | {} | {} | {} | {} | {} |", 
                        i, 
                        matchByDistance.getName(),
                        frmt.format(matchByDistance.getInclusionScore()), 
                        frmt.format(matchByDistance.getInclusionScore() / needle.getInclusionScore()), 
                        frmt.format(matchByDistance.getDistanceToFingerprint(needle)), 
                        frmt.format(eucDiffR)
                );
            }
            
            if(position == 0) {
                return MatchingStrategy.Status.OK;
            } else {
                return MatchingStrategy.Status.NOT_PERFECT;
            }
        }
    }
    
}
