/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.androidlibid.proto.ao;

import net.java.ao.Entity;
import net.java.ao.OneToMany;

/**
 *
 * @author Christof Rabensteiner <christof.rabensteiner@gmail.com>
 */

public interface LibraryEntity extends Entity {
    
    String getGroupId();
    void setGroupId(String groupID);
    
    String getArtifactId();
    void setArtifactId(String artifactID);

    String getVersion();
    void setVersion(String version);

    byte[] getVector();
    void setVector(byte[] vector);
    
    @OneToMany
    public PackageEntity[] getPackages();
}
