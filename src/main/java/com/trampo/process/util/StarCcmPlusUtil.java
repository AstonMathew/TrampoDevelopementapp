package com.trampo.process.util;

import java.util.List;

import com.trampo.process.domain.CcmPlus;
import com.trampo.process.domain.StarCcmPrecision;

public class StarCcmPlusUtil {
  
  private static List<CcmPlus> installedCcmPluses;
  private static CcmPlus defaultMixedPrecisionVersion;
  private static CcmPlus defaultDoublePrecisionVersion;
  
  
  public static List<CcmPlus> getInstalledCcmPluses() {
    return installedCcmPluses;
  }
  
  public static void setInstalledCcmPluses(List<CcmPlus> installedCcmPluses) {
    StarCcmPlusUtil.installedCcmPluses = installedCcmPluses;
    for (CcmPlus ccmPlus : installedCcmPluses) {
      if(ccmPlus.getPrecision().equals(StarCcmPrecision.DOUBLE) 
          && (defaultDoublePrecisionVersion == null 
          || ccmPlus.getVersion().compareTo(defaultDoublePrecisionVersion.getVersion()) == 1)){
        defaultDoublePrecisionVersion = ccmPlus;
      }else if(ccmPlus.getPrecision().equals(StarCcmPrecision.MIXED) 
          && (defaultDoublePrecisionVersion == null 
          || ccmPlus.getVersion().compareTo(defaultMixedPrecisionVersion.getVersion()) == 1)){
        defaultMixedPrecisionVersion = ccmPlus;
      }
    }
  }
  
  public static CcmPlus getDefaultMixedPrecisionVersion() {
    return defaultMixedPrecisionVersion;
  }
  
  public static CcmPlus getDefaultDoublePrecisionVersion() {
    return defaultDoublePrecisionVersion;
  }
}
