package com.trampo.process.util;

import java.util.Set;
import com.trampo.process.domain.CcmPlus;
import com.trampo.process.domain.StarCcmPrecision;

public class StarCcmPlusUtil {
  
  private static Set<CcmPlus> installedCcmPluses;
  private static CcmPlus defaultMixedPrecisionVersion;
  private static CcmPlus defaultDoublePrecisionVersion;
  
  
  public static Set<CcmPlus> getInstalledCcmPluses() {
    return installedCcmPluses;
  }
  
  public static void setInstalledCcmPluses(Set<CcmPlus> installedCcmPluses) {
    StarCcmPlusUtil.installedCcmPluses = installedCcmPluses;
    defaultDoublePrecisionVersion = null;
    defaultMixedPrecisionVersion = null;
    for (CcmPlus ccmPlus : installedCcmPluses) {
      if(ccmPlus.getPrecision().equals(StarCcmPrecision.DOUBLE) 
          && (defaultDoublePrecisionVersion == null 
          || ccmPlus.getVersion().compareTo(defaultDoublePrecisionVersion.getVersion()) > 0)){
        defaultDoublePrecisionVersion = ccmPlus;
      }else if(ccmPlus.getPrecision().equals(StarCcmPrecision.MIXED) 
          && (defaultDoublePrecisionVersion == null 
          || ccmPlus.getVersion().compareTo(defaultMixedPrecisionVersion.getVersion()) > 0)){
        defaultMixedPrecisionVersion = ccmPlus;
      }
    }
  }
  
  public static void remove(CcmPlus ccmPlus) {
    installedCcmPluses.remove(ccmPlus);
    setInstalledCcmPluses(installedCcmPluses);
  }
  
  public static CcmPlus getDefaultMixedPrecisionVersion() {
    return defaultMixedPrecisionVersion;
  }
  
  public static CcmPlus getDefaultDoublePrecisionVersion() {
    return defaultDoublePrecisionVersion;
  }
}
