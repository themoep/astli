package org.androidlibid.proto.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;


/**
 *
 * @author Christof Rabensteiner <christof.rabensteiner@gmail.com>
 */

public interface Method extends Entity {
   
    public Clazz getClazz();
    public void setClazz(Clazz clazz);
    
    byte[] getVector();
    void setVector(byte[] vector);
    
    @StringLength(StringLength.UNLIMITED)
    String getName();
    void setName(String name);

    @Indexed
    @StringLength(StringLength.UNLIMITED)
    public void setSignature(String signature);
    public String getSignature();
    
}
